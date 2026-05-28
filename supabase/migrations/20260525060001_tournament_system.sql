-- Migration: Tournament System — Complete database layer
-- Date: 2026-05-25
-- Context: Adds tournament hosting, Swiss brackets, team applications,
--          match rosters, host auth, player stats, and group chat support.
-- Dependencies: Existing tables (profiles, teams, team_members, conversations, app_notifications)
-- Security: All mutations go through SECURITY DEFINER RPCs with auth.uid() checks.
--           Sensitive fields in tournament_match_room_secrets with strict RLS.
--           No plaintext passwords stored anywhere.

-- ═══════════════════════════════════════════════════════════════
-- 0. EXTENSIONS
-- ═══════════════════════════════════════════════════════════════

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ═══════════════════════════════════════════════════════════════
-- 1. PROFILES ALTER — Add tournament host columns
-- ═══════════════════════════════════════════════════════════════

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'is_tournament_host'
    ) THEN
        ALTER TABLE profiles ADD COLUMN is_tournament_host BOOLEAN DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'telegram_username'
    ) THEN
        ALTER TABLE profiles ADD COLUMN telegram_username TEXT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'tournaments_hosted'
    ) THEN
        ALTER TABLE profiles ADD COLUMN tournaments_hosted INTEGER DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'tournaments_completed'
    ) THEN
        ALTER TABLE profiles ADD COLUMN tournaments_completed INTEGER DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'tournaments_cancelled'
    ) THEN
        ALTER TABLE profiles ADD COLUMN tournaments_cancelled INTEGER DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'host_trust_score'
    ) THEN
        ALTER TABLE profiles ADD COLUMN host_trust_score NUMERIC(3,1) DEFAULT 5.0;
    END IF;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 2. CONVERSATIONS ALTER — Add tournament_match_id column
-- ═══════════════════════════════════════════════════════════════

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'conversations' AND column_name = 'tournament_match_id'
    ) THEN
        ALTER TABLE conversations ADD COLUMN tournament_match_id UUID;
    END IF;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 3. TOURNAMENT HOST REQUESTS — Users requesting tournament_host role
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tournament_host_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    motivation TEXT NOT NULL,
    experience TEXT,
    telegram_channel TEXT,
    social_links TEXT[],
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'approved', 'rejected')),
    admin_notes TEXT,
    reviewed_by UUID REFERENCES profiles(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(user_id)  -- one active request per user at a time
);

-- ═══════════════════════════════════════════════════════════════
-- 4. TOURNAMENTS — Core tournament table
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tournaments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL CHECK (char_length(title) <= 100),
    description TEXT NOT NULL,
    logo_url TEXT,
    prize_type TEXT NOT NULL CHECK (prize_type IN ('real_money', 'diamonds', 'skin', 'star_pass', 'other')),
    prize_description TEXT,
    max_teams INTEGER NOT NULL DEFAULT 16 CHECK (max_teams >= 4 AND max_teams <= 64),
    min_team_size INTEGER NOT NULL DEFAULT 5 CHECK (min_team_size >= 3 AND min_team_size <= 7),
    best_of INTEGER NOT NULL DEFAULT 1 CHECK (best_of IN (1, 2)),
    region TEXT NOT NULL DEFAULT 'EU',
    skill_level TEXT NOT NULL DEFAULT 'ALL',
    swiss_rounds INTEGER,
    current_round INTEGER DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'draft'
        CHECK (status IN ('draft', 'registration', 'check_in', 'in_progress', 'completed', 'cancelled')),
    registration_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    check_in_deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    is_live_stream_enabled BOOLEAN DEFAULT FALSE,
    is_flagged BOOLEAN DEFAULT FALSE,
    flagged_reason TEXT,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- ═══════════════════════════════════════════════════════════════
-- 5. TOURNAMENT REQUIREMENTS — Up to 15 requirements per tournament
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tournament_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('telegram_subscribe', 'youtube_subscribe', 'custom')),
    label TEXT NOT NULL,
    url TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- ═══════════════════════════════════════════════════════════════
-- 6. TOURNAMENT APPLICATIONS — Teams applying to join
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tournament_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'accepted', 'rejected', 'blocked')),
    rejection_reason TEXT,
    attempt_number INTEGER NOT NULL DEFAULT 1,
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by UUID REFERENCES profiles(id),
    UNIQUE(tournament_id, team_id)  -- one active application per team per tournament
);

-- ═══════════════════════════════════════════════════════════════
-- 7. TOURNAMENT TEAMS — Accepted teams with Swiss scoring
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tournament_teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    checked_in BOOLEAN DEFAULT FALSE,
    checked_in_at TIMESTAMP WITH TIME ZONE,
    swiss_wins INTEGER DEFAULT 0,
    swiss_losses INTEGER DEFAULT 0,
    swiss_draws INTEGER DEFAULT 0,
    swiss_points INTEGER DEFAULT 0,
    buchholz_score NUMERIC(6,1) DEFAULT 0.0,
    sonneborn_berger NUMERIC(6,1) DEFAULT 0.0,
    final_placement INTEGER,
    is_disqualified BOOLEAN DEFAULT FALSE,
    disqualification_reason TEXT,
    disqualified_at TIMESTAMP WITH TIME ZONE,
    disqualified_by UUID REFERENCES profiles(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(tournament_id, team_id)
);

-- ═══════════════════════════════════════════════════════════════
-- 8. TOURNAMENT SWISS MATCHES — Swiss round pairings + results
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tournament_swiss_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,
    match_number INTEGER NOT NULL,
    team_a_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    team_b_id UUID REFERENCES teams(id) ON DELETE CASCADE,  -- NULL = bye
    conversation_id UUID REFERENCES conversations(id) ON DELETE SET NULL,
    status TEXT NOT NULL DEFAULT 'scheduled'
        CHECK (status IN ('scheduled', 'in_progress', 'completed', 'disputed', 'cancelled', 'bye')),
    scheduled_at TIMESTAMP WITH TIME ZONE,
    no_show_grace_period_min INTEGER DEFAULT 15,
    match_auto_complete_at TIMESTAMP WITH TIME ZONE,
    winner_team_id UUID REFERENCES teams(id),
    is_draw BOOLEAN DEFAULT FALSE,
    game_a_score INTEGER DEFAULT 0,
    game_b_score INTEGER DEFAULT 0,
    result_submitted_at TIMESTAMP WITH TIME ZONE,
    result_submitted_by UUID REFERENCES profiles(id),
    dispute_reason TEXT,
    dispute_resolved_by UUID REFERENCES profiles(id),
    dispute_resolution TEXT,
    live_stream_url TEXT,
    is_bye BOOLEAN GENERATED ALWAYS AS (team_b_id IS NULL) STORED,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(tournament_id, round_number, match_number)
);

-- ═══════════════════════════════════════════════════════════════
-- 9. TOURNAMENT MATCH ROSTERS — Active players selected by leaders per match
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tournament_match_rosters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES tournament_swiss_matches(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    game_number INTEGER NOT NULL DEFAULT 1 CHECK (game_number IN (1, 2)),
    is_active BOOLEAN DEFAULT TRUE,
    assigned_by UUID NOT NULL REFERENCES profiles(id),
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(match_id, team_id, user_id, game_number)
);

-- ═══════════════════════════════════════════════════════════════
-- 10. TOURNAMENT MATCH ROOM SECRETS — Participant-only room credentials
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tournament_match_room_secrets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES tournament_swiss_matches(id) ON DELETE CASCADE,
    room_id TEXT NOT NULL,
    room_password TEXT,
    dropped_by UUID NOT NULL REFERENCES profiles(id),
    dropped_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(match_id)
);

-- ═══════════════════════════════════════════════════════════════
-- 11. TOURNAMENT HOST ACCOUNTS — Host auth metadata only (no passwords)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tournament_host_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    host_user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    auth_user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    email TEXT NOT NULL,
    created_by UUID REFERENCES profiles(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(tournament_id),
    UNIQUE(auth_user_id)
);

-- ═══════════════════════════════════════════════════════════════
-- 12. TOURNAMENT PLAYER STATS — Per-player tournament history
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS tournament_player_stats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    placement INTEGER,
    matches_won INTEGER DEFAULT 0,
    matches_lost INTEGER DEFAULT 0,
    matches_drawn INTEGER DEFAULT 0,
    points_earned INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(tournament_id, user_id)
);

-- ═══════════════════════════════════════════════════════════════
-- 13. CONVERSATION PARTICIPANTS — Normalized participant table for group chat
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS conversation_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'member'
        CHECK (role IN ('team_a_leader', 'team_b_leader', 'host', 'admin', 'member')),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(conversation_id, user_id)
);

-- ═══════════════════════════════════════════════════════════════
-- 14. ENABLE ROW LEVEL SECURITY on all new tables
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE tournament_host_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournaments ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_requirements ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_swiss_matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_match_rosters ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_match_room_secrets ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_host_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE tournament_player_stats ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversation_participants ENABLE ROW LEVEL SECURITY;

-- ═══════════════════════════════════════════════════════════════
-- 15. RLS POLICIES
-- ═══════════════════════════════════════════════════════════════

-- ── tournament_host_requests ──────────────────────────────────

-- Users can view their own host request
CREATE POLICY "Users can view own host request" ON tournament_host_requests
    FOR SELECT USING (user_id = auth.uid());

-- Anyone can create a host request for themselves
CREATE POLICY "Users can create own host request" ON tournament_host_requests
    FOR INSERT WITH CHECK (user_id = auth.uid());

-- Admins can view all host requests (service_role bypasses RLS, but this
-- covers anon-key admin panel usage where admin is NOT auth.uid())
-- We rely on the admin panel using service_role for admin reads.
-- For regular users, they only see their own (policy above).

-- ── tournaments ──────────────────────────────────────────────

-- Anyone can view published tournaments (not draft)
CREATE POLICY "Anyone can view published tournaments" ON tournaments
    FOR SELECT USING (
        status IN ('registration', 'check_in', 'in_progress', 'completed')
        OR host_user_id = auth.uid()
    );

-- Authenticated users can create their own tournaments (host_user_id must match auth.uid())
CREATE POLICY "Authenticated users can create tournaments" ON tournaments
    FOR INSERT WITH CHECK (
        host_user_id = auth.uid()
    );

-- Host can update their own tournament
CREATE POLICY "Hosts can update own tournaments" ON tournaments
    FOR UPDATE USING (host_user_id = auth.uid());

-- ── tournament_requirements ──────────────────────────────────

-- Anyone can view requirements for published tournaments
CREATE POLICY "Anyone can view tournament requirements" ON tournament_requirements
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM tournaments t
            WHERE t.id = tournament_requirements.tournament_id
            AND (t.status IN ('registration', 'check_in', 'in_progress', 'completed') OR t.host_user_id = auth.uid())
        )
    );

-- Host can manage requirements for their tournament
CREATE POLICY "Hosts can manage tournament requirements" ON tournament_requirements
    FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_requirements.tournament_id AND host_user_id = auth.uid())
    );

CREATE POLICY "Hosts can update tournament requirements" ON tournament_requirements
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_requirements.tournament_id AND host_user_id = auth.uid())
    );

CREATE POLICY "Hosts can delete tournament requirements" ON tournament_requirements
    FOR DELETE USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_requirements.tournament_id AND host_user_id = auth.uid())
    );

-- ── tournament_applications ──────────────────────────────────

-- Team members can see applications for their teams
-- Host can see applications for their tournament
CREATE POLICY "Users can view relevant applications" ON tournament_applications
    FOR SELECT USING (
        -- Host of the tournament
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_applications.tournament_id AND host_user_id = auth.uid())
        -- Team leader or member of the applying team
        OR EXISTS (
            SELECT 1 FROM teams t
            JOIN team_members tm ON tm.team_id = t.id
            WHERE t.id = tournament_applications.team_id AND tm.user_id = auth.uid()
        )
    );

-- Team leader can apply for their team
CREATE POLICY "Team leaders can apply" ON tournament_applications
    FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM teams WHERE id = tournament_applications.team_id AND leader_id = auth.uid())
    );

-- Host can update application status (accept/reject)
CREATE POLICY "Hosts can review applications" ON tournament_applications
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_applications.tournament_id AND host_user_id = auth.uid())
    );

-- ── tournament_teams ─────────────────────────────────────────

-- Anyone can view teams in published tournaments
CREATE POLICY "Anyone can view tournament teams" ON tournament_teams
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM tournaments t
            WHERE t.id = tournament_teams.tournament_id
            AND (t.status IN ('registration', 'check_in', 'in_progress', 'completed') OR t.host_user_id = auth.uid())
        )
    );

-- Host can manage tournament teams (check-in, disqualify)
CREATE POLICY "Hosts can manage tournament teams" ON tournament_teams
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_teams.tournament_id AND host_user_id = auth.uid())
    );

-- ── tournament_swiss_matches ─────────────────────────────────

-- Participants and host can view match details (but NOT room secrets)
CREATE POLICY "Match participants can view matches" ON tournament_swiss_matches
    FOR SELECT USING (
        -- Host of the tournament
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_swiss_matches.tournament_id AND host_user_id = auth.uid())
        -- Member of team A or team B
        OR EXISTS (
            SELECT 1 FROM team_members tm
            WHERE tm.team_id IN (tournament_swiss_matches.team_a_id, tournament_swiss_matches.team_b_id)
            AND tm.user_id = auth.uid()
        )
        -- Published tournament: anyone can see basic match info
        OR EXISTS (
            SELECT 1 FROM tournaments t
            WHERE t.id = tournament_swiss_matches.tournament_id
            AND t.status IN ('in_progress', 'completed')
        )
    );

-- Host can create matches (Swiss pairings)
CREATE POLICY "Hosts can create matches" ON tournament_swiss_matches
    FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_swiss_matches.tournament_id AND host_user_id = auth.uid())
    );

-- Host can update match results
CREATE POLICY "Hosts can update match results" ON tournament_swiss_matches
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_swiss_matches.tournament_id AND host_user_id = auth.uid())
    );

-- ── tournament_match_rosters ─────────────────────────────────

-- Team leader can view rosters for their team's matches
-- Host can view all rosters
CREATE POLICY "Match roster participants can view" ON tournament_match_rosters
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = (
            SELECT tournament_id FROM tournament_swiss_matches WHERE id = tournament_match_rosters.match_id
        ) AND host_user_id = auth.uid())
        OR EXISTS (
            SELECT 1 FROM team_members tm
            WHERE tm.team_id = tournament_match_rosters.team_id AND tm.user_id = auth.uid()
        )
    );

-- Team leader can set roster for their team
CREATE POLICY "Team leaders can set rosters" ON tournament_match_rosters
    FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM teams WHERE id = tournament_match_rosters.team_id AND leader_id = auth.uid())
        AND tournament_match_rosters.assigned_by = auth.uid()
    );

-- Team leader can update their own roster entries
CREATE POLICY "Team leaders can update rosters" ON tournament_match_rosters
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM teams WHERE id = tournament_match_rosters.team_id AND leader_id = auth.uid())
    );

-- ── tournament_match_room_secrets ────────────────────────────

-- STRICT: Only match participants (team leaders + host) can view room secrets
CREATE POLICY "Match participants can view room secrets" ON tournament_match_room_secrets
    FOR SELECT USING (
        -- Host of the tournament
        EXISTS (
            SELECT 1 FROM tournament_swiss_matches m
            JOIN tournaments t ON t.id = m.tournament_id
            WHERE m.id = tournament_match_room_secrets.match_id AND t.host_user_id = auth.uid()
        )
        -- Leader of team A or team B
        OR EXISTS (
            SELECT 1 FROM tournament_swiss_matches m
            WHERE m.id = tournament_match_room_secrets.match_id
            AND (m.team_a_id IN (SELECT id FROM teams WHERE leader_id = auth.uid())
                 OR m.team_b_id IN (SELECT id FROM teams WHERE leader_id = auth.uid()))
        )
    );

-- Host can drop room credentials
CREATE POLICY "Hosts can drop room secrets" ON tournament_match_room_secrets
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM tournament_swiss_matches m
            JOIN tournaments t ON t.id = m.tournament_id
            WHERE m.id = tournament_match_room_secrets.match_id AND t.host_user_id = auth.uid()
        )
        AND tournament_match_room_secrets.dropped_by = auth.uid()
    );

-- ── tournament_host_accounts ─────────────────────────────────

-- Host can view their own account info
CREATE POLICY "Hosts can view own account" ON tournament_host_accounts
    FOR SELECT USING (
        host_user_id = auth.uid()
        OR auth_user_id = auth.uid()
    );

-- Only service_role can insert (admin API route creates accounts)
-- No direct INSERT policy for anon-key users

-- ── tournament_player_stats ──────────────────────────────────

-- Anyone can view player stats for completed tournaments
CREATE POLICY "Anyone can view tournament player stats" ON tournament_player_stats
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM tournaments t
            WHERE t.id = tournament_player_stats.tournament_id
            AND t.status = 'completed'
        )
        OR user_id = auth.uid()
    );

-- ── conversation_participants ────────────────────────────────

-- Participants can view who's in their conversation
CREATE POLICY "Conversation participants can view members" ON conversation_participants
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = conversation_participants.conversation_id
            AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
        )
        OR EXISTS (
            SELECT 1 FROM conversation_participants cp
            WHERE cp.conversation_id = conversation_participants.conversation_id
            AND cp.user_id = auth.uid()
        )
    );

-- RPCs and host can add participants (handled via SECURITY DEFINER)
-- No direct INSERT policy — participants are added by RPC functions

-- ── Update existing conversation policies to support conversation_participants ──

-- Drop old conversation SELECT policy and replace with participant-aware one
DROP POLICY IF EXISTS "Conversation participants can view" ON conversations;
CREATE POLICY "Conversation participants can view" ON conversations
    FOR SELECT USING (
        participant_a_id = auth.uid()
        OR participant_b_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversation_participants cp
            WHERE cp.conversation_id = conversations.id AND cp.user_id = auth.uid()
        )
    );

-- Drop old conversation INSERT policy and replace
DROP POLICY IF EXISTS "Conversation participants can insert" ON conversations;
CREATE POLICY "Conversation participants can insert" ON conversations
    FOR INSERT WITH CHECK (
        participant_a_id = auth.uid()
        OR participant_b_id = auth.uid()
    );

-- Drop old conversation UPDATE policy and replace
DROP POLICY IF EXISTS "Conversation participants can update" ON conversations;
CREATE POLICY "Conversation participants can update" ON conversations
    FOR UPDATE USING (
        participant_a_id = auth.uid()
        OR participant_b_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversation_participants cp
            WHERE cp.conversation_id = conversations.id AND cp.user_id = auth.uid()
        )
    );

-- Update message policies to support conversation_participants
DROP POLICY IF EXISTS "Conversation members can view messages" ON messages;
CREATE POLICY "Conversation members can view messages" ON messages
    FOR SELECT USING (
        sender_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
        )
        OR EXISTS (
            SELECT 1 FROM conversation_participants cp
            JOIN conversations c ON c.id = cp.conversation_id
            WHERE c.id = messages.conversation_id AND cp.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "Conversation members can send messages" ON messages;
CREATE POLICY "Conversation members can send messages" ON messages
    FOR INSERT WITH CHECK (
        sender_id = auth.uid()
        AND (
            EXISTS (
                SELECT 1 FROM conversations c
                WHERE c.id = messages.conversation_id
                AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
            )
            OR EXISTS (
                SELECT 1 FROM conversation_participants cp
                WHERE cp.conversation_id = messages.conversation_id AND cp.user_id = auth.uid()
            )
        )
    );

-- ═══════════════════════════════════════════════════════════════
-- 16. RPC FUNCTIONS
-- ═══════════════════════════════════════════════════════════════

-- ── 16.1 apply_for_tournament ────────────────────────────────

CREATE OR REPLACE FUNCTION apply_for_tournament(
    p_tournament_id UUID,
    p_team_id UUID
)
RETURNS JSON AS $$
DECLARE
    v_tournament RECORD;
    v_team RECORD;
    v_member_count INTEGER;
    v_missing_telegram TEXT[];
    v_existing_status TEXT;
    v_rejection_count INTEGER;
    v_application_id UUID;
BEGIN
    -- Get tournament
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    -- Check tournament is in registration phase
    IF v_tournament.status != 'registration' THEN
        RETURN json_build_object('success', false, 'error', 'Tournament is not accepting applications');
    END IF;

    -- Check caller is team leader
    SELECT * INTO v_team FROM teams WHERE id = p_team_id AND leader_id = auth.uid();
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Only team leader can apply');
    END IF;

    -- Check team size
    SELECT COUNT(*) INTO v_member_count FROM team_members WHERE team_id = p_team_id;
    IF v_member_count < v_tournament.min_team_size THEN
        RETURN json_build_object('success', false, 'error',
            format('Team needs at least %s members (currently %s)', v_tournament.min_team_size, v_member_count));
    END IF;

    -- Check all team members have telegram_username
    SELECT array_agg(p.username) INTO v_missing_telegram
        FROM team_members tm
        JOIN profiles p ON p.id = tm.user_id
        WHERE tm.team_id = p_team_id AND (p.telegram_username IS NULL OR p.telegram_username = '');
    IF v_missing_telegram IS NOT NULL AND array_length(v_missing_telegram, 1) > 0 THEN
        RETURN json_build_object('success', false, 'error', 'missing_telegram_users',
            'missing_telegram_users', v_missing_telegram);
    END IF;

    -- Check if team already has an active application
    SELECT status INTO v_existing_status
        FROM tournament_applications
        WHERE tournament_id = p_tournament_id AND team_id = p_team_id;

    IF v_existing_status = 'pending' THEN
        RETURN json_build_object('success', false, 'error', 'Application already pending');
    END IF;

    IF v_existing_status = 'accepted' THEN
        RETURN json_build_object('success', false, 'error', 'Team already accepted');
    END IF;

    IF v_existing_status = 'blocked' THEN
        RETURN json_build_object('success', false, 'error', 'Team is blocked from this tournament');
    END IF;

    -- If previously rejected, check rejection count
    IF v_existing_status = 'rejected' THEN
        SELECT COUNT(*) INTO v_rejection_count
            FROM tournament_applications
            WHERE tournament_id = p_tournament_id AND team_id = p_team_id
              AND status IN ('rejected', 'blocked');
        IF v_rejection_count >= 3 THEN
            RETURN json_build_object('success', false, 'error', 'Team is blocked after 3 rejections');
        END IF;
    END IF;

    -- Check tournament capacity
    IF (SELECT COUNT(*) FROM tournament_teams WHERE tournament_id = p_tournament_id) >= v_tournament.max_teams THEN
        RETURN json_build_object('success', false, 'error', 'Tournament is full');
    END IF;

    -- Create or re-create application
    INSERT INTO tournament_applications (tournament_id, team_id, status, attempt_number)
        VALUES (p_tournament_id, p_team_id, 'pending',
            COALESCE((SELECT MAX(attempt_number) FROM tournament_applications
                      WHERE tournament_id = p_tournament_id AND team_id = p_team_id), 0) + 1)
        ON CONFLICT (tournament_id, team_id) DO UPDATE SET
            status = 'pending',
            rejection_reason = NULL,
            attempt_number = tournament_applications.attempt_number + 1,
            applied_at = TIMEZONE('utc', NOW()),
            reviewed_at = NULL,
            reviewed_by = NULL
        RETURNING id INTO v_application_id;

    -- Notify tournament host
    INSERT INTO app_notifications (user_id, type, title, message, action_id)
        VALUES (v_tournament.host_user_id, 'TOURNAMENT_APPLICATION_NEW',
            'New Tournament Application',
            format('Team %s applied to your tournament %s', v_team.name, v_tournament.title),
            p_tournament_id::TEXT);

    RETURN json_build_object('success', true, 'application_id', v_application_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.2 review_tournament_application ────────────────────────

CREATE OR REPLACE FUNCTION review_tournament_application(
    p_application_id UUID,
    p_decision TEXT,
    p_rejection_reason TEXT DEFAULT NULL
)
RETURNS JSON AS $$
DECLARE
    v_application RECORD;
    v_tournament RECORD;
    v_team RECORD;
    v_rejection_count INTEGER;
BEGIN
    -- Validate decision
    IF p_decision NOT IN ('accepted', 'rejected') THEN
        RETURN json_build_object('success', false, 'error', 'Invalid decision');
    END IF;

    -- Get application
    SELECT * INTO v_application FROM tournament_applications WHERE id = p_application_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Application not found');
    END IF;

    -- Verify caller is tournament host
    SELECT * INTO v_tournament FROM tournaments WHERE id = v_application.tournament_id;
    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can review applications');
    END IF;

    -- Verify application is pending
    IF v_application.status != 'pending' THEN
        RETURN json_build_object('success', false, 'error', 'Application is not pending');
    END IF;

    -- Get team info
    SELECT * INTO v_team FROM teams WHERE id = v_application.team_id;

    IF p_decision = 'accepted' THEN
        -- Accept the application
        UPDATE tournament_applications SET
            status = 'accepted',
            reviewed_at = TIMEZONE('utc', NOW()),
            reviewed_by = auth.uid()
            WHERE id = p_application_id;

        -- Add team to tournament_teams
        INSERT INTO tournament_teams (tournament_id, team_id)
            VALUES (v_application.tournament_id, v_application.team_id)
            ON CONFLICT (tournament_id, team_id) DO NOTHING;

        -- Notify team leader
        INSERT INTO app_notifications (user_id, type, title, message, action_id)
            VALUES (v_team.leader_id, 'TOURNAMENT_APPLICATION_ACCEPTED',
                'Application Accepted!',
                format('Your team %s has been accepted to %s!', v_team.name, v_tournament.title),
                v_tournament.id::TEXT);

    ELSE
        -- Reject the application
        UPDATE tournament_applications SET
            status = 'rejected',
            rejection_reason = p_rejection_reason,
            reviewed_at = TIMEZONE('utc', NOW()),
            reviewed_by = auth.uid()
            WHERE id = p_application_id;

        -- Check rejection count for auto-block (trigger handles this, but we also notify)
        SELECT COUNT(*) INTO v_rejection_count
            FROM tournament_applications
            WHERE tournament_id = v_application.tournament_id
              AND team_id = v_application.team_id
              AND status IN ('rejected', 'blocked');

        -- Notify team leader
        IF v_rejection_count >= 3 THEN
            INSERT INTO app_notifications (user_id, type, title, message, action_id)
                VALUES (v_team.leader_id, 'TOURNAMENT_APPLICATION_BLOCKED',
                    'Application Blocked',
                    format('Your team %s has been blocked from %s after 3 rejections.', v_team.name, v_tournament.title),
                    v_tournament.id::TEXT);
        ELSE
            INSERT INTO app_notifications (user_id, type, title, message, action_id)
                VALUES (v_team.leader_id, 'TOURNAMENT_APPLICATION_REJECTED',
                    'Application Rejected',
                    format('Your team %s was rejected from %s. Reason: %s',
                        v_team.name, v_tournament.title, COALESCE(p_rejection_reason, 'Not specified')),
                    v_tournament.id::TEXT);
        END IF;
    END IF;

    RETURN json_build_object('success', true, 'decision', p_decision);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.3 generate_swiss_pairings ─────────────────────────────

CREATE OR REPLACE FUNCTION generate_swiss_pairings(
    p_tournament_id UUID
)
RETURNS JSON AS $$
DECLARE
    v_tournament RECORD;
    v_new_round INTEGER;
    v_team_record RECORD;
    v_team_ids UUID[];
    v_paired BOOLEAN[];
    v_match_num INTEGER;
    v_match_id UUID;
    v_conversation_id UUID;
    v_team_a_leader UUID;
    v_team_b_leader UUID;
    v_found_match BOOLEAN;
    v_j INTEGER;
    v_bye_team_idx INTEGER;
    v_bye_team_id UUID;
    v_matches_created INTEGER := 0;
    v_byes_created INTEGER := 0;
BEGIN
    -- Get tournament
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    -- Verify caller is host
    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can generate pairings');
    END IF;

    -- Verify tournament is in check_in or in_progress
    IF v_tournament.status NOT IN ('check_in', 'in_progress') THEN
        RETURN json_build_object('success', false, 'error', 'Tournament must be in check-in or in-progress phase');
    END IF;

    -- Get checked-in teams
    SELECT array_agg(tt.team_id ORDER BY
        CASE WHEN v_tournament.current_round = 0 THEN RANDOM() ELSE 0 END,
        tt.swiss_points DESC,
        tt.buchholz_score DESC
    ) INTO v_team_ids
    FROM tournament_teams tt
    WHERE tt.tournament_id = p_tournament_id AND tt.checked_in = TRUE AND tt.is_disqualified = FALSE;

    IF v_team_ids IS NULL OR array_length(v_team_ids, 1) < 4 THEN
        RETURN json_build_object('success', false, 'error', 'Need at least 4 checked-in teams for Swiss pairings');
    END IF;

    -- Check if current round already has matches
    v_new_round := v_tournament.current_round + 1;

    IF v_new_round > COALESCE(v_tournament.swiss_rounds, CEIL(LOG(2, v_tournament.max_teams))::INTEGER) THEN
        RETURN json_build_object('success', false, 'error', 'All Swiss rounds have been played');
    END IF;

    -- Initialize paired array
    v_paired := array_fill(FALSE, ARRAY[array_length(v_team_ids, 1)]);

    v_match_num := 0;

    -- Pair teams
    FOR i IN 1..array_length(v_team_ids, 1) LOOP
        IF v_paired[i] THEN CONTINUE; END IF;

        v_found_match := FALSE;

        -- Find opponent: not already paired, not played before
        FOR j IN i+1..array_length(v_team_ids, 1) LOOP
            IF v_paired[j] THEN CONTINUE; END IF;

            -- Check no rematch
            IF NOT EXISTS (
                SELECT 1 FROM tournament_swiss_matches m
                WHERE m.tournament_id = p_tournament_id
                  AND (
                    (m.team_a_id = v_team_ids[i] AND m.team_b_id = v_team_ids[j])
                    OR (m.team_a_id = v_team_ids[j] AND m.team_b_id = v_team_ids[i])
                  )
            ) THEN
                -- Found valid opponent
                v_match_num := v_match_num + 1;

                -- Create conversation for match chat
                SELECT t.leader_id INTO v_team_a_leader FROM teams t WHERE t.id = v_team_ids[i];
                SELECT t.leader_id INTO v_team_b_leader FROM teams t WHERE t.id = v_team_ids[j];

                INSERT INTO conversations (tournament_match_id, participant_a_id, participant_a_name,
                    participant_a_team_id, participant_b_id, participant_b_name, participant_b_team_id)
                VALUES (NULL, v_team_a_leader,
                    (SELECT username FROM profiles WHERE id = v_team_a_leader),
                    v_team_ids[i],
                    v_team_b_leader,
                    (SELECT username FROM profiles WHERE id = v_team_b_leader),
                    v_team_ids[j]
                ) RETURNING id INTO v_conversation_id;

                -- Add 3 participants via conversation_participants
                INSERT INTO conversation_participants (conversation_id, user_id, role) VALUES
                    (v_conversation_id, v_team_a_leader, 'team_a_leader'),
                    (v_conversation_id, v_team_b_leader, 'team_b_leader'),
                    (v_conversation_id, v_tournament.host_user_id, 'host');

                -- Create match
                INSERT INTO tournament_swiss_matches (
                    tournament_id, round_number, match_number,
                    team_a_id, team_b_id, conversation_id, status
                ) VALUES (
                    p_tournament_id, v_new_round, v_match_num,
                    v_team_ids[i], v_team_ids[j], v_conversation_id, 'scheduled'
                ) RETURNING id INTO v_match_id;

                -- Update conversation with match reference
                UPDATE conversations SET tournament_match_id = v_match_id WHERE id = v_conversation_id;

                v_paired[i] := TRUE;
                v_paired[j] := TRUE;
                v_found_match := TRUE;
                v_matches_created := v_matches_created + 1;
                EXIT; -- found opponent for team i
            END IF;
        END LOOP;

        -- If no opponent found, give bye
        IF NOT v_found_match THEN
            v_match_num := v_match_num + 1;

            INSERT INTO tournament_swiss_matches (
                tournament_id, round_number, match_number,
                team_a_id, team_b_id, status, is_draw, winner_team_id
            ) VALUES (
                p_tournament_id, v_new_round, v_match_num,
                v_team_ids[i], NULL, 'bye', FALSE, v_team_ids[i]
            );

            -- Auto-award bye win points
            UPDATE tournament_teams SET
                swiss_wins = swiss_wins + 1,
                swiss_points = swiss_points + 3
                WHERE tournament_id = p_tournament_id AND team_id = v_team_ids[i];

            v_paired[i] := TRUE;
            v_byes_created := v_byes_created + 1;
        END IF;
    END LOOP;

    -- Update tournament round and status
    UPDATE tournaments SET
        current_round = v_new_round,
        status = 'in_progress',
        updated_at = TIMEZONE('utc', NOW())
        WHERE id = p_tournament_id;

    -- Notify all team leaders of their match
    INSERT INTO app_notifications (user_id, type, title, message, action_id)
        SELECT t.leader_id, 'TOURNAMENT_ROUND_ADVANCED',
            'New Round Generated!',
            format('Round %s of %s has been generated. Check your match!', v_new_round, v_tournament.title),
            p_tournament_id::TEXT
        FROM tournament_teams tt
        JOIN teams t ON t.id = tt.team_id
        WHERE tt.tournament_id = p_tournament_id AND tt.checked_in = TRUE AND tt.is_disqualified = FALSE;

    RETURN json_build_object(
        'success', true,
        'round', v_new_round,
        'matches_created', v_matches_created,
        'byes_created', v_byes_created
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.4 set_tournament_match_roster ─────────────────────────

CREATE OR REPLACE FUNCTION set_tournament_match_roster(
    p_match_id UUID,
    p_team_id UUID,
    p_game_number INTEGER,
    p_player_ids UUID[]
)
RETURNS JSON AS $$
DECLARE
    v_match RECORD;
    v_best_of INTEGER;
    v_min_team_size INTEGER;
    v_roster_size INTEGER;
BEGIN
    -- Get match
    SELECT * INTO v_match FROM tournament_swiss_matches WHERE id = p_match_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Match not found');
    END IF;

    -- Verify caller is team leader
    IF NOT EXISTS (SELECT 1 FROM teams WHERE id = p_team_id AND leader_id = auth.uid()) THEN
        RETURN json_build_object('success', false, 'error', 'Only team leader can set roster');
    END IF;

    -- Verify team is in this match
    IF v_match.team_a_id != p_team_id AND v_match.team_b_id != p_team_id THEN
        RETURN json_build_object('success', false, 'error', 'Team is not in this match');
    END IF;

    -- Get tournament settings
    SELECT best_of, min_team_size INTO v_best_of, v_min_team_size
        FROM tournaments WHERE id = v_match.tournament_id;

    -- Verify game number is valid for this tournament's best_of
    IF p_game_number < 1 OR p_game_number > COALESCE(v_best_of, 1) THEN
        RETURN json_build_object('success', false, 'error', 'Invalid game number');
    END IF;

    -- Verify roster size
    v_roster_size := array_length(p_player_ids, 1);
    IF v_roster_size IS NULL OR v_roster_size < v_min_team_size THEN
        RETURN json_build_object('success', false, 'error',
            format('Roster must have at least %s players', v_min_team_size));
    END IF;

    -- Verify all players are team members
    IF EXISTS (
        SELECT unnest(p_player_ids) AS pid
        EXCEPT
        SELECT user_id FROM team_members WHERE team_id = p_team_id
    ) THEN
        RETURN json_build_object('success', false, 'error', 'All roster players must be team members');
    END IF;

    -- Delete existing roster for this match/team/game
    DELETE FROM tournament_match_rosters
        WHERE match_id = p_match_id AND team_id = p_team_id AND game_number = p_game_number;

    -- Insert new roster
    INSERT INTO tournament_match_rosters (match_id, team_id, user_id, game_number, is_active, assigned_by)
        SELECT p_match_id, p_team_id, unnest(p_player_ids), p_game_number, TRUE, auth.uid();

    -- Notify team members
    INSERT INTO app_notifications (user_id, type, title, message, action_id)
        SELECT unnest(p_player_ids), 'TOURNAMENT_ROSTER_LOCKED',
            'Roster Locked',
            'You have been selected for the tournament match roster.',
            p_match_id::TEXT;

    RETURN json_build_object('success', true, 'roster_size', v_roster_size);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.5 submit_tournament_match_result ──────────────────────

CREATE OR REPLACE FUNCTION submit_tournament_match_result(
    p_match_id UUID,
    p_winner_team_id UUID,
    p_is_draw BOOLEAN DEFAULT FALSE,
    p_game_a_score INTEGER DEFAULT 0,
    p_game_b_score INTEGER DEFAULT 0
)
RETURNS JSON AS $$
DECLARE
    v_match RECORD;
    v_tournament RECORD;
    v_team_a_name TEXT;
    v_team_b_name TEXT;
BEGIN
    -- Get match
    SELECT * INTO v_match FROM tournament_swiss_matches WHERE id = p_match_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Match not found');
    END IF;

    -- Verify caller is tournament host
    SELECT * INTO v_tournament FROM tournaments WHERE id = v_match.tournament_id;
    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can submit results');
    END IF;

    -- Verify match is in progress or scheduled
    IF v_match.status NOT IN ('scheduled', 'in_progress') THEN
        RETURN json_build_object('success', false, 'error', 'Match is not in a submittable state');
    END IF;

    -- Validate winner
    IF NOT p_is_draw THEN
        IF p_winner_team_id IS NULL THEN
            RETURN json_build_object('success', false, 'error', 'Winner team ID required for non-draw result');
        END IF;
        IF p_winner_team_id != v_match.team_a_id AND p_winner_team_id != v_match.team_b_id THEN
            RETURN json_build_object('success', false, 'error', 'Winner must be one of the match teams');
        END IF;
    END IF;

    -- Update match result
    UPDATE tournament_swiss_matches SET
        status = 'completed',
        winner_team_id = p_winner_team_id,
        is_draw = p_is_draw,
        game_a_score = p_game_a_score,
        game_b_score = p_game_b_score,
        result_submitted_at = TIMEZONE('utc', NOW()),
        result_submitted_by = auth.uid(),
        updated_at = TIMEZONE('utc', NOW())
        WHERE id = p_match_id;

    -- Update Swiss scores
    IF p_is_draw THEN
        -- Draw: 1 point each (BO2 only)
        UPDATE tournament_teams SET
            swiss_draws = swiss_draws + 1,
            swiss_points = swiss_points + 1
            WHERE tournament_id = v_match.tournament_id AND team_id = v_match.team_a_id;
        UPDATE tournament_teams SET
            swiss_draws = swiss_draws + 1,
            swiss_points = swiss_points + 1
            WHERE tournament_id = v_match.tournament_id AND team_id = v_match.team_b_id;
    ELSE
        -- Winner gets 3 points, loser gets 0
        UPDATE tournament_teams SET
            swiss_wins = swiss_wins + 1,
            swiss_points = swiss_points + 3
            WHERE tournament_id = v_match.tournament_id AND team_id = p_winner_team_id;

        UPDATE tournament_teams SET
            swiss_losses = swiss_losses + 1
            WHERE tournament_id = v_match.tournament_id
              AND team_id IN (v_match.team_a_id, v_match.team_b_id)
              AND team_id != p_winner_team_id;
    END IF;

    -- Recalculate tiebreakers
    PERFORM recalculate_tiebreakers(v_match.tournament_id);

    -- Award points to active roster players
    PERFORM award_tournament_match_points(p_match_id, p_winner_team_id, p_is_draw);

    -- Notify both teams
    SELECT name INTO v_team_a_name FROM teams WHERE id = v_match.team_a_id;
    SELECT name INTO v_team_b_name FROM teams WHERE id = v_match.team_b_id;

    INSERT INTO app_notifications (user_id, type, title, message, action_id)
        SELECT t.leader_id, 'TOURNAMENT_MATCH_RESULT',
            'Match Result',
            format('Match %s vs %s: %s',
                v_team_a_name, v_team_b_name,
                CASE WHEN p_is_draw THEN 'Draw!'
                     ELSE format('%s wins!', (SELECT name FROM teams WHERE id = p_winner_team_id))
                END),
            v_match.tournament_id::TEXT
        FROM teams t
        WHERE t.id IN (v_match.team_a_id, v_match.team_b_id);

    RETURN json_build_object('success', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.6 award_tournament_match_points ───────────────────────

CREATE OR REPLACE FUNCTION award_tournament_match_points(
    p_match_id UUID,
    p_winner_team_id UUID,
    p_is_draw BOOLEAN DEFAULT FALSE
)
RETURNS VOID AS $$
DECLARE
    v_roster_entry RECORD;
    v_is_winner BOOLEAN;
    v_tournament_id UUID;
BEGIN
    -- Get tournament_id from match
    SELECT tournament_id INTO v_tournament_id FROM tournament_swiss_matches WHERE id = p_match_id;

    -- Award points to each active roster player
    FOR v_roster_entry IN
        SELECT mr.user_id, mr.team_id, mr.game_number
        FROM tournament_match_rosters mr
        WHERE mr.match_id = p_match_id AND mr.is_active = TRUE
    LOOP
        v_is_winner := (v_roster_entry.team_id = p_winner_team_id);

        INSERT INTO tournament_player_stats (tournament_id, user_id, team_id,
            matches_won, matches_lost, matches_drawn, points_earned)
        VALUES (
            v_tournament_id,
            v_roster_entry.user_id,
            v_roster_entry.team_id,
            CASE WHEN v_is_winner THEN 1 ELSE 0 END,
            CASE WHEN v_is_winner THEN 0 WHEN p_is_draw THEN 0 ELSE 1 END,
            CASE WHEN p_is_draw THEN 1 ELSE 0 END,
            CASE WHEN v_is_winner THEN 25 WHEN p_is_draw THEN 10 ELSE 5 END
        )
        ON CONFLICT (tournament_id, user_id) DO UPDATE SET
            matches_won = tournament_player_stats.matches_won + CASE WHEN v_is_winner THEN 1 ELSE 0 END,
            matches_lost = tournament_player_stats.matches_lost + CASE WHEN NOT v_is_winner AND NOT p_is_draw THEN 1 ELSE 0 END,
            matches_drawn = tournament_player_stats.matches_drawn + CASE WHEN p_is_draw THEN 1 ELSE 0 END,
            points_earned = tournament_player_stats.points_earned + CASE WHEN v_is_winner THEN 25 WHEN p_is_draw THEN 10 ELSE 5 END;
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.7 update_tournament_scores ────────────────────────────

CREATE OR REPLACE FUNCTION update_tournament_scores(p_tournament_id UUID)
RETURNS VOID AS $$
BEGIN
    -- Recalculate all team scores from match results
    UPDATE tournament_teams tt SET
        swiss_wins = (
            SELECT COUNT(*) FROM tournament_swiss_matches m
            WHERE m.tournament_id = p_tournament_id
              AND m.status = 'completed'
              AND m.winner_team_id = tt.team_id AND NOT m.is_draw
        ),
        swiss_losses = (
            SELECT COUNT(*) FROM tournament_swiss_matches m
            WHERE m.tournament_id = p_tournament_id
              AND m.status = 'completed'
              AND m.winner_team_id IS NOT NULL AND m.winner_team_id != tt.team_id
              AND (m.team_a_id = tt.team_id OR m.team_b_id = tt.team_id)
              AND NOT m.is_draw
        ),
        swiss_draws = (
            SELECT COUNT(*) FROM tournament_swiss_matches m
            WHERE m.tournament_id = p_tournament_id
              AND m.status = 'completed'
              AND m.is_draw
              AND (m.team_a_id = tt.team_id OR m.team_b_id = tt.team_id)
        ),
        swiss_points = (
            SELECT COALESCE(SUM(
                CASE
                    WHEN m.winner_team_id = tt.team_id AND NOT m.is_draw THEN 3
                    WHEN m.is_draw THEN 1
                    ELSE 0
                END
            ), 0)
            FROM tournament_swiss_matches m
            WHERE m.tournament_id = p_tournament_id
              AND m.status = 'completed'
              AND (m.team_a_id = tt.team_id OR m.team_b_id = tt.team_id)
        )
    WHERE tt.tournament_id = p_tournament_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.8 recalculate_tiebreakers ─────────────────────────────

CREATE OR REPLACE FUNCTION recalculate_tiebreakers(p_tournament_id UUID)
RETURNS VOID AS $$
BEGIN
    -- Buchholz: sum of all opponents' swiss_points
    UPDATE tournament_teams tt SET
        buchholz_score = (
            SELECT COALESCE(SUM(opp.swiss_points), 0)
            FROM tournament_swiss_matches m
            JOIN tournament_teams opp ON opp.tournament_id = p_tournament_id
                AND (opp.team_id = m.team_a_id OR opp.team_id = m.team_b_id)
                AND opp.team_id != tt.team_id
            WHERE m.tournament_id = p_tournament_id
              AND m.status = 'completed'
              AND (m.team_a_id = tt.team_id OR m.team_b_id = tt.team_id)
        )
    WHERE tt.tournament_id = p_tournament_id;

    -- Sonneborn-Berger: sum of (defeated opponents' points * 1) + (drawn opponents' points * 0.5)
    UPDATE tournament_teams tt SET
        sonneborn_berger = (
            SELECT COALESCE(SUM(
                CASE
                    WHEN m.winner_team_id = tt.team_id AND NOT m.is_draw THEN opp.swiss_points
                    WHEN m.is_draw THEN opp.swiss_points * 0.5
                    ELSE 0
                END
            ), 0)
            FROM tournament_swiss_matches m
            JOIN tournament_teams opp ON opp.tournament_id = p_tournament_id
                AND (opp.team_id = m.team_a_id OR opp.team_id = m.team_b_id)
                AND opp.team_id != tt.team_id
            WHERE m.tournament_id = p_tournament_id
              AND m.status = 'completed'
              AND (m.team_a_id = tt.team_id OR m.team_b_id = tt.team_id)
        )
    WHERE tt.tournament_id = p_tournament_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.9 disqualify_tournament_team ──────────────────────────

CREATE OR REPLACE FUNCTION disqualify_tournament_team(
    p_tournament_id UUID,
    p_team_id UUID,
    p_reason TEXT DEFAULT NULL
)
RETURNS JSON AS $$
DECLARE
    v_tournament RECORD;
    v_team RECORD;
    v_active_match_id UUID;
    v_opponent_team_id UUID;
BEGIN
    -- Get tournament
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    -- Verify caller is host
    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can disqualify teams');
    END IF;

    -- Get team
    SELECT * INTO v_team FROM teams WHERE id = p_team_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Team not found');
    END IF;

    -- Disqualify the team
    UPDATE tournament_teams SET
        is_disqualified = TRUE,
        disqualification_reason = p_reason,
        disqualified_at = TIMEZONE('utc', NOW()),
        disqualified_by = auth.uid()
        WHERE tournament_id = p_tournament_id AND team_id = p_team_id;

    -- If team has an active (scheduled/in_progress) match, auto-win for opponent
    SELECT id, CASE WHEN team_a_id = p_team_id THEN team_b_id ELSE team_a_id END
        INTO v_active_match_id, v_opponent_team_id
        FROM tournament_swiss_matches
        WHERE tournament_id = p_tournament_id
          AND (team_a_id = p_team_id OR team_b_id = p_team_id)
          AND status IN ('scheduled', 'in_progress')
        LIMIT 1;

    IF v_active_match_id IS NOT NULL AND v_opponent_team_id IS NOT NULL THEN
        -- Auto-complete the match in favor of the opponent
        UPDATE tournament_swiss_matches SET
            status = 'completed',
            winner_team_id = v_opponent_team_id,
            result_submitted_at = TIMEZONE('utc', NOW()),
            result_submitted_by = auth.uid(),
            updated_at = TIMEZONE('utc', NOW())
            WHERE id = v_active_match_id;

        -- Award points to opponent
        UPDATE tournament_teams SET
            swiss_wins = swiss_wins + 1,
            swiss_points = swiss_points + 3
            WHERE tournament_id = p_tournament_id AND team_id = v_opponent_team_id;

        -- Recalculate tiebreakers
        PERFORM recalculate_tiebreakers(p_tournament_id);
    END IF;

    -- Notify team leader
    INSERT INTO app_notifications (user_id, type, title, message, action_id)
        VALUES (v_team.leader_id, 'TOURNAMENT_TEAM_DISQUALIFIED',
            'Team Disqualified',
            format('Your team %s has been disqualified from %s. Reason: %s',
                v_team.name, v_tournament.title, COALESCE(p_reason, 'Not specified')),
            p_tournament_id::TEXT);

    RETURN json_build_object('success', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.10 check_tournament_no_shows ──────────────────────────

CREATE OR REPLACE FUNCTION check_tournament_no_shows(p_tournament_id UUID)
RETURNS JSON AS $$
DECLARE
    v_count INTEGER := 0;
    v_match RECORD;
BEGIN
    -- Find overdue matches (past auto_complete_at, still not completed)
    FOR v_match IN
        SELECT id, team_a_id, team_b_id
        FROM tournament_swiss_matches
        WHERE tournament_id = p_tournament_id
          AND status IN ('scheduled', 'in_progress')
          AND match_auto_complete_at IS NOT NULL
          AND match_auto_complete_at < TIMEZONE('utc', NOW())
    LOOP
        -- Mark as cancelled (no winner — no-show, no points awarded)
        UPDATE tournament_swiss_matches SET
            status = 'cancelled',
            updated_at = TIMEZONE('utc', NOW())
            WHERE id = v_match.id;

        v_count := v_count + 1;
    END LOOP;

    RETURN json_build_object('success', true, 'auto_completed_count', v_count);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.11 cancel_tournament ──────────────────────────────────

CREATE OR REPLACE FUNCTION cancel_tournament(
    p_tournament_id UUID,
    p_cancellation_reason TEXT DEFAULT NULL
)
RETURNS JSON AS $$
DECLARE
    v_tournament RECORD;
BEGIN
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    -- Verify caller is host
    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can cancel');
    END IF;

    -- Cannot cancel completed tournament
    IF v_tournament.status = 'completed' THEN
        RETURN json_build_object('success', false, 'error', 'Cannot cancel a completed tournament');
    END IF;

    -- Update tournament status
    UPDATE tournaments SET
        status = 'cancelled',
        cancelled_at = TIMEZONE('utc', NOW()),
        cancellation_reason = p_cancellation_reason,
        updated_at = TIMEZONE('utc', NOW())
        WHERE id = p_tournament_id;

    -- Update host reputation
    UPDATE profiles SET
        tournaments_cancelled = tournaments_cancelled + 1,
        host_trust_score = GREATEST(host_trust_score - 0.5, 1.0)
        WHERE id = v_tournament.host_user_id;

    -- Notify all accepted teams
    INSERT INTO app_notifications (user_id, type, title, message, action_id)
        SELECT t.leader_id, 'TOURNAMENT_CANCELLED',
            'Tournament Cancelled',
            format('%s has been cancelled. Reason: %s', v_tournament.title, COALESCE(p_cancellation_reason, 'Not specified')),
            p_tournament_id::TEXT
        FROM tournament_teams tt
        JOIN teams t ON t.id = tt.team_id
        WHERE tt.tournament_id = p_tournament_id;

    RETURN json_build_object('success', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.12 complete_tournament ─────────────────────────────────

CREATE OR REPLACE FUNCTION complete_tournament(p_tournament_id UUID)
RETURNS JSON AS $$
DECLARE
    v_tournament RECORD;
    v_placement INTEGER := 1;
    v_team_record RECORD;
BEGIN
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    -- Verify caller is host
    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can complete tournament');
    END IF;

    -- Verify all matches are completed or cancelled
    IF EXISTS (
        SELECT 1 FROM tournament_swiss_matches
        WHERE tournament_id = p_tournament_id
          AND status IN ('scheduled', 'in_progress', 'disputed')
    ) THEN
        RETURN json_build_object('success', false, 'error', 'All matches must be completed first');
    END IF;

    -- Set placements based on Swiss standings
    FOR v_team_record IN
        SELECT tt.team_id, tt.swiss_points, tt.buchholz_score, tt.sonneborn_berger,
               tt.swiss_wins, tt.swiss_losses, tt.swiss_draws
            FROM tournament_teams tt
            WHERE tt.tournament_id = p_tournament_id AND tt.is_disqualified = FALSE
            ORDER BY tt.swiss_points DESC, tt.buchholz_score DESC, tt.sonneborn_berger DESC
    LOOP
        UPDATE tournament_teams SET final_placement = v_placement
            WHERE tournament_id = p_tournament_id AND team_id = v_team_record.team_id;

        -- Create player stats for each team member
        INSERT INTO tournament_player_stats (tournament_id, user_id, team_id, placement,
            matches_won, matches_lost, matches_drawn)
            SELECT p_tournament_id, tm.user_id, v_team_record.team_id, v_placement,
                v_team_record.swiss_wins, v_team_record.swiss_losses, v_team_record.swiss_draws
            FROM team_members tm WHERE tm.team_id = v_team_record.team_id
            ON CONFLICT (tournament_id, user_id) DO UPDATE SET
                placement = v_placement,
                matches_won = v_team_record.swiss_wins,
                matches_lost = v_team_record.swiss_losses,
                matches_drawn = v_team_record.swiss_draws;

        v_placement := v_placement + 1;
    END LOOP;

    -- Mark disqualified teams with last placement
    UPDATE tournament_teams SET final_placement = v_placement
        WHERE tournament_id = p_tournament_id AND is_disqualified = TRUE AND final_placement IS NULL;

    -- Update tournament status
    UPDATE tournaments SET
        status = 'completed',
        completed_at = TIMEZONE('utc', NOW()),
        updated_at = TIMEZONE('utc', NOW())
        WHERE id = p_tournament_id;

    -- Update host reputation
    UPDATE profiles SET
        tournaments_completed = tournaments_completed + 1,
        tournaments_hosted = tournaments_hosted + 1,
        host_trust_score = LEAST(host_trust_score + 0.3, 10.0)
        WHERE id = v_tournament.host_user_id;

    -- Notify all participants
    INSERT INTO app_notifications (user_id, type, title, message, action_id)
        SELECT t.leader_id, 'TOURNAMENT_COMPLETED',
            'Tournament Completed!',
            format('%s has finished! Check final standings.', v_tournament.title),
            p_tournament_id::TEXT
        FROM tournament_teams tt
        JOIN teams t ON t.id = tt.team_id
        WHERE tt.tournament_id = p_tournament_id;

    RETURN json_build_object('success', true, 'placements_set', v_placement - 1);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 16.13 check_in_tournament_team ─────────────────────────────

CREATE OR REPLACE FUNCTION check_in_tournament_team(
    p_tournament_id UUID,
    p_team_id UUID
)
RETURNS JSON AS $$
DECLARE
    v_tournament RECORD;
    v_is_member BOOLEAN;
BEGIN
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    -- Tournament must be in check_in phase
    IF v_tournament.status != 'check_in' THEN
        RETURN json_build_object('success', false, 'error', 'Tournament is not in check-in phase');
    END IF;

    -- Caller must be team leader
    IF NOT EXISTS (SELECT 1 FROM teams WHERE id = p_team_id AND leader_id = auth.uid()) THEN
        RETURN json_build_object('success', false, 'error', 'Only team leader can check in');
    END IF;

    -- Team must be accepted (in tournament_teams)
    IF NOT EXISTS (SELECT 1 FROM tournament_teams WHERE tournament_id = p_tournament_id AND team_id = p_team_id) THEN
        RETURN json_build_object('success', false, 'error', 'Team is not in this tournament');
    END IF;

    -- Check in
    UPDATE tournament_teams SET
        checked_in = TRUE,
        checked_in_at = TIMEZONE('utc', NOW())
        WHERE tournament_id = p_tournament_id AND team_id = p_team_id;

    RETURN json_build_object('success', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════
-- 17. TRIGGERS & CONSTRAINTS
-- ═══════════════════════════════════════════════════════════════

-- ── 17.1 One tournament per host per 7 days ──────────────────

CREATE OR REPLACE FUNCTION enforce_weekly_tournament_limit()
RETURNS TRIGGER AS $$
DECLARE
    v_recent_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_recent_count
        FROM tournaments
        WHERE host_user_id = NEW.host_user_id
          AND status != 'cancelled'
          AND created_at > TIMEZONE('utc', NOW()) - INTERVAL '7 days';

    IF v_recent_count >= 1 THEN
        RAISE EXCEPTION 'Tournament host can only create 1 tournament per 7 days';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_weekly_tournament_limit ON tournaments;
CREATE TRIGGER trg_weekly_tournament_limit
    BEFORE INSERT ON tournaments
    FOR EACH ROW
    EXECUTE FUNCTION enforce_weekly_tournament_limit();

-- ── 17.2 Auto-calculate swiss_rounds on insert ──────────────

CREATE OR REPLACE FUNCTION auto_calculate_swiss_rounds()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.swiss_rounds IS NULL THEN
        NEW.swiss_rounds := CEIL(LOG(2, GREATEST(NEW.max_teams, 4)))::INTEGER;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_swiss_rounds ON tournaments;
CREATE TRIGGER trg_swiss_rounds
    BEFORE INSERT ON tournaments
    FOR EACH ROW
    EXECUTE FUNCTION auto_calculate_swiss_rounds();

-- ── 17.3 Max 15 requirements per tournament, max 5 telegram_subscribe ──

CREATE OR REPLACE FUNCTION enforce_requirement_limits()
RETURNS TRIGGER AS $$
DECLARE
    v_total_count INTEGER;
    v_telegram_count INTEGER;
BEGIN
    SELECT COUNT(*), COUNT(*) FILTER (WHERE type = 'telegram_subscribe')
        INTO v_total_count, v_telegram_count
        FROM tournament_requirements
        WHERE tournament_id = NEW.tournament_id;

    IF v_total_count >= 15 THEN
        RAISE EXCEPTION 'Maximum 15 requirements per tournament';
    END IF;

    IF NEW.type = 'telegram_subscribe' AND v_telegram_count >= 5 THEN
        RAISE EXCEPTION 'Maximum 5 Telegram subscribe requirements per tournament';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_requirement_limits ON tournament_requirements;
CREATE TRIGGER trg_requirement_limits
    BEFORE INSERT ON tournament_requirements
    FOR EACH ROW
    EXECUTE FUNCTION enforce_requirement_limits();

-- ── 17.4 Auto-block after 3 rejections ──────────────────────

CREATE OR REPLACE FUNCTION auto_block_after_3_rejections()
RETURNS TRIGGER AS $$
DECLARE
    v_rejection_count INTEGER;
BEGIN
    IF NEW.status = 'rejected' THEN
        SELECT COUNT(*) INTO v_rejection_count
            FROM tournament_applications
            WHERE tournament_id = NEW.tournament_id
              AND team_id = NEW.team_id
              AND status IN ('rejected', 'blocked');

        IF v_rejection_count >= 3 THEN
            NEW.status := 'blocked';
            NEW.rejection_reason := COALESCE(NEW.rejection_reason, '') || ' [AUTO-BLOCKED: 3 rejections]';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auto_block_3_rejections ON tournament_applications;
CREATE TRIGGER trg_auto_block_3_rejections
    BEFORE UPDATE ON tournament_applications
    FOR EACH ROW
    WHEN (NEW.status = 'rejected' AND OLD.status = 'pending')
    EXECUTE FUNCTION auto_block_after_3_rejections();

-- ── 17.5 Set match_auto_complete_at when scheduled_at is set ──

CREATE OR REPLACE FUNCTION set_match_auto_complete()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.scheduled_at IS NOT NULL AND NEW.match_auto_complete_at IS NULL THEN
        NEW.match_auto_complete_at := NEW.scheduled_at + (NEW.no_show_grace_period_min || ' minutes')::INTERVAL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_match_auto_complete ON tournament_swiss_matches;
CREATE TRIGGER trg_match_auto_complete
    BEFORE INSERT OR UPDATE ON tournament_swiss_matches
    FOR EACH ROW
    WHEN (NEW.scheduled_at IS NOT NULL)
    EXECUTE FUNCTION set_match_auto_complete();

-- ── 17.6 Host trust score range constraint ───────────────────

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'valid_host_trust_score' AND conrelid = 'profiles'::regclass
    ) THEN
        ALTER TABLE profiles ADD CONSTRAINT valid_host_trust_score
            CHECK (host_trust_score >= 1.0 AND host_trust_score <= 10.0);
    END IF;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 18. INDEXES
-- ═══════════════════════════════════════════════════════════════

-- Tournament list filters
CREATE INDEX IF NOT EXISTS idx_tournaments_status ON tournaments(status);
CREATE INDEX IF NOT EXISTS idx_tournaments_host ON tournaments(host_user_id);
CREATE INDEX IF NOT EXISTS idx_tournaments_status_deadline ON tournaments(status, registration_deadline);
CREATE INDEX IF NOT EXISTS idx_tournaments_region_skill ON tournaments(region, skill_level, status);
CREATE INDEX IF NOT EXISTS idx_tournaments_created ON tournaments(created_at DESC);

-- Tournament requirements
CREATE INDEX IF NOT EXISTS idx_tournament_requirements_tournament ON tournament_requirements(tournament_id);

-- Tournament applications
CREATE INDEX IF NOT EXISTS idx_tournament_applications_tournament ON tournament_applications(tournament_id);
CREATE INDEX IF NOT EXISTS idx_tournament_applications_team ON tournament_applications(team_id);
CREATE INDEX IF NOT EXISTS idx_tournament_applications_status ON tournament_applications(tournament_id, status);

-- Tournament teams
CREATE INDEX IF NOT EXISTS idx_tournament_teams_tournament ON tournament_teams(tournament_id);
CREATE INDEX IF NOT EXISTS idx_tournament_teams_team ON tournament_teams(team_id);
CREATE INDEX IF NOT EXISTS idx_tournament_teams_standings ON tournament_teams(tournament_id, swiss_points DESC, buchholz_score DESC);

-- Swiss matches
CREATE INDEX IF NOT EXISTS idx_tournament_matches_tournament ON tournament_swiss_matches(tournament_id);
CREATE INDEX IF NOT EXISTS idx_tournament_matches_round ON tournament_swiss_matches(tournament_id, round_number);
CREATE INDEX IF NOT EXISTS idx_tournament_matches_team_a ON tournament_swiss_matches(team_a_id);
CREATE INDEX IF NOT EXISTS idx_tournament_matches_team_b ON tournament_swiss_matches(team_b_id);
CREATE INDEX IF NOT EXISTS idx_tournament_matches_status ON tournament_swiss_matches(tournament_id, status);

-- Match rosters
CREATE INDEX IF NOT EXISTS idx_tournament_rosters_match ON tournament_match_rosters(match_id);
CREATE INDEX IF NOT EXISTS idx_tournament_rosters_team ON tournament_match_rosters(team_id);
CREATE INDEX IF NOT EXISTS idx_tournament_rosters_user ON tournament_match_rosters(user_id);

-- Room secrets
CREATE INDEX IF NOT EXISTS idx_tournament_room_secrets_match ON tournament_match_room_secrets(match_id);

-- Host accounts
CREATE INDEX IF NOT EXISTS idx_tournament_host_accounts_tournament ON tournament_host_accounts(tournament_id);
CREATE INDEX IF NOT EXISTS idx_tournament_host_accounts_host ON tournament_host_accounts(host_user_id);

-- Player stats
CREATE INDEX IF NOT EXISTS idx_tournament_player_stats_tournament ON tournament_player_stats(tournament_id);
CREATE INDEX IF NOT EXISTS idx_tournament_player_stats_user ON tournament_player_stats(user_id);
CREATE INDEX IF NOT EXISTS idx_tournament_player_stats_team ON tournament_player_stats(team_id);

-- Conversation participants
CREATE INDEX IF NOT EXISTS idx_conversation_participants_conversation ON conversation_participants(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversation_participants_user ON conversation_participants(user_id);

-- Host requests
CREATE INDEX IF NOT EXISTS idx_tournament_host_requests_user ON tournament_host_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_tournament_host_requests_status ON tournament_host_requests(status);

-- Profile tournament columns
CREATE INDEX IF NOT EXISTS idx_profiles_is_tournament_host ON profiles(is_tournament_host);
CREATE INDEX IF NOT EXISTS idx_profiles_telegram_username ON profiles(telegram_username);

-- ═══════════════════════════════════════════════════════════════
-- 19. REALTIME PUBLICATION
-- ═══════════════════════════════════════════════════════════════

ALTER PUBLICATION supabase_realtime ADD TABLE tournaments;
ALTER PUBLICATION supabase_realtime ADD TABLE tournament_applications;
ALTER PUBLICATION supabase_realtime ADD TABLE tournament_teams;
ALTER PUBLICATION supabase_realtime ADD TABLE tournament_swiss_matches;
ALTER PUBLICATION supabase_realtime ADD TABLE tournament_match_rosters;
ALTER PUBLICATION supabase_realtime ADD TABLE tournament_host_requests;
ALTER PUBLICATION supabase_realtime ADD TABLE conversation_participants;

-- ═══════════════════════════════════════════════════════════════
-- 20. STORAGE BUCKET for tournament logos
-- ═══════════════════════════════════════════════════════════════

INSERT INTO storage.buckets (id, name, public)
    VALUES ('tournament-logos', 'tournament-logos', true)
    ON CONFLICT (id) DO NOTHING;

-- Storage policy: hosts can upload logos for their tournaments
CREATE POLICY "Hosts can upload tournament logos" ON storage.objects
    FOR INSERT WITH CHECK (
        bucket_id = 'tournament-logos'
        AND auth.role() = 'authenticated'
    );

-- Storage policy: anyone can view tournament logos (public bucket)
CREATE POLICY "Anyone can view tournament logos" ON storage.objects
    FOR SELECT USING (bucket_id = 'tournament-logos');

-- Storage policy: hosts can delete their own tournament logos
CREATE POLICY "Hosts can delete tournament logos" ON storage.objects
    FOR DELETE USING (
        bucket_id = 'tournament-logos'
        AND auth.uid()::TEXT = (storage.foldername(name))[1]
    );

-- ═══════════════════════════════════════════════════════════════
-- 21. NOTIFY POSTGREST TO RELOAD SCHEMA
-- ═══════════════════════════════════════════════════════════════

NOTIFY pgrst, 'reload schema';
