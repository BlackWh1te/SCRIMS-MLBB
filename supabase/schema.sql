-- MLBB Scrim Host - Database Schema
-- PostgreSQL with Supabase

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Profiles table (extends Supabase Auth)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL,
    email TEXT NOT NULL,
    mlbb_id TEXT,
    is_admin BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Teams table
CREATE TABLE IF NOT EXISTS teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT UNIQUE NOT NULL,
    leader_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    description TEXT,
    min_players INTEGER DEFAULT 5,
    max_players INTEGER DEFAULT 7,
    available_days TEXT[] DEFAULT ARRAY[1, 2, 3, 4, 5, 6, 7],
    available_time_start TIME,
    available_time_end TIME,
    timezone TEXT DEFAULT 'UTC',
    total_xp INTEGER DEFAULT 0,
    current_tier TEXT DEFAULT 'Bronze',
    current_division INTEGER DEFAULT 1,
    -- P1-5: Team stats (phantom fields now materialized)
    reputation REAL DEFAULT 5.0,
    can_post_scrims_until TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    total_scrims INTEGER DEFAULT 0,
    completed_scrims INTEGER DEFAULT 0,
    no_shows INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Team members table
CREATE TABLE IF NOT EXISTS team_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    role TEXT DEFAULT 'Member',
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(team_id, user_id)
);

-- Team invitations table
CREATE TABLE IF NOT EXISTS team_invitations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    invited_user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    invited_by UUID REFERENCES profiles(id),
    status TEXT DEFAULT 'Pending',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    responded_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(team_id, invited_user_id)
);

-- Player stats table (per-player scrim statistics)
CREATE TABLE IF NOT EXISTS player_stats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE UNIQUE,
    pts INTEGER DEFAULT 0,
    wins INTEGER DEFAULT 0,
    losses INTEGER DEFAULT 0,
    matches_play INTEGER DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Notifications table (real-time notifications for users)
CREATE TABLE IF NOT EXISTS app_notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    type TEXT NOT NULL,                   -- SCRIM_INVITE, MATCH_RESULT, XP_GAIN, TEAM_INVITE, MESSAGE, SYSTEM
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    action_id TEXT,                        -- Reference to related entity (scrim_id, match_id, team_id, etc.)
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Scrims table (available scrims posted by teams)
CREATE TABLE IF NOT EXISTS scrims (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    scheduled_date DATE NOT NULL,
    scheduled_time TIME NOT NULL,
    best_of INTEGER DEFAULT 1,          -- 1=BO1, 2=BO2, 3=BO3, 4=BO4, 5=BO5
    status TEXT DEFAULT 'Open',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    -- Opponent team fields
    opponent_team_id UUID REFERENCES teams(id),
    opponent_team_name TEXT,
    conversation_id UUID,
    -- Ready flow
    team_a_ready BOOLEAN DEFAULT FALSE,
    team_b_ready BOOLEAN DEFAULT FALSE,
    team_a_ready_at TIMESTAMP WITH TIME ZONE,
    team_b_ready_at TIMESTAMP WITH TIME ZONE,
    -- Screenshot flow
    team_a_screenshot_url TEXT,
    team_b_screenshot_url TEXT,
    team_a_screenshot_uploaded_at TIMESTAMP WITH TIME ZONE,
    team_b_screenshot_uploaded_at TIMESTAMP WITH TIME ZONE,
    -- Result
    winner_team_id UUID REFERENCES teams(id),
    result_submitted_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    cancelled_by UUID REFERENCES profiles(id),
    -- P1-7: Scrim search/filter fields (phantom fields now materialized)
    game_mode TEXT DEFAULT 'RANKED',
    region TEXT DEFAULT 'EU',
    skill_level TEXT DEFAULT 'ALL',
    max_players INTEGER DEFAULT 10,
    current_players INTEGER DEFAULT 0
);

-- Scrim applications table
CREATE TABLE IF NOT EXISTS scrim_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scrim_id UUID REFERENCES scrims(id) ON DELETE CASCADE,
    applicant_team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'Pending',
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(scrim_id, applicant_team_id)
);

-- Scrim rosters table (captain assigns active/substitute players per scrim)
CREATE TABLE IF NOT EXISTS scrim_rosters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scrim_id UUID REFERENCES scrims(id) ON DELETE CASCADE,
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT FALSE,     -- true = playing (pts affected), false = substitute
    assigned_by UUID REFERENCES profiles(id),  -- captain who assigned
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(scrim_id, team_id, user_id)
);

-- Matches table (confirmed matches)
CREATE TABLE IF NOT EXISTS matches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scrim_id UUID REFERENCES scrims(id),
    team_a_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    team_b_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    scheduled_date DATE NOT NULL,
    scheduled_time TIME NOT NULL,
    room_id TEXT,
    room_password TEXT,
    status TEXT DEFAULT 'Scheduled',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Messages table (chat between team leaders)
CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    match_id UUID REFERENCES matches(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    sender_team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    -- P1-6: Message metadata (phantom fields now materialized)
    sender_name TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    type TEXT DEFAULT 'TEXT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Match results table
CREATE TABLE IF NOT EXISTS match_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    match_id UUID REFERENCES matches(id) ON DELETE CASCADE,
    team_a_screenshot_url TEXT,
    team_b_screenshot_url TEXT,
    winner_team_id UUID REFERENCES teams(id),
    admin_verified BOOLEAN DEFAULT FALSE,
    verified_by UUID REFERENCES profiles(id),
    verification_notes TEXT,
    xp_awarded BOOLEAN DEFAULT FALSE,
    pts_awarded BOOLEAN DEFAULT FALSE,
    -- P1-4: Admin review fields (phantom fields now materialized)
    admin_verdict TEXT,
    punished_team_id UUID REFERENCES teams(id),
    punishment_duration_hours INTEGER DEFAULT 0,
    reviewed_by_admin_id UUID REFERENCES profiles(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    no_show_team_id UUID REFERENCES teams(id),
    match_actually_played BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_teams_leader ON teams(leader_id);
CREATE INDEX IF NOT EXISTS idx_team_members_team ON team_members(team_id);
CREATE INDEX IF NOT EXISTS idx_team_members_user ON team_members(user_id);
CREATE INDEX IF NOT EXISTS idx_scrims_team ON scrims(team_id);
CREATE INDEX IF NOT EXISTS idx_scrims_status ON scrims(status);
CREATE INDEX IF NOT EXISTS idx_scrims_opponent ON scrims(opponent_team_id);
CREATE INDEX IF NOT EXISTS idx_scrim_applications_scrim ON scrim_applications(scrim_id);
CREATE INDEX IF NOT EXISTS idx_scrim_applications_team ON scrim_applications(applicant_team_id);
CREATE INDEX IF NOT EXISTS idx_scrim_rosters_scrim ON scrim_rosters(scrim_id);
CREATE INDEX IF NOT EXISTS idx_scrim_rosters_team ON scrim_rosters(team_id);
CREATE INDEX IF NOT EXISTS idx_scrim_rosters_user ON scrim_rosters(user_id);
CREATE INDEX IF NOT EXISTS idx_matches_team_a ON matches(team_a_id);
CREATE INDEX IF NOT EXISTS idx_matches_team_b ON matches(team_b_id);
CREATE INDEX IF NOT EXISTS idx_matches_status ON matches(status);
CREATE INDEX IF NOT EXISTS idx_messages_match ON messages(match_id);
CREATE INDEX IF NOT EXISTS idx_messages_created ON messages(created_at);
CREATE INDEX IF NOT EXISTS idx_match_results_match ON match_results(match_id);
CREATE INDEX IF NOT EXISTS idx_team_invitations_team ON team_invitations(team_id);
CREATE INDEX IF NOT EXISTS idx_team_invitations_user ON team_invitations(invited_user_id);
CREATE INDEX IF NOT EXISTS idx_player_stats_user ON player_stats(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON app_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON app_notifications(created_at);

-- ═══════════════════════════════════════════════════════════════
-- FUNCTIONS
-- ═══════════════════════════════════════════════════════════════

-- Award/deduct points to active roster players after scrim completion
CREATE OR REPLACE FUNCTION award_scrim_points(
    p_scrim_id UUID,
    p_winner_team_id UUID,
    p_pts_per_win INTEGER DEFAULT 25,
    p_pts_per_loss INTEGER DEFAULT 15
)
RETURNS VOID AS $$
DECLARE
    roster_entry RECORD;
    is_winner BOOLEAN;
BEGIN
    -- Update each active roster player's stats
    FOR roster_entry IN
        SELECT sr.user_id, sr.team_id, sr.is_active
        FROM scrim_rosters sr
        WHERE sr.scrim_id = p_scrim_id AND sr.is_active = TRUE
    LOOP
        is_winner := (roster_entry.team_id = p_winner_team_id);

        -- Insert or update player_stats
        INSERT INTO player_stats (user_id, pts, wins, losses, matches_play)
        VALUES (
            roster_entry.user_id,
            CASE WHEN is_winner THEN p_pts_per_win ELSE -p_pts_per_loss END,
            CASE WHEN is_winner THEN 1 ELSE 0 END,
            CASE WHEN is_winner THEN 0 ELSE 1 END,
            1
        )
        ON CONFLICT (user_id) DO UPDATE SET
            pts = player_stats.pts + CASE WHEN is_winner THEN p_pts_per_win ELSE -p_pts_per_loss END,
            wins = player_stats.wins + CASE WHEN is_winner THEN 1 ELSE 0 END,
            losses = player_stats.losses + CASE WHEN is_winner THEN 0 ELSE 1 END,
            matches_play = player_stats.matches_play + 1,
            updated_at = TIMEZONE('utc', NOW());
    END LOOP;

    -- Mark pts as awarded in match result
    UPDATE match_results
    SET pts_awarded = TRUE
    WHERE match_id IN (
        SELECT m.id FROM matches m WHERE m.scrim_id = p_scrim_id
    );
END;
$$ LANGUAGE plpgsql;

-- ═══════════════════════════════════════════════════════════════
-- RPC FUNCTIONS (P2 fixes)
-- ═══════════════════════════════════════════════════════════════

-- P2-1: Get team stats (wins, losses, total scrims, etc.)
CREATE OR REPLACE FUNCTION get_team_stats(p_team_id UUID)
RETURNS TABLE (
    total_scrims BIGINT,
    completed_scrims BIGINT,
    wins BIGINT,
    losses BIGINT,
    total_points BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        COUNT(*)::BIGINT AS total_scrims,
        COUNT(*) FILTER (WHERE s.status = 'COMPLETED')::BIGINT AS completed_scrims,
        COUNT(*) FILTER (WHERE s.winner_team_id = p_team_id AND s.status = 'COMPLETED')::BIGINT AS wins,
        COUNT(*) FILTER (WHERE s.winner_team_id IS NOT NULL AND s.winner_team_id != p_team_id AND s.status = 'COMPLETED')::BIGINT AS losses,
        COALESCE(SUM(ps.pts) FILTER (WHERE tm.team_id = p_team_id), 0)::BIGINT AS total_points
    FROM scrims s
    LEFT JOIN team_members tm ON tm.team_id = p_team_id
    LEFT JOIN player_stats ps ON ps.user_id = tm.user_id
    WHERE s.team_id = p_team_id OR s.opponent_team_id = p_team_id;
END;
$$ LANGUAGE plpgsql;

-- P2-2: Get available scrims (open scrims that a team can apply to)
CREATE OR REPLACE FUNCTION get_available_scrims(
    p_team_id UUID,
    p_game_mode TEXT DEFAULT NULL,
    p_region TEXT DEFAULT NULL,
    p_skill_level TEXT DEFAULT NULL
)
RETURNS SETOF scrims AS $$
BEGIN
    RETURN QUERY
    SELECT *
    FROM scrims
    WHERE status = 'Open'
      AND team_id != p_team_id
      AND (p_game_mode IS NULL OR game_mode = p_game_mode)
      AND (p_region IS NULL OR region = p_region)
      AND (p_skill_level IS NULL OR skill_level = p_skill_level)
    ORDER BY created_at DESC;
END;
$$ LANGUAGE plpgsql;

-- P2-3: Mark conversation as read for a user
CREATE OR REPLACE FUNCTION mark_conversation_as_read(
    p_match_id UUID,
    p_user_id UUID
)
RETURNS VOID AS $$
BEGIN
    UPDATE messages
    SET is_read = TRUE
    WHERE match_id = p_match_id
      AND sender_id != p_user_id
      AND is_read = FALSE;
END;
$$ LANGUAGE plpgsql;

-- P2-4: Delete user and cascade cleanup
CREATE OR REPLACE FUNCTION delete_user(p_user_id UUID)
RETURNS VOID AS $$
BEGIN
    -- team_members cleanup
    DELETE FROM team_members WHERE user_id = p_user_id;
    -- team_invitations cleanup
    DELETE FROM team_invitations WHERE invited_user_id = p_user_id OR invited_by = p_user_id;
    -- scrim_rosters cleanup
    DELETE FROM scrim_rosters WHERE user_id = p_user_id;
    -- scrim_applications cleanup (as applicant)
    DELETE FROM scrim_applications WHERE applicant_team_id IN (
        SELECT team_id FROM team_members WHERE user_id = p_user_id
    );
    -- player_stats cleanup
    DELETE FROM player_stats WHERE user_id = p_user_id;
    -- profiles cleanup
    DELETE FROM profiles WHERE id = p_user_id;
    -- Finally delete the auth user (requires admin privileges)
    -- This is handled by Supabase Auth API, not here.
END;
$$ LANGUAGE plpgsql;
