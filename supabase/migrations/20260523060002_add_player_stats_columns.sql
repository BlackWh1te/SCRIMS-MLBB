-- Migration: Add achievement tracking columns to player_stats table
-- Fixes: Enables full achievement tracking that the app expects
-- Date: 2026-05-23

-- Add missing achievement tracking columns to player_stats
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'player_stats' AND column_name = 'best_win_streak') THEN
        ALTER TABLE player_stats ADD COLUMN best_win_streak INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'player_stats' AND column_name = 'ratings_given') THEN
        ALTER TABLE player_stats ADD COLUMN ratings_given INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'player_stats' AND column_name = 'has_regional_top') THEN
        ALTER TABLE player_stats ADD COLUMN has_regional_top BOOLEAN DEFAULT FALSE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'player_stats' AND column_name = 'jungler_wins') THEN
        ALTER TABLE player_stats ADD COLUMN jungler_wins INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'player_stats' AND column_name = 'roamer_wins') THEN
        ALTER TABLE player_stats ADD COLUMN roamer_wins INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'player_stats' AND column_name = 'night_wins') THEN
        ALTER TABLE player_stats ADD COLUMN night_wins INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'player_stats' AND column_name = 'five_star_matches') THEN
        ALTER TABLE player_stats ADD COLUMN five_star_matches INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'player_stats' AND column_name = 'has_flawless_victory') THEN
        ALTER TABLE player_stats ADD COLUMN has_flawless_victory BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- Update the award_scrim_points function to also track role-specific and time-based stats
-- This is a simplified version; full implementation would require roster role data
CREATE OR REPLACE FUNCTION update_player_stats_after_scrim(
    p_user_id UUID,
    p_is_winner BOOLEAN,
    p_role TEXT DEFAULT NULL,
    p_is_night_match BOOLEAN DEFAULT FALSE,
    p_is_flawless BOOLEAN DEFAULT FALSE
)
RETURNS VOID AS $$
BEGIN
    UPDATE player_stats SET
        pts = GREATEST(pts + CASE WHEN p_is_winner THEN 25 ELSE -15 END, 0),
        wins = wins + CASE WHEN p_is_winner THEN 1 ELSE 0 END,
        losses = losses + CASE WHEN p_is_winner THEN 0 ELSE 1 END,
        matches_play = matches_play + 1,
        jungler_wins = jungler_wins + CASE WHEN p_is_winner AND p_role = 'Jungler' THEN 1 ELSE 0 END,
        roamer_wins = roamer_wins + CASE WHEN p_is_winner AND p_role = 'Roamer' THEN 1 ELSE 0 END,
        night_wins = night_wins + CASE WHEN p_is_winner AND p_is_night_match THEN 1 ELSE 0 END,
        five_star_matches = five_star_matches + CASE WHEN p_is_flawless THEN 1 ELSE 0 END,
        has_flawless_victory = has_flawless_victory OR p_is_flawless,
        updated_at = TIMEZONE('utc', NOW())
    WHERE user_id = p_user_id;
END;
$$ LANGUAGE plpgsql;

-- Notify PostgREST to reload schema cache
NOTIFY pgrst, 'reload schema';
