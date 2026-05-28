-- Add missing extended profile/stats columns to lfg_posts table
-- This fixes the 400 Bad Request error when creating an LFG post

ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS ranked_win_rate TEXT;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS wins INT DEFAULT 0;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS losses INT DEFAULT 0;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS pts INT DEFAULT 0;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS in_game_id TEXT;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS city TEXT;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS screenshot_url TEXT;
