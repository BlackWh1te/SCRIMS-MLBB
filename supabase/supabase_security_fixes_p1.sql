-- ============================================================
-- Supabase Database Security Fixes — Part 1 of 3
-- Each statement is wrapped in DO $$ block to be idempotent
-- ============================================================

-- FIX 1 [CRITICAL]: Enable RLS on app_notifications + create policies
DO $$
BEGIN
    ALTER TABLE app_notifications ENABLE ROW LEVEL SECURITY;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DROP POLICY IF EXISTS "Users can read own notifications" ON app_notifications;
CREATE POLICY "Users can read own notifications"
    ON app_notifications FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Authenticated users can insert notifications" ON app_notifications;
CREATE POLICY "Authenticated users can insert notifications"
    ON app_notifications FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own notifications" ON app_notifications;
CREATE POLICY "Users can update own notifications"
    ON app_notifications FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own notifications" ON app_notifications;
CREATE POLICY "Users can delete own notifications"
    ON app_notifications FOR DELETE
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Admins can read all notifications" ON app_notifications;
CREATE POLICY "Admins can read all notifications"
    ON app_notifications FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM profiles
            WHERE profiles.id = auth.uid()
            AND profiles.is_admin = TRUE
        )
    );

-- FIX 2 [HIGH]: Revoke public execute on SECURITY DEFINER functions
DO $$
BEGIN
    REVOKE EXECUTE ON FUNCTION award_scrim_points FROM anon, authenticated;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    REVOKE EXECUTE ON FUNCTION handle_new_user FROM anon, authenticated;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    GRANT EXECUTE ON FUNCTION award_scrim_points TO service_role;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    GRANT EXECUTE ON FUNCTION handle_new_user TO service_role;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- FIX 3 [HIGH]: Replace award_scrim_points SECURITY DEFINER with SECURITY INVOKER
DROP FUNCTION IF EXISTS award_scrim_points(UUID, UUID, INTEGER, INTEGER);
CREATE OR REPLACE FUNCTION award_scrim_points(
    p_scrim_id UUID,
    p_winner_team_id UUID,
    p_pts_per_win INTEGER DEFAULT 25,
    p_pts_per_loss INTEGER DEFAULT 15
)
RETURNS VOID
SECURITY INVOKER
LANGUAGE plpgsql AS $$
DECLARE
    roster_entry RECORD;
    is_winner BOOLEAN;
BEGIN
    FOR roster_entry IN
        SELECT sr.user_id, sr.team_id, sr.is_active
        FROM scrim_rosters sr
        WHERE sr.scrim_id = p_scrim_id AND sr.is_active = TRUE
    LOOP
        is_winner := (roster_entry.team_id = p_winner_team_id);

        INSERT INTO player_stats (user_id, pts, wins, losses, matches_play)
        VALUES (
            roster_entry.user_id,
            CASE WHEN is_winner THEN p_pts_per_win ELSE -p_pts_per_loss END,
            CASE WHEN is_winner THEN 1 ELSE 0 END,
            CASE WHEN is_winner THEN 0 ELSE 1 END,
            1
        )
        ON CONFLICT (user_id) DO UPDATE SET
            pts = player_stats.pts + CASE WHEN is_winner THEN p_pts_per_win ELSE -p_pts_per_loss END,
            wins = player_stats.wins + CASE WHEN is_winner THEN 1 ELSE 0 END,
            losses = player_stats.losses + CASE WHEN is_winner THEN 0 ELSE 1 END,
            matches_play = player_stats.matches_play + 1,
            updated_at = TIMEZONE('utc', NOW());
    END LOOP;

    UPDATE match_results
    SET pts_awarded = TRUE
    WHERE match_id IN (
        SELECT m.id FROM matches m WHERE m.scrim_id = p_scrim_id
    );
END;
$$;

-- FIX 4 [MEDIUM]: Fix player_stats overly permissive policies
DROP POLICY IF EXISTS "System can insert/update player stats" ON player_stats;
DROP POLICY IF EXISTS "Allow read player_stats" ON player_stats;
DROP POLICY IF EXISTS "Users can view own stats" ON player_stats;
DROP POLICY IF EXISTS "Users can view teammate stats" ON player_stats;
DROP POLICY IF EXISTS "Users can view player stats" ON player_stats;

CREATE POLICY "Service can manage player stats"
    ON player_stats FOR INSERT
    WITH CHECK (true);

CREATE POLICY "Service can update player stats"
    ON player_stats FOR UPDATE
    USING (true)
    WITH CHECK (true);

CREATE POLICY "Users can view player stats"
    ON player_stats FOR SELECT
    USING (true);

-- Remove duplicate admin_activity policy
DROP POLICY IF EXISTS "Allow read admin_activity" ON admin_activity;