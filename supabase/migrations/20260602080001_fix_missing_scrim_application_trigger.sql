-- Fix missing scrim application notification trigger
-- Root cause: the on_scrim_application_change trigger was either never applied
-- or dropped during a schema migration, causing apply/approve/reject/cancel
-- actions to silently skip inserting app_notifications.
--
-- This migration:
-- 1. Recreates handle_scrim_application_notification with all status transitions
-- 2. Recreates the trigger
-- 3. Updates apply_to_scrim to insert a host notification directly (fallback)
-- 4. Updates approve_scrim_application to insert an approval notification directly

-- ═══════════════════════════════════════════════════════════════════════════════
-- 1. Trigger function: covers INSERT + all UPDATE transitions
-- ═══════════════════════════════════════════════════════════════════════════════
CREATE OR REPLACE FUNCTION public.handle_scrim_application_notification()
RETURNS TRIGGER AS $$
DECLARE
    v_host_id UUID;
    v_host_team_name TEXT;
    v_applicant_leader_id UUID;
    v_applicant_team_name TEXT;
BEGIN
    -- Get Host Info (Team leader of the scrim)
    SELECT t.leader_id, t.name INTO v_host_id, v_host_team_name
    FROM scrims s
    JOIN teams t ON s.team_id = t.id
    WHERE s.id = NEW.scrim_id;

    -- Get Applicant Info (Leader of the applying team)
    SELECT t.leader_id, t.name INTO v_applicant_leader_id, v_applicant_team_name
    FROM teams t
    WHERE t.id = NEW.applicant_team_id;

    IF (TG_OP = 'INSERT') THEN
        -- Notify Host about new application
        IF v_host_id IS NOT NULL THEN
            INSERT INTO app_notifications (user_id, type, title, message, body, action_id, data)
            VALUES (
                v_host_id,
                'SCRIM_APPLICATION_NEW',
                'New Scrim Application',
                format('Team %s applied to your scrim!', v_applicant_team_name),
                format('Team %s applied to your scrim!', v_applicant_team_name),
                NEW.scrim_id::TEXT,
                jsonb_build_object('scrim_id', NEW.scrim_id::TEXT, 'applicant_team_id', NEW.applicant_team_id::TEXT)
            );
        END IF;

    ELSIF (TG_OP = 'UPDATE') THEN
        -- Re-activation: Rejected/Cancelled -> Pending (re-application)
        IF (OLD.status IN ('Rejected', 'Cancelled') AND NEW.status = 'Pending') THEN
            IF v_host_id IS NOT NULL THEN
                INSERT INTO app_notifications (user_id, type, title, message, body, action_id, data)
                VALUES (
                    v_host_id,
                    'SCRIM_APPLICATION_NEW',
                    'New Scrim Application',
                    format('Team %s re-applied to your scrim!', v_applicant_team_name),
                    format('Team %s re-applied to your scrim!', v_applicant_team_name),
                    NEW.scrim_id::TEXT,
                    jsonb_build_object('scrim_id', NEW.scrim_id::TEXT, 'applicant_team_id', NEW.applicant_team_id::TEXT)
                );
            END IF;

        -- Approval: Pending -> Accepted
        ELSIF (OLD.status = 'Pending' AND NEW.status = 'Accepted') THEN
            IF v_applicant_leader_id IS NOT NULL THEN
                INSERT INTO app_notifications (user_id, type, title, message, body, action_id, data)
                VALUES (
                    v_applicant_leader_id,
                    'SCRIM_APPLICATION_APPROVED',
                    'Scrim Approved!',
                    format('Your application to %s''s scrim was approved!', v_host_team_name),
                    format('Your application to %s''s scrim was approved!', v_host_team_name),
                    NEW.scrim_id::TEXT,
                    jsonb_build_object('scrim_id', NEW.scrim_id::TEXT, 'host_team_id', (SELECT team_id FROM scrims WHERE id = NEW.scrim_id)::TEXT)
                );
            END IF;

        -- Rejection: Pending -> Rejected
        ELSIF (OLD.status = 'Pending' AND NEW.status = 'Rejected') THEN
            IF v_applicant_leader_id IS NOT NULL THEN
                INSERT INTO app_notifications (user_id, type, title, message, body, action_id, data)
                VALUES (
                    v_applicant_leader_id,
                    'SCRIM_APPLICATION_REJECTED',
                    'Scrim Declined',
                    format('Your application to %s''s scrim was declined.', v_host_team_name),
                    format('Your application to %s''s scrim was declined.', v_host_team_name),
                    NEW.scrim_id::TEXT,
                    jsonb_build_object('scrim_id', NEW.scrim_id::TEXT)
                );
            END IF;

        -- Cancellation: Pending -> Cancelled
        ELSIF (OLD.status = 'Pending' AND NEW.status = 'Cancelled') THEN
            IF v_host_id IS NOT NULL THEN
                INSERT INTO app_notifications (user_id, type, title, message, body, action_id, data)
                VALUES (
                    v_host_id,
                    'SCRIM_APPLICATION_CANCELLED',
                    'Application Cancelled',
                    format('Team %s cancelled their application to your scrim.', v_applicant_team_name),
                    format('Team %s cancelled their application to your scrim.', v_applicant_team_name),
                    NEW.scrim_id::TEXT,
                    jsonb_build_object('scrim_id', NEW.scrim_id::TEXT, 'applicant_team_id', NEW.applicant_team_id::TEXT)
                );
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 2. Recreate trigger (idempotent)
-- ═══════════════════════════════════════════════════════════════════════════════
DROP TRIGGER IF EXISTS on_scrim_application_change ON scrim_applications;
CREATE TRIGGER on_scrim_application_change
    AFTER INSERT OR UPDATE ON scrim_applications
    FOR EACH ROW EXECUTE FUNCTION public.handle_scrim_application_notification();

-- ═══════════════════════════════════════════════════════════════════════════════
-- 3. Defensive: make apply_to_scrim insert host notification directly
--    (belt-and-suspenders in case trigger is ever dropped again)
-- ═══════════════════════════════════════════════════════════════════════════════
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
    v_host_leader_id UUID;
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

    -- Defensive: notify host leader directly (trigger may be missing)
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    IF v_host_leader_id IS NOT NULL THEN
        INSERT INTO app_notifications (user_id, type, title, message, body, action_id, data)
        VALUES (
            v_host_leader_id,
            'SCRIM_APPLICATION_NEW',
            'New Scrim Application',
            format('Team %s applied to your scrim!', v_applicant_team.name),
            format('Team %s applied to your scrim!', v_applicant_team.name),
            p_scrim_id::TEXT,
            jsonb_build_object('scrim_id', p_scrim_id::TEXT, 'applicant_team_id', p_applicant_team_id::TEXT)
        );
    END IF;

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;

-- ═══════════════════════════════════════════════════════════════════════════════
-- 4. Defensive: make approve_scrim_application insert approval notification directly
-- ═══════════════════════════════════════════════════════════════════════════════
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
    v_applicant_leader_id UUID;
    v_rejected_leader_id UUID;
    v_other_app RECORD;
    v_result JSON;
BEGIN
    -- Fetch the application and its scrim
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

    -- Fetch scrim status and host team
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

    -- Fetch applicant leader for approval notification
    SELECT leader_id INTO v_applicant_leader_id FROM teams WHERE id = v_applicant_team_id;

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

    -- Defensive: notify applicant leader directly about approval
    IF v_applicant_leader_id IS NOT NULL THEN
        INSERT INTO app_notifications (user_id, type, title, message, body, action_id, data)
        VALUES (
            v_applicant_leader_id,
            'SCRIM_APPLICATION_APPROVED',
            'Scrim Approved!',
            format('Your application to %s''s scrim was approved!', v_host_team_name),
            format('Your application to %s''s scrim was approved!', v_host_team_name),
            v_scrim_id::TEXT,
            jsonb_build_object('scrim_id', v_scrim_id::TEXT, 'host_team_id', v_scrim_team_id::TEXT)
        );
    END IF;

    -- Lock the scrim: set to Accepted with opponent and conversation
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
