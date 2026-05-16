-- ============================================================
-- Supabase Database Security Fixes — Part 2 of 3
-- Fix all auth_rls_initplan issues and optimize RLS policies
-- ============================================================

-- ═══════════════════════════════════════════════════════════════
-- FIX 5 [PERFORMANCE]: Fix auth_rls_initplan — optimize auth.uid() calls
-- Apply (select auth.uid()) pattern to all policies
-- ═══════════════════════════════════════════════════════════════

-- profiles
DROP POLICY IF EXISTS "Users can update own profile" ON profiles;
DROP POLICY IF EXISTS "Admins can update any profile" ON profiles;
DROP POLICY IF EXISTS "Users can view own profile" ON profiles;
DROP POLICY IF EXISTS "Allow read profiles" ON profiles;

CREATE POLICY "Users can view profiles"
    ON profiles FOR SELECT
    USING (true);

CREATE POLICY "Users can update own profile"
    ON profiles FOR UPDATE
    USING ((select auth.uid()) = id)
    WITH CHECK ((select auth.uid()) = id);

CREATE POLICY "Admins can update any profile"
    ON profiles FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM profiles p
            WHERE p.id = (select auth.uid())
            AND p.is_admin = TRUE
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM profiles p
            WHERE p.id = (select auth.uid())
            AND p.is_admin = TRUE
        )
    );

-- teams
DROP POLICY IF EXISTS "Users can create teams" ON teams;
DROP POLICY IF EXISTS "Team leaders can update own team" ON teams;
DROP POLICY IF EXISTS "Admins can update any team" ON teams;
DROP POLICY IF EXISTS "Team leaders can delete own team" ON teams;
DROP POLICY IF EXISTS "Allow read teams" ON teams;

CREATE POLICY "Allow read teams"
    ON teams FOR SELECT
    USING (true);

CREATE POLICY "Users can create teams"
    ON teams FOR INSERT
    WITH CHECK ((select auth.uid()) = leader_id);

CREATE POLICY "Team leaders can update own team"
    ON teams FOR UPDATE
    USING ((select auth.uid()) = leader_id)
    WITH CHECK ((select auth.uid()) = leader_id);

CREATE POLICY "Admins can update any team"
    ON teams FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM profiles p
            WHERE p.id = (select auth.uid())
            AND p.is_admin = TRUE
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM profiles p
            WHERE p.id = (select auth.uid())
            AND p.is_admin = TRUE
        )
    );

CREATE POLICY "Team leaders can delete own team"
    ON teams FOR DELETE
    USING ((select auth.uid()) = leader_id);

-- team_members
DROP POLICY IF EXISTS "Team leaders can add members" ON team_members;
DROP POLICY IF EXISTS "Team leaders can update members" ON team_members;
DROP POLICY IF EXISTS "Team leaders can remove members" ON team_members;
DROP POLICY IF EXISTS "Users can view own memberships" ON team_members;
DROP POLICY IF EXISTS "Allow read team_members" ON team_members;

CREATE POLICY "Allow read team_members"
    ON team_members FOR SELECT
    USING (true);

CREATE POLICY "Team leaders can add members"
    ON team_members FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = team_id AND t.leader_id = (select auth.uid())
        )
    );

CREATE POLICY "Team leaders can update members"
    ON team_members FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = team_id AND t.leader_id = (select auth.uid())
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = team_id AND t.leader_id = (select auth.uid())
        )
    );

CREATE POLICY "Team leaders can remove members"
    ON team_members FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = team_id AND t.leader_id = (select auth.uid())
        )
        OR (select auth.uid()) = user_id
    );

-- scrims
DROP POLICY IF EXISTS "Team leaders can create scrims" ON scrims;
DROP POLICY IF EXISTS "Team leaders can update own scrims" ON scrims;
DROP POLICY IF EXISTS "Team leaders can delete own scrims" ON scrims;
DROP POLICY IF EXISTS "Allow read scrims" ON scrims;

CREATE POLICY "Allow read scrims"
    ON scrims FOR SELECT
    USING (true);

CREATE POLICY "Team leaders can create scrims"
    ON scrims FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = team_id AND t.leader_id = (select auth.uid())
        )
    );

CREATE POLICY "Team leaders can update own scrims"
    ON scrims FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = team_id AND t.leader_id = (select auth.uid())
        )
    );

CREATE POLICY "Team leaders can delete own scrims"
    ON scrims FOR DELETE
    USING (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = team_id AND t.leader_id = (select auth.uid())
        )
    );

-- scrim_applications
DROP POLICY IF EXISTS "Team leaders can apply to scrims" ON scrim_applications;
DROP POLICY IF EXISTS "Scrim owners can accept/reject applications" ON scrim_applications;
DROP POLICY IF EXISTS "Allow read scrim_applications" ON scrim_applications;

CREATE POLICY "Allow read scrim_applications"
    ON scrim_applications FOR SELECT
    USING (true);

CREATE POLICY "Team leaders can apply to scrims"
    ON scrim_applications FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = applicant_team_id AND t.leader_id = (select auth.uid())
        )
    );

CREATE POLICY "Scrim owners can accept/reject applications"
    ON scrim_applications FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM scrims s
            JOIN teams t ON t.id = s.team_id
            WHERE s.id = scrim_id AND t.leader_id = (select auth.uid())
        )
    );

-- matches
DROP POLICY IF EXISTS "Users can view matches they participate in" ON matches;
DROP POLICY IF EXISTS "Team leaders can update match details" ON matches;
DROP POLICY IF EXISTS "Allow read matches" ON matches;

CREATE POLICY "Allow read matches"
    ON matches FOR SELECT
    USING (true);

CREATE POLICY "Team leaders can update match details"
    ON matches FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE (t.id = team_a_id OR t.id = team_b_id)
            AND t.leader_id = (select auth.uid())
        )
    );

-- messages
DROP POLICY IF EXISTS "Match participants can view messages" ON messages;
DROP POLICY IF EXISTS "Match participants can send messages" ON messages;
DROP POLICY IF EXISTS "Allow read messages" ON messages;

CREATE POLICY "Allow read messages"
    ON messages FOR SELECT
    USING (true);

CREATE POLICY "Match participants can send messages"
    ON messages FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM matches m
            JOIN teams t ON t.id = m.team_a_id OR t.id = m.team_b_id
            WHERE m.id = match_id AND t.leader_id = (select auth.uid())
        )
    );