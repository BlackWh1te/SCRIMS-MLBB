-- ──────────────────────────────────────────────────────────────────────────────
-- Migration: team_invite_notifications
-- Date: 2026-06-28
--
-- Problem
-- -------
-- Team invites are recorded by inserting a row into team_members with
-- role = 'INVITED'.  No trigger existed to create an app_notifications row
-- for the invited user, so their notification badge never lit up and
-- NotificationType.TEAM_INVITE was never delivered from the DB.
--
-- Fix
-- ---
-- Add an AFTER INSERT trigger on team_members that fires whenever a row is
-- inserted with role = 'INVITED', and inserts a TEAM_INVITE notification for
-- that user.  Writes both (message, action_id) and (body) columns so it works
-- regardless of whether the app is running schema.sql or the older migration
-- column layout.
--
-- Also correct the RLS policies on app_notifications so that:
--   - Users can INSERT rows only for themselves (needed for client-side
--     createNotification() calls).
--   - Users can UPDATE only their own rows (mark as read / delete).
-- ──────────────────────────────────────────────────────────────────────────────

-- ── 1. Create the trigger function ───────────────────────────────────────────

CREATE OR REPLACE FUNCTION notify_on_team_invite()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_team_name TEXT;
BEGIN
    -- Only fire for INVITED role inserts
    IF NEW.role <> 'INVITED' THEN
        RETURN NEW;
    END IF;

    -- Lookup team name (best-effort; fall back gracefully if NULL)
    SELECT name INTO v_team_name
    FROM teams
    WHERE id = NEW.team_id;

    -- Insert the notification for the invited user.
    -- Dual-write message+body so both old and new app versions display it.
    INSERT INTO app_notifications
        (user_id, type, title, message, body, action_id)
    VALUES (
        NEW.user_id,
        'TEAM_INVITE',
        'Team Invitation',
        format('You have been invited to join %s!', COALESCE(v_team_name, 'a team')),
        format('You have been invited to join %s!', COALESCE(v_team_name, 'a team')),
        NEW.team_id::TEXT
    )
    -- Silently skip if a duplicate somehow arrives (same user + team_id within
    -- the same second, e.g. from a concurrent insert).
    ON CONFLICT DO NOTHING;

    RETURN NEW;
END;
$$;

-- ── 2. Attach trigger to team_members ────────────────────────────────────────

DROP TRIGGER IF EXISTS trg_team_invite_notification ON team_members;

CREATE TRIGGER trg_team_invite_notification
    AFTER INSERT ON team_members
    FOR EACH ROW
    EXECUTE FUNCTION notify_on_team_invite();

-- ── 3. Ensure RLS policies exist for INSERT / UPDATE on app_notifications ────
-- (SELECT policy already exists from schema.sql)

-- Allow users to create notifications for themselves (client-side calls)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'app_notifications'
          AND policyname = 'Users can insert their own notifications'
    ) THEN
        EXECUTE $policy$
            CREATE POLICY "Users can insert their own notifications"
                ON app_notifications
                FOR INSERT
                WITH CHECK (user_id = auth.uid());
        $policy$;
    END IF;
END $$;

-- Allow users to update (mark read / soft-delete) their own notifications
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'app_notifications'
          AND policyname = 'Users can update their own notifications'
    ) THEN
        EXECUTE $policy$
            CREATE POLICY "Users can update their own notifications"
                ON app_notifications
                FOR UPDATE
                USING (user_id = auth.uid());
        $policy$;
    END IF;
END $$;

-- Allow users to delete their own notifications
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'app_notifications'
          AND policyname = 'Users can delete their own notifications'
    ) THEN
        EXECUTE $policy$
            CREATE POLICY "Users can delete their own notifications"
                ON app_notifications
                FOR DELETE
                USING (user_id = auth.uid());
        $policy$;
    END IF;
END $$;
