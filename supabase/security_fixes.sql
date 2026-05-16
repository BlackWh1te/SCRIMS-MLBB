-- ═══════════════════════════════════════════════════════════════
-- MLBB Scrim Host - SECURITY FIXES MIGRATION
-- Run this in your Supabase SQL Editor (or psql) to resolve
-- all database linter findings.
-- ═══════════════════════════════════════════════════════════════

-- ┌─────────────────────────────────────────────────────────────┐
-- │ 1. ENABLE RLS ON TABLES MISSING IT (CRITICAL)              │
-- └─────────────────────────────────────────────────────────────┘

ALTER TABLE IF EXISTS public.player_stats ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.scrim_rosters ENABLE ROW LEVEL SECURITY;

-- player_stats policies
CREATE POLICY "Users can view own stats"
  ON public.player_stats FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can view teammate stats"
  ON public.player_stats FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM public.team_members tm
      WHERE tm.user_id = public.player_stats.user_id
      AND tm.team_id IN (
        SELECT team_id FROM public.team_members WHERE user_id = auth.uid()
      )
    )
  );

CREATE POLICY "System can insert/update player stats"
  ON public.player_stats FOR ALL
  USING (true)
  WITH CHECK (true);

-- scrim_rosters policies
CREATE POLICY "Users can view scrim rosters they participate in"
  ON public.scrim_rosters FOR SELECT
  USING (
    scrim_id IN (
      SELECT s.id FROM public.scrims s
      JOIN public.teams t ON s.team_id = t.id
      WHERE t.leader_id = auth.uid()
    )
    OR EXISTS (
      SELECT 1 FROM public.scrim_applications sa
      WHERE sa.scrim_id = scrim_id
      AND sa.applicant_team_id IN (
        SELECT team_id FROM public.team_members WHERE user_id = auth.uid()
      )
    )
    OR user_id = auth.uid()
  );

CREATE POLICY "Team leaders can manage own scrim rosters"
  ON public.scrim_rosters FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM public.teams
      WHERE id = team_id AND leader_id = auth.uid()
    )
  );

-- ┌─────────────────────────────────────────────────────────────┐
-- │ 2. FIX SEARCH PATH ON ALL FUNCTIONS (WARN)                  │
-- └─────────────────────────────────────────────────────────────┘

-- Schema-level helper: re-create functions with SET search_path
-- and SECURITY INVOKER where appropriate

-- award_xp - simple UPDATE, switch to SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.award_xp(
    team_id UUID,
    xp_amount INTEGER
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  UPDATE teams
  SET total_xp = total_xp + xp_amount
  WHERE id = team_id;
END;
$$;

-- get_team_stats - read-only, switch to SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.get_team_stats(team_id UUID)
RETURNS TABLE(
  total_matches INTEGER,
  wins INTEGER,
  losses INTEGER,
  win_rate NUMERIC
)
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  RETURN QUERY
  SELECT
    COUNT(*)::INTEGER as total_matches,
    COUNT(CASE WHEN winner_team_id = team_id THEN 1 END)::INTEGER as wins,
    COUNT(CASE WHEN winner_team_id != team_id THEN 1 END)::INTEGER as losses,
    ROUND(
      COUNT(CASE WHEN winner_team_id = team_id THEN 1 END)::NUMERIC /
      NULLIF(COUNT(*), 0) * 100,
      2
    ) as win_rate
  FROM match_results mr
  JOIN matches m ON mr.match_id = m.id
  WHERE (m.team_a_id = team_id OR m.team_b_id = team_id)
  AND mr.admin_verified = true;
END;
$$;

-- get_available_scrims - read-only, switch to SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.get_available_scrims(team_id UUID)
RETURNS TABLE(
  scrim_id UUID,
  team_name TEXT,
  scheduled_date DATE,
  scheduled_time TIME,
  tier TEXT,
  division INTEGER
)
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  RETURN QUERY
  SELECT
    s.id as scrim_id,
    t.name as team_name,
    s.scheduled_date,
    s.scheduled_time,
    t.current_tier as tier,
    t.current_division as division
  FROM scrims s
  JOIN teams t ON s.team_id = t.id
  WHERE s.status = 'Open'
  AND s.team_id != get_available_scrims.team_id
  AND s.id NOT IN (
    SELECT scrim_id FROM scrim_applications WHERE applicant_team_id = get_available_scrims.team_id
  )
  ORDER BY s.scheduled_date, s.scheduled_time;
END;
$$;

-- is_team_leader - read-only, switch to SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.is_team_leader(
    user_id UUID,
    team_id UUID
)
RETURNS BOOLEAN
LANGUAGE plpgsql
IMMUTABLE
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM teams
    WHERE id = team_id AND leader_id = user_id
  );
END;
$$;

-- get_user_team - read-only, switch to SECURITY INVOKER
CREATE OR REPLACE FUNCTION public.get_user_team(user_id UUID)
RETURNS TABLE(
  team_id UUID,
  team_name TEXT,
  role TEXT
)
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  RETURN QUERY
  SELECT
    t.id as team_id,
    t.name as team_name,
    tm.role as role
  FROM team_members tm
  JOIN teams t ON tm.team_id = t.id
  WHERE tm.user_id = get_user_team.user_id;
END;
$$;

-- handle_new_user - TRIGGER FUNCTION: keep SECURITY DEFINER,
-- but revoke RPC access so it can't be called via REST API.
-- Only auth triggers should invoke it.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.profiles (id, username, email)
  VALUES (
    NEW.id,
    SPLIT_PART(NEW.email, '@', 1),
    NEW.email
  );
  RETURN NEW;
END;
$$;

-- Also fix calculate_tier and calculate_division (not flagged but good practice)
CREATE OR REPLACE FUNCTION public.calculate_tier(xp INTEGER)
RETURNS TEXT
LANGUAGE plpgsql
IMMUTABLE
SECURITY INVOKER
SET search_path = public
AS $$
BEGIN
  IF xp < 100 THEN
    RETURN 'Bronze';
  ELSIF xp < 300 THEN
    RETURN 'Silver';
  ELSIF xp < 600 THEN
    RETURN 'Gold';
  ELSIF xp < 1000 THEN
    RETURN 'Platinum';
  ELSIF xp < 1500 THEN
    RETURN 'Diamond';
  ELSIF xp < 2000 THEN
    RETURN 'Master';
  ELSE
    RETURN 'Grandmaster';
  END IF;
END;
$$;

CREATE OR REPLACE FUNCTION public.calculate_division(xp INTEGER, tier TEXT)
RETURNS INTEGER
LANGUAGE plpgsql
IMMUTABLE
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
  tier_min INTEGER;
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
  RETURN FLOOR((xp - tier_min) / 50) + 1;
END;
$$;

-- update_team_tier trigger function - not exposed via RPC but fix anyway
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

-- award_scrim_points - internal utility, keep DEFINER but protect RPC access
CREATE OR REPLACE FUNCTION public.award_scrim_points(
    p_scrim_id UUID,
    p_winner_team_id UUID,
    p_pts_per_win INTEGER DEFAULT 25,
    p_pts_per_loss INTEGER DEFAULT 15
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
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
            pts = player_stats.pts + CASE WHEN is_winner THEN p_pts_per_win ELSE -p_pts_per_loss END,
            wins = player_stats.wins + CASE WHEN is_winner THEN 1 ELSE 0 END,
            losses = player_stats.losses + CASE WHEN is_winner THEN 0 ELSE 1 END,
            matches_play = player_stats.matches_play + 1,
            updated_at = TIMEZONE('utc', NOW());
    END LOOP;

    UPDATE match_results
    SET pts_awarded = TRUE
    WHERE match_id IN (
        SELECT m.id FROM matches m WHERE m.scrim_id = p_scrim_id
    );
END;
$$;

-- ┌─────────────────────────────────────────────────────────────┐
-- │ 3. REVOKE RPC EXECUTE FROM anon ON TRIGGER/INTERNAL FUNCS   │
-- └─────────────────────────────────────────────────────────────┘

-- Prevent anon from calling sensitive functions via /rest/v1/rpc/
REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM anon;
REVOKE EXECUTE ON FUNCTION public.award_scrim_points(UUID, UUID, INTEGER, INTEGER) FROM anon;

-- Prevent authenticated users from calling internal admin functions via RPC
-- (These should only be called by triggers or admin backend)
REVOKE EXECUTE ON FUNCTION public.award_scrim_points(UUID, UUID, INTEGER, INTEGER) FROM authenticated;

-- ┌─────────────────────────────────────────────────────────────┐
-- │ 4. FIX admin_activity RLS POLICY (if table exists)          │
-- └─────────────────────────────────────────────────────────────┘

-- Only run if the table was created (may have been added after schema.sql)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'public' AND table_name = 'admin_activity'
  ) THEN
    ALTER TABLE public.admin_activity ENABLE ROW LEVEL SECURITY;

    -- Drop the overly permissive policy
    DROP POLICY IF EXISTS "Allow insert admin_activity" ON public.admin_activity;

    -- Recreate with proper admin check
    CREATE POLICY "Admins can insert admin_activity"
      ON public.admin_activity FOR INSERT
      WITH CHECK (
        EXISTS (
          SELECT 1 FROM public.profiles
          WHERE id = auth.uid() AND is_admin = true
        )
      );

    CREATE POLICY "Admins can view admin_activity"
      ON public.admin_activity FOR SELECT
      USING (
        EXISTS (
          SELECT 1 FROM public.profiles
          WHERE id = auth.uid() AND is_admin = true
        )
      );
  END IF;
END $$;

-- ┌─────────────────────────────────────────────────────────────┐
-- │ 5. FIX STORAGE BUCKET POLICIES (list prevention)           │
-- └─────────────────────────────────────────────────────────────┘

-- Remove broad listing policies that allow anyone to list all objects.
-- Keep object-level access via signed URLs or authenticated ownership checks.

-- Drop existing broad SELECT policies on storage.objects if they exist
DROP POLICY IF EXISTS "Public read for team logos" ON storage.objects;
DROP POLICY IF EXISTS "Public read for avatars" ON storage.objects;

-- Recreate with object-level access only (no listing).
-- Users can read objects they own or that are explicitly shared.
-- For public display of team logos / avatars, use signed URLs instead.

CREATE POLICY "Authenticated users can read their own uploads"
  ON storage.objects FOR SELECT
  USING (
    auth.role() = 'authenticated'
    AND owner = auth.uid()
  );

CREATE POLICY "Users can upload to match-screenshots"
  ON storage.objects FOR INSERT
  WITH CHECK (
    bucket_id = 'match-screenshots'
    AND auth.role() = 'authenticated'
  );

CREATE POLICY "Users can upload to user-avatars"
  ON storage.objects FOR INSERT
  WITH CHECK (
    bucket_id = 'user-avatars'
    AND auth.role() = 'authenticated'
    AND owner = auth.uid()
  );

CREATE POLICY "Users can upload to team-logos"
  ON storage.objects FOR INSERT
  WITH CHECK (
    bucket_id = 'team-logos'
    AND auth.role() = 'authenticated'
    AND owner = auth.uid()
  );

-- ┌─────────────────────────────────────────────────────────────┐
-- │ DONE                                                        │
-- └─────────────────────────────────────────────────────────────┘
