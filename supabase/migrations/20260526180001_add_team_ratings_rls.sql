-- Migration: Add RLS to team_ratings table
-- Date: 2026-05-26
-- Context: Supabase linter flagged team_ratings as missing RLS.
--          Ratings are public peer feedback, but we still need
--          RLS policies to prevent unauthorized writes.

-- ═══════════════════════════════════════════════════════════════
-- 1. ENABLE RLS
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE team_ratings ENABLE ROW LEVEL SECURITY;

-- ═══════════════════════════════════════════════════════════════
-- 2. RLS POLICIES
-- ═══════════════════════════════════════════════════════════════

-- Anyone can view team ratings (public peer feedback)
DROP POLICY IF EXISTS "Anyone can view team ratings" ON team_ratings;
CREATE POLICY "Anyone can view team ratings" ON team_ratings
    FOR SELECT USING (true);

-- Authenticated users can submit a rating (app validates rater identity)
DROP POLICY IF EXISTS "Authenticated users can submit ratings" ON team_ratings;
CREATE POLICY "Authenticated users can submit ratings" ON team_ratings
    FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

-- Users can only update their own ratings
DROP POLICY IF EXISTS "Users can update own ratings" ON team_ratings;
CREATE POLICY "Users can update own ratings" ON team_ratings
    FOR UPDATE USING (rater_user_id = auth.uid());

-- Users can only delete their own ratings
DROP POLICY IF EXISTS "Users can delete own ratings" ON team_ratings;
CREATE POLICY "Users can delete own ratings" ON team_ratings
    FOR DELETE USING (rater_user_id = auth.uid());

NOTIFY pgrst, 'reload schema';
