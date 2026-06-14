-- Remaining scrim atomic fixes
-- Covers reject, cancel, apply, auto-cancel, and per-scrim screenshot upload

-- 1. Atomic reject application (prevents rejecting already-approved or already-rejected apps)
CREATE OR REPLACE FUNCTION public.reject_scrim_application(
    p_application_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_app RECORD;
    v_scrim RECORD;
    v_host_leader_id UUID;
    v_result JSON;
BEGIN
    -- Lock the application row
    SELECT * INTO v_app
    FROM scrim_applications
    WHERE id = p_application_id
    FOR UPDATE;

    IF v_app IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Application not found');
    END IF;

    -- Lock the scrim row
    SELECT * INTO v_scrim
    FROM scrims
    WHERE id = v_app.scrim_id
    FOR UPDATE;

    IF v_scrim IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Scrim not found');
    END IF;

    -- Verify caller is host team leader
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    IF v_host_leader_id IS NULL OR v_host_leader_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only the host team leader can reject applications');
    END IF;

    -- Verify application is still pending
    IF v_app.status != 'Pending' THEN
        RETURN json_build_object('success', false, 'error', 'Application is no longer pending');
    END IF;

    -- Atomically reject
    UPDATE scrim_applications
    SET status = 'Rejected'
    WHERE id = p_application_id;

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = v_scrim.id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;

-- 2. Atomic cancel application (prevents cancelling already-handled apps)
CREATE OR REPLACE FUNCTION public.cancel_scrim_application(
    p_application_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_app RECORD;
    v_applicant_team RECORD;
    v_result JSON;
BEGIN
    -- Lock the application row
    SELECT * INTO v_app
    FROM scrim_applications
    WHERE id = p_application_id
    FOR UPDATE;

    IF v_app IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Application not found');
    END IF;

    -- Verify caller is the applicant team leader
    SELECT * INTO v_applicant_team FROM teams WHERE id = v_app.applicant_team_id;
    IF v_applicant_team IS NULL OR v_applicant_team.leader_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only the applicant team leader can cancel this application');
    END IF;

    -- Verify application is still pending
    IF v_app.status != 'Pending' THEN
        RETURN json_build_object('success', false, 'error', 'Application is no longer pending');
    END IF;

    -- Atomically cancel (map to Rejected since DB enum has only 3 base values, but we now have Cancelled)
    UPDATE scrim_applications
    SET status = 'Cancelled'
    WHERE id = p_application_id;

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = v_app.scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;

-- 3. Atomic apply to scrim (prevents applying to already-filled scrims)
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
    v_existing_app INTEGER;
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

    -- Check for duplicate pending application
    SELECT COUNT(*) INTO v_existing_app
    FROM scrim_applications
    WHERE scrim_id = p_scrim_id
      AND applicant_team_id = p_applicant_team_id
      AND status = 'Pending';

    IF v_existing_app > 0 THEN
        RETURN json_build_object('success', false, 'error', 'Your team already has a pending application for this scrim');
    END IF;

    -- Create application
    INSERT INTO scrim_applications (scrim_id, applicant_team_id, status)
    VALUES (p_scrim_id, p_applicant_team_id, 'Pending');

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;

-- 4. Atomic auto-cancel scrim (prevents double-cancel race)
CREATE OR REPLACE FUNCTION public.auto_cancel_scrim(
    p_scrim_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_scrim RECORD;
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

    -- Do not overwrite already-completed or already-cancelled
    IF v_scrim.status IN ('Completed', 'Cancelled') THEN
        RETURN json_build_object('success', true, 'message', 'Already in terminal state');
    END IF;

    -- Atomically cancel
    UPDATE scrims
    SET status = 'Cancelled',
        cancellation_reason = 'Auto-cancelled: no result submitted within the 2-hour deadline',
        cancelled_by = NULL
    WHERE id = p_scrim_id;

    -- Delete any remaining pending applications for this scrim
    DELETE FROM scrim_applications
    WHERE scrim_id = p_scrim_id AND status = 'Pending';

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;

-- 5. Atomic per-scrim screenshot upload
CREATE OR REPLACE FUNCTION public.upload_scrim_screenshot(
    p_scrim_id UUID,
    p_team_id UUID,
    p_screenshot_url TEXT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_scrim RECORD;
    v_host_leader_id UUID;
    v_opponent_leader_id UUID;
    v_is_team_a BOOLEAN;
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

    -- Verify scrim is in progress
    IF v_scrim.status != 'In Progress' THEN
        RETURN json_build_object('success', false, 'error', 'Scrim is not in progress');
    END IF;

    -- Verify team is a participant
    IF v_scrim.team_id != p_team_id AND v_scrim.opponent_team_id != p_team_id THEN
        RETURN json_build_object('success', false, 'error', 'Team is not a participant');
    END IF;

    -- Verify caller is a team leader
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    SELECT leader_id INTO v_opponent_leader_id FROM teams WHERE id = v_scrim.opponent_team_id;
    IF auth.uid() NOT IN (v_host_leader_id, v_opponent_leader_id) THEN
        RETURN json_build_object('success', false, 'error', 'Only team leaders can upload screenshots');
    END IF;

    v_is_team_a := (v_scrim.team_id = p_team_id);

    -- Update screenshot atomically
    IF v_is_team_a THEN
        UPDATE scrims
        SET team_a_screenshot_url = p_screenshot_url,
            team_a_screenshot_uploaded_at = TIMEZONE('utc', NOW())
        WHERE id = p_scrim_id;
    ELSE
        UPDATE scrims
        SET team_b_screenshot_url = p_screenshot_url,
            team_b_screenshot_uploaded_at = TIMEZONE('utc', NOW())
        WHERE id = p_scrim_id;
    END IF;

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;
