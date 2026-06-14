-- Backfill avatar_url in lfg_posts from profiles table
-- This fixes the issue for existing posts that were created before the avatar_url column was added

UPDATE lfg_posts
SET avatar_url = profiles.avatar_url
FROM profiles
WHERE lfg_posts.player_id = profiles.id
  AND lfg_posts.avatar_url IS NULL;

-- Also update any existing nulls to ensure everything is synced
UPDATE lfg_posts
SET avatar_url = profiles.avatar_url
FROM profiles
WHERE lfg_posts.player_id = profiles.id;
