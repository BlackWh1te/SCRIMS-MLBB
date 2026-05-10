-- MLBB Scrim Host - Database Triggers
-- PostgreSQL with Supabase

-- Trigger to create profile when user is created
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (id, username, email)
  VALUES (
    NEW.id,
    SPLIT_PART(NEW.email, '@', 1),
    NEW.email
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to call handle_new_user on user creation
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- Function to calculate tier based on XP
CREATE OR REPLACE FUNCTION public.calculate_tier(xp INTEGER)
RETURNS TEXT AS $$
BEGIN
  IF xp < 100 THEN
    RETURN 'Bronze';
  ELSEIF xp < 300 THEN
    RETURN 'Silver';
  ELSEIF xp < 600 THEN
    RETURN 'Gold';
  ELSEIF xp < 1000 THEN
    RETURN 'Platinum';
  ELSEIF xp < 1500 THEN
    RETURN 'Diamond';
  ELSEIF xp < 2000 THEN
    RETURN 'Master';
  ELSE
    RETURN 'Grandmaster';
  END IF;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Function to calculate division within tier
CREATE OR REPLACE FUNCTION public.calculate_division(xp INTEGER, tier TEXT)
RETURNS INTEGER AS $$
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
$$ LANGUAGE plpgsql IMMUTABLE;

-- Trigger to update team tier and division when XP changes
CREATE OR REPLACE FUNCTION public.update_team_tier()
RETURNS TRIGGER AS $$
BEGIN
  NEW.current_tier := public.calculate_tier(NEW.total_xp);
  NEW.current_division := public.calculate_division(NEW.total_xp, NEW.current_tier);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER on_team_xp_change
  BEFORE UPDATE OF total_xp ON teams
  FOR EACH ROW EXECUTE PROCEDURE public.update_team_tier();