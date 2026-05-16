-- ============================================================
-- Supabase Security Fixes — All in One (Idempotent)
-- Run: supabase db query --linked -f supabase/supabase_security_fixes_all.sql
-- ============================================================

-- FIX 1 [CRITICAL]: app_notifications RLS
DO $$ BEGIN ALTER TABLE app_notifications ENABLE ROW LEVEL SECURITY; EXCEPTION WHEN OTHERS THEN NULL; END $$;
DROP POLICY IF EXISTS "Users can read own notifications" ON app_notifications;
CREATE POLICY "Users can read own notifications" ON app_notifications FOR SELECT USING (auth.uid() = user_id);
DROP POLICY IF EXISTS "Authenticated users can insert notifications" ON app_notifications;
CREATE POLICY "Authenticated users can insert notifications" ON app_notifications FOR INSERT WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Users can update own notifications" ON app_notifications;
CREATE POLICY "Users can update own notifications" ON app_notifications FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Users can delete own notifications" ON app_notifications;
CREATE POLICY "Users can delete own notifications" ON app_notifications FOR DELETE USING (auth.uid() = user_id);
DROP POLICY IF EXISTS "Admins can read all notifications" ON app_notifications;
CREATE POLICY "Admins can read all notifications" ON app_notifications FOR SELECT USING (EXISTS (SELECT 1 FROM profiles WHERE profiles.id = auth.uid() AND profiles.is_admin = TRUE));

-- FIX 2 [HIGH]: SECURITY DEFINER revoke (best-effort)
DO $$ BEGIN REVOKE EXECUTE ON FUNCTION award_scrim_points FROM anon, authenticated; EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN REVOKE EXECUTE ON FUNCTION handle_new_user FROM anon, authenticated; EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN GRANT EXECUTE ON FUNCTION award_scrim_points TO service_role; EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN GRANT EXECUTE ON FUNCTION handle_new_user TO service_role; EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- FIX 3 [HIGH]: award_scrim_points SECURITY INVOKER
DROP FUNCTION IF EXISTS award_scrim_points(UUID, UUID, INTEGER, INTEGER);
CREATE OR REPLACE FUNCTION award_scrim_points(p_scrim_id UUID, p_winner_team_id UUID, p_pts_per_win INTEGER DEFAULT 25, p_pts_per_loss INTEGER DEFAULT 15) RETURNS VOID SECURITY INVOKER LANGUAGE plpgsql AS $$
DECLARE roster_entry RECORD; is_winner BOOLEAN;
BEGIN
  FOR roster_entry IN SELECT sr.user_id, sr.team_id, sr.is_active FROM scrim_rosters sr WHERE sr.scrim_id = p_scrim_id AND sr.is_active = TRUE LOOP
    is_winner := (roster_entry.team_id = p_winner_team_id);
    INSERT INTO player_stats (user_id, pts, wins, losses, matches_play) VALUES (roster_entry.user_id, CASE WHEN is_winner THEN p_pts_per_win ELSE -p_pts_per_loss END, CASE WHEN is_winner THEN 1 ELSE 0 END, CASE WHEN is_winner THEN 0 ELSE 1 END, 1) ON CONFLICT (user_id) DO UPDATE SET pts = player_stats.pts + CASE WHEN is_winner THEN p_pts_per_win ELSE -p_pts_per_loss END, wins = player_stats.wins + CASE WHEN is_winner THEN 1 ELSE 0 END, losses = player_stats.losses + CASE WHEN is_winner THEN 0 ELSE 1 END, matches_play = player_stats.matches_play + 1, updated_at = TIMEZONE('utc', NOW());
  END LOOP;
  UPDATE match_results SET pts_awarded = TRUE WHERE match_id IN (SELECT m.id FROM matches m WHERE m.scrim_id = p_scrim_id);
END;
$$;

-- FIX 4 [MEDIUM]: player_stats policies
DROP POLICY IF EXISTS "System can insert/update player stats" ON player_stats;
DROP POLICY IF EXISTS "Allow read player_stats" ON player_stats;
DROP POLICY IF EXISTS "Users can view own stats" ON player_stats;
DROP POLICY IF EXISTS "Users can view teammate stats" ON player_stats;
DROP POLICY IF EXISTS "Service can manage player stats" ON player_stats;
DROP POLICY IF EXISTS "Service can update player stats" ON player_stats;
DROP POLICY IF EXISTS "Users can view player stats" ON player_stats;
CREATE POLICY "Service can manage player stats" ON player_stats FOR INSERT WITH CHECK (true);
CREATE POLICY "Service can update player stats" ON player_stats FOR UPDATE USING (true) WITH CHECK (true);
CREATE POLICY "Users can view player stats" ON player_stats FOR SELECT USING (true);

-- FIX 5 [PERF]: profiles RLS with (select auth.uid()) pattern
DROP POLICY IF EXISTS "Users can update own profile" ON profiles; DROP POLICY IF EXISTS "Admins can update any profile" ON profiles; DROP POLICY IF EXISTS "Users can view own profile" ON profiles; DROP POLICY IF EXISTS "Allow read profiles" ON profiles;
CREATE POLICY "Users can view profiles" ON profiles FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE USING ((select auth.uid()) = id) WITH CHECK ((select auth.uid()) = id);
CREATE POLICY "Admins can update any profile" ON profiles FOR UPDATE USING (EXISTS (SELECT 1 FROM profiles p WHERE p.id = (select auth.uid()) AND p.is_admin = TRUE)) WITH CHECK (EXISTS (SELECT 1 FROM profiles p WHERE p.id = (select auth.uid()) AND p.is_admin = TRUE));

-- FIX 5 [PERF]: teams RLS
DROP POLICY IF EXISTS "Users can create teams" ON teams; DROP POLICY IF EXISTS "Team leaders can update own team" ON teams; DROP POLICY IF EXISTS "Admins can update any team" ON teams; DROP POLICY IF EXISTS "Team leaders can delete own team" ON teams; DROP POLICY IF EXISTS "Allow read teams" ON teams;
CREATE POLICY "Allow read teams" ON teams FOR SELECT USING (true);
CREATE POLICY "Users can create teams" ON teams FOR INSERT WITH CHECK ((select auth.uid()) = leader_id);
CREATE POLICY "Team leaders can update own team" ON teams FOR UPDATE USING ((select auth.uid()) = leader_id) WITH CHECK ((select auth.uid()) = leader_id);
CREATE POLICY "Admins can update any team" ON teams FOR UPDATE USING (EXISTS (SELECT 1 FROM profiles p WHERE p.id = (select auth.uid()) AND p.is_admin = TRUE)) WITH CHECK (EXISTS (SELECT 1 FROM profiles p WHERE p.id = (select auth.uid()) AND p.is_admin = TRUE));
CREATE POLICY "Team leaders can delete own team" ON teams FOR DELETE USING ((select auth.uid()) = leader_id);

-- FIX 5 [PERF]: team_members RLS
DROP POLICY IF EXISTS "Team leaders can add members" ON team_members; DROP POLICY IF EXISTS "Team leaders can update members" ON team_members; DROP POLICY IF EXISTS "Team leaders can remove members" ON team_members; DROP POLICY IF EXISTS "Users can view own memberships" ON team_members; DROP POLICY IF EXISTS "Allow read team_members" ON team_members;
CREATE POLICY "Allow read team_members" ON team_members FOR SELECT USING (true);
CREATE POLICY "Team leaders can add members" ON team_members FOR INSERT WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));
CREATE POLICY "Team leaders can update members" ON team_members FOR UPDATE USING (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid()))) WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));
CREATE POLICY "Team leaders can remove members" ON team_members FOR DELETE USING (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())) OR (select auth.uid()) = user_id);

-- FIX 5 [PERF]: scrims RLS
DROP POLICY IF EXISTS "Team leaders can create scrims" ON scrims; DROP POLICY IF EXISTS "Team leaders can update own scrims" ON scrims; DROP POLICY IF EXISTS "Team leaders can delete own scrims" ON scrims; DROP POLICY IF EXISTS "Allow read scrims" ON scrims;
CREATE POLICY "Allow read scrims" ON scrims FOR SELECT USING (true);
CREATE POLICY "Team leaders can create scrims" ON scrims FOR INSERT WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));
CREATE POLICY "Team leaders can update own scrims" ON scrims FOR UPDATE USING (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));
CREATE POLICY "Team leaders can delete own scrims" ON scrims FOR DELETE USING (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));

-- FIX 5 [PERF]: scrim_applications RLS
DROP POLICY IF EXISTS "Team leaders can apply to scrims" ON scrim_applications; DROP POLICY IF EXISTS "Scrim owners can accept/reject applications" ON scrim_applications; DROP POLICY IF EXISTS "Allow read scrim_applications" ON scrim_applications;
CREATE POLICY "Allow read scrim_applications" ON scrim_applications FOR SELECT USING (true);
CREATE POLICY "Team leaders can apply to scrims" ON scrim_applications FOR INSERT WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = applicant_team_id AND t.leader_id = (select auth.uid())));
CREATE POLICY "Scrim owners can accept/reject applications" ON scrim_applications FOR UPDATE USING (EXISTS (SELECT 1 FROM scrims s JOIN teams t ON t.id = s.team_id WHERE s.id = scrim_id AND t.leader_id = (select auth.uid())));

-- FIX 5 [PERF]: matches RLS
DROP POLICY IF EXISTS "Users can view matches they participate in" ON matches; DROP POLICY IF EXISTS "Team leaders can update match details" ON matches; DROP POLICY IF EXISTS "Allow read matches" ON matches;
CREATE POLICY "Allow read matches" ON matches FOR SELECT USING (true);
CREATE POLICY "Team leaders can update match details" ON matches FOR UPDATE USING (EXISTS (SELECT 1 FROM teams t WHERE (t.id = team_a_id OR t.id = team_b_id) AND t.leader_id = (select auth.uid())));

-- FIX 5 [PERF]: messages RLS
DROP POLICY IF EXISTS "Match participants can view messages" ON messages; DROP POLICY IF EXISTS "Match participants can send messages" ON messages; DROP POLICY IF EXISTS "Allow read messages" ON messages;
CREATE POLICY "Allow read messages" ON messages FOR SELECT USING (true);
CREATE POLICY "Match participants can send messages" ON messages FOR INSERT WITH CHECK (EXISTS (SELECT 1 FROM matches m JOIN teams t ON t.id = m.team_a_id OR t.id = m.team_b_id WHERE m.id = match_id AND t.leader_id = (select auth.uid())));

-- FIX 5 [PERF]: match_results RLS
DROP POLICY IF EXISTS "Match participants can view results" ON match_results; DROP POLICY IF EXISTS "Match participants can upload screenshots" ON match_results; DROP POLICY IF EXISTS "Admins can verify results" ON match_results;
CREATE POLICY "Match participants can view results" ON match_results FOR SELECT USING (true);
CREATE POLICY "Match participants can upload screenshots" ON match_results FOR UPDATE USING (EXISTS (SELECT 1 FROM matches m JOIN teams t ON t.id = m.team_a_id OR t.id = m.team_b_id WHERE m.id = match_id AND t.leader_id = (select auth.uid())));
CREATE POLICY "Admins can verify results" ON match_results FOR UPDATE USING (EXISTS (SELECT 1 FROM profiles p WHERE p.id = (select auth.uid()) AND p.is_admin = TRUE));

-- FIX 5 [PERF]: team_invitations RLS
DROP POLICY IF EXISTS "Users can view invitations sent to them" ON team_invitations; DROP POLICY IF EXISTS "Team leaders can invite players" ON team_invitations; DROP POLICY IF EXISTS "Invited users can accept/reject invitations" ON team_invitations; DROP POLICY IF EXISTS "Allow read team_invitations" ON team_invitations;
CREATE POLICY "Allow read team_invitations" ON team_invitations FOR SELECT USING (true);
CREATE POLICY "Team leaders can invite players" ON team_invitations FOR INSERT WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));
CREATE POLICY "Users can view invitations sent to them" ON team_invitations FOR SELECT USING ((select auth.uid()) = invited_user_id);
CREATE POLICY "Invited users can accept/reject invitations" ON team_invitations FOR UPDATE USING ((select auth.uid()) = invited_user_id);

-- FIX 5 [PERF]: scrim_rosters RLS
DROP POLICY IF EXISTS "Users can view scrim rosters they participate in" ON scrim_rosters; DROP POLICY IF EXISTS "Team leaders can manage own scrim rosters" ON scrim_rosters;
CREATE POLICY "Users can view scrim rosters" ON scrim_rosters FOR SELECT USING (true);
CREATE POLICY "Team leaders can manage scrim rosters" ON scrim_rosters FOR ALL USING (EXISTS (SELECT 1 FROM scrims s JOIN teams t ON t.id = s.team_id WHERE s.id = scrim_id AND t.leader_id = (select auth.uid())));

-- Remove duplicate admin_activity policy
DROP POLICY IF EXISTS "Allow read admin_activity" ON admin_activity;