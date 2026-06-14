-- Harden PTS against tampered clients.
--
-- Rules after this migration:
-- 1. Android clients can read player_stats, but cannot insert/update/delete it.
-- 2. award_scrim_points is server-only and idempotent per scrim.
-- 3. Completing a scrim awards PTS inside the database transaction via trigger.
-- 4. Profile stats-like columns, if present, cannot be edited by authenticated users.

BEGIN;

ALTER TABLE public.player_stats ENABLE ROW LEVEL SECURITY;

ALTER TABLE public.scrims
    ADD COLUMN IF NOT EXISTS points_awarded BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing completed scrims may already have been awarded by older app builds.
-- Mark them closed so the new server-side award path cannot duplicate old PTS.
UPDATE public.scrims
SET points_awarded = TRUE
WHERE status = 'Completed'
  AND winner_team_id IS NOT NULL
  AND points_awarded = FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'pts_non_negative'
          AND conrelid = 'public.player_stats'::regclass
    ) THEN
        ALTER TABLE public.player_stats
            ADD CONSTRAINT pts_non_negative CHECK (pts >= 0);
    END IF;
END $$;

-- Remove every known policy that allowed direct or ambiguous player_stats writes.
DROP POLICY IF EXISTS "Users can insert own player stats" ON public.player_stats;
DROP POLICY IF EXISTS "Block direct player_stats updates" ON public.player_stats;
DROP POLICY IF EXISTS "Block direct player_stats deletes" ON public.player_stats;
DROP POLICY IF EXISTS "System can insert/update player stats" ON public.player_stats;
DROP POLICY IF EXISTS "Service can manage player stats" ON public.player_stats;
DROP POLICY IF EXISTS "Service can update player stats" ON public.player_stats;
DROP POLICY IF EXISTS "Service role can insert player stats" ON public.player_stats;
DROP POLICY IF EXISTS "Service role can update player stats" ON public.player_stats;
DROP POLICY IF EXISTS "Service role can delete player stats" ON public.player_stats;
DROP POLICY IF EXISTS "Allow read player_stats" ON public.player_stats;
DROP POLICY IF EXISTS "Users can view own stats" ON public.player_stats;
DROP POLICY IF EXISTS "Users can view teammate stats" ON public.player_stats;
DROP POLICY IF EXISTS "Users can view player stats" ON public.player_stats;

CREATE POLICY "Users can view player stats"
    ON public.player_stats
    FOR SELECT
    TO anon, authenticated
    USING (true);

CREATE POLICY "Service role can insert player stats"
    ON public.player_stats
    FOR INSERT
    TO service_role
    WITH CHECK (true);

CREATE POLICY "Service role can update player stats"
    ON public.player_stats
    FOR UPDATE
    TO service_role
    USING (true)
    WITH CHECK (true);

CREATE POLICY "Service role can delete player stats"
    ON public.player_stats
    FOR DELETE
    TO service_role
    USING (true);

REVOKE INSERT, UPDATE, DELETE ON public.player_stats FROM PUBLIC, anon, authenticated;
GRANT SELECT ON public.player_stats TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.player_stats TO service_role;

-- If legacy profile stat columns exist in an environment, do not let normal users
-- edit them through the broad "Users can update own profile" policy.
DO $$
DECLARE
    v_col TEXT;
BEGIN
    FOREACH v_col IN ARRAY ARRAY['points', 'pts', 'wins', 'losses', 'matches_play'] LOOP
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'profiles'
              AND column_name = v_col
        ) THEN
            EXECUTE format(
                'REVOKE UPDATE (%I) ON public.profiles FROM PUBLIC, anon, authenticated',
                v_col
            );
        END IF;
    END LOOP;
END $$;

CREATE OR REPLACE FUNCTION public.award_scrim_points(
    p_scrim_id UUID,
    p_winner_team_id UUID,
    p_pts_per_win INTEGER DEFAULT 25,
    p_pts_per_loss INTEGER DEFAULT 15
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_scrim RECORD;
    v_team_a_tier TEXT;
    v_team_b_tier TEXT;
    v_tier_diff_a INT;
    v_tier_diff_b INT;
    v_tier_order TEXT[] := ARRAY['Bronze', 'Silver', 'Gold', 'Grandmaster', 'Epic', 'Legend', 'Mythic'];
    v_idx_a INT := 1;
    v_idx_b INT := 1;
    v_i INT;
    v_base_win CONSTANT INT := 25;
    v_base_loss CONSTANT INT := 15;
    roster_entry RECORD;
BEGIN
    SELECT
        s.id,
        s.team_id,
        s.opponent_team_id,
        s.winner_team_id,
        s.status,
        COALESCE(s.best_of, 1) AS best_of,
        COALESCE(s.points_awarded, FALSE) AS points_awarded
    INTO v_scrim
    FROM public.scrims s
    WHERE s.id = p_scrim_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Scrim not found';
    END IF;

    IF v_scrim.status <> 'Completed' THEN
        RAISE EXCEPTION 'Scrim must be completed before PTS can be awarded';
    END IF;

    IF p_winner_team_id IS NULL
       OR (
           p_winner_team_id IS DISTINCT FROM v_scrim.team_id
           AND p_winner_team_id IS DISTINCT FROM v_scrim.opponent_team_id
       )
       OR p_winner_team_id IS DISTINCT FROM v_scrim.winner_team_id THEN
        RAISE EXCEPTION 'Winner must match the completed scrim winner';
    END IF;

    IF v_scrim.points_awarded THEN
        RETURN;
    END IF;

    SELECT COALESCE(t.current_tier, 'Bronze') INTO v_team_a_tier
    FROM public.teams t
    WHERE t.id = v_scrim.team_id;

    SELECT COALESCE(t.current_tier, 'Bronze') INTO v_team_b_tier
    FROM public.teams t
    WHERE t.id = v_scrim.opponent_team_id;

    FOR v_i IN 1..array_length(v_tier_order, 1) LOOP
        IF v_tier_order[v_i] = COALESCE(v_team_a_tier, 'Bronze') THEN
            v_idx_a := v_i;
        END IF;
        IF v_tier_order[v_i] = COALESCE(v_team_b_tier, 'Bronze') THEN
            v_idx_b := v_i;
        END IF;
    END LOOP;

    v_tier_diff_a := v_idx_a - v_idx_b;
    v_tier_diff_b := v_idx_b - v_idx_a;

    FOR roster_entry IN
        SELECT sr.user_id, sr.team_id
        FROM public.scrim_rosters sr
        WHERE sr.scrim_id = p_scrim_id
          AND sr.is_active = TRUE
          AND sr.team_id IN (v_scrim.team_id, v_scrim.opponent_team_id)
    LOOP
        DECLARE
            v_my_tier_diff INT;
            v_final_pts_change INT;
            v_is_winner BOOLEAN;
        BEGIN
            v_is_winner := (roster_entry.team_id = p_winner_team_id);

            IF roster_entry.team_id = v_scrim.team_id THEN
                v_my_tier_diff := v_tier_diff_a;
            ELSE
                v_my_tier_diff := v_tier_diff_b;
            END IF;

            IF v_my_tier_diff <= 0 THEN
                v_final_pts_change := CASE WHEN v_is_winner THEN v_base_win ELSE -v_base_loss END;
            ELSIF v_my_tier_diff = 1 THEN
                v_final_pts_change := CASE
                    WHEN v_is_winner THEN ROUND(v_base_win * 0.80)
                    ELSE -ROUND(v_base_loss * 1.20)
                END;
            ELSE
                v_final_pts_change := CASE
                    WHEN v_is_winner THEN ROUND(v_base_win * 0.70)
                    ELSE -ROUND(v_base_loss * 1.33)
                END;
            END IF;

            v_final_pts_change := v_final_pts_change * v_scrim.best_of;

            INSERT INTO public.player_stats (user_id, pts, wins, losses, matches_play)
            VALUES (
                roster_entry.user_id,
                GREATEST(v_final_pts_change, 0),
                CASE WHEN v_is_winner THEN 1 ELSE 0 END,
                CASE WHEN v_is_winner THEN 0 ELSE 1 END,
                1
            )
            ON CONFLICT (user_id) DO UPDATE SET
                pts = GREATEST(public.player_stats.pts + v_final_pts_change, 0),
                wins = public.player_stats.wins + CASE WHEN v_is_winner THEN 1 ELSE 0 END,
                losses = public.player_stats.losses + CASE WHEN v_is_winner THEN 0 ELSE 1 END,
                matches_play = public.player_stats.matches_play + 1,
                updated_at = TIMEZONE('utc', NOW());
        END;
    END LOOP;

    UPDATE public.scrims
    SET points_awarded = TRUE
    WHERE id = p_scrim_id;

    UPDATE public.match_results mr
    SET pts_awarded = TRUE
    WHERE mr.match_id IN (
        SELECT m.id
        FROM public.matches m
        WHERE m.scrim_id = p_scrim_id
    );
END;
$$;

REVOKE ALL ON FUNCTION public.award_scrim_points(UUID, UUID, INTEGER, INTEGER) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.award_scrim_points(UUID, UUID, INTEGER, INTEGER) FROM anon, authenticated;
GRANT EXECUTE ON FUNCTION public.award_scrim_points(UUID, UUID, INTEGER, INTEGER) TO service_role;

CREATE OR REPLACE FUNCTION public.award_scrim_points_after_completion()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
BEGIN
    IF NEW.status = 'Completed'
       AND NEW.winner_team_id IS NOT NULL
       AND COALESCE(NEW.points_awarded, FALSE) = FALSE
       AND (
           OLD.status IS DISTINCT FROM NEW.status
           OR OLD.winner_team_id IS DISTINCT FROM NEW.winner_team_id
       ) THEN
        PERFORM public.award_scrim_points(NEW.id, NEW.winner_team_id, 25, 15);
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_award_scrim_points_after_completion ON public.scrims;
CREATE TRIGGER trg_award_scrim_points_after_completion
    AFTER UPDATE OF status, winner_team_id
    ON public.scrims
    FOR EACH ROW
    EXECUTE FUNCTION public.award_scrim_points_after_completion();

REVOKE ALL ON FUNCTION public.award_scrim_points_after_completion() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.award_scrim_points_after_completion() FROM anon, authenticated;
GRANT EXECUTE ON FUNCTION public.award_scrim_points_after_completion() TO service_role;

DO $$
BEGIN
    EXECUTE 'REVOKE EXECUTE ON FUNCTION public.update_player_stats_after_scrim(uuid, boolean, text, boolean, boolean, int) FROM PUBLIC';
    EXECUTE 'REVOKE EXECUTE ON FUNCTION public.update_player_stats_after_scrim(uuid, boolean, text, boolean, boolean, int) FROM anon, authenticated';
    EXECUTE 'GRANT EXECUTE ON FUNCTION public.update_player_stats_after_scrim(uuid, boolean, text, boolean, boolean, int) TO service_role';
EXCEPTION WHEN undefined_function THEN
    NULL;
END $$;

NOTIFY pgrst, 'reload schema';

COMMIT;
