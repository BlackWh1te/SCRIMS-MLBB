-- Add targeted LFG posting ban to profiles
-- This allows admins to block users from posting on Find Player without fully banning them from the app

ALTER TABLE profiles ADD COLUMN IF NOT EXISTS lfg_posting_banned_until TIMESTAMP WITH TIME ZONE;

-- Function to check if a user can post LFG
CREATE OR REPLACE FUNCTION can_post_lfg(p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
AS $$
    SELECT NOT (
        is_banned = TRUE OR
        (lfg_posting_banned_until IS NOT NULL AND lfg_posting_banned_until > NOW())
    )
    FROM profiles
    WHERE id = p_user_id;
$$;

-- Update LFG insert policy to use the function
DROP POLICY IF EXISTS "Users can create own LFG post" ON lfg_posts;
CREATE POLICY "Users can create own LFG post" ON lfg_posts
    FOR INSERT WITH CHECK (
        player_id = auth.uid() AND
        (SELECT can_post_lfg(auth.uid()))
    );
