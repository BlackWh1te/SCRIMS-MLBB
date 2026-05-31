-- Fix approval flow: 'Filled' -> 'Accepted', row locking, reject-other-apps instead of delete
-- Fix transition_to_ready_check: 'Filled' -> 'Accepted'

-- 1. Fix approve_scrim_application: use 'Accepted', add FOR UPDATE, reject others instead of delete
CREATE OR REPLACE FUNCTION public.approve_scrim_application(
    p_application_id UUID,
    p_conversation_id UUID DEFAULT NULL
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_scrim_id UUID;
    v_scrim_team_id UUID;
    v_applicant_team_id UUID;
    v_scrim_status TEXT;
    v_app_status TEXT;
    v_host_leader_id UUID;
    v_host_team_name TEXT;
    v_rejected_leader_id UUID;
    v_other_app RECORD;
    v_result JSON;
BEGIN
    -- Lock the application row (prevents race with cancel/reject)
    SELECT sa.scrim_id, sa.applicant_team_id, sa.status
    INTO v_scrim_id, v_applicant_team_id, v_app_status
    FROM scrim_applications sa
    WHERE sa.id = p_application_id
    FOR UPDATE;

    IF v_scrim_id IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Application not found');
    END IF;

    IF v_app_status != 'Pending' THEN
        RETURN json_build_object('success', false, 'error', 'Application is no longer pending');
    END IF;

    -- Lock the scrim row (prevents concurrent approvals of different apps)
    SELECT s.team_id, s.status
    INTO v_scrim_team_id, v_scrim_status
    FROM scrims s
    WHERE s.id = v_scrim_id
    FOR UPDATE;

    IF v_scrim_status != 'Open' THEN
        RETURN json_build_object('success', false, 'error', 'Scrim is no longer open for applications');
    END IF;

    -- Verify caller is the host team leader
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim_team_id;
    IF v_host_leader_id IS NULL OR v_host_leader_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only the host team leader can approve applications');
    END IF;

    -- Fetch host team name for notifications
    SELECT name INTO v_host_team_name FROM teams WHERE id = v_scrim_team_id;

    -- Reject (not delete) all other pending applications for this scrim
    FOR v_other_app IN (
        SELECT id, applicant_team_id
        FROM scrim_applications
        WHERE scrim_id = v_scrim_id
          AND status = 'Pending'
          AND id != p_application_id
        FOR UPDATE
    ) LOOP
        -- Notify the leader of the rejected team
        SELECT leader_id INTO v_rejected_leader_id
        FROM teams WHERE id = v_other_app.applicant_team_id;

        IF v_rejected_leader_id IS NOT NULL THEN
            INSERT INTO app_notifications (user_id, type, title, message, body, action_id, data)
            VALUES (
                v_rejected_leader_id,
                'SCRIM_APPLICATION_REJECTED',
                'Scrim Declined',
                format('Team %s found an opponent for their scrim. Your application was declined.', v_host_team_name),
                format('Team %s found an opponent for their scrim. Your application was declined.', v_host_team_name),
                v_scrim_id::TEXT,
                jsonb_build_object('scrim_id', v_scrim_id::TEXT)
            );
        END IF;

        -- Mark as Rejected (preserves history, triggers rejection notification)
        UPDATE scrim_applications
        SET status = 'Rejected'
        WHERE id = v_other_app.id;
    END LOOP;

    -- Approve the selected application
    UPDATE scrim_applications
    SET status = 'Accepted'
    WHERE id = p_application_id;

    -- Lock the scrim: set to Accepted (matches valid_scrim_status constraint) with opponent and conversation
    UPDATE scrims
    SET status = 'Accepted',
        opponent_team_id = v_applicant_team_id,
        opponent_team_name = (SELECT name FROM teams WHERE id = v_applicant_team_id),
        conversation_id = p_conversation_id
    WHERE id = v_scrim_id;

    -- Return the updated scrim as JSON
    SELECT to_jsonb(s) INTO v_result
    FROM scrims s
    WHERE s.id = v_scrim_id;

    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;

-- 2. Fix transition_to_ready_check: check for 'Accepted' instead of 'Filled'
CREATE OR REPLACE FUNCTION public.transition_to_ready_check(
    p_scrim_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_scrim RECORD;
    v_host_leader_id UUID;
    v_now TIMESTAMP WITH TIME ZONE;
    v_scheduled TIMESTAMP WITH TIME ZONE;
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

    -- Verify caller is host leader
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    IF v_host_leader_id IS NULL OR v_host_leader_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only the host team leader can start ready check');
    END IF;

    -- Verify status is Accepted (was 'Filled' — broke after valid_scrim_status constraint added)
    IF v_scrim.status != 'Accepted' THEN
        RETURN json_build_object('success', false, 'error', 'Scrim must be filled before ready check');
    END IF;

    -- Verify opponent exists
    IF v_scrim.opponent_team_id IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'No opponent has been set');
    END IF;

    -- Verify scheduled time has been reached (or within 5 minutes)
    v_now := TIMEZONE('utc', NOW());
    v_scheduled := v_scrim.scheduled_date + v_scrim.scheduled_time;
    IF v_scheduled > v_now + INTERVAL '5 minutes' THEN
        RETURN json_build_object('success', false, 'error', 'Ready check can only start within 5 minutes of scheduled time');
    END IF;

    -- Atomically transition to ready check and reset flags
    UPDATE scrims
    SET status = 'Ready',
        team_a_ready = FALSE,
        team_b_ready = FALSE,
        team_a_ready_at = NULL,
        team_b_ready_at = NULL
    WHERE id = p_scrim_id;

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;
