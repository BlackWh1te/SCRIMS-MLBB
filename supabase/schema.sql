-- MLBB Scrim Host - Database Schema
-- PostgreSQL with Supabase

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Profiles table (extends Supabase Auth)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE NOT NULL,
    email TEXT NOT NULL,
    mlbb_id TEXT UNIQUE, -- Enforce 1-to-1 Game ID link
    is_admin BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    is_banned BOOLEAN DEFAULT FALSE,
    ban_reason TEXT,
    banned_at TIMESTAMP WITH TIME ZONE,
    banned_by UUID REFERENCES profiles(id),
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
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
    reputation NUMERIC(3,1) DEFAULT 5.0,
    can_post_scrims_until TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    total_scrims INTEGER DEFAULT 0,
    completed_scrims INTEGER DEFAULT 0,
    no_shows INTEGER DEFAULT 0,
    is_open_for_applications BOOLEAN DEFAULT FALSE,
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

-- Team applications table (players apply to open teams)
CREATE TABLE IF NOT EXISTS team_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    applicant_user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'Pending',
    message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    responded_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(team_id, applicant_user_id)
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
    best_of INTEGER DEFAULT 1 CHECK (best_of IN (1, 3, 5)),          -- 1=BO1, 3=BO3, 5=BO5
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
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    match_id UUID REFERENCES matches(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    sender_team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    -- P1-6: Message metadata (phantom fields now materialized)
    sender_name TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    type TEXT DEFAULT 'TEXT',
    image_url TEXT,
    voice_url TEXT,
    voice_duration INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Conversations table (chat conversations between scrim participants)
CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scrim_id UUID REFERENCES scrims(id) ON DELETE CASCADE,
    participant_a_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    participant_a_name TEXT,
    participant_a_team_id UUID REFERENCES teams(id),
    participant_a_team_name TEXT,
    participant_b_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    participant_b_name TEXT,
    participant_b_team_id UUID REFERENCES teams(id),
    participant_b_team_name TEXT,
    last_message TEXT DEFAULT '',
    last_message_time TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    chat_opens_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    participant_a_typing BOOLEAN DEFAULT FALSE,
    participant_b_typing BOOLEAN DEFAULT FALSE,
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
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversations_participant_a ON conversations(participant_a_id);
CREATE INDEX IF NOT EXISTS idx_conversations_participant_b ON conversations(participant_b_id);
CREATE INDEX IF NOT EXISTS idx_conversations_scrim ON conversations(scrim_id);
CREATE INDEX IF NOT EXISTS idx_match_results_match ON match_results(match_id);
CREATE INDEX IF NOT EXISTS idx_team_invitations_team ON team_invitations(team_id);
CREATE INDEX IF NOT EXISTS idx_team_invitations_user ON team_invitations(invited_user_id);
CREATE INDEX IF NOT EXISTS idx_player_stats_user ON player_stats(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON app_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON app_notifications(created_at);

-- ═══════════════════════════════════════════════════════════════
-- CHECK CONSTRAINTS
-- ═══════════════════════════════════════════════════════════════

-- ═══════════════════════════════════════════════════════════════
-- REALTIME — Enable Supabase Realtime for live updates
-- Run these in the Supabase SQL Editor or via supabase CLI
-- ═══════════════════════════════════════════════════════════════

-- Enable Realtime publication for key tables
-- This allows the Android client to subscribe to INSERT/UPDATE/DELETE events
ALTER PUBLICATION supabase_realtime ADD TABLE messages;
ALTER PUBLICATION supabase_realtime ADD TABLE scrims;
ALTER PUBLICATION supabase_realtime ADD TABLE scrim_applications;
ALTER PUBLICATION supabase_realtime ADD TABLE app_notifications;
ALTER PUBLICATION supabase_realtime ADD TABLE teams;
ALTER PUBLICATION supabase_realtime ADD TABLE team_invitations;
ALTER PUBLICATION supabase_realtime ADD TABLE team_members;
ALTER PUBLICATION supabase_realtime ADD TABLE conversations;
ALTER PUBLICATION supabase_realtime ADD TABLE lfg_posts;
ALTER PUBLICATION supabase_realtime ADD TABLE player_stats;

-- If the publication doesn't exist yet, create it first:
-- CREATE PUBLICATION supabase_realtime FOR TABLE messages, scrims, scrim_applications, app_notifications, teams, team_invitations, team_members, conversations;

-- Enable RLS on all Realtime-enabled tables (required for Supabase Realtime)
-- Users should only see their own messages, notifications, and team data
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE scrims ENABLE ROW LEVEL SECURITY;
ALTER TABLE scrim_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE team_invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE team_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;

-- ═══════════════════════════════════════════════════════════════
-- REALTIME RLS Policies — Allow authenticated users to subscribe
-- ═══════════════════════════════════════════════════════════════

-- Messages: sender OR any participant of the parent conversation can view messages
CREATE POLICY "Conversation members can view messages" ON messages
    FOR SELECT USING (
        sender_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
        )
    );

-- INSERT: sender must be a participant of the parent conversation
CREATE POLICY "Conversation members can send messages" ON messages
    FOR INSERT WITH CHECK (
        sender_id = auth.uid()
        AND EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
        )
    );

-- UPDATE: only sender can update (e.g. read receipts, edits)
CREATE POLICY "Message sender can update" ON messages
    FOR UPDATE USING (sender_id = auth.uid());

-- DELETE: only sender can delete their own message
CREATE POLICY "Message sender can delete" ON messages
    FOR DELETE USING (sender_id = auth.uid());

-- Scrims: anyone can see open scrims, participants can see their scrims
CREATE POLICY "Users can view scrims" ON scrims
    FOR SELECT USING (
        status = 'Open'
        OR team_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM team_members tm
            WHERE tm.team_id = scrims.team_id AND tm.user_id = auth.uid()
        )
        OR EXISTS (
            SELECT 1 FROM team_members tm
            WHERE tm.team_id = scrims.opponent_team_id AND tm.user_id = auth.uid()
        )
    );

-- Notifications: users can only see their own notifications
CREATE POLICY "Users can view their notifications" ON app_notifications
    FOR SELECT USING (user_id = auth.uid());

-- Teams: anyone can view teams, members can see full details
CREATE POLICY "Users can view teams" ON teams
    FOR SELECT USING (true);

-- Team invitations: users can see invites they sent or received
CREATE POLICY "Users can view their invitations" ON team_invitations
    FOR SELECT USING (
        invited_user_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM teams t WHERE t.id = team_invitations.team_id AND t.leader_id = auth.uid()
        )
    );

-- Team members: anyone can see team membership
CREATE POLICY "Users can view team members" ON team_members
    FOR SELECT USING (true);

-- Conversations: participants can see their conversations
CREATE POLICY "Conversation participants can view" ON conversations
    FOR SELECT USING (
        participant_a_id = auth.uid() OR participant_b_id = auth.uid()
    );

-- INSERT: authenticated users can create conversations where they are a participant
CREATE POLICY "Conversation participants can insert" ON conversations
    FOR INSERT WITH CHECK (
        participant_a_id = auth.uid() OR participant_b_id = auth.uid()
    );

-- UPDATE: participants can update typing status / last_message
CREATE POLICY "Conversation participants can update" ON conversations
    FOR UPDATE USING (
        participant_a_id = auth.uid() OR participant_b_id = auth.uid()
    );

-- Scrim applications: team members can see applications for their scrims
CREATE POLICY "Users can view scrim applications" ON scrim_applications
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM scrims s WHERE s.id = scrim_applications.scrim_id
            AND (s.team_id IN (SELECT team_id FROM team_members WHERE user_id = auth.uid())
                 OR applicant_team_id IN (SELECT team_id FROM team_members WHERE user_id = auth.uid()))
        )
    );

-- Player stats: pts cannot go negative
ALTER TABLE player_stats ADD CONSTRAINT pts_non_negative CHECK (pts >= 0);

-- Scrim status enum
ALTER TABLE scrims ADD CONSTRAINT valid_scrim_status CHECK (status IN ('Open', 'Pending', 'Accepted', 'Ready', 'In Progress', 'Completed', 'Cancelled'));

-- Match status enum
ALTER TABLE matches ADD CONSTRAINT valid_match_status CHECK (status IN ('Scheduled', 'In Progress', 'Completed', 'Cancelled'));

-- Scrim application status enum
ALTER TABLE scrim_applications ADD CONSTRAINT valid_application_status CHECK (status IN ('Pending', 'Accepted', 'Rejected'));

-- Team invitation status enum
ALTER TABLE team_invitations ADD CONSTRAINT valid_invitation_status CHECK (status IN ('Pending', 'Accepted', 'Rejected'));

-- Team reputation range
ALTER TABLE teams ADD CONSTRAINT valid_reputation CHECK (reputation >= 0 AND reputation <= 10);

-- Additional performance indexes
CREATE INDEX IF NOT EXISTS idx_scrims_scheduled_date ON scrims(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_scrims_region ON scrims(region);
CREATE INDEX IF NOT EXISTS idx_scrims_game_mode ON scrims(game_mode);
CREATE INDEX IF NOT EXISTS idx_scrims_skill_level ON scrims(skill_level);
CREATE INDEX IF NOT EXISTS idx_profiles_username ON profiles(username);
CREATE INDEX IF NOT EXISTS idx_profiles_mlbb_id ON profiles(mlbb_id);
CREATE INDEX IF NOT EXISTS idx_profiles_is_banned ON profiles(is_banned);
CREATE INDEX IF NOT EXISTS idx_app_notifications_type ON app_notifications(type);

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
            pts = GREATEST(player_stats.pts + CASE WHEN is_winner THEN p_pts_per_win ELSE -p_pts_per_loss END, 0),
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
    p_conversation_id UUID,
    p_user_id UUID
)
RETURNS VOID AS $$
BEGIN
    UPDATE messages
    SET is_read = TRUE,
        read_at = TIMEZONE('utc', NOW())
    WHERE conversation_id = p_conversation_id
      AND sender_id != p_user_id
      AND is_read = FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Auto-update conversation last_message on new message insert
CREATE OR REPLACE FUNCTION update_conversation_last_message()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE conversations
    SET
        last_message = NEW.content,
        last_message_time = NEW.created_at
    WHERE id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_conversation_last_message ON messages;
CREATE TRIGGER trg_update_conversation_last_message
    AFTER INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION update_conversation_last_message();

-- P2-4: Delete user and cascade cleanup
CREATE OR REPLACE FUNCTION delete_user(p_user_id UUID)
RETURNS VOID AS $$
BEGIN
    -- Delete scrim_applications FIRST (depends on team_members for lookup)
    DELETE FROM scrim_applications WHERE applicant_team_id IN (
        SELECT team_id FROM team_members WHERE user_id = p_user_id
    );
    -- team_members cleanup
    DELETE FROM team_members WHERE user_id = p_user_id;
    -- team_invitations cleanup
    DELETE FROM team_invitations WHERE invited_user_id = p_user_id OR invited_by = p_user_id;
    -- scrim_rosters cleanup
    DELETE FROM scrim_rosters WHERE user_id = p_user_id;
    -- player_stats cleanup
    DELETE FROM player_stats WHERE user_id = p_user_id;
    -- notifications cleanup
    DELETE FROM app_notifications WHERE user_id = p_user_id;
    -- profiles cleanup
    DELETE FROM profiles WHERE id = p_user_id;
    -- Finally delete the auth user (requires admin privileges)
    -- This is handled by Supabase Auth API, not here.
END;
$$ LANGUAGE plpgsql;

-- ═══════════════════════════════════════════════════════════════
-- RPC FUNCTIONS (P4 messaging fixes)
-- ═══════════════════════════════════════════════════════════════

-- Count unread messages per conversation for a user
CREATE OR REPLACE FUNCTION get_conversation_unread_count(
    p_conversation_id UUID,
    p_user_id UUID
)
RETURNS INTEGER AS $$
DECLARE
    unread_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO unread_count
    FROM messages
    WHERE conversation_id = p_conversation_id
      AND sender_id != p_user_id
      AND is_read = FALSE;
    RETURN COALESCE(unread_count, 0);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Get conversations with unread counts for a user
CREATE OR REPLACE FUNCTION get_conversations_for_user(p_user_id UUID)
RETURNS TABLE (
    id UUID,
    scrim_id UUID,
    participant_a_id UUID,
    participant_a_name TEXT,
    participant_a_team_id UUID,
    participant_a_team_name TEXT,
    participant_b_id UUID,
    participant_b_name TEXT,
    participant_b_team_id UUID,
    participant_b_team_name TEXT,
    last_message TEXT,
    last_message_time TIMESTAMP WITH TIME ZONE,
    chat_opens_at TIMESTAMP WITH TIME ZONE,
    participant_a_typing BOOLEAN,
    participant_b_typing BOOLEAN,
    unread_count BIGINT,
    created_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.scrim_id,
        c.participant_a_id,
        c.participant_a_name,
        c.participant_a_team_id,
        c.participant_a_team_name,
        c.participant_b_id,
        c.participant_b_name,
        c.participant_b_team_id,
        c.participant_b_team_name,
        c.last_message,
        c.last_message_time,
        c.chat_opens_at,
        c.participant_a_typing,
        c.participant_b_typing,
        COALESCE(
            (SELECT COUNT(*) FROM messages m
             WHERE m.conversation_id = c.id
               AND m.sender_id != p_user_id
               AND m.is_read = FALSE),
            0
        )::BIGINT AS unread_count,
        c.created_at
    FROM conversations c
    WHERE c.participant_a_id = p_user_id OR c.participant_b_id = p_user_id
    ORDER BY c.last_message_time DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Enforce chat gate (server-side lock before chatOpensAt)
CREATE OR REPLACE FUNCTION enforce_chat_gate()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM conversations c
        WHERE c.id = NEW.conversation_id
          AND c.chat_opens_at > TIMEZONE('utc', NOW())
    ) THEN
        RAISE EXCEPTION 'Chat is locked until %', (
            SELECT c.chat_opens_at FROM conversations c WHERE c.id = NEW.conversation_id
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_enforce_chat_gate ON messages;
CREATE TRIGGER trg_enforce_chat_gate
    BEFORE INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION enforce_chat_gate();

-- ═══════════════════════════════════════════════════════════════
-- MIGRATIONS — Run these if the column doesn't exist yet
-- Fix: PGRST204 "could not find is_open_for_applications column"
-- This happens when the schema was applied before the column was added.
-- ═══════════════════════════════════════════════════════════════

-- Add is_open_for_applications column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'teams' AND column_name = 'is_open_for_applications'
    ) THEN
        ALTER TABLE teams ADD COLUMN is_open_for_applications BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- Add logo_url column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'teams' AND column_name = 'logo_url'
    ) THEN
        ALTER TABLE teams ADD COLUMN logo_url TEXT;
    END IF;
END $$;

-- Notify PostgREST to reload schema cache after adding columns
NOTIFY pgrst, 'reload schema';
