-- Migration: Fix tournament creation RLS + add LFG posts RLS policies
-- Date: 2026-05-26
-- Context:
--   1. Tournament creation RLS required is_tournament_host=TRUE which blocked
--      regular users from creating tournaments. Relaxed to allow any authenticated user.
--   2. lfg_posts table had no RLS policies defined, causing INSERT failures when
--      RLS was enabled on the table. Adding proper SELECT/INSERT/DELETE policies.

-- ═══════════════════════════════════════════════════════════════
-- 1. FIX TOURNAMENT CREATION RLS
-- ═══════════════════════════════════════════════════════════════

-- Drop the restrictive policy that required is_tournament_host = TRUE
DROP POLICY IF EXISTS "Hosts can create tournaments" ON tournaments;

-- Replace with a policy that allows any authenticated user to create tournaments
-- (host_user_id must match the authenticated user)
CREATE POLICY "Authenticated users can create tournaments" ON tournaments
    FOR INSERT WITH CHECK (
        host_user_id = auth.uid()
    );

-- ═══════════════════════════════════════════════════════════════
-- 2. LFG POSTS RLS POLICIES
-- ═══════════════════════════════════════════════════════════════

-- Ensure RLS is enabled on lfg_posts
ALTER TABLE lfg_posts ENABLE ROW LEVEL SECURITY;

-- Anyone can view LFG posts (public board for finding players)
CREATE POLICY "Anyone can view LFG posts" ON lfg_posts
    FOR SELECT USING (true);

-- Authenticated users can create their own LFG post
CREATE POLICY "Users can create own LFG post" ON lfg_posts
    FOR INSERT WITH CHECK (player_id = auth.uid());

-- Users can update their own LFG post
CREATE POLICY "Users can update own LFG post" ON lfg_posts
    FOR UPDATE USING (player_id = auth.uid());

-- Users can delete their own LFG post
CREATE POLICY "Users can delete own LFG post" ON lfg_posts
    FOR DELETE USING (player_id = auth.uid());

-- ═══════════════════════════════════════════════════════════════
-- 3. ADD view_count COLUMN TO lfg_posts
-- ═══════════════════════════════════════════════════════════════

-- Track how many times a player card was expanded (viewed)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'lfg_posts' AND column_name = 'view_count'
    ) THEN
        ALTER TABLE lfg_posts ADD COLUMN view_count INTEGER NOT NULL DEFAULT 0;
    END IF;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 4. RPC for atomic view count increment
-- ═══════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION increment_lfg_view_count(p_post_id UUID)
RETURNS VOID
LANGUAGE sql
SECURITY DEFINER
AS $$
    UPDATE lfg_posts SET view_count = view_count + 1 WHERE id = p_post_id;
$$;
