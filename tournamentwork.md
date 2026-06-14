# TOURNAMENT SYSTEM — REVIEWED IMPLEMENTATION PLAN

> Status: reviewed against the current Android app, Supabase schema, and AdminPanel.
> This is not a feature implementation. It is the corrected plan to follow before writing code.

## REVIEW DECISIONS THAT OVERRIDE OLDER SECTIONS

- Build must start from a stable baseline: Android Kotlin compilation and AdminPanel service-role exposure must be fixed before tournament implementation.
- Do not store generated host passwords in any database table. Host account creation must happen in a server-only API route or Edge Function using the Supabase Auth admin API.
- Do not expose `SUPABASE_SERVICE_ROLE_KEY` through any `NEXT_PUBLIC_*` environment variable. Browser code must use anon-key clients only; privileged work must run server-side.
- Do not extend `conversations` with `participant_c_*` columns. Use a normalized `conversation_participants` table so tournament chat supports host, both leaders, admins, support, substitutes, and future roles.
- Do not allow direct broad table updates for match results. Workflow mutations must go through RPCs with explicit caller checks and narrow updates.
- Sensitive fields such as `room_password`, dispute notes, admin overrides, and host account data must not be readable through public tournament match queries.
- Android tournament data must reuse the existing `UnifiedCacheManager` L1 memory + Room stale-while-revalidate pattern.

## TABLE OF CONTENTS
1. [Database Schema](#1-database-schema)
2. [RLS Policies](#2-rls-policies)
3. [RPC Functions](#3-rpc-functions)
4. [Triggers & Constraints](#4-triggers--constraints)
5. [Admin Panel Changes](#5-admin-panel-changes)
6. [Android APK Changes](#6-android-apk-changes)
7. [Notification Types](#7-notification-types)
8. [Implementation Order](#8-implementation-order)
9. [Security & Optimization Requirements](#9-security--optimization-requirements)

---

## 1. DATABASE SCHEMA

### 1.1 Profile Changes (6 new columns)

```sql
-- Role & identity
ALTER TABLE profiles ADD COLUMN is_tournament_host BOOLEAN DEFAULT FALSE;
ALTER TABLE profiles ADD COLUMN telegram_username TEXT;

-- Host reputation tracking
ALTER TABLE profiles ADD COLUMN tournaments_hosted INTEGER DEFAULT 0;
ALTER TABLE profiles ADD COLUMN tournaments_completed INTEGER DEFAULT 0;
ALTER TABLE profiles ADD COLUMN tournaments_cancelled INTEGER DEFAULT 0;
ALTER TABLE profiles ADD COLUMN host_trust_score DECIMAL(3,1) DEFAULT 5.0;

-- Constraints
ALTER TABLE profiles ADD CONSTRAINT valid_host_trust
    CHECK (host_trust_score >= 1.0 AND host_trust_score <= 10.0);
```

### 1.2 `tournament_host_requests` — Users requesting host role

```sql
CREATE TABLE tournament_host_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    motivation TEXT NOT NULL,              -- Why they want host role
    experience TEXT,                        -- Previous tournament hosting experience
    telegram_channel TEXT,                  -- Their main Telegram channel
    social_links TEXT,                      -- Optional: YouTube, Twitch, etc.
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'approved', 'rejected')),
    reviewed_by UUID REFERENCES profiles(id),
    admin_notes TEXT,                       -- Optional reason if rejected
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    reviewed_at TIMESTAMPTZ,
    UNIQUE(user_id)                         -- One active request per user
);
```

### 1.3 `tournaments` — Core tournament table

```sql
CREATE TABLE tournaments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    host_user_id UUID NOT NULL REFERENCES profiles(id),

    -- Basic info
    title TEXT NOT NULL,                    -- Max 100 chars
    description TEXT,                       -- Max 200 words (~1500 chars)
    logo_url TEXT,                          -- Optional, uploaded to Supabase Storage

    -- Prize
    prize_type TEXT NOT NULL
        CHECK (prize_type IN ('real_money', 'diamonds', 'skin', 'star_pass', 'other')),
    prize_description TEXT,                 -- Details of the prize

    -- Tournament config
    status TEXT NOT NULL DEFAULT 'draft'
        CHECK (status IN ('draft', 'registration', 'check_in', 'in_progress', 'completed', 'cancelled')),
    best_of INTEGER NOT NULL DEFAULT 1
        CHECK (best_of IN (1, 2)),          -- BO1 or BO2 per Swiss round
    max_teams INTEGER NOT NULL DEFAULT 16
        CHECK (max_teams >= 4 AND max_teams <= 64),
    min_team_size INTEGER NOT NULL DEFAULT 5 -- Team must have >= this many members to apply
        CHECK (min_team_size >= 3 AND min_team_size <= 7),
    swiss_rounds INTEGER,                   -- Auto-calculated: ceil(log2(max_teams))
    current_round INTEGER DEFAULT 0,        -- Current Swiss round (0 = not started)

    -- Discovery/filtering
    region TEXT DEFAULT 'EU',
    skill_level TEXT DEFAULT 'ALL'
        CHECK (skill_level IN ('beginner', 'intermediate', 'advanced', 'all')),

    -- Deadlines
    registration_deadline TIMESTAMPTZ NOT NULL,
    check_in_deadline TIMESTAMPTZ NOT NULL,  -- After this, host generates Swiss

    -- Live feature (optional)
    is_live_enabled BOOLEAN DEFAULT FALSE,

    -- Admin oversight
    is_flagged BOOLEAN DEFAULT FALSE,
    flag_reason TEXT,

    -- Cancellation
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,

    -- Timestamps
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

-- 1 tournament per host per 7 days (enforced via trigger)
CREATE INDEX idx_tournaments_host ON tournaments(host_user_id);
CREATE INDEX idx_tournaments_status ON tournaments(status);
CREATE INDEX idx_tournaments_region ON tournaments(region);
CREATE INDEX idx_tournaments_skill ON tournaments(skill_level);
```

### 1.4 `tournament_requirements` — Up to 15 requirements per tournament

```sql
CREATE TABLE tournament_requirements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    type TEXT NOT NULL
        CHECK (type IN ('telegram_subscribe', 'custom')),
    label TEXT NOT NULL,                    -- Display text e.g. "Subscribe to @channel"
    url TEXT,                               -- Optional link (Telegram channel URL)
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

-- Max 15 requirements per tournament, max 5 telegram_subscribe
CREATE INDEX idx_tournament_reqs ON tournament_requirements(tournament_id);
```

### 1.5 `tournament_applications` — Teams applying to join

```sql
CREATE TABLE tournament_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'pending'
        CHECK (status IN ('pending', 'accepted', 'rejected', 'blocked')),
    rejection_reason TEXT,                  -- Optional, shown to team
    attempt_number INTEGER NOT NULL DEFAULT 1,
    applied_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    reviewed_at TIMESTAMPTZ,

    UNIQUE(tournament_id, team_id, attempt_number)
);

-- After 3 rejections -> auto blocked for that tournament (enforced via trigger)
CREATE INDEX idx_tournament_apps ON tournament_applications(tournament_id);
CREATE INDEX idx_tournament_apps_team ON tournament_applications(team_id);
CREATE INDEX idx_tournament_apps_status ON tournament_applications(status);
```

### 1.6 `tournament_teams` — Accepted teams in the tournament

```sql
CREATE TABLE tournament_teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,

    -- Check-in
    checked_in BOOLEAN DEFAULT FALSE,
    checked_in_at TIMESTAMPTZ,

    -- Swiss scoring
    swiss_wins INTEGER DEFAULT 0,
    swiss_losses INTEGER DEFAULT 0,
    swiss_draws INTEGER DEFAULT 0,
    swiss_points INTEGER DEFAULT 0,         -- Win=3, Draw=1, Loss=0
    buchholz_score DECIMAL DEFAULT 0,       -- Tiebreaker: sum of opponents' points
    sonneborn_berger DECIMAL DEFAULT 0,     -- Tiebreaker: weighted opponent points

    -- Elimination
    is_eliminated BOOLEAN DEFAULT FALSE,
    final_placement INTEGER,                -- Set when tournament completes

    UNIQUE(tournament_id, team_id)
);

CREATE INDEX idx_tournament_teams ON tournament_teams(tournament_id);
CREATE INDEX idx_tournament_teams_team ON tournament_teams(team_id);
```

### 1.7 `tournament_swiss_matches` — Swiss round pairings

```sql
CREATE TABLE tournament_swiss_matches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    round_number INTEGER NOT NULL,

    -- Teams
    team_a_id UUID NOT NULL REFERENCES teams(id),
    team_b_id UUID REFERENCES teams(id),    -- NULL = bye (auto-win)

    -- Match status
    status TEXT NOT NULL DEFAULT 'scheduled'
        CHECK (status IN ('scheduled', 'in_progress', 'completed', 'cancelled', 'disputed', 'forfeit')),
    scheduled_at TIMESTAMPTZ,               -- Set by tournament_host
    completed_at TIMESTAMPTZ,

    -- Result
    winner_team_id UUID REFERENCES teams(id),
    is_draw BOOLEAN DEFAULT FALSE,          -- BO2: 1-1 result

    -- Room setup status only. Room credentials live in tournament_match_room_secrets.
    room_ready BOOLEAN DEFAULT FALSE,
    room_revealed_at TIMESTAMPTZ,

    -- Live stream (optional)
    live_url TEXT,                          -- YouTube/Twitch link

    -- BO2 tracking
    game_a_winner_team_id UUID REFERENCES teams(id),  -- Game 1 winner
    game_b_winner_team_id UUID REFERENCES teams(id),  -- Game 2 winner (BO2 only)

    -- No-show handling
    no_show_team_id UUID REFERENCES teams(id),
    disqualified_team_id UUID REFERENCES teams(id),
    disqualification_reason TEXT,
    disqualified_by UUID REFERENCES profiles(id),
    disqualified_at TIMESTAMPTZ,
    no_show_grace_period_min INTEGER DEFAULT 15,
    match_auto_complete_at TIMESTAMPTZ,     -- scheduled_at + grace period

    -- Dispute
    is_disputed BOOLEAN DEFAULT FALSE,
    dispute_reason TEXT,
    dispute_submitted_by UUID REFERENCES profiles(id),  -- Team that disputed
    admin_override BOOLEAN DEFAULT FALSE,    -- Admin forced result
    admin_override_by UUID REFERENCES profiles(id),

    -- Chat: conversation between Team A leader + Team B leader + tournament host
    conversation_id UUID REFERENCES conversations(id),

    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

CREATE INDEX idx_swiss_matches_tournament ON tournament_swiss_matches(tournament_id);
CREATE INDEX idx_swiss_matches_round ON tournament_swiss_matches(tournament_id, round_number);
CREATE INDEX idx_swiss_matches_team_a ON tournament_swiss_matches(team_a_id);
CREATE INDEX idx_swiss_matches_team_b ON tournament_swiss_matches(team_b_id);
CREATE INDEX idx_swiss_matches_status ON tournament_swiss_matches(status);
CREATE INDEX idx_swiss_matches_conversation ON tournament_swiss_matches(conversation_id);
```

### 1.7.1 `tournament_match_rosters` — Active players selected by leaders

Tournament points must be awarded only to the players who actually participated in the match. Before a tournament match starts, each team leader selects the active roster for that specific match.

```sql
CREATE TABLE tournament_match_rosters (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    match_id UUID NOT NULL REFERENCES tournament_swiss_matches(id) ON DELETE CASCADE,
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    game_number INTEGER NOT NULL DEFAULT 1 CHECK (game_number IN (1, 2)),
    selected_by UUID NOT NULL REFERENCES profiles(id),
    replaced_user_id UUID REFERENCES profiles(id),
    substitution_reason TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    selected_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(match_id, team_id, game_number, user_id)
);

CREATE INDEX idx_tournament_match_rosters_match ON tournament_match_rosters(match_id);
CREATE INDEX idx_tournament_match_rosters_team ON tournament_match_rosters(match_id, team_id, game_number);
CREATE INDEX idx_tournament_match_rosters_user ON tournament_match_rosters(user_id);
```

Rules:
- Team leader selects 5 active players for each game.
- BO1 uses `game_number = 1`.
- BO2 can keep the same roster for game 2 or submit a different game 2 roster.
- A leader may replace a player between games if a player disconnects, loses internet, or cannot continue.
- The replacement must be a current member of that team.
- Keep the previous game roster rows for audit and points history; do not overwrite game 1 when changing game 2.
- Selected players must be current team members.
- Only selected active players for the played game receive win/loss and points for that game.
- Substitutes or non-selected team members do not receive match points.
- This mirrors the existing scrim roster/points concept and should reuse that scoring logic where possible.

Sensitive room credentials are stored separately and are never exposed through public match list/detail queries:

```sql
CREATE TABLE tournament_match_room_secrets (
    match_id UUID PRIMARY KEY REFERENCES tournament_swiss_matches(id) ON DELETE CASCADE,
    room_id TEXT NOT NULL,
    room_password TEXT NOT NULL,
    created_by UUID NOT NULL REFERENCES profiles(id),
    revealed_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

ALTER TABLE tournament_match_room_secrets ENABLE ROW LEVEL SECURITY;
```

### 1.8 `tournament_host_accounts` — Host account metadata only

```sql
CREATE TABLE tournament_host_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE UNIQUE,
    host_user_id UUID NOT NULL REFERENCES profiles(id),

    -- Supabase Auth account created by a server-only AdminPanel API route or Edge Function.
    -- Never store generated passwords or recovery secrets in this table.
    auth_user_id UUID REFERENCES auth.users(id),
    email TEXT NOT NULL,

    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES profiles(id),
    last_login_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW())
);

CREATE INDEX idx_host_accounts_tournament ON tournament_host_accounts(tournament_id);
CREATE INDEX idx_host_accounts_host ON tournament_host_accounts(host_user_id);
```

Account creation flow:
- Admin approves host request in AdminPanel.
- Server-only route uses Supabase Auth admin API with the service-role key.
- Route returns a one-time temporary password or sends an invite/reset email.
- Database stores only `auth_user_id`, `email`, state, and audit metadata.
- Browser/client code never receives or imports the service-role key.

### 1.9 `tournament_player_stats` — Per-player tournament history

```sql
CREATE TABLE tournament_player_stats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tournament_id UUID NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,

    placement INTEGER,                      -- Final standing
    matches_won INTEGER DEFAULT 0,
    matches_lost INTEGER DEFAULT 0,
    matches_drawn INTEGER DEFAULT 0,
    prize_received BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(tournament_id, user_id)
);

CREATE INDEX idx_tournament_player_stats ON tournament_player_stats(tournament_id);
CREATE INDEX idx_tournament_player_stats_user ON tournament_player_stats(user_id);
```

### 1.10 Conversation Participants — Group-safe tournament match chat

The existing `conversations` table is 1v1 (`participant_a` + `participant_b`). Tournament match chat needs at least Team A leader, Team B leader, and tournament host, and may later need admin/support/substitute users. Do not add fixed `participant_c_*` columns.

```sql
ALTER TABLE conversations ADD COLUMN tournament_match_id UUID REFERENCES tournament_swiss_matches(id);

CREATE TABLE conversation_participants (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    team_id UUID REFERENCES teams(id),
    role TEXT NOT NULL CHECK (role IN ('team_a_leader', 'team_b_leader', 'host', 'admin', 'support', 'substitute')),
    can_send BOOLEAN DEFAULT TRUE,
    is_typing BOOLEAN DEFAULT FALSE,
    last_read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT TIMEZONE('utc', NOW()),
    PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_conversation_participants_user ON conversation_participants(user_id);
CREATE INDEX idx_conversation_participants_conversation ON conversation_participants(conversation_id);
```

This approach:
- Reuses the existing conversation + messages infrastructure
- Adds one participant table instead of hard-coding a third participant
- Supports host, both leaders, admin intervention, support, and future group chat
- Keeps existing 1v1 columns temporarily for backward compatibility during migration
- `tournament_match_id` links the conversation back to the Swiss match

---

## 2. RLS POLICIES

### 2.1 tournament_host_requests

```sql
ALTER TABLE tournament_host_requests ENABLE ROW LEVEL SECURITY;

-- Users can insert their own request
CREATE POLICY "Users can submit host request" ON tournament_host_requests
    FOR INSERT WITH CHECK (user_id = auth.uid());

-- Users can view their own request
CREATE POLICY "Users can view own request" ON tournament_host_requests
    FOR SELECT USING (user_id = auth.uid());

-- Admins can view all requests (service role bypasses RLS)
-- Host accounts cannot see these (separate auth)
```

### 2.2 tournaments

```sql
ALTER TABLE tournaments ENABLE ROW LEVEL SECURITY;

-- Anyone can view published tournaments (not draft)
CREATE POLICY "Users can view published tournaments" ON tournaments
    FOR SELECT USING (status != 'draft');

-- Host can view their own draft tournaments
CREATE POLICY "Host can view own drafts" ON tournaments
    FOR SELECT USING (host_user_id = auth.uid());

-- Only the host can update their tournament
CREATE POLICY "Host can update own tournament" ON tournaments
    FOR UPDATE USING (host_user_id = auth.uid());

-- Only tournament_host users can insert
CREATE POLICY "Tournament hosts can create" ON tournaments
    FOR INSERT WITH CHECK (
        host_user_id = auth.uid()
        AND EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND is_tournament_host = TRUE)
    );
```

### 2.3 tournament_requirements

```sql
ALTER TABLE tournament_requirements ENABLE ROW LEVEL SECURITY;

-- Anyone can view requirements for published tournaments
CREATE POLICY "Users can view requirements" ON tournament_requirements
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_requirements.tournament_id AND status != 'draft')
    );

-- Host can manage their tournament requirements
CREATE POLICY "Host can manage requirements" ON tournament_requirements
    FOR ALL USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_requirements.tournament_id AND host_user_id = auth.uid())
    );
```

### 2.4 tournament_applications

```sql
ALTER TABLE tournament_applications ENABLE ROW LEVEL SECURITY;

-- Team members can view their own applications
CREATE POLICY "Teams can view own applications" ON tournament_applications
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM team_members WHERE team_id = tournament_applications.team_id AND user_id = auth.uid())
    );

-- Host can view applications for their tournament
CREATE POLICY "Host can view tournament applications" ON tournament_applications
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_applications.tournament_id AND host_user_id = auth.uid())
    );

-- Team leader can apply (must be team leader)
CREATE POLICY "Team leaders can apply" ON tournament_applications
    FOR INSERT WITH CHECK (
        EXISTS (SELECT 1 FROM teams WHERE id = tournament_applications.team_id AND leader_id = auth.uid())
    );

-- Host can update application status (accept/reject)
CREATE POLICY "Host can review applications" ON tournament_applications
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_applications.tournament_id AND host_user_id = auth.uid())
    );
```

### 2.5 tournament_teams

```sql
ALTER TABLE tournament_teams ENABLE ROW LEVEL SECURITY;

-- Anyone can view teams in published tournaments
CREATE POLICY "Users can view tournament teams" ON tournament_teams
    FOR SELECT USING (true);

-- Host can manage teams in their tournament
CREATE POLICY "Host can manage tournament teams" ON tournament_teams
    FOR ALL USING (
        EXISTS (SELECT 1 FROM tournaments WHERE id = tournament_teams.tournament_id AND host_user_id = auth.uid())
    );

-- Team leader can check in their own team
CREATE POLICY "Team leader can check in" ON tournament_teams
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM teams WHERE id = tournament_teams.team_id AND leader_id = auth.uid())
    );
```

### 2.6 tournament_swiss_matches

```sql
ALTER TABLE tournament_swiss_matches ENABLE ROW LEVEL SECURITY;

-- Public match view must not include sensitive columns.
-- Use a database view such as public_tournament_matches for spectator data.
CREATE POLICY "Users can view matches" ON tournament_swiss_matches
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM tournaments t
            WHERE t.id = tournament_swiss_matches.tournament_id
              AND t.status IN ('registration', 'check_in', 'in_progress', 'completed')
        )
    );

-- Host can manage matches in their tournament, but workflow updates should still
-- go through RPC functions to prevent broad client-side writes.
CREATE POLICY "Host can manage matches" ON tournament_swiss_matches
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM tournaments
            WHERE id = tournament_swiss_matches.tournament_id
              AND host_user_id = (select auth.uid())
        )
    );

-- No broad UPDATE policy for team members.
-- Tournament hosts submit final results via submit_tournament_match_result(...) RPC only.
```

### 2.6.1 tournament_match_rosters

```sql
ALTER TABLE tournament_match_rosters ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Match participants can view rosters" ON tournament_match_rosters
    FOR SELECT USING (
        EXISTS (
            SELECT 1
            FROM tournament_swiss_matches m
            WHERE m.id = tournament_match_rosters.match_id
              AND (
                  m.team_a_id IN (SELECT team_id FROM team_members WHERE user_id = (select auth.uid()))
                  OR m.team_b_id IN (SELECT team_id FROM team_members WHERE user_id = (select auth.uid()))
                  OR EXISTS (
                      SELECT 1 FROM tournaments t
                      WHERE t.id = m.tournament_id
                        AND t.host_user_id = (select auth.uid())
                  )
              )
        )
    );

CREATE POLICY "Team leaders can set own match roster" ON tournament_match_rosters
    FOR INSERT WITH CHECK (
        selected_by = (select auth.uid())
        AND EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = tournament_match_rosters.team_id
              AND t.leader_id = (select auth.uid())
        )
        AND EXISTS (
            SELECT 1 FROM team_members tm
            WHERE tm.team_id = tournament_match_rosters.team_id
              AND tm.user_id = tournament_match_rosters.user_id
        )
    );

CREATE POLICY "Team leaders can update own match roster" ON tournament_match_rosters
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = tournament_match_rosters.team_id
              AND t.leader_id = (select auth.uid())
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = tournament_match_rosters.team_id
              AND t.leader_id = (select auth.uid())
        )
    );
```

Additional roster rules:
- Leaders can change only their own team roster.
- Roster for game 1 should lock when game 1 starts or when the host marks game 1 as completed.
- Roster for game 2 remains editable until game 2 starts, so a leader can replace a player whose internet drops after game 1.
- Host can view both rosters in APK host tools and the tournament host web panel.

### 2.7 tournament_host_accounts

```sql
ALTER TABLE tournament_host_accounts ENABLE ROW LEVEL SECURITY;

-- Only the host_user_id owner can see their own account
CREATE POLICY "Host can view own account" ON tournament_host_accounts
    FOR SELECT USING (host_user_id = auth.uid());
```

### 2.8 tournament_player_stats

```sql
ALTER TABLE tournament_player_stats ENABLE ROW LEVEL SECURITY;

-- Anyone can view tournament stats
CREATE POLICY "Users can view tournament stats" ON tournament_player_stats
    FOR SELECT USING (true);
```

### 2.9 Conversations RLS Update (participant table)

```sql
CREATE OR REPLACE POLICY "Conversation participants can view" ON conversations
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM conversation_participants cp
            WHERE cp.conversation_id = conversations.id
              AND cp.user_id = (select auth.uid())
        )
    );

CREATE OR REPLACE POLICY "Conversation participants can update read state only" ON conversations
    FOR UPDATE USING (
        EXISTS (
            SELECT 1 FROM conversation_participants cp
            WHERE cp.conversation_id = conversations.id
              AND cp.user_id = (select auth.uid())
        )
    );

CREATE OR REPLACE POLICY "Conversation members can view messages" ON messages
    FOR SELECT USING (
        sender_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversation_participants cp
            WHERE cp.conversation_id = messages.conversation_id
              AND cp.user_id = (select auth.uid())
        )
    );

CREATE OR REPLACE POLICY "Conversation members can send messages" ON messages
    FOR INSERT WITH CHECK (
        sender_id = (select auth.uid())
        AND EXISTS (
            SELECT 1 FROM conversation_participants cp
            WHERE cp.conversation_id = messages.conversation_id
              AND cp.user_id = (select auth.uid())
              AND cp.can_send = TRUE
        )
    );
```

### 2.10 Room Secrets RLS

```sql
ALTER TABLE tournament_match_room_secrets ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Room secrets visible to match participants" ON tournament_match_room_secrets
    FOR SELECT USING (
        EXISTS (
            SELECT 1
            FROM tournament_swiss_matches m
            JOIN conversation_participants cp ON cp.conversation_id = m.conversation_id
            WHERE m.id = tournament_match_room_secrets.match_id
              AND cp.user_id = (select auth.uid())
        )
    );

CREATE POLICY "Only host can upsert room secrets" ON tournament_match_room_secrets
    FOR ALL USING (
        EXISTS (
            SELECT 1
            FROM tournament_swiss_matches m
            JOIN tournaments t ON t.id = m.tournament_id
            WHERE m.id = tournament_match_room_secrets.match_id
              AND t.host_user_id = (select auth.uid())
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1
            FROM tournament_swiss_matches m
            JOIN tournaments t ON t.id = m.tournament_id
            WHERE m.id = tournament_match_room_secrets.match_id
              AND t.host_user_id = (select auth.uid())
        )
    );
```

---

## 3. RPC FUNCTIONS

### 3.1 `apply_for_tournament` — Team applies with full validation

```sql
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
    v_existing_attempts INTEGER;
    v_current_status TEXT;
    v_next_attempt INTEGER;
BEGIN
    -- Get tournament info
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN RETURN json_build_object('success', false, 'error', 'Tournament not found'); END IF;

    -- Check tournament is in registration phase
    IF v_tournament.status != 'registration' THEN
        RETURN json_build_object('success', false, 'error', 'Tournament is not accepting applications');
    END IF;

    -- Check if team is already accepted
    SELECT status INTO v_current_status FROM tournament_applications
        WHERE tournament_id = p_tournament_id AND team_id = p_team_id AND status = 'accepted';
    IF FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Team already accepted');
    END IF;

    -- Check if team is blocked
    SELECT status INTO v_current_status FROM tournament_applications
        WHERE tournament_id = p_tournament_id AND team_id = p_team_id AND status = 'blocked';
    IF FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Team is blocked from this tournament (3 rejections)');
    END IF;

    -- Count existing attempts
    SELECT COUNT(*) INTO v_existing_attempts FROM tournament_applications
        WHERE tournament_id = p_tournament_id AND team_id = p_team_id;

    IF v_existing_attempts >= 3 THEN
        -- Auto-block after 3 attempts
        INSERT INTO tournament_applications (tournament_id, team_id, status, attempt_number, rejection_reason)
            VALUES (p_tournament_id, p_team_id, 'blocked', 4, 'Automatically blocked after 3 rejections');
        RETURN json_build_object('success', false, 'error', 'Team blocked after 3 rejections');
    END IF;

    -- Check team size >= min_team_size
    SELECT COUNT(*) INTO v_member_count FROM team_members WHERE team_id = p_team_id;
    IF v_member_count < v_tournament.min_team_size THEN
        RETURN json_build_object('success', false, 'error',
            format('Team needs at least %s members (currently %s)', v_tournament.min_team_size, v_member_count));
    END IF;

    -- Check ALL team members have telegram_username
    SELECT array_agg(p.username) INTO v_missing_telegram
        FROM team_members tm
        JOIN profiles p ON p.id = tm.user_id
        WHERE tm.team_id = p_team_id
          AND (p.telegram_username IS NULL OR p.telegram_username = '');

    IF array_length(v_missing_telegram, 1) > 0 THEN
        RETURN json_build_object(
            'success', false,
            'error', 'Some teammates need to add their Telegram username',
            'missing_telegram_users', v_missing_telegram
        );
    END IF;

    -- Check max_teams not exceeded
    IF (SELECT COUNT(*) FROM tournament_applications WHERE tournament_id = p_tournament_id AND status = 'accepted')
        >= v_tournament.max_teams THEN
        RETURN json_build_object('success', false, 'error', 'Tournament is full');
    END IF;

    -- All checks passed — create application
    v_next_attempt := v_existing_attempts + 1;

    INSERT INTO tournament_applications (tournament_id, team_id, status, attempt_number)
        VALUES (p_tournament_id, p_team_id, 'pending', v_next_attempt);

    -- Notify tournament host
    INSERT INTO app_notifications (user_id, type, title, message, action_id)
        VALUES (
            v_tournament.host_user_id,
            'TOURNAMENT_APPLICATION_NEW',
            'New Team Application',
            format('Team applied to your tournament: %s', v_tournament.title),
            p_tournament_id::TEXT
        );

    RETURN json_build_object('success', true, 'attempt_number', v_next_attempt);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 3.2 `review_tournament_application` — Host accepts/rejects

```sql
CREATE OR REPLACE FUNCTION review_tournament_application(
    p_application_id UUID,
    p_decision TEXT,          -- 'accepted' or 'rejected'
    p_rejection_reason TEXT DEFAULT NULL,
    p_reviewer_user_id UUID  -- Must be the tournament host
)
RETURNS JSON AS $$
DECLARE
    v_app RECORD;
    v_tournament RECORD;
    v_team_members RECORD[];
BEGIN
    -- Get application
    SELECT * INTO v_app FROM tournament_applications WHERE id = p_application_id;
    IF NOT FOUND THEN RETURN json_build_object('success', false, 'error', 'Application not found'); END IF;

    -- Get tournament
    SELECT * INTO v_tournament FROM tournaments WHERE id = v_app.tournament_id;

    -- Verify reviewer is the host
    IF v_tournament.host_user_id != p_reviewer_user_id THEN
        RETURN json_build_object('success', false, 'error', 'Only the tournament host can review applications');
    END IF;

    -- Update application
    UPDATE tournament_applications
        SET status = p_decision,
            rejection_reason = p_rejection_reason,
            reviewed_at = TIMEZONE('utc', NOW())
        WHERE id = p_application_id;

    IF p_decision = 'accepted' THEN
        -- Add team to tournament_teams
        INSERT INTO tournament_teams (tournament_id, team_id)
            VALUES (v_app.tournament_id, v_app.team_id)
            ON CONFLICT (tournament_id, team_id) DO NOTHING;

        -- Notify team leader
        INSERT INTO app_notifications (user_id, type, title, message, action_id)
            SELECT t.leader_id, 'TOURNAMENT_APPLICATION_ACCEPTED',
                'Application Accepted!',
                format('Your team was accepted into: %s', v_tournament.title),
                v_tournament.id::TEXT
            FROM teams t WHERE t.id = v_app.team_id;

    ELSIF p_decision = 'rejected' THEN
        -- Check if this was the 3rd rejection -> auto-block
        IF v_app.attempt_number >= 3 THEN
            UPDATE tournament_applications
                SET status = 'blocked',
                    rejection_reason = COALESCE(p_rejection_reason, '') || ' [AUTO-BLOCKED: 3 rejections]'
                WHERE id = p_application_id;
        END IF;

        -- Notify team leader with reason
        INSERT INTO app_notifications (user_id, type, title, message, action_id)
            SELECT t.leader_id, 'TOURNAMENT_APPLICATION_REJECTED',
                'Application Rejected',
                format('Your team was rejected from: %s. Reason: %s',
                    v_tournament.title,
                    COALESCE(p_rejection_reason, 'Not specified')),
                v_tournament.id::TEXT
            FROM teams t WHERE t.id = v_app.team_id;
    END IF;

    RETURN json_build_object('success', true, 'decision', p_decision);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 3.3 `generate_swiss_pairings` — Swiss bracket generation (host triggers)

```sql
CREATE OR REPLACE FUNCTION generate_swiss_pairings(p_tournament_id UUID)
RETURNS JSON AS $$
DECLARE
    v_tournament RECORD;
    v_round INTEGER;
    v_teams RECORD[];
    v_pair_count INTEGER;
    v_team_a RECORD;
    v_team_b RECORD;
    v_conversation_id UUID;
    v_host_profile RECORD;
    v_team_a_leader RECORD;
    v_team_b_leader RECORD;
    v_i INTEGER;
    v_j INTEGER;
    v_paired BOOLEAN[];
    v_match_id UUID;
BEGIN
    -- Get tournament
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN RETURN json_build_object('success', false, 'error', 'Tournament not found'); END IF;

    -- Only host can generate (checked at application layer, but verify status)
    IF v_tournament.status NOT IN ('check_in', 'registration') THEN
        RETURN json_build_object('success', false, 'error', 'Tournament must be in check-in or registration phase');
    END IF;

    -- Get checked-in teams sorted by points (for non-round-1 pairing)
    v_round := v_tournament.current_round + 1;

    IF v_round = 1 THEN
        -- Round 1: random pairing
        SELECT array_agg(tt.* ORDER BY RANDOM()) INTO v_teams
            FROM tournament_teams tt
            WHERE tt.tournament_id = p_tournament_id AND tt.checked_in = TRUE;
    ELSE
        -- Round 2+: sort by swiss_points descending, then buchholz
        SELECT array_agg(tt.* ORDER BY tt.swiss_points DESC, tt.buchholz_score DESC) INTO v_teams
            FROM tournament_teams tt
            WHERE tt.tournament_id = p_tournament_id AND tt.is_eliminated = FALSE;
    END IF;

    -- Need at least 4 teams
    IF array_length(v_teams, 1) < 4 THEN
        RETURN json_build_object('success', false, 'error', 'Need at least 4 checked-in teams for Swiss');
    END IF;

    -- Get host profile for conversation creation
    SELECT * INTO v_host_profile FROM profiles WHERE id = v_tournament.host_user_id;

    -- Initialize paired array
    v_paired := array_fill(false, ARRAY[array_length(v_teams, 1)]);

    -- Pair teams
    v_i := 1;
    WHILE v_i <= array_length(v_teams, 1) LOOP
        IF v_paired[v_i] THEN v_i := v_i + 1; CONTINUE; END IF;

        -- Find best opponent: closest score, not already played, not paired
        v_j := v_i + 1;
        v_team_b := NULL;

        WHILE v_j <= array_length(v_teams, 1) LOOP
            IF NOT v_paired[v_j] THEN
                -- Check if these teams already played each other
                IF NOT EXISTS (
                    SELECT 1 FROM tournament_swiss_matches
                    WHERE tournament_id = p_tournament_id
                      AND round_number < v_round
                      AND ((team_a_id = v_teams[v_i].team_id AND team_b_id = v_teams[v_j].team_id)
                           OR (team_a_id = v_teams[v_j].team_id AND team_b_id = v_teams[v_i].team_id))
                ) THEN
                    v_team_b := v_teams[v_j];
                    v_paired[v_j] := true;
                    EXIT;
                END IF;
            END IF;
            v_j := v_j + 1;
        END LOOP;

        IF v_team_b IS NULL THEN
            -- Bye: team gets auto-win
            INSERT INTO tournament_swiss_matches (tournament_id, round_number, team_a_id, team_b_id, status, winner_team_id, is_draw)
                VALUES (p_tournament_id, v_round, v_teams[v_i].team_id, NULL, 'completed', v_teams[v_i].team_id, false);

            -- Update team points for bye
            UPDATE tournament_teams SET
                swiss_wins = swiss_wins + 1,
                swiss_points = swiss_points + 3
                WHERE tournament_id = p_tournament_id AND team_id = v_teams[v_i].team_id;
        ELSE
            -- Create match
            INSERT INTO tournament_swiss_matches (tournament_id, round_number, team_a_id, team_b_id, status)
                VALUES (p_tournament_id, v_round, v_teams[v_i].team_id, v_team_b.team_id, 'scheduled')
                RETURNING id INTO v_match_id;

            -- Create conversation: Team A leader + Team B leader + tournament host
            SELECT * INTO v_team_a_leader FROM profiles p
                JOIN teams t ON t.leader_id = p.id WHERE t.id = v_teams[v_i].team_id;
            SELECT * INTO v_team_b_leader FROM profiles p
                JOIN teams t ON t.leader_id = p.id WHERE t.id = v_team_b.team_id;

            INSERT INTO conversations (
                scrim_id,  -- NULL for tournament matches
                participant_a_id, participant_a_name, participant_a_team_id, participant_a_team_name,
                participant_b_id, participant_b_name, participant_b_team_id, participant_b_team_name,
                tournament_match_id
            ) VALUES (
                NULL,
                v_team_a_leader.id, v_team_a_leader.username, v_teams[v_i].team_id,
                    (SELECT name FROM teams WHERE id = v_teams[v_i].team_id),
                v_team_b_leader.id, v_team_b_leader.username, v_team_b.team_id,
                    (SELECT name FROM teams WHERE id = v_team_b.team_id),
                v_match_id
            ) RETURNING id INTO v_conversation_id;

            INSERT INTO conversation_participants (conversation_id, user_id, team_id, role)
            VALUES
                (v_conversation_id, v_team_a_leader.id, v_teams[v_i].team_id, 'team_a_leader'),
                (v_conversation_id, v_team_b_leader.id, v_team_b.team_id, 'team_b_leader'),
                (v_conversation_id, v_host_profile.id, NULL, 'host');

            -- Link conversation to match
            UPDATE tournament_swiss_matches SET conversation_id = v_conversation_id
                WHERE id = v_match_id;

            -- Notify both team leaders
            INSERT INTO app_notifications (user_id, type, title, message, action_id) VALUES
                (v_team_a_leader.id, 'TOURNAMENT_MATCH_SCHEDULED',
                    'Match Scheduled',
                    format('Your Round %s match in %s is ready. Check in with your opponent!', v_round, v_tournament.title),
                    p_tournament_id::TEXT),
                (v_team_b_leader.id, 'TOURNAMENT_MATCH_SCHEDULED',
                    'Match Scheduled',
                    format('Your Round %s match in %s is ready. Check in with your opponent!', v_round, v_tournament.title),
                    p_tournament_id::TEXT);
        END IF;

        v_paired[v_i] := true;
        v_i := v_i + 1;
    END LOOP;

    -- Update tournament round and status
    UPDATE tournaments SET
        current_round = v_round,
        status = 'in_progress',
        updated_at = TIMEZONE('utc', NOW())
        WHERE id = p_tournament_id;

    RETURN json_build_object('success', true, 'round', v_round, 'matches_created', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 3.4 `submit_tournament_match_result` — Host-authoritative tournament result

> Updated user story: teams do not decide the tournament winner. Each team leader selects the active players for that match, then the tournament host confirms winner/loser/draw because the host creates the room and spectates the match. Only selected active players receive win/loss and points.
>
> The older team-submission/dispute SQL below is superseded. Keep it only as historical reference until rewritten; implementation must use the host-authoritative contract in section 3.4.1.

```sql
CREATE OR REPLACE FUNCTION submit_tournament_match_result(
    p_match_id UUID,
    p_submitter_user_id UUID,
    p_winner_team_id UUID,
    p_is_draw BOOLEAN DEFAULT FALSE,
    p_game_a_winner UUID DEFAULT NULL,
    p_game_b_winner UUID DEFAULT NULL
)
RETURNS JSON AS $$
DECLARE
    v_match RECORD;
    v_tournament RECORD;
    v_submitter_team UUID;
BEGIN
    SELECT * INTO v_match FROM tournament_swiss_matches WHERE id = p_match_id;
    IF NOT FOUND THEN RETURN json_build_object('success', false, 'error', 'Match not found'); END IF;

    SELECT * INTO v_tournament FROM tournaments WHERE id = v_match.tournament_id;

    -- Determine which team the submitter belongs to
    IF EXISTS (SELECT 1 FROM team_members WHERE team_id = v_match.team_a_id AND user_id = p_submitter_user_id) THEN
        v_submitter_team := v_match.team_a_id;
    ELSIF EXISTS (SELECT 1 FROM team_members WHERE team_id = v_match.team_b_id AND user_id = p_submitter_user_id) THEN
        v_submitter_team := v_match.team_b_id;
    ELSIF p_submitter_user_id = v_tournament.host_user_id THEN
        -- Host can submit result directly (no dispute possible)
        UPDATE tournament_swiss_matches SET
            winner_team_id = p_winner_team_id,
            is_draw = p_is_draw,
            game_a_winner_team_id = p_game_a_winner,
            game_b_winner_team_id = p_game_b_winner,
            status = 'completed',
            completed_at = TIMEZONE('utc', NOW()),
            admin_override = true,
            admin_override_by = p_submitter_user_id
            WHERE id = p_match_id;

        PERFORM update_tournament_scores(p_match_id);
        RETURN json_build_object('success', true, 'method', 'host_override');
    ELSE
        RETURN json_build_object('success', false, 'error', 'Not a participant of this match');
    END IF;

    -- Check if there's already a result submitted by the OTHER team
    -- If the other team already submitted a DIFFERENT winner -> dispute
    IF v_match.winner_team_id IS NOT NULL AND v_match.winner_team_id != p_winner_team_id AND NOT v_match.is_draw THEN
        -- DISPUTE: both teams claim different winner
        UPDATE tournament_swiss_matches SET
            is_disputed = true,
            dispute_reason = format('Team claims %s as winner, other team claims %s',
                p_winner_team_id, v_match.winner_team_id),
            dispute_submitted_by = p_submitter_user_id,
            status = 'disputed'
            WHERE id = p_match_id;

        -- Notify host about dispute
        INSERT INTO app_notifications (user_id, type, title, message, action_id)
            VALUES (v_tournament.host_user_id, 'TOURNAMENT_DISPUTE',
                'Match Dispute',
                format('Dispute in Round %s of %s. Please review and decide.', v_match.round_number, v_tournament.title),
                v_match.id::TEXT);

        RETURN json_build_object('success', true, 'method', 'disputed');
    END IF;

    -- No dispute — record result
    UPDATE tournament_swiss_matches SET
        winner_team_id = p_winner_team_id,
        is_draw = p_is_draw,
        game_a_winner_team_id = p_game_a_winner,
        game_b_winner_team_id = p_game_b_winner,
        status = 'completed',
        completed_at = TIMEZONE('utc', NOW())
        WHERE id = p_match_id;

    PERFORM update_tournament_scores(p_match_id);

    RETURN json_build_object('success', true, 'method', 'agreed');
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 3.4.1 Host-authoritative result contract

Implementation must replace the older team-submission/dispute flow with this behavior:

```sql
-- submit_tournament_match_result(
--     p_match_id UUID,
--     p_winner_team_id UUID,
--     p_is_draw BOOLEAN DEFAULT FALSE,
--     p_game_a_winner UUID DEFAULT NULL,
--     p_game_b_winner UUID DEFAULT NULL
-- )
```

Rules:
- Caller must be `(select auth.uid()) = tournaments.host_user_id`.
- Winner must be either `team_a_id` or `team_b_id`, unless `p_is_draw = TRUE`.
- Both leaders must have selected exactly 5 active players in `tournament_match_rosters` for every played game.
- Completing the match must be idempotent; re-running cannot double-award points.
- Normal tournament result validation is not an admin responsibility.
- Admins only intervene for abuse, host misconduct, fraud, or support escalation.

Result flow:
1. Team A leader selects active roster for game 1.
2. Team B leader selects active roster for game 1.
3. Match starts and host creates/spectates the room.
4. For BO2, leaders may update game 2 roster before game 2 starts.
5. Host submits the final result.
6. Existing scrim-style points logic awards win/loss and points only to selected players for each played game.
7. Tournament Swiss standings update after points are awarded.

### 3.4.2 `set_tournament_match_roster` — Team leader selects active players

```sql
CREATE OR REPLACE FUNCTION set_tournament_match_roster(
    p_match_id UUID,
    p_team_id UUID,
    p_game_number INTEGER,
    p_player_ids UUID[]
)
RETURNS JSON AS $$
DECLARE
    v_match RECORD;
    v_player_id UUID;
BEGIN
    SELECT * INTO v_match FROM tournament_swiss_matches WHERE id = p_match_id;
    IF NOT FOUND THEN RETURN json_build_object('success', false, 'error', 'Match not found'); END IF;

    IF p_team_id NOT IN (v_match.team_a_id, v_match.team_b_id) THEN
        RETURN json_build_object('success', false, 'error', 'Team is not in this match');
    END IF;

    IF p_game_number NOT IN (1, 2) THEN
        RETURN json_build_object('success', false, 'error', 'Invalid game number');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM teams WHERE id = p_team_id AND leader_id = (select auth.uid())) THEN
        RETURN json_build_object('success', false, 'error', 'Only team leader can set match roster');
    END IF;

    IF array_length(p_player_ids, 1) IS DISTINCT FROM 5 THEN
        RETURN json_build_object('success', false, 'error', 'Exactly 5 active players must be selected');
    END IF;

    DELETE FROM tournament_match_rosters
        WHERE match_id = p_match_id AND team_id = p_team_id AND game_number = p_game_number;

    FOREACH v_player_id IN ARRAY p_player_ids LOOP
        IF NOT EXISTS (
            SELECT 1 FROM team_members
            WHERE team_id = p_team_id AND user_id = v_player_id
        ) THEN
            RETURN json_build_object('success', false, 'error', 'Selected player is not a team member');
        END IF;

        INSERT INTO tournament_match_rosters (
            match_id, tournament_id, team_id, user_id, game_number, selected_by, is_active
        ) VALUES (
            p_match_id, v_match.tournament_id, p_team_id, v_player_id, p_game_number, (select auth.uid()), TRUE
        );
    END LOOP;

    RETURN json_build_object('success', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

For BO2 substitutions:
- Use the same function with `p_game_number = 2`.
- The game 2 roster can be different from game 1.
- Example: game 1 roster A/B/C/D/E, then E loses internet, leader selects A/B/C/D/F for game 2.
- Scoring must award game 1 points to A/B/C/D/E and game 2 points to A/B/C/D/F.

### 3.4.3 `award_tournament_match_points` — Reuse existing scrim scoring concept

This should reuse the existing scrim result/points model as closely as possible. The tournament-specific difference is the player set: use `tournament_match_rosters`, not the full team.

```sql
-- Pseudocode contract:
-- 1. Load completed tournament match.
-- 2. Load active selected players for team A and team B by game_number.
-- 3. For selected winner players in each played game: add win + win points.
-- 4. For selected loser players in each played game: add loss + loss points.
-- 5. For draw: add draw stats/points if supported.
-- 6. Do not award points to non-selected players.
-- 7. Keep this idempotent so re-running cannot double-award points.
```

### 3.5 `update_tournament_scores` — Recalculate Swiss scores after match result

```sql
CREATE OR REPLACE FUNCTION update_tournament_scores(p_match_id UUID)
RETURNS VOID AS $$
DECLARE
    v_match RECORD;
    v_tournament_id UUID;
BEGIN
    SELECT * INTO v_match FROM tournament_swiss_matches WHERE id = p_match_id;
    v_tournament_id := v_match.tournament_id;

    -- Update winning team
    IF v_match.winner_team_id IS NOT NULL AND NOT v_match.is_draw THEN
        UPDATE tournament_teams SET
            swiss_wins = swiss_wins + 1,
            swiss_points = swiss_points + 3
            WHERE tournament_id = v_tournament_id AND team_id = v_match.winner_team_id;

        -- Update losing team
        IF v_match.team_a_id = v_match.winner_team_id THEN
            UPDATE tournament_teams SET
                swiss_losses = swiss_losses + 1
                WHERE tournament_id = v_tournament_id AND team_id = v_match.team_b_id;
        ELSE
            UPDATE tournament_teams SET
                swiss_losses = swiss_losses + 1
                WHERE tournament_id = v_tournament_id AND team_id = v_match.team_a_id;
        END IF;
    END IF;

    -- Draw (BO2: 1-1)
    IF v_match.is_draw THEN
        UPDATE tournament_teams SET
            swiss_draws = swiss_draws + 1,
            swiss_points = swiss_points + 1
            WHERE tournament_id = v_tournament_id
              AND team_id IN (v_match.team_a_id, v_match.team_b_id);
    END IF;

    -- Bye win (team_b_id is NULL)
    IF v_match.team_b_id IS NULL THEN
        -- Already handled above (winner_team_id = team_a_id)
        NULL;
    END IF;

    -- Recalculate Buchholz and Sonneborn-Berger for all teams in tournament
    PERFORM recalculate_tiebreakers(v_tournament_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 3.6 `recalculate_tiebreakers` — Buchholz + Sonneborn-Berger

```sql
CREATE OR REPLACE FUNCTION recalculate_tiebreakers(p_tournament_id UUID)
RETURNS VOID AS $$
DECLARE
    v_team RECORD;
    v_buchholz DECIMAL;
    v_sb DECIMAL;
BEGIN
    FOR v_team IN
        SELECT tt.team_id FROM tournament_teams tt
        WHERE tt.tournament_id = p_tournament_id
    LOOP
        -- Buchholz: sum of all opponents' swiss_points
        SELECT COALESCE(SUM(opp.swiss_points), 0) INTO v_buchholz
            FROM tournament_swiss_matches m
            JOIN tournament_teams opp ON opp.tournament_id = p_tournament_id
                AND (opp.team_id = m.team_a_id OR opp.team_id = m.team_b_id)
            WHERE m.tournament_id = p_tournament_id
              AND m.status = 'completed'
              AND (m.team_a_id = v_team.team_id OR m.team_b_id = v_team.team_id)
              AND opp.team_id != v_team.team_id;

        -- Sonneborn-Berger: sum of (defeated opponents' points * 1) + (drawn opponents' points * 0.5)
        SELECT COALESCE(SUM(
            CASE
                WHEN m.winner_team_id = v_team.team_id THEN opp.swiss_points
                WHEN m.is_draw THEN opp.swiss_points * 0.5
                ELSE 0
            END
        ), 0) INTO v_sb
            FROM tournament_swiss_matches m
            JOIN tournament_teams opp ON opp.tournament_id = p_tournament_id
                AND (opp.team_id = m.team_a_id OR opp.team_id = m.team_b_id)
            WHERE m.tournament_id = p_tournament_id
              AND m.status = 'completed'
              AND (m.team_a_id = v_team.team_id OR m.team_b_id = v_team.team_id)
              AND opp.team_id != v_team.team_id;

        UPDATE tournament_teams SET
            buchholz_score = v_buchholz,
            sonneborn_berger = v_sb
            WHERE tournament_id = p_tournament_id AND team_id = v_team.team_id;
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 3.7 `check_tournament_no_shows` — Auto-complete overdue matches

```sql
CREATE OR REPLACE FUNCTION check_tournament_no_shows(p_tournament_id UUID)
RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER := 0;
    v_match RECORD;
BEGIN
    FOR v_match IN
        SELECT * FROM tournament_swiss_matches
        WHERE tournament_id = p_tournament_id
          AND status = 'scheduled'
          AND match_auto_complete_at IS NOT NULL
          AND match_auto_complete_at < TIMEZONE('utc', NOW())
    LOOP
        -- Auto-award win to the team that showed up
        -- If neither showed up, cancel the match
        UPDATE tournament_swiss_matches SET
            no_show_team_id = v_match.team_b_id,  -- Assume team_b didn't show
            winner_team_id = v_match.team_a_id,
            status = 'completed',
            completed_at = TIMEZONE('utc', NOW())
            WHERE id = v_match.id;

        PERFORM update_tournament_scores(v_match.id);
        v_count := v_count + 1;
    END LOOP;

    RETURN v_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 3.7.1 `disqualify_tournament_team` — Host no-response / no-show action

If a match should start and a team leader does not respond, the tournament host can disqualify that team from the match from either the Android APK host tools or the tournament host web panel.

```sql
CREATE OR REPLACE FUNCTION disqualify_tournament_team(
    p_match_id UUID,
    p_team_id UUID,
    p_reason TEXT DEFAULT 'Team leader did not respond at match start'
)
RETURNS JSON AS $$
DECLARE
    v_match RECORD;
    v_tournament RECORD;
    v_winner_team_id UUID;
BEGIN
    SELECT * INTO v_match FROM tournament_swiss_matches WHERE id = p_match_id;
    IF NOT FOUND THEN RETURN json_build_object('success', false, 'error', 'Match not found'); END IF;

    SELECT * INTO v_tournament FROM tournaments WHERE id = v_match.tournament_id;

    IF v_tournament.host_user_id != (select auth.uid()) THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can disqualify a team');
    END IF;

    IF p_team_id NOT IN (v_match.team_a_id, v_match.team_b_id) THEN
        RETURN json_build_object('success', false, 'error', 'Team is not in this match');
    END IF;

    v_winner_team_id := CASE
        WHEN p_team_id = v_match.team_a_id THEN v_match.team_b_id
        ELSE v_match.team_a_id
    END;

    UPDATE tournament_swiss_matches SET
        status = 'forfeit',
        no_show_team_id = p_team_id,
        disqualified_team_id = p_team_id,
        disqualification_reason = p_reason,
        disqualified_by = (select auth.uid()),
        disqualified_at = TIMEZONE('utc', NOW()),
        winner_team_id = v_winner_team_id,
        completed_at = TIMEZONE('utc', NOW())
        WHERE id = p_match_id;

    -- Forfeit updates tournament standings. Player points should usually not be
    -- awarded unless the product decision explicitly says forfeit wins grant points.
    PERFORM update_tournament_scores(p_match_id);

    RETURN json_build_object('success', true, 'winner_team_id', v_winner_team_id);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

Rules:
- Host must choose which team did not respond.
- The other team receives the match win in tournament standings.
- By default, no player points are awarded for no-show/forfeit because no players participated.
- If you later want forfeit wins to grant points, make it a separate explicit product rule.
- Disqualification action must be audited and visible in match history.

### 3.8 `cancel_tournament` — Host cancels tournament

```sql
CREATE OR REPLACE FUNCTION cancel_tournament(
    p_tournament_id UUID,
    p_cancellation_reason TEXT,
    p_cancelled_by UUID
)
RETURNS JSON AS $$
DECLARE
    v_tournament RECORD;
BEGIN
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN RETURN json_build_object('success', false, 'error', 'Tournament not found'); END IF;

    -- Only host or admin can cancel
    IF v_tournament.host_user_id != p_cancelled_by THEN
        RETURN json_build_object('success', false, 'error', 'Only the host can cancel');
    END IF;

    -- Update tournament
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
            format('%s has been cancelled. Reason: %s', v_tournament.title, p_cancellation_reason),
            p_tournament_id::TEXT
        FROM tournament_teams tt
        JOIN teams t ON t.id = tt.team_id
        WHERE tt.tournament_id = p_tournament_id;

    RETURN json_build_object('success', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 3.9 `complete_tournament` — Finalize tournament, set placements

```sql
CREATE OR REPLACE FUNCTION complete_tournament(p_tournament_id UUID)
RETURNS JSON AS $$
DECLARE
    v_tournament RECORD;
    v_placement INTEGER := 1;
    v_team_record RECORD;
BEGIN
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN RETURN json_build_object('success', false, 'error', 'Tournament not found'); END IF;

    -- Set placements based on Swiss standings
    FOR v_team_record IN
        SELECT tt.team_id, tt.swiss_points, tt.buchholz_score, tt.sonneborn_berger
            FROM tournament_teams tt
            WHERE tt.tournament_id = p_tournament_id
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
            ON CONFLICT (tournament_id, user_id) DO NOTHING;

        v_placement := v_placement + 1;
    END LOOP;

    -- Update tournament status
    UPDATE tournaments SET
        status = 'completed',
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
```

### 3.10 Host account creation — server-only API route, not SQL password storage

Do not create host passwords in SQL and do not store plaintext passwords. Supabase Auth admin operations require the service-role key, so this belongs in an AdminPanel server-only route or Supabase Edge Function.

Required route behavior:

```typescript
// src/app/api/tournament-host/create-account/route.ts
// Server-only. Never import this from client components.

// 1. Authenticate current admin session.
// 2. Validate tournament id and approved host request.
// 3. Use SUPABASE_SERVICE_ROLE_KEY only on the server.
// 4. Create Supabase Auth user with invite email or temporary password.
// 5. Insert metadata into tournament_host_accounts without password_plain.
// 6. Write admin audit log.
// 7. Return success plus email. Return a temporary password only if it is never persisted.
```

Database metadata insert:

```sql
INSERT INTO tournament_host_accounts (
    tournament_id,
    host_user_id,
    auth_user_id,
    email,
    created_by
) VALUES (
    p_tournament_id,
    p_host_user_id,
    p_auth_user_id,
    p_email,
    (select auth.uid())
);
```

If a SQL helper is still used, it must only validate authorization and insert metadata. It must not generate or return passwords.

---

## 4. TRIGGERS & CONSTRAINTS

### 4.1 One tournament per host per 7 days

```sql
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

CREATE TRIGGER trg_weekly_tournament_limit
    BEFORE INSERT ON tournaments
    FOR EACH ROW
    EXECUTE FUNCTION enforce_weekly_tournament_limit();
```

### 4.2 Auto-calculate swiss_rounds on insert

```sql
CREATE OR REPLACE FUNCTION auto_calculate_swiss_rounds()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.swiss_rounds IS NULL THEN
        NEW.swiss_rounds := CEIL(LOG(2, NEW.max_teams))::INTEGER;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_swiss_rounds
    BEFORE INSERT ON tournaments
    FOR EACH ROW
    EXECUTE FUNCTION auto_calculate_swiss_rounds();
```

### 4.3 Max 15 requirements per tournament, max 5 telegram_subscribe

```sql
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

CREATE TRIGGER trg_requirement_limits
    BEFORE INSERT ON tournament_requirements
    FOR EACH ROW
    EXECUTE FUNCTION enforce_requirement_limits();
```

### 4.4 Auto-block after 3 rejections

```sql
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

CREATE TRIGGER trg_auto_block_3_rejections
    BEFORE UPDATE ON tournament_applications
    FOR EACH ROW
    WHEN (NEW.status = 'rejected' AND OLD.status = 'pending')
    EXECUTE FUNCTION auto_block_after_3_rejections();
```

### 4.5 Set match_auto_complete_at when scheduled_at is set

```sql
CREATE OR REPLACE FUNCTION set_match_auto_complete()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.scheduled_at IS NOT NULL AND NEW.match_auto_complete_at IS NULL THEN
        NEW.match_auto_complete_at := NEW.scheduled_at + (NEW.no_show_grace_period_min || ' minutes')::INTERVAL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_match_auto_complete
    BEFORE INSERT OR UPDATE ON tournament_swiss_matches
    FOR EACH ROW
    WHEN (NEW.scheduled_at IS NOT NULL)
    EXECUTE FUNCTION set_match_auto_complete();
```

### 4.6 Enable Realtime for new tables

```sql
ALTER PUBLICATION supabase_realtime ADD TABLE tournaments;
ALTER PUBLICATION supabase_realtime ADD TABLE tournament_applications;
ALTER PUBLICATION supabase_realtime ADD TABLE tournament_teams;
ALTER PUBLICATION supabase_realtime ADD TABLE tournament_swiss_matches;
ALTER PUBLICATION supabase_realtime ADD TABLE tournament_match_rosters;
ALTER PUBLICATION supabase_realtime ADD TABLE tournament_host_requests;
```

---

## 5. ADMIN PANEL CHANGES (Next.js)

### 5.1 New Admin Page: `/dashboard/tournament-requests`

- List all `tournament_host_requests` with status badges
- Approve/reject with admin notes
- On approve: sets `profiles.is_tournament_host = TRUE`, sends notification to user
- Shows user profile info (username, MLBB ID, existing teams)

### 5.2 New Admin Page: `/dashboard/tournaments`

- Overview of ALL tournaments (admin oversight)
- Flag/unflag tournaments
- Force-cancel abusive tournaments
- Take over tournament (reassign host)
- View any tournament's Swiss bracket
- Admin override on disputed matches

### 5.3 New Host Section: `/host/*` (SEPARATE from admin dashboard)

**Auth system**: Supabase Auth (NOT hardcoded admin credentials)

| Route | Purpose |
|-------|---------|
| `/host/login` | Login with auto-generated email/password |
| `/host/[tournamentId]/dashboard` | Main host dashboard |
| `/host/[tournamentId]/applications` | Review team applications |
| `/host/[tournamentId]/bracket` | Swiss bracket view + management |
| `/host/[tournamentId]/matches` | Match scheduling + results |
| `/host/[tournamentId]/settings` | Edit tournament details |
| `/host/[tournamentId]/chat` | View match conversations |

**Host Dashboard Features**:
- Real-time team application count
- Accept/reject teams with optional reason
- View team details: all player MLBB IDs + Telegram usernames
- Generate Swiss bracket button (after check-in deadline)
- Set match times for each pairing
- Drop room ID/password for matches
- View each team's selected roster for game 1 and game 2
- Mark a team as no-show/disqualified when a leader does not respond at match start
- Add live stream URL (optional)
- Resolve disputed matches
- Mark no-shows
- Complete tournament (finalize placements)

**Security**:
- Separate `HostAuthProvider` context (Supabase Auth)
- Host can ONLY see their own tournament data
- RLS policies enforce data isolation
- No access to admin pages (`/dashboard/*`)
- Middleware redirects `/host/*` to host auth, not admin auth

### 5.4 Middleware Update

```typescript
// Add /host/* routes with separate auth check
// /host/login — public (no auth required)
// /host/[tournamentId]/* — requires Supabase Auth session
//   AND user must be the host of that tournament
```

### 5.5 New Supabase Storage Bucket

```sql
-- For tournament logos
INSERT INTO storage.buckets (id, name, public) VALUES ('tournament-logos', 'tournament-logos', true);
```

---

## 6. ANDROID APK CHANGES

### 6.1 New Data Models

| File | Purpose |
|------|---------|
| `Tournament.kt` | Tournament data class |
| `TournamentRequirement.kt` | Requirement data class |
| `TournamentApplication.kt` | Application data class |
| `TournamentTeam.kt` | Tournament team with Swiss scores |
| `TournamentSwissMatch.kt` | Swiss match pairing |
| `TournamentHostRequest.kt` | Host request form data |

### 6.2 New Repository

| File | Purpose |
|------|---------|
| `TournamentRepository.kt` | Interface |
| `SupabaseTournamentRepository.kt` | Supabase REST implementation |

### 6.3 New ViewModel

| File | Purpose |
|------|---------|
| `TournamentViewModel.kt` | Tournament state management |

### 6.4 New Navigation Routes

```kotlin
object TournamentList : Screen("tournament_list")
object TournamentDetail : Screen("tournament_detail/{tournamentId}") {
    fun createRoute(tournamentId: String) = "tournament_detail/$tournamentId"
}
object TournamentHostRequest : Screen("tournament_host_request")
object TournamentCreate : Screen("tournament_create")
object TournamentHostPanel : Screen("tournament_host_panel/{tournamentId}") {
    fun createRoute(tournamentId: String) = "tournament_host_panel/$tournamentId"
}
```

### 6.5 New Screens

| Screen | Who Sees It | Purpose |
|--------|------------|---------|
| **TournamentListScreen** | Everyone | Browse all tournaments, filter by status/prize/region/skill |
| **TournamentDetailScreen** | Everyone | View tournament info, requirements, teams, Swiss table, apply |
| **TournamentHostRequestScreen** | Regular users | Form to request tournament_host role |
| **TournamentCreateScreen** | tournament_host users | Create tournament (title, desc, prize, requirements, logo, deadlines) |
| **TournamentHostPanelScreen** | tournament_host | View/edit their tournament, manage applications, schedule matches, submit official results |
| **TournamentMatchRosterScreen** | Team leaders | Select the 5 active players for a tournament match |

### 6.6 Profile Update

Add `telegramUsername` field to `UserProfile.kt`:
```kotlin
data class UserProfile(
    // ... existing fields ...
    val telegramUsername: String? = null,
    val isTournamentHost: Boolean = false,
    val hostTrustScore: Float = 5.0f,
)
```

### 6.7 Telegram Username Gate Flow

When team leader clicks "Apply" on TournamentDetailScreen:

```
1. Call apply_for_tournament(tournamentId, teamId) RPC
2. If returns { success: false, error: "missing_telegram_users", missing_telegram_users: [...] }
   → Show dialog: "These teammates need to add their Telegram username: [list]"
   → Each listed player sees a banner on their own profile
3. If returns { success: false, error: "Team needs at least 5 members (currently 3)" }
   → Show dialog: "Your team needs more members to join this tournament"
4. If returns { success: true }
   → Show success, track application status
```

### 6.8 Application Tracking

- Team leader sees application status in tournament detail
- If rejected: shows reason, "Re-Apply" button (if attempts < 3)
- If blocked: shows "Blocked from this tournament"
- Notification on status change

### 6.9 Swiss Table View

- In TournamentDetailScreen, show Swiss bracket/table
- Teams see their upcoming match + countdown timer
- Team leader sees a "Select Players" action before match start
- Spectators see full bracket without match details
- After each round, standings update in real-time (Supabase Realtime)

### 6.9.1 Tournament Match Roster Selection

- Once a team is accepted and Swiss pairings are generated, the leader sees their upcoming match.
- Before game 1 starts, each leader selects exactly 5 active players from their current team members.
- For BO2, before game 2 starts, each leader can keep the same 5 or submit a new game 2 roster.
- If a player loses internet after game 1, the leader can replace that player for game 2 with another current team member.
- The selection is saved to `tournament_match_rosters`.
- Only selected players for each played game earn win/loss and points from that game.
- Non-selected members can still belong to the team but do not receive match points.
- This should reuse the existing scrim roster UX and validation patterns where possible.

### 6.10 Tournament Match Chat

- When match is scheduled, conversation auto-created
- 3 participants: Team A leader, Team B leader, tournament host
- Host drops room ID/password through a participant-only room secret flow or sends it as a participant-only system message
- Reuses existing ChatScreen after adding `conversation_participants` support
- Chat opens at match scheduled time
- After match completes, chat becomes read-only (history only)

### 6.10.1 Tournament Result Validation

- Tournament host is the normal authority for match result validation.
- Host submits winner/loser/draw from the host panel after spectating the room.
- Admin does not validate ordinary tournament wins/losses.
- Admin only handles abuse reports, host misconduct, fraud, or support escalation.
- The result submission must call existing scrim-style points logic, scoped to selected tournament match roster players.

### 6.10.2 No-Response / Disqualification Flow

- If match time arrives and a team leader does not respond, the host can mark that team as no-show/disqualified.
- This action must be available in both the Android APK host tools and the tournament host web panel.
- Host selects the non-responsive team and enters an optional reason.
- The other team receives the tournament match win.
- By default, player win/loss points are not awarded for a no-show because no active roster actually played.
- The disqualification is shown in tournament match history and audit logs.

### 6.11 Bottom Navigation Update

Add tournament icon to `AppBottomNav`:
- New tab: "Tournaments" (trophy icon) between Home and Teams

### 6.12 SupabaseClient Update

Add new table constants:
```kotlin
const val TABLE_TOURNAMENTS = "tournaments"
const val TABLE_TOURNAMENT_REQUIREMENTS = "tournament_requirements"
const val TABLE_TOURNAMENT_APPLICATIONS = "tournament_applications"
const val TABLE_TOURNAMENT_TEAMS = "tournament_teams"
const val TABLE_TOURNAMENT_SWISS_MATCHES = "tournament_swiss_matches"
const val TABLE_TOURNAMENT_MATCH_ROSTERS = "tournament_match_rosters"
const val TABLE_TOURNAMENT_MATCH_ROOM_SECRETS = "tournament_match_room_secrets"
const val TABLE_TOURNAMENT_HOST_REQUESTS = "tournament_host_requests"
const val TABLE_TOURNAMENT_HOST_ACCOUNTS = "tournament_host_accounts"
const val TABLE_TOURNAMENT_PLAYER_STATS = "tournament_player_stats"
const val BUCKET_TOURNAMENT_LOGOS = "tournament-logos"
```

---

## 7. NOTIFICATION TYPES

### 7.1 New NotificationType enum values (Android)

```kotlin
enum class NotificationType {
    // ... existing types ...
    TOURNAMENT_HOST_APPROVED,
    TOURNAMENT_HOST_REJECTED,
    TOURNAMENT_APPLICATION_NEW,       // For host: new team applied
    TOURNAMENT_APPLICATION_ACCEPTED,  // For team: you got in
    TOURNAMENT_APPLICATION_REJECTED,  // For team: you were rejected
    TOURNAMENT_APPLICATION_BLOCKED,   // For team: blocked after 3 rejections
    TOURNAMENT_MATCH_SCHEDULED,       // For teams: match is scheduled
    TOURNAMENT_MATCH_STARTING,        // For teams: match starts in 15 min
    TOURNAMENT_ROSTER_REQUIRED,       // For leaders: select 5 active players
    TOURNAMENT_ROSTER_LOCKED,         // For team: active roster confirmed
    TOURNAMENT_ROSTER_CHANGE_ALLOWED, // For leaders: game 2 roster can be changed
    TOURNAMENT_ROOM_READY,            // For teams: host dropped room ID
    TOURNAMENT_MATCH_RESULT,          // For teams: match result recorded
    TOURNAMENT_TEAM_DISQUALIFIED,     // For team: no-show/forfeit decision
    TOURNAMENT_ROUND_ADVANCED,        // For all: new Swiss round generated
    TOURNAMENT_CANCELLED,             // For all: tournament cancelled
    TOURNAMENT_COMPLETED,             // For all: tournament finished
    TOURNAMENT_DISPUTE,               // For host: match disputed
    TOURNAMENT_NO_SHOW,              // For host: team didn't show up
}
```

### 7.2 Admin Panel Notification Bell

Add tournament-related notifications to `NotificationBell.tsx`:
- New tournament host requests (for admins)
- Disputed matches (for admins)

---

## 8. IMPLEMENTATION ORDER

### Phase 0: Stabilize Existing Project
| Step | What | Files |
|------|------|-------|
| 0.1 | Fix Android Kotlin compilation blockers before tournament work | `AuthNavigation.kt`, `BannedScreen.kt` |
| 0.2 | Remove browser access to service-role Supabase key | `AdminPanel/src/lib/supabase.ts`, AdminPanel API routes |
| 0.3 | Decide server-only privileged route pattern | AdminPanel `src/app/api/*` |
| 0.4 | Run baseline checks | Android `gradlew test`, AdminPanel `npm.cmd run build` |

### Phase 1: Database Foundation
| Step | What | Files |
|------|------|-------|
| 1.1 | Write migration SQL | `supabase/migrations/20240601_tournament_system.sql` |
| 1.2 | Add `conversation_participants` and room secrets table | Same migration |
| 1.3 | Add RLS policies, RPC grants/revokes, and audit logging | Same migration |
| 1.4 | Add indexes for filters, joins, and active tournament screens | Same migration |
| 1.5 | Run migration on Supabase | SQL Editor |
| 1.6 | Verify tables + RLS + triggers | Supabase Dashboard |
| 1.7 | Add `tournament-logos` storage bucket with owner-scoped policies | Supabase Storage |

### Phase 2: Admin Panel — Admin Features
| Step | What | Files |
|------|------|-------|
| 2.1 | Tournament host request review page | `src/app/dashboard/tournament-requests/page.tsx` |
| 2.2 | Tournament oversight page | `src/app/dashboard/tournaments/page.tsx` |
| 2.3 | Add nav items to DashboardLayout | `src/components/DashboardLayout.tsx` |
| 2.4 | Update SecureQueryBuilder whitelist | `src/lib/security.ts` |
| 2.5 | Add tournament i18n strings | `src/locales/en.ts`, `src/locales/ru.ts` |
| 2.6 | Add tournament types | `src/types/database.ts` |

### Phase 3: Admin Panel — Host Section
| Step | What | Files |
|------|------|-------|
| 3.1 | Host auth context (Supabase Auth) | `src/contexts/HostAuthContext.tsx` |
| 3.2 | Host login page | `src/app/host/login/page.tsx` |
| 3.3 | Host layout | `src/app/host/layout.tsx` |
| 3.4 | Host dashboard | `src/app/host/[tournamentId]/dashboard/page.tsx` |
| 3.5 | Host applications page | `src/app/host/[tournamentId]/applications/page.tsx` |
| 3.6 | Host bracket page | `src/app/host/[tournamentId]/bracket/page.tsx` |
| 3.7 | Host matches page | `src/app/host/[tournamentId]/matches/page.tsx` |
| 3.8 | Host settings page | `src/app/host/[tournamentId]/settings/page.tsx` |
| 3.9 | Middleware update for /host/* | `src/middleware.ts` |
| 3.10 | API route: create host auth account | `src/app/api/tournament-host/create-account/route.ts` |
| 3.11 | Host no-show/disqualification action | Host matches page + server route/RPC |

### Phase 4: Android — Data Layer
| Step | What | Files |
|------|------|-------|
| 4.1 | Tournament data models | `data/model/Tournament.kt` + related |
| 4.2 | Tournament repository interface | `data/repository/TournamentRepository.kt` |
| 4.3 | Supabase tournament repository | `data/repository/SupabaseTournamentRepository.kt` |
| 4.4 | Update SupabaseConfig table constants | `data/service/SupabaseClient.kt` |
| 4.5 | Update UserProfile model | `data/model/UserProfile.kt` |
| 4.6 | Add Room entities/DAOs for cacheable tournament data | `data/local/*` |
| 4.7 | Add tournament match roster model and per-game RPC calls | `data/model/*`, tournament repository |
| 4.8 | Wire `UnifiedCacheManager` cache keys + invalidation | Tournament repository |
| 4.9 | Tournament ViewModel | `viewmodel/TournamentViewModel.kt` |

### Phase 5: Android — UI Screens
| Step | What | Files |
|------|------|-------|
| 5.1 | TournamentListScreen | `ui/screens/TournamentListScreen.kt` |
| 5.2 | TournamentDetailScreen | `ui/screens/TournamentDetailScreen.kt` |
| 5.3 | TournamentHostRequestScreen | `ui/screens/TournamentHostRequestScreen.kt` |
| 5.4 | TournamentCreateScreen | `ui/screens/TournamentCreateScreen.kt` |
| 5.5 | TournamentHostPanelScreen | `ui/screens/TournamentHostPanelScreen.kt` |
| 5.6 | Add navigation routes | `ui/navigation/AuthNavigation.kt` |
| 5.7 | Update bottom nav | `ui/components/AppBottomNav.kt` |
| 5.8 | Update ProfileScreen (telegram field) | `ui/screens/ProfileScreen.kt` |
| 5.9 | Update ChatScreen for `conversation_participants` | `ui/screens/ChatScreen.kt` |
| 5.10 | Add match roster selection screen/action | Reuse scrim roster patterns |
| 5.11 | Add host no-show/disqualification action | Host APK tools / host panel screen |

### Phase 6: Integration & Polish
| Step | What | Files |
|------|------|-------|
| 6.1 | Wire notifications for all tournament events | Both projects |
| 6.2 | Test Swiss bracket generation end-to-end | Supabase + Admin |
| 6.3 | Test match chat with 3 participants | Both projects |
| 6.4 | Test dispute resolution flow | Admin Panel |
| 6.5 | Test no-show auto-complete | Supabase RPC |
| 6.6 | Test cancellation + reputation update | Supabase RPC |
| 6.7 | Test complete tournament lifecycle | Full stack |

---

## 9. SECURITY & OPTIMIZATION REQUIREMENTS

### 9.1 Security Baseline

- AdminPanel browser code must use anon-key Supabase clients only.
- Service-role key must only be read in server-only API routes, Edge Functions, migration scripts, or trusted admin tooling.
- Every tournament RPC using `SECURITY DEFINER` must set `search_path = public`, validate `(select auth.uid())`, revoke public execute, and grant only the exact roles that need it.
- Tournament workflow transitions must be idempotent. Repeated requests should return the existing result, not duplicate applications, matches, conversations, or notifications.
- Public tournament and match reads must use safe views or selected columns that exclude room credentials, admin notes, private Telegram usernames, and internal dispute data.
- Storage policies for `tournament-logos` must restrict uploads to the tournament host or admin and validate file type/size in the client and server route.
- Add an audit table for host approvals, account creation, application decisions, bracket generation, result overrides, cancellations, room credential updates, and dispute decisions.
- Audit all roster substitutions and no-show/disqualification actions, including actor, reason, previous player, replacement player, game number, and timestamp.

### 9.2 Database Optimization

- Add composite indexes for common filters: `(status, registration_deadline)`, `(region, skill_level, status)`, `(tournament_id, status)`, `(tournament_id, round_number)`, `(conversation_id, user_id)`.
- Use `(select auth.uid())` in RLS policies to avoid repeated auth initplan work.
- Prefer RPCs for multi-table mutations so writes are atomic and cache invalidation can be predictable.
- Do not subscribe globally to all tournament tables. Subscribe only to the active tournament detail, match, applications, or notification scope.
- Keep tournament list payloads small: list views should not load requirements, all teams, all matches, or chat data.

### 9.3 Android Cache Strategy

Use the existing `UnifiedCacheManager` pattern:

| Data | Cache key | Memory TTL | Room TTL | Invalidate when |
|------|-----------|------------|----------|-----------------|
| Tournament list | `tournaments_list_{filters}` | 2 min | 10 min | tournament create/update/cancel |
| Tournament detail | `tournament_detail_{id}` | 1 min | 5 min | application, team, match, status update |
| My tournament applications | `tournament_apps_user_{userId}` | 30 sec | 2 min | apply/reapply/status change |
| Tournament matches | `tournament_matches_{id}` | 15 sec | 1 min | schedule/result/dispute/round advance |
| Tournament match roster | `tournament_roster_{matchId}_{teamId}_{game}` | 15 sec | 1 min | leader changes selected players |
| Host dashboard | `tournament_host_{id}` | 15 sec | 1 min | any host action |

Repository rules:
- Emit cached Room data immediately, then refresh network data in the background.
- Use prefix invalidation after writes: `tournaments_`, `tournament_detail_`, `tournament_apps_`, `tournament_matches_`, `tournament_host_`.
- Promote Realtime updates into Room and memory cache instead of only invalidating and refetching.
- Use paginated APIs for tournament lists and applications. Default page size should be 20-50.

### 9.4 AdminPanel Optimization

- Put privileged Supabase calls behind server routes.
- Cache read-heavy admin overview data server-side with short TTLs or explicit refresh.
- Keep host dashboard queries scoped by `tournamentId`.
- Avoid client-side broad `.select('*')` on tournament tables.
- Fix existing lint/compiler-rule errors before adding large host sections, otherwise future changes will be hard to review.

---

## QUICK REFERENCE: Table Summary

| # | Table | Purpose |
|---|-------|---------|
| 1 | `tournament_host_requests` | Users requesting host role |
| 2 | `tournaments` | Core tournament data |
| 3 | `tournament_requirements` | Up to 15 requirements per tournament |
| 4 | `tournament_applications` | Teams applying to join |
| 5 | `tournament_teams` | Accepted teams with Swiss scores |
| 6 | `tournament_swiss_matches` | Swiss round pairings + results |
| 7 | `tournament_match_rosters` | Active players selected by leaders for match points |
| 8 | `tournament_host_accounts` | Host auth metadata only; no plaintext passwords |
| 9 | `tournament_player_stats` | Per-player tournament history |
| 10 | `conversation_participants` | Group-safe chat membership for tournament conversations |
| 11 | `tournament_match_room_secrets` | Participant-only room credentials |
| 12 | `profiles` (extended) | + is_tournament_host, telegram_username, host reputation |

## QUICK REFERENCE: RPC Functions

| # | Function | Purpose |
|---|----------|---------|
| 1 | `apply_for_tournament` | Team applies with full validation (size, telegram, blocked check) |
| 2 | `review_tournament_application` | Host accepts/rejects with auto-block after 3 |
| 3 | `generate_swiss_pairings` | Swiss bracket generation (host triggers) |
| 4 | `set_tournament_match_roster` | Team leader selects 5 active players |
| 5 | `submit_tournament_match_result` | Host-authoritative result submission |
| 6 | `award_tournament_match_points` | Award win/loss points only to selected players |
| 7 | `update_tournament_scores` | Recalculate Swiss scores after result |
| 8 | `recalculate_tiebreakers` | Buchholz + Sonneborn-Berger |
| 9 | `disqualify_tournament_team` | Host marks non-responsive team as no-show/forfeit |
| 10 | `check_tournament_no_shows` | Auto-complete overdue matches if automated fallback is enabled |
| 11 | `cancel_tournament` | Host cancels with reputation penalty |
| 12 | `complete_tournament` | Finalize placements + player stats |
| 13 | `create_tournament_host_account` | Server-only API route creates Supabase Auth account and stores metadata only |

## QUICK REFERENCE: Key Design Decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Match chat | Add `conversation_participants` | Avoids hard-coded 3-user chat and supports admin/support later |
| Host auth | Supabase Auth (separate from admin) | Admin uses hardcoded creds; hosts need real auth accounts |
| Swiss rounds | Auto-calculated ceil(log2(max_teams)) | Standard Swiss format |
| BO2 draws | 1 point each | Standard Swiss scoring |
| Match roster | Leaders select 5 active players per match | Only participating players receive win/loss and points |
| BO2 substitution | Leaders can submit a separate game 2 roster | Handles internet drops and mid-match replacement without rewriting game 1 history |
| Result authority | Tournament host confirms result | Host creates/spectates room and is responsible for ordinary tournament result validation |
| No-response handling | Host can disqualify no-show team from APK or host web panel | Keeps tournament moving when a leader does not respond |
| Swiss trigger | Host clicks button | Gives host control over timing |
| Weekly limit | 1 tournament per host per 7 days | DB trigger enforces |
| Application limit | 3 attempts then auto-block | DB trigger enforces |
| Tiebreakers | Buchholz + Sonneborn-Berger | Standard Swiss tiebreakers |
| No-show | 15 min grace, auto-complete | DB trigger sets deadline |
| Host account | Server-only Auth admin route, no stored plaintext password | Prevents credential leakage and keeps service-role key off the client |
