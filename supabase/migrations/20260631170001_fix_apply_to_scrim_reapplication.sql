-- Fix apply_to_scrim: allow re-application after rejection/cancellation
-- The UNIQUE(scrim_id, applicant_team_id) constraint prevents inserting a new row
-- when a team previously applied. Instead of INSERT, we UPDATE existing Rejected/Cancelled
-- rows back to Pending.

CREATE OR REPLACE FUNCTION public.apply_to_scrim(
    p_scrim_id UUID,
    p_applicant_team_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_scrim RECORD;
    v_applicant_team RECORD;
    v_existing_app RECORD;
    v_result JSON;
BEGIN
    -- Lock the scrim row
    SELECT * INTO v_scrim
    FROM scrims
    WHERE id = p_scrim_id
    FOR UPDATE;

    IF v_scrim IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Scrim not found');
    END IF;

    -- Verify scrim is OPEN
    IF v_scrim.status != 'Open' THEN
        RETURN json_build_object('success', false, 'error', 'Scrim is no longer open for applications');
    END IF;

    -- Cannot apply to own scrim
    IF v_scrim.team_id = p_applicant_team_id THEN
        RETURN json_build_object('success', false, 'error', 'You cannot apply to your own scrim');
    END IF;

    -- Verify applicant team exists and caller is leader
    SELECT * INTO v_applicant_team FROM teams WHERE id = p_applicant_team_id;
    IF v_applicant_team IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Applicant team not found');
    END IF;
    IF v_applicant_team.leader_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only the team leader can apply');
    END IF;

    -- Check for existing application (any status)
    SELECT * INTO v_existing_app
    FROM scrim_applications
    WHERE scrim_id = p_scrim_id
      AND applicant_team_id = p_applicant_team_id;

    IF v_existing_app IS NOT NULL THEN
        IF v_existing_app.status = 'Pending' THEN
            RETURN json_build_object('success', false, 'error', 'Your team already has a pending application for this scrim');
        ELSIF v_existing_app.status IN ('Rejected', 'Cancelled') THEN
            -- Re-activate the old application instead of inserting (avoids unique constraint)
            UPDATE scrim_applications
            SET status = 'Pending',
                applied_at = TIMEZONE('utc', NOW())
            WHERE id = v_existing_app.id;
        ELSE
            RETURN json_build_object('success', false, 'error', 'Application already handled for this scrim');
        END IF;
    ELSE
        -- No existing application — create new
        INSERT INTO scrim_applications (scrim_id, applicant_team_id, status)
        VALUES (p_scrim_id, p_applicant_team_id, 'Pending');
    END IF;

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;
