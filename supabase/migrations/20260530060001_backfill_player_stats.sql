-- Migration: Backfill player_stats for existing profiles
-- Date: 2026-05-30
-- Context:
--   The leaderboard queries the player_stats table. Users who signed up before
--   the handle_new_user trigger was updated have profiles but no player_stats rows,
--   causing them to be invisible on the leaderboard. This migration creates
--   default player_stats (Bronze tier, 0 pts) for every profile that is missing one.

INSERT INTO public.player_stats (user_id, pts, wins, losses, matches_play)
SELECT
    p.id,
    0,
    0,
    0,
    0
FROM public.profiles p
LEFT JOIN public.player_stats ps ON ps.user_id = p.id
WHERE ps.id IS NULL;
