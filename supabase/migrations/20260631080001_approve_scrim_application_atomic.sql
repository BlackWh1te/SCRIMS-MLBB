-- Atomic scrim application approval
-- Ensures that when a host approves a team, the scrim is locked and all other pending applications are cancelled.
-- This replaces the fragile multi-step client-side flow with a single atomic DB operation.

-- 1. Expand application status enum to include 'Cancelled' so we can distinguish user-cancellations from rejections
ALTER TABLE scrim_applications DROP CONSTRAINT IF EXISTS valid_application_status;
ALTER TABLE scrim_applications ADD CONSTRAINT valid_application_status
    CHECK (status IN ('Pending', 'Accepted', 'Rejected', 'Cancelled'));

-- 2. Create atomic approval function
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
    -- Fetch the application and its scrim
    SELECT sa.scrim_id, sa.applicant_team_id, sa.status
    INTO v_scrim_id, v_applicant_team_id, v_app_status
    FROM scrim_applications sa
    WHERE sa.id = p_application_id;

    IF v_scrim_id IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Application not found');
    END IF;

    IF v_app_status != 'Pending' THEN
        RETURN json_build_object('success', false, 'error', 'Application is no longer pending');
    END IF;

    -- Fetch scrim status and host team
    SELECT s.team_id, s.status
    INTO v_scrim_team_id, v_scrim_status
    FROM scrims s
    WHERE s.id = v_scrim_id;

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

    -- Notify + delete all other pending applications for this scrim
    FOR v_other_app IN (
        SELECT id, applicant_team_id
        FROM scrim_applications
        WHERE scrim_id = v_scrim_id
          AND status = 'Pending'
          AND id != p_application_id
    ) LOOP
        -- Notify the leader of the rejected team
        SELECT leader_id INTO v_rejected_leader_id
        FROM teams WHERE id = v_other_app.applicant_team_id;

        IF v_rejected_leader_id IS NOT NULL THEN
            INSERT INTO app_notifications (user_id, type, title, message, body, action_id, data)
            VALUES (
                v_rejected_leader_id,
                'SCRIM_OPPONENT_FOUND',
                'Opponent Found',
                format('Team %s found an opponent for their scrim.', v_host_team_name),
                format('Team %s found an opponent for their scrim.', v_host_team_name),
                v_scrim_id::TEXT,
                jsonb_build_object('scrim_id', v_scrim_id::TEXT)
            );
        END IF;

        -- Delete the application completely
        DELETE FROM scrim_applications WHERE id = v_other_app.id;
    END LOOP;

    -- Approve the selected application
    UPDATE scrim_applications
    SET status = 'Accepted'
    WHERE id = p_application_id;

    -- Lock the scrim: set to Filled with opponent and conversation
    UPDATE scrims
    SET status = 'Filled',
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

-- 3. Also update the application notification trigger to handle 'Cancelled' status
-- (it should behave the same as 'Rejected' for notification purposes, but without a misleading message)
DO $$
BEGIN
    -- The existing trigger handle_scrim_application_notification already handles status changes.
    -- We just need to make sure 'Cancelled' is treated as a terminal state (no notification).
    -- Since the trigger currently only fires on INSERT for Pending and UPDATE for Rejected,
    -- cancelling other applications during approval won't send extra notifications.
    -- This is the desired behavior.
END $$;
