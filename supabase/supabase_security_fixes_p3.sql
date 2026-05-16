-- ============================================================
-- Supabase Database Security Fixes — Part 3 of 3
-- Fix remaining tables and verify
-- ============================================================

-- match_results
DROP POLICY IF EXISTS "Match participants can view results" ON match_results;
DROP POLICY IF EXISTS "Match participants can upload screenshots" ON match_results;
DROP POLICY IF EXISTS "Admins can verify results" ON match_results;

CREATE POLICY "Match participants can view results"
    ON match_results FOR SELECT
    USING (true);

CREATE POLICY "Match participants can upload screenshots"
    ON match_results FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM matches m
            JOIN teams t ON t.id = m.team_a_id OR t.id = m.team_b_id
            WHERE m.id = match_id AND t.leader_id = (select auth.uid())
        )
    );

CREATE POLICY "Admins can verify results"
    ON match_results FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM profiles p
            WHERE p.id = (select auth.uid())
            AND p.is_admin = TRUE
        )
    );

-- team_invitations
DROP POLICY IF EXISTS "Users can view invitations sent to them" ON team_invitations;
DROP POLICY IF EXISTS "Team leaders can invite players" ON team_invitations;
DROP POLICY IF EXISTS "Invited users can accept/reject invitations" ON team_invitations;
DROP POLICY IF EXISTS "Allow read team_invitations" ON team_invitations;

CREATE POLICY "Allow read team_invitations"
    ON team_invitations FOR SELECT
    USING (true);

CREATE POLICY "Team leaders can invite players"
    ON team_invitations FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM teams t
            WHERE t.id = team_id AND t.leader_id = (select auth.uid())
        )
    );

CREATE POLICY "Users can view invitations sent to them"
    ON team_invitations FOR SELECT
    USING ((select auth.uid()) = invited_user_id);

CREATE POLICY "Invited users can accept/reject invitations"
    ON team_invitations FOR UPDATE
    USING ((select auth.uid()) = invited_user_id);

-- scrim_rosters
DROP POLICY IF EXISTS "Users can view scrim rosters they participate in" ON scrim_rosters;
DROP POLICY IF EXISTS "Team leaders can manage own scrim rosters" ON scrim_rosters;

CREATE POLICY "Users can view scrim rosters"
    ON scrim_rosters FOR SELECT
    USING (true);

CREATE POLICY "Team leaders can manage scrim rosters"
    ON scrim_rosters FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM scrims s
            JOIN teams t ON t.id = s.team_id
            WHERE s.id = scrim_id AND t.leader_id = (select auth.uid())
        )
    );

-- ═══════════════════════════════════════════════════════════════
-- VERIFICATION
-- ═══════════════════════════════════════════════════════════════

SELECT
    schemaname,
    tablename,
    rowsecurity,
    polname,
    polcmd
FROM pg_policies
WHERE schemaname = 'public'
ORDER BY tablename, polcmd;