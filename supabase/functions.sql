-- MLBB Scrim Host - Custom SQL Functions
-- PostgreSQL with Supabase

-- Function to award XP to a team
CREATE OR REPLACE FUNCTION public.award_xp(team_id UUID, xp_amount INTEGER)
RETURNS VOID AS $$
BEGIN
  UPDATE teams
  SET total_xp = total_xp + xp_amount
  WHERE id = team_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get team stats
CREATE OR REPLACE FUNCTION public.get_team_stats(team_id UUID)
RETURNS TABLE(
  total_matches INTEGER,
  wins INTEGER,
  losses INTEGER,
  win_rate NUMERIC
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    COUNT(*) as total_matches,
    COUNT(CASE WHEN winner_team_id = team_id THEN 1 END) as wins,
    COUNT(CASE WHEN winner_team_id != team_id THEN 1 END) as losses,
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
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get available scrims for a team
CREATE OR REPLACE FUNCTION public.get_available_scrims(team_id UUID)
RETURNS TABLE(
  scrim_id UUID,
  team_name TEXT,
  scheduled_date DATE,
  scheduled_time TIME,
  tier TEXT,
  division INTEGER
) AS $$
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
  AND s.team_id != team_id
  AND s.id NOT IN (
    SELECT scrim_id FROM scrim_applications WHERE applicant_team_id = team_id
  )
  ORDER BY s.scheduled_date, s.scheduled_time;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to check if user is team leader
CREATE OR REPLACE FUNCTION public.is_team_leader(user_id UUID, team_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
  RETURN EXISTS (
    SELECT 1 FROM teams
    WHERE id = team_id AND leader_id = user_id
  );
END;
$$ LANGUAGE plpgsql IMMUTABLE SECURITY DEFINER;

-- Function to get user's team
CREATE OR REPLACE FUNCTION public.get_user_team(user_id UUID)
RETURNS TABLE(
  team_id UUID,
  team_name TEXT,
  role TEXT
) AS $$
BEGIN
  RETURN QUERY
  SELECT
    t.id as team_id,
    t.name as team_name,
    tm.role as role
  FROM team_members tm
  JOIN teams t ON tm.team_id = t.id
  WHERE tm.user_id = user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;