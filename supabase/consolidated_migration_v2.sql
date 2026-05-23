-- ═══════════════════════════════════════════════════════════════════════════════
-- MLBB Scrim Host — Consolidated Migration v2
-- Replaces ALL previous security_fixes*.sql and supabase_security_fixes*.sql files.
-- Run this ONCE in Supabase SQL Editor (or psql) to bring DB to current state.
-- Idempotent where possible; destructive changes are clearly marked.
-- ═══════════════════════════════════════════════════════════════════════════════

-- ┌─────────────────────────────────────────────────────────────────────────────┐
-- │ SECTION 1: RLS — Enable on all tables + correct policies                   │
-- └─────────────────────────────────────────────────────────────────────────────┘

-- 1a. Enable RLS on every table (idempotent)
ALTER TABLE IF EXISTS public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.team_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.scrims ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.scrim_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.scrim_rosters ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.match_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.team_invitations ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.player_stats ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.app_notifications ENABLE ROW LEVEL SECURITY;

-- 1b. profiles — SELECT public, UPDATE own/admin only
DROP POLICY IF EXISTS "Users can view profiles" ON profiles;
DROP POLICY IF EXISTS "Users can view all profiles" ON profiles;
DROP POLICY IF EXISTS "Users can view all profiles" ON profiles;
CREATE POLICY "Users can view all profiles" ON profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can update own profile" ON profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON profiles;
CREATE POLICY "Users can update own profile" ON profiles FOR UPDATE
  USING ((select auth.uid()) = id) WITH CHECK ((select auth.uid()) = id);

DROP POLICY IF EXISTS "Admins can update any profile" ON profiles;
DROP POLICY IF EXISTS "Admins can update any profile" ON profiles;
CREATE POLICY "Admins can update any profile" ON profiles FOR UPDATE
  USING (EXISTS (SELECT 1 FROM profiles p WHERE p.id = (select auth.uid()) AND p.is_admin = TRUE))
  WITH CHECK (EXISTS (SELECT 1 FROM profiles p WHERE p.id = (select auth.uid()) AND p.is_admin = TRUE));

-- 1c. teams
DROP POLICY IF EXISTS "Allow read teams" ON teams;
DROP POLICY IF EXISTS "Users can view all teams" ON teams;
DROP POLICY IF EXISTS "Users can view all teams" ON teams;
CREATE POLICY "Users can view all teams" ON teams FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can create teams" ON teams;
DROP POLICY IF EXISTS "Users can create teams" ON teams;
CREATE POLICY "Users can create teams" ON teams FOR INSERT
  WITH CHECK ((select auth.uid()) = leader_id);

DROP POLICY IF EXISTS "Team leaders can update own team" ON teams;
DROP POLICY IF EXISTS "Team leaders can update own team" ON teams;
CREATE POLICY "Team leaders can update own team" ON teams FOR UPDATE
  USING ((select auth.uid()) = leader_id) WITH CHECK ((select auth.uid()) = leader_id);

DROP POLICY IF EXISTS "Admins can update any team" ON teams;
DROP POLICY IF EXISTS "Admins can update any team" ON teams;
CREATE POLICY "Admins can update any team" ON teams FOR UPDATE
  USING (EXISTS (SELECT 1 FROM profiles p WHERE p.id = (select auth.uid()) AND p.is_admin = TRUE))
  WITH CHECK (EXISTS (SELECT 1 FROM profiles p WHERE p.id = (select auth.uid()) AND p.is_admin = TRUE));

DROP POLICY IF EXISTS "Team leaders can delete own team" ON teams;
DROP POLICY IF EXISTS "Team leaders can delete own team" ON teams;
CREATE POLICY "Team leaders can delete own team" ON teams FOR DELETE
  USING ((select auth.uid()) = leader_id);

-- 1d. team_members
DROP POLICY IF EXISTS "Allow read team_members" ON team_members;
DROP POLICY IF EXISTS "Users can view team members" ON team_members;
DROP POLICY IF EXISTS "Users can view team members" ON team_members;
CREATE POLICY "Users can view team members" ON team_members FOR SELECT USING (true);

DROP POLICY IF EXISTS "Team leaders can add members" ON team_members;
DROP POLICY IF EXISTS "Team leaders can add members" ON team_members;
CREATE POLICY "Team leaders can add members" ON team_members FOR INSERT
  WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));

DROP POLICY IF EXISTS "Team leaders can update members" ON team_members;
DROP POLICY IF EXISTS "Team leaders can update members" ON team_members;
CREATE POLICY "Team leaders can update members" ON team_members FOR UPDATE
  USING (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())))
  WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));

DROP POLICY IF EXISTS "Team leaders can remove members" ON team_members;
DROP POLICY IF EXISTS "Team leaders can remove members" ON team_members;
CREATE POLICY "Team leaders can remove members" ON team_members FOR DELETE
  USING (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())) OR (select auth.uid()) = user_id);

-- 1e. scrims
DROP POLICY IF EXISTS "Allow read scrims" ON scrims;
DROP POLICY IF EXISTS "Users can view all scrims" ON scrims;
DROP POLICY IF EXISTS "Users can view all scrims" ON scrims;
CREATE POLICY "Users can view all scrims" ON scrims FOR SELECT USING (true);

DROP POLICY IF EXISTS "Team leaders can create scrims" ON scrims;
DROP POLICY IF EXISTS "Team leaders can create scrims" ON scrims;
CREATE POLICY "Team leaders can create scrims" ON scrims FOR INSERT
  WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));

DROP POLICY IF EXISTS "Team leaders can update own scrims" ON scrims;
DROP POLICY IF EXISTS "Team leaders can update own scrims" ON scrims;
CREATE POLICY "Team leaders can update own scrims" ON scrims FOR UPDATE
  USING (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));

DROP POLICY IF EXISTS "Team leaders can delete own scrims" ON scrims;
DROP POLICY IF EXISTS "Team leaders can delete own scrims" ON scrims;
CREATE POLICY "Team leaders can delete own scrims" ON scrims FOR DELETE
  USING (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));

-- 1f. scrim_applications
DROP POLICY IF EXISTS "Allow read scrim_applications" ON scrim_applications;
DROP POLICY IF EXISTS "Users can view scrim applications" ON scrim_applications;
DROP POLICY IF EXISTS "Users can view scrim applications" ON scrim_applications;
CREATE POLICY "Users can view scrim applications" ON scrim_applications FOR SELECT USING (true);

DROP POLICY IF EXISTS "Team leaders can apply to scrims" ON scrim_applications;
DROP POLICY IF EXISTS "Team leaders can apply to scrims" ON scrim_applications;
CREATE POLICY "Team leaders can apply to scrims" ON scrim_applications FOR INSERT
  WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = applicant_team_id AND t.leader_id = (select auth.uid())));

DROP POLICY IF EXISTS "Scrim owners can accept/reject applications" ON scrim_applications;
DROP POLICY IF EXISTS "Scrim owners can accept/reject applications" ON scrim_applications;
CREATE POLICY "Scrim owners can accept/reject applications" ON scrim_applications FOR UPDATE
  USING (EXISTS (SELECT 1 FROM scrims s JOIN teams t ON t.id = s.team_id WHERE s.id = scrim_id AND t.leader_id = (select auth.uid())));

-- 1g. scrim_rosters — FIX: split FOR ALL into granular policies
DROP POLICY IF EXISTS "Users can view scrim rosters they participate in" ON scrim_rosters;
DROP POLICY IF EXISTS "Users can view scrim rosters" ON scrim_rosters;
DROP POLICY IF EXISTS "Team leaders can manage own scrim rosters" ON scrim_rosters;
DROP POLICY IF EXISTS "Team leaders can manage scrim rosters" ON scrim_rosters;

DROP POLICY IF EXISTS "Users can view scrim rosters" ON scrim_rosters;
CREATE POLICY "Users can view scrim rosters" ON scrim_rosters FOR SELECT USING (true);

DROP POLICY IF EXISTS "Team leaders can add scrim roster entries" ON scrim_rosters;
CREATE POLICY "Team leaders can add scrim roster entries" ON scrim_rosters FOR INSERT
  WITH CHECK (EXISTS (SELECT 1 FROM scrims s JOIN teams t ON t.id = s.team_id WHERE s.id = scrim_id AND t.leader_id = (select auth.uid())));

DROP POLICY IF EXISTS "Team leaders can update scrim roster entries" ON scrim_rosters;
CREATE POLICY "Team leaders can update scrim roster entries" ON scrim_rosters FOR UPDATE
  USING (EXISTS (SELECT 1 FROM scrims s JOIN teams t ON t.id = s.team_id WHERE s.id = scrim_id AND t.leader_id = (select auth.uid())));

-- NOTE: No DELETE policy for scrim_rosters — use UPDATE is_active=FALSE instead.
-- If DELETE is needed, restrict to own team only:
DROP POLICY IF EXISTS "Team leaders can delete own team roster entries" ON scrim_rosters;
CREATE POLICY "Team leaders can delete own team roster entries" ON scrim_rosters FOR DELETE
  USING (EXISTS (SELECT 1 FROM scrims s JOIN teams t ON t.id = s.team_id WHERE s.id = scrim_id AND t.leader_id = (select auth.uid())));

-- 1h. matches — FIX: restrict SELECT to participants only (was world-readable)
DROP POLICY IF EXISTS "Allow read matches" ON matches;
DROP POLICY IF EXISTS "Users can view matches they participate in" ON matches;
DROP POLICY IF EXISTS "Match participants can view matches" ON matches;
CREATE POLICY "Match participants can view matches" ON matches FOR SELECT
  USING (
    team_a_id IN (SELECT team_id FROM team_members WHERE user_id = (select auth.uid()))
    OR team_b_id IN (SELECT team_id FROM team_members WHERE user_id = (select auth.uid()))
    OR EXISTS (SELECT 1 FROM profiles WHERE id = (select auth.uid()) AND is_admin = true)
  );

DROP POLICY IF EXISTS "Team leaders can update match details" ON matches;
CREATE POLICY "Team leaders can update match details" ON matches FOR UPDATE
  USING (EXISTS (SELECT 1 FROM teams t WHERE (t.id = team_a_id OR t.id = team_b_id) AND t.leader_id = (select auth.uid())));

-- 1i. messages — FIX: restrict to match participants only (was world-readable)
DROP POLICY IF EXISTS "Allow read messages" ON messages;
DROP POLICY IF EXISTS "Match participants can view messages" ON messages;
DROP POLICY IF EXISTS "Match participants can view messages" ON messages;
CREATE POLICY "Match participants can view messages" ON messages FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM matches m
      WHERE m.id = match_id
      AND (
        m.team_a_id IN (SELECT team_id FROM team_members WHERE user_id = (select auth.uid()))
        OR m.team_b_id IN (SELECT team_id FROM team_members WHERE user_id = (select auth.uid()))
      )
    )
  );

DROP POLICY IF EXISTS "Match participants can send messages" ON messages;
DROP POLICY IF EXISTS "Match participants can send messages" ON messages;
CREATE POLICY "Match participants can send messages" ON messages FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM matches m
      WHERE m.id = match_id
      AND (
        m.team_a_id IN (SELECT team_id FROM team_members WHERE user_id = (select auth.uid()))
        OR m.team_b_id IN (SELECT team_id FROM team_members WHERE user_id = (select auth.uid()))
      )
    )
    AND sender_id = (select auth.uid())
  );

-- 1j. match_results
DROP POLICY IF EXISTS "Match participants can view results" ON match_results;
DROP POLICY IF EXISTS "Match participants can view results" ON match_results;
CREATE POLICY "Match participants can view results" ON match_results FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM matches m
      WHERE m.id = match_id
      AND (
        m.team_a_id IN (SELECT team_id FROM team_members WHERE user_id = (select auth.uid()))
        OR m.team_b_id IN (SELECT team_id FROM team_members WHERE user_id = (select auth.uid()))
      )
    )
    OR EXISTS (SELECT 1 FROM profiles WHERE id = (select auth.uid()) AND is_admin = true)
  );

DROP POLICY IF EXISTS "Match participants can upload screenshots" ON match_results;
DROP POLICY IF EXISTS "Match participants can upload screenshots" ON match_results;
CREATE POLICY "Match participants can upload screenshots" ON match_results FOR UPDATE
  USING (EXISTS (SELECT 1 FROM matches m JOIN teams t ON t.id = m.team_a_id OR t.id = m.team_b_id WHERE m.id = match_id AND t.leader_id = (select auth.uid())));

DROP POLICY IF EXISTS "Admins can verify results" ON match_results;
DROP POLICY IF EXISTS "Admins can verify results" ON match_results;
CREATE POLICY "Admins can verify results" ON match_results FOR UPDATE
  USING (EXISTS (SELECT 1 FROM profiles p WHERE p.id = (select auth.uid()) AND p.is_admin = TRUE));

-- 1k. team_invitations
DROP POLICY IF EXISTS "Allow read team_invitations" ON team_invitations;
DROP POLICY IF EXISTS "Users can view own invitations" ON team_invitations;
DROP POLICY IF EXISTS "Users can view invitations sent to them" ON team_invitations;
CREATE POLICY "Users can view own invitations" ON team_invitations FOR SELECT
  USING (
    (select auth.uid()) = invited_user_id
    OR EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid()))
  );

DROP POLICY IF EXISTS "Team leaders can invite players" ON team_invitations;
DROP POLICY IF EXISTS "Team leaders can invite players" ON team_invitations;
CREATE POLICY "Team leaders can invite players" ON team_invitations FOR INSERT
  WITH CHECK (EXISTS (SELECT 1 FROM teams t WHERE t.id = team_id AND t.leader_id = (select auth.uid())));

DROP POLICY IF EXISTS "Invited users can accept/reject invitations" ON team_invitations;
DROP POLICY IF EXISTS "Invited users can accept/reject invitations" ON team_invitations;
CREATE POLICY "Invited users can accept/reject invitations" ON team_invitations FOR UPDATE
  USING ((select auth.uid()) = invited_user_id);

-- 1l. player_stats — FIX: restrict INSERT/UPDATE to service_role only
DROP POLICY IF EXISTS "Service can manage player stats" ON player_stats;
DROP POLICY IF EXISTS "Service can update player stats" ON player_stats;
DROP POLICY IF EXISTS "Users can view player stats" ON player_stats;
DROP POLICY IF EXISTS "Users can view own stats" ON player_stats;
DROP POLICY IF EXISTS "Users can view teammate stats" ON player_stats;
DROP POLICY IF EXISTS "System can insert/update player stats" ON player_stats;
DROP POLICY IF EXISTS "Users can view player stats" ON player_stats;
DROP POLICY IF EXISTS "Service role can insert player stats" ON player_stats;
DROP POLICY IF EXISTS "Service role can update player stats" ON player_stats;

CREATE POLICY "Users can view player stats" ON player_stats FOR SELECT USING (true);

-- Only service_role (backend) can insert/update stats — enforced by checking auth.jwt() role
DROP POLICY IF EXISTS "Service role can insert player stats" ON player_stats;
CREATE POLICY "Service role can insert player stats" ON player_stats FOR INSERT
  WITH CHECK (auth.jwt()->>'role' = 'service_role');

DROP POLICY IF EXISTS "Service role can update player stats" ON player_stats;
CREATE POLICY "Service role can update player stats" ON player_stats FOR UPDATE
  USING (auth.jwt()->>'role' = 'service_role')
  WITH CHECK (auth.jwt()->>'role' = 'service_role');

-- 1m. app_notifications
DROP POLICY IF EXISTS "Users can read own notifications" ON app_notifications;
DROP POLICY IF EXISTS "Users can read own notifications" ON app_notifications;
CREATE POLICY "Users can read own notifications" ON app_notifications FOR SELECT
  USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Authenticated users can insert notifications" ON app_notifications;
DROP POLICY IF EXISTS "Service role can insert notifications" ON app_notifications;
CREATE POLICY "Service role can insert notifications" ON app_notifications FOR INSERT
  WITH CHECK (auth.jwt()->>'role' = 'service_role' OR (select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can update own notifications" ON app_notifications;
DROP POLICY IF EXISTS "Users can update own notifications" ON app_notifications;
CREATE POLICY "Users can update own notifications" ON app_notifications FOR UPDATE
  USING ((select auth.uid()) = user_id) WITH CHECK ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can delete own notifications" ON app_notifications;
DROP POLICY IF EXISTS "Users can delete own notifications" ON app_notifications;
CREATE POLICY "Users can delete own notifications" ON app_notifications FOR DELETE
  USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Admins can read all notifications" ON app_notifications;
DROP POLICY IF EXISTS "Admins can read all notifications" ON app_notifications;
CREATE POLICY "Admins can read all notifications" ON app_notifications FOR SELECT
  USING (EXISTS (SELECT 1 FROM profiles WHERE id = (select auth.uid()) AND is_admin = TRUE));


-- ┌─────────────────────────────────────────────────────────────────────────────┐
-- │ SECTION 2: Functions — Security fixes                                       │
-- └─────────────────────────────────────────────────────────────────────────────┘

-- 2a. handle_new_user — keep SECURITY DEFINER (trigger needs it) but revoke RPC
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id, username, email)
  VALUES (NEW.id, SPLIT_PART(NEW.email, '@', 1), NEW.email);
  RETURN NEW;
END;
$$;

DO $$ BEGIN
  REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM anon, authenticated;
  GRANT EXECUTE ON FUNCTION public.handle_new_user() TO service_role;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 2b. award_scrim_points — SECURITY INVOKER + SET search_path
CREATE OR REPLACE FUNCTION public.award_scrim_points(
    p_scrim_id UUID,
    p_winner_team_id UUID,
    p_pts_per_win INTEGER DEFAULT 25,
    p_pts_per_loss INTEGER DEFAULT 15
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
    roster_entry RECORD;
    is_winner BOOLEAN;
BEGIN
    FOR roster_entry IN
        SELECT sr.user_id, sr.team_id, sr.is_active
        FROM scrim_rosters sr
        WHERE sr.scrim_id = p_scrim_id AND sr.is_active = TRUE
    LOOP
        is_winner := (roster_entry.team_id = p_winner_team_id);
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

    UPDATE match_results
    SET pts_awarded = TRUE
    WHERE match_id IN (SELECT m.id FROM matches m WHERE m.scrim_id = p_scrim_id);
END;
$$;

DO $$ BEGIN
  REVOKE EXECUTE ON FUNCTION public.award_scrim_points(UUID, UUID, INTEGER, INTEGER) FROM anon, authenticated;
  GRANT EXECUTE ON FUNCTION public.award_scrim_points(UUID, UUID, INTEGER, INTEGER) TO service_role;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 2c. award_xp — SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.award_xp(team_id UUID, xp_amount INTEGER)
RETURNS VOID
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  UPDATE teams SET total_xp = total_xp + xp_amount WHERE id = team_id;
END;
$$;

-- 2d. get_team_stats — SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.get_team_stats(team_id UUID)
RETURNS TABLE(total_matches INTEGER, wins INTEGER, losses INTEGER, win_rate NUMERIC)
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  RETURN QUERY
  SELECT
    COUNT(*)::INTEGER as total_matches,
    COUNT(CASE WHEN winner_team_id = get_team_stats.team_id THEN 1 END)::INTEGER as wins,
    COUNT(CASE WHEN winner_team_id != get_team_stats.team_id THEN 1 END)::INTEGER as losses,
    ROUND(
      COUNT(CASE WHEN winner_team_id = get_team_stats.team_id THEN 1 END)::NUMERIC /
      NULLIF(COUNT(*), 0) * 100, 2
    ) as win_rate
  FROM match_results mr
  JOIN matches m ON mr.match_id = m.id
  WHERE (m.team_a_id = get_team_stats.team_id OR m.team_b_id = get_team_stats.team_id)
  AND mr.admin_verified = true;
END;
$$;

-- 2e. get_available_scrims — SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.get_available_scrims(team_id UUID)
RETURNS TABLE(scrim_id UUID, team_name TEXT, scheduled_date DATE, scheduled_time TIME, tier TEXT, division INTEGER)
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  RETURN QUERY
  SELECT s.id, t.name, s.scheduled_date, s.scheduled_time, t.current_tier, t.current_division
  FROM scrims s
  JOIN teams t ON s.team_id = t.id
  WHERE s.status = 'Open'
  AND s.team_id != get_available_scrims.team_id
  AND s.id NOT IN (SELECT sa.scrim_id FROM scrim_applications sa WHERE sa.applicant_team_id = get_available_scrims.team_id)
  ORDER BY s.scheduled_date, s.scheduled_time;
END;
$$;

-- 2f. is_team_leader — SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.is_team_leader(user_id UUID, team_id UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql
IMMUTABLE
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  RETURN EXISTS (SELECT 1 FROM teams WHERE id = is_team_leader.team_id AND leader_id = is_team_leader.user_id);
END;
$$;

-- 2g. get_user_team — SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.get_user_team(user_id UUID)
RETURNS TABLE(team_id UUID, team_name TEXT, role TEXT)
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  RETURN QUERY
  SELECT t.id, t.name, tm.role
  FROM team_members tm
  JOIN teams t ON tm.team_id = t.id
  WHERE tm.user_id = get_user_team.user_id;
END;
$$;

-- 2h. calculate_tier — IMMUTABLE + SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.calculate_tier(xp INTEGER)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  IF xp < 100 THEN RETURN 'Bronze';
  ELSIF xp < 300 THEN RETURN 'Silver';
  ELSIF xp < 600 THEN RETURN 'Gold';
  ELSIF xp < 1000 THEN RETURN 'Platinum';
  ELSIF xp < 1500 THEN RETURN 'Diamond';
  ELSIF xp < 2000 THEN RETURN 'Master';
  ELSE RETURN 'Grandmaster';
  END IF;
END;
$$;

-- 2i. calculate_division — FIX: clamp to 1-4 range
CREATE OR REPLACE FUNCTION public.calculate_division(xp INTEGER, tier TEXT)
RETURNS INTEGER
LANGUAGE plpgsql
IMMUTABLE
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
  tier_min INTEGER;
  raw_div INTEGER;
BEGIN
  CASE tier
    WHEN 'Bronze' THEN tier_min := 0;
    WHEN 'Silver' THEN tier_min := 100;
    WHEN 'Gold' THEN tier_min := 300;
    WHEN 'Platinum' THEN tier_min := 600;
    WHEN 'Diamond' THEN tier_min := 1000;
    WHEN 'Master' THEN tier_min := 1500;
    ELSE tier_min := 2000;
  END CASE;

  raw_div := FLOOR((xp - tier_min) / 50) + 1;
  -- Clamp division to valid range [1, 4]
  RETURN GREATEST(1, LEAST(4, raw_div));
END;
$$;

-- 2j. update_team_tier trigger — SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.update_team_tier()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  NEW.current_tier := public.calculate_tier(NEW.total_xp);
  NEW.current_division := public.calculate_division(NEW.total_xp, NEW.current_tier);
  RETURN NEW;
END;
$$;

-- 2k. delete_user — FIX: correct deletion order (scrim_applications before team_members)
CREATE OR REPLACE FUNCTION public.delete_user(p_user_id UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  -- Delete scrim_applications FIRST (depends on team_members for lookup)
  DELETE FROM scrim_applications WHERE applicant_team_id IN (
    SELECT team_id FROM team_members WHERE user_id = p_user_id
  );
  -- Now safe to delete team_members
  DELETE FROM team_members WHERE user_id = p_user_id;
  -- team_invitations
  DELETE FROM team_invitations WHERE invited_user_id = p_user_id OR invited_by = p_user_id;
  -- scrim_rosters
  DELETE FROM scrim_rosters WHERE user_id = p_user_id;
  -- player_stats
  DELETE FROM player_stats WHERE user_id = p_user_id;
  -- notifications
  DELETE FROM app_notifications WHERE user_id = p_user_id;
  -- profiles (cascades to auth.users via FK)
  DELETE FROM profiles WHERE id = p_user_id;
END;
$$;


-- ┌─────────────────────────────────────────────────────────────────────────────┐
-- │ SECTION 3: CHECK Constraints                                               │
-- └─────────────────────────────────────────────────────────────────────────────┘

-- 3a. player_stats.pts >= 0
DO $$ BEGIN
  ALTER TABLE player_stats ADD CONSTRAINT pts_non_negative CHECK (pts >= 0);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 3b. scrims.best_of IN (1, 3, 5)
DO $$ BEGIN
  ALTER TABLE scrims ADD CONSTRAINT valid_best_of CHECK (best_of IN (1, 3, 5));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 3c. scrims.status enum
DO $$ BEGIN
  ALTER TABLE scrims ADD CONSTRAINT valid_scrim_status CHECK (status IN ('Open', 'Pending', 'Accepted', 'Ready', 'In Progress', 'Completed', 'Cancelled'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 3d. matches.status enum
DO $$ BEGIN
  ALTER TABLE matches ADD CONSTRAINT valid_match_status CHECK (status IN ('Scheduled', 'In Progress', 'Completed', 'Cancelled'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 3e. scrim_applications.status enum
DO $$ BEGIN
  ALTER TABLE scrim_applications ADD CONSTRAINT valid_application_status CHECK (status IN ('Pending', 'Accepted', 'Rejected'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 3f. team_invitations.status enum
DO $$ BEGIN
  ALTER TABLE team_invitations ADD CONSTRAINT valid_invitation_status CHECK (status IN ('Pending', 'Accepted', 'Rejected'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 3g. teams.reputation between 0 and 10
DO $$ BEGIN
  ALTER TABLE teams ADD CONSTRAINT valid_reputation CHECK (reputation >= 0 AND reputation <= 10);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;


-- ┌─────────────────────────────────────────────────────────────────────────────┐
-- │ SECTION 4: Schema improvements                                             │
-- └─────────────────────────────────────────────────────────────────────────────┘

-- 4a. Add updated_at column to profiles
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW());

-- 4b. Change reputation from REAL to NUMERIC(3,1)
-- NOTE: This is a type change — back up data first if table is populated.
ALTER TABLE public.teams ALTER COLUMN reputation TYPE NUMERIC(3,1);

-- 4c. Add missing indexes
CREATE INDEX IF NOT EXISTS idx_scrims_scheduled_date ON scrims(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_scrims_region ON scrims(region);
CREATE INDEX IF NOT EXISTS idx_scrims_game_mode ON scrims(game_mode);
CREATE INDEX IF NOT EXISTS idx_scrims_skill_level ON scrims(skill_level);
CREATE INDEX IF NOT EXISTS idx_profiles_username ON profiles(username);
CREATE INDEX IF NOT EXISTS idx_profiles_mlbb_id ON profiles(mlbb_id);
CREATE INDEX IF NOT EXISTS idx_profiles_is_banned ON profiles(is_banned);
CREATE INDEX IF NOT EXISTS idx_app_notifications_type ON app_notifications(type);

-- 4d. Add updated_at trigger for profiles
CREATE OR REPLACE FUNCTION public.update_profiles_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = TIMEZONE('utc', NOW());
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_profiles_update ON profiles;
CREATE TRIGGER on_profiles_update
  BEFORE UPDATE ON profiles
  FOR EACH ROW EXECUTE FUNCTION public.update_profiles_updated_at();


-- ┌─────────────────────────────────────────────────────────────────────────────┐
-- │ SECTION 5: Storage bucket policies                                          │
-- └─────────────────────────────────────────────────────────────────────────────┘

-- Drop overly broad policies
DROP POLICY IF EXISTS "Public read for team logos" ON storage.objects;
DROP POLICY IF EXISTS "Public read for avatars" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can read their own uploads" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can read public buckets" ON storage.objects;
DROP POLICY IF EXISTS "Users can upload to match-screenshots" ON storage.objects;
DROP POLICY IF EXISTS "Users can upload to user-avatars" ON storage.objects;
DROP POLICY IF EXISTS "Users can upload to team-logos" ON storage.objects;

-- Users can read objects in public-facing buckets (for display)
DROP POLICY IF EXISTS "Authenticated users can read public buckets" ON storage.objects;
CREATE POLICY "Authenticated users can read public buckets" ON storage.objects FOR SELECT
  USING (
    bucket_id IN ('match-screenshots', 'user-avatars', 'team-logos')
    AND auth.role() = 'authenticated'
  );

DROP POLICY IF EXISTS "Users can upload to match-screenshots" ON storage.objects;
CREATE POLICY "Users can upload to match-screenshots" ON storage.objects FOR INSERT
  WITH CHECK (bucket_id = 'match-screenshots' AND auth.role() = 'authenticated');

DROP POLICY IF EXISTS "Users can upload to user-avatars" ON storage.objects;
CREATE POLICY "Users can upload to user-avatars" ON storage.objects FOR INSERT
  WITH CHECK (bucket_id = 'user-avatars' AND auth.role() = 'authenticated' AND owner = auth.uid());

DROP POLICY IF EXISTS "Users can upload to team-logos" ON storage.objects;
CREATE POLICY "Users can upload to team-logos" ON storage.objects FOR INSERT
  WITH CHECK (bucket_id = 'team-logos' AND auth.role() = 'authenticated' AND owner = auth.uid());


-- ═══════════════════════════════════════════════════════════════════════════════
-- END OF CONSOLIDATED MIGRATION v2
-- ═══════════════════════════════════════════════════════════════════════════════
