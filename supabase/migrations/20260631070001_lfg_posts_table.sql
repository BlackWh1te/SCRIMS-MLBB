-- Create lfg_posts table if it doesn't exist
CREATE TABLE IF NOT EXISTS lfg_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    player_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'FLEX',
    region TEXT NOT NULL DEFAULT 'UTC',
    skill_level TEXT NOT NULL DEFAULT 'ALL',
    message TEXT,
    main_heroes TEXT[] DEFAULT '{}',
    bio TEXT,
    rank TEXT,
    total_matches INT DEFAULT 0,
    win_rate TEXT,
    is_available BOOLEAN DEFAULT TRUE,
    use_mic BOOLEAN DEFAULT FALSE,
    playstyle_tags TEXT[] DEFAULT '{}',
    discord TEXT,
    telegram TEXT,
    vk TEXT,
    facebook TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Enable RLS
ALTER TABLE lfg_posts ENABLE ROW LEVEL SECURITY;

-- Policies
DROP POLICY IF EXISTS "Public can view LFG posts" ON lfg_posts;
CREATE POLICY "Public can view LFG posts" ON lfg_posts
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can manage their own LFG posts" ON lfg_posts;
CREATE POLICY "Users can manage their own LFG posts" ON lfg_posts
    FOR ALL USING (auth.uid() = player_id);

-- Add missing columns if table already existed without them
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS main_heroes TEXT[] DEFAULT '{}';
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS rank TEXT;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS total_matches INT DEFAULT 0;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS win_rate TEXT;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS is_available BOOLEAN DEFAULT TRUE;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS use_mic BOOLEAN DEFAULT FALSE;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS playstyle_tags TEXT[] DEFAULT '{}';
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS discord TEXT;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS telegram TEXT;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS vk TEXT;
ALTER TABLE lfg_posts ADD COLUMN IF NOT EXISTS facebook TEXT;
