-- ============================================================
-- MLBB Scrim Host — Backend Sync Migration
-- Run this in Supabase SQL Editor to sync DB with app models
-- ============================================================

-- ─────────────────────────────────────────
-- P1-4: match_results — admin-review columns
-- ─────────────────────────────────────────
ALTER TABLE match_results
    ADD COLUMN IF NOT EXISTS admin_verdict        TEXT,         -- AdminVerdict enum string
    ADD COLUMN IF NOT EXISTS punished_team_id     UUID REFERENCES teams(id),
    ADD COLUMN IF NOT EXISTS punishment_duration_hours INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reviewed_by_admin_id UUID,         -- auth.users reference
    ADD COLUMN IF NOT EXISTS reviewed_at          TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS no_show_team_id      UUID REFERENCES teams(id),
    ADD COLUMN IF NOT EXISTS match_actually_played BOOLEAN DEFAULT FALSE;

-- ─────────────────────────────────────────
-- P1-5: teams — stats and reputation columns
-- ─────────────────────────────────────────
ALTER TABLE teams
    ADD COLUMN IF NOT EXISTS reputation          NUMERIC(3,1) DEFAULT 5.0,  -- 1.0–5.0 star rating
    ADD COLUMN IF NOT EXISTS can_post_scrims_until TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()), -- NULL = not banned
    ADD COLUMN IF NOT EXISTS total_scrims        INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS completed_scrims    INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS no_shows            INT DEFAULT 0;

-- ─────────────────────────────────────────
-- P1-6: messages — senderName, isRead, type
-- ─────────────────────────────────────────
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS sender_name TEXT,
    ADD COLUMN IF NOT EXISTS is_read     BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS type        TEXT DEFAULT 'TEXT'; -- TEXT | SYSTEM | APPLY

-- ─────────────────────────────────────────
-- P1-7: scrims — gameMode, region, skillLevel, maxPlayers, currentPlayers
-- ─────────────────────────────────────────
ALTER TABLE scrims
    ADD COLUMN IF NOT EXISTS game_mode      TEXT DEFAULT 'RANKED',       -- GameMode enum
    ADD COLUMN IF NOT EXISTS region         TEXT DEFAULT 'EU',            -- Region enum
    ADD COLUMN IF NOT EXISTS skill_level    TEXT DEFAULT 'ALL',           -- SkillLevel enum
    ADD COLUMN IF NOT EXISTS max_players    INT  DEFAULT 5,
    ADD COLUMN IF NOT EXISTS current_players INT DEFAULT 0;

-- ─────────────────────────────────────────
-- P1-3: team_invitations — bridge table
-- The app uses team_members.role = 'Invited' as the invitation state.
-- This view makes the team_invitations table readable in the same way.
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS team_invitations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id     UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    invited_by  UUID NOT NULL,                         -- auth.users
    invited_user_id UUID NOT NULL,                     -- auth.users
    status      TEXT NOT NULL DEFAULT 'PENDING',       -- PENDING | ACCEPTED | DECLINED
    created_at  TIMESTAMPTZ DEFAULT now(),
    responded_at TIMESTAMPTZ
);

-- Sync view: shows pending invitations from team_members role='Invited'
-- so both tables stay consistent during the transition period
CREATE OR REPLACE VIEW v_pending_invitations AS
    SELECT
        id,
        team_id,
        user_id AS invited_user_id,
        'PENDING' AS status,
        joined_at AS created_at
    FROM team_members
    WHERE role = 'Invited';

-- ─────────────────────────────────────────
-- P2-1: get_team_stats RPC
-- ─────────────────────────────────────────
DROP FUNCTION IF EXISTS get_team_stats(UUID);
CREATE OR REPLACE FUNCTION get_team_stats(p_team_id UUID)
RETURNS JSON
LANGUAGE plpgsql SECURITY DEFINER
AS $$
DECLARE
    result JSON;
BEGIN
    SELECT json_build_object(
        'team_id',          t.id,
        'name',             t.name,
        'total_scrims',     t.total_scrims,
        'completed_scrims', t.completed_scrims,
        'no_shows',         t.no_shows,
        'reputation',       t.reputation,
        'win_count',        COALESCE(win_stats.wins, 0),
        'loss_count',       COALESCE(loss_stats.losses, 0)
    )
    INTO result
    FROM teams t
    LEFT JOIN (
        SELECT team_id, COUNT(*) AS wins
        FROM scrims
        WHERE winner_team_id = team_id AND status = 'Completed'
        GROUP BY team_id
    ) win_stats ON win_stats.team_id = t.id
    LEFT JOIN (
        SELECT team_id, COUNT(*) AS losses
        FROM scrims
        WHERE (team_id = p_team_id OR opponent_team_id = p_team_id)
          AND winner_team_id != p_team_id
          AND winner_team_id IS NOT NULL
          AND status = 'Completed'
        GROUP BY team_id
    ) loss_stats ON loss_stats.team_id = t.id
    WHERE t.id = p_team_id;

    RETURN result;
END;
$$;

-- ─────────────────────────────────────────
-- P2-2: get_available_scrims RPC
-- ─────────────────────────────────────────
DROP FUNCTION IF EXISTS get_available_scrims(TEXT, TEXT, TEXT, INT, INT);
CREATE OR REPLACE FUNCTION get_available_scrims(
    p_game_mode   TEXT DEFAULT NULL,
    p_region      TEXT DEFAULT NULL,
    p_skill_level TEXT DEFAULT NULL,
    p_limit       INT  DEFAULT 50,
    p_offset      INT  DEFAULT 0
)
RETURNS SETOF scrims
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM scrims
    WHERE status = 'Open'
      AND (p_game_mode   IS NULL OR game_mode   = p_game_mode)
      AND (p_region      IS NULL OR region      = p_region)
      AND (p_skill_level IS NULL OR skill_level = p_skill_level)
    ORDER BY scheduled_date ASC, scheduled_time ASC
    LIMIT p_limit OFFSET p_offset;
END;
$$;

-- ─────────────────────────────────────────
-- P2-3: mark_conversation_as_read RPC
-- ─────────────────────────────────────────
DROP FUNCTION IF EXISTS mark_conversation_as_read(UUID, UUID);
CREATE OR REPLACE FUNCTION mark_conversation_as_read(
    p_match_id UUID,
    p_user_id  UUID
)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
AS $$
BEGIN
    UPDATE messages
    SET is_read = TRUE
    WHERE match_id = p_match_id
      AND sender_id != p_user_id  -- don't mark own messages as "read by you"
      AND is_read = FALSE;
END;
$$;

-- ─────────────────────────────────────────
-- P2-4: delete_user soft-delete support
-- Ensure the profiles table has a deleted column for soft-delete
-- (already handled in app by PATCH to profiles)
-- ─────────────────────────────────────────
ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS deleted    BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS role       TEXT,
    ADD COLUMN IF NOT EXISTS bio        TEXT,
    ADD COLUMN IF NOT EXISTS main_heroes TEXT[];

-- Row-Level Security: hide deleted profiles from normal queries
DROP POLICY IF EXISTS "Hide deleted profiles" ON profiles;
CREATE POLICY "Hide deleted profiles"
    ON profiles FOR SELECT
    USING (deleted IS NOT TRUE);

-- ─────────────────────────────────────────
-- Helper: auto-update currentPlayers on scrims
-- ─────────────────────────────────────────
DROP FUNCTION IF EXISTS update_scrim_player_count();
CREATE OR REPLACE FUNCTION update_scrim_player_count()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    UPDATE scrims
    SET current_players = (
        SELECT COUNT(*) FROM scrim_rosters
        WHERE scrim_id = COALESCE(NEW.scrim_id, OLD.scrim_id) AND is_active = TRUE
    )
    WHERE id = COALESCE(NEW.scrim_id, OLD.scrim_id);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_update_player_count ON scrim_rosters;
CREATE TRIGGER trg_update_player_count
    AFTER INSERT OR DELETE OR UPDATE ON scrim_rosters
    FOR EACH ROW EXECUTE FUNCTION update_scrim_player_count();

-- ─────────────────────────────────────────
-- App Notifications Table
-- ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS app_notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    type TEXT DEFAULT 'INFO', -- INFO, SUCCESS, WARNING, ERROR
    is_read BOOLEAN DEFAULT FALSE,
    data JSONB DEFAULT '{}'::jsonb, -- Additional data (e.g., team_id, scrim_id)
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

-- Index for faster queries
CREATE INDEX IF NOT EXISTS idx_app_notifications_user ON app_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_app_notifications_read ON app_notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_app_notifications_created ON app_notifications(created_at DESC);
