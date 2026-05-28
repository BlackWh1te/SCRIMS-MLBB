-- Scrim Application Notifications and LFG Avatar Fix
-- Date: 2026-05-31

-- 1. Add avatar_url to lfg_posts
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS avatar_url TEXT;

-- 2. Update valid_application_status constraint to match app enums
ALTER TABLE scrim_applications DROP CONSTRAINT IF EXISTS valid_application_status;
ALTER TABLE scrim_applications ADD CONSTRAINT valid_application_status 
    CHECK (status IN ('Pending', 'APPROVED', 'REJECTED', 'CANCELLED', 'Accepted'));

-- 3. Scrim Application Notification Trigger Function
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
            INSERT INTO app_notifications (user_id, type, title, body, data)
            VALUES (
                v_host_id,
                'SCRIM_APPLICATION_NEW',
                'New Scrim Application',
                format('Team %s applied to your scrim!', v_applicant_team_name),
                jsonb_build_object('scrim_id', NEW.scrim_id::TEXT, 'applicant_team_id', NEW.applicant_team_id::TEXT)
            );
        END IF;
    ELSIF (TG_OP = 'UPDATE') THEN
        -- Only notify if status changed from Pending
        IF (OLD.status = 'Pending' AND NEW.status != 'Pending') THEN
            IF (NEW.status = 'APPROVED' OR NEW.status = 'Accepted') THEN
                -- Notify Applicant about approval
                IF v_applicant_leader_id IS NOT NULL THEN
                    INSERT INTO app_notifications (user_id, type, title, body, data)
                    VALUES (
                        v_applicant_leader_id,
                        'SCRIM_APPLICATION_APPROVED',
                        'Scrim Approved!',
                        format('Your application to %s''s scrim was approved!', v_host_team_name),
                        jsonb_build_object('scrim_id', NEW.scrim_id::TEXT, 'host_team_id', (SELECT team_id FROM scrims WHERE id = NEW.scrim_id)::TEXT)
                    );
                END IF;
            ELSIF (NEW.status = 'REJECTED') THEN
                -- Notify Applicant about rejection
                IF v_applicant_leader_id IS NOT NULL THEN
                    INSERT INTO app_notifications (user_id, type, title, body, data)
                    VALUES (
                        v_applicant_leader_id,
                        'SCRIM_APPLICATION_REJECTED',
                        'Scrim Declined',
                        format('Your application to %s''s scrim was declined.', v_host_team_name),
                        jsonb_build_object('scrim_id', NEW.scrim_id::TEXT)
                    );
                END IF;
            END IF;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Create Trigger
DROP TRIGGER IF EXISTS on_scrim_application_change ON scrim_applications;
CREATE TRIGGER on_scrim_application_change
    AFTER INSERT OR UPDATE ON scrim_applications
    FOR EACH ROW EXECUTE FUNCTION public.handle_scrim_application_notification();
