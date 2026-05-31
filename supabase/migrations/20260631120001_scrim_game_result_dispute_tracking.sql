-- Migration: Add dispute tracking to scrim_game_results for admin resolution
-- Date: 2026-05-31
-- Purpose: Both team leaders must independently select the same winner.
--          If they disagree, the game is flagged as disputed for admin review.

-- 1. Add columns to track each team's winner selection
ALTER TABLE scrim_game_results
    ADD COLUMN IF NOT EXISTS team_a_selected_winner_id UUID REFERENCES teams(id),
    ADD COLUMN IF NOT EXISTS team_b_selected_winner_id UUID REFERENCES teams(id),
    ADD COLUMN IF NOT EXISTS admin_override_winner_id UUID REFERENCES teams(id),
    ADD COLUMN IF NOT EXISTS is_disputed BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Update status constraint to include DISPUTED
ALTER TABLE scrim_game_results DROP CONSTRAINT IF EXISTS valid_game_result_status;
ALTER TABLE scrim_game_results ADD CONSTRAINT valid_game_result_status
    CHECK (status IN ('Pending', 'Awaiting Opponent', 'Both Uploaded', 'Winner Selected', 'Disputed', 'Confirmed'));

-- 3. Drop and recreate the old select_game_winner to track both selections + detect disputes
DROP FUNCTION IF EXISTS public.select_game_winner(UUID, INTEGER, UUID);

CREATE OR REPLACE FUNCTION public.select_game_winner(
    p_scrim_id UUID,
    p_game_number INTEGER,
    p_winner_team_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_game RECORD;
    v_scrim RECORD;
    v_host_leader_id UUID;
    v_opponent_leader_id UUID;
    v_is_team_a BOOLEAN;
    v_result JSON;
BEGIN
    -- Lock the parent scrim first
    SELECT * INTO v_scrim
    FROM scrims
    WHERE id = p_scrim_id
    FOR UPDATE;

    IF v_scrim IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Scrim not found');
    END IF;

    -- Verify scrim is in progress
    IF v_scrim.status != 'In Progress' THEN
        RETURN json_build_object('success', false, 'error', 'Scrim is not in progress');
    END IF;

    -- Lock the game result row
    SELECT * INTO v_game
    FROM scrim_game_results
    WHERE scrim_id = p_scrim_id AND game_number = p_game_number
    FOR UPDATE;

    IF v_game IS NULL THEN
        RETURN json_build_object('success', false, 'error', format('Game %s not found', p_game_number));
    END IF;

    -- Verify winner is a participant
    IF p_winner_team_id NOT IN (v_scrim.team_id, v_scrim.opponent_team_id) THEN
        RETURN json_build_object('success', false, 'error', 'Winner must be a participating team');
    END IF;

    -- Verify caller is a team leader
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    SELECT leader_id INTO v_opponent_leader_id FROM teams WHERE id = v_scrim.opponent_team_id;
    IF auth.uid() NOT IN (v_host_leader_id, v_opponent_leader_id) THEN
        RETURN json_build_object('success', false, 'error', 'Only team leaders can select the winner');
    END IF;

    -- Determine which team the caller represents
    v_is_team_a := (auth.uid() = v_host_leader_id);

    -- Verify both screenshots are uploaded
    IF v_game.team_a_screenshot_url IS NULL OR v_game.team_b_screenshot_url IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Both teams must upload screenshots before selecting a winner');
    END IF;

    -- Prevent re-selection by the same team
    IF v_is_team_a AND v_game.team_a_selected_winner_id IS NOT NULL THEN
        RETURN json_build_object('success', false, 'error', 'Your team already selected a winner for this game');
    END IF;
    IF NOT v_is_team_a AND v_game.team_b_selected_winner_id IS NOT NULL THEN
        RETURN json_build_object('success', false, 'error', 'Your team already selected a winner for this game');
    END IF;

    -- Record this team's selection
    IF v_is_team_a THEN
        UPDATE scrim_game_results
        SET team_a_selected_winner_id = p_winner_team_id,
            updated_at = TIMEZONE('utc', NOW())
        WHERE id = v_game.id;
    ELSE
        UPDATE scrim_game_results
        SET team_b_selected_winner_id = p_winner_team_id,
            updated_at = TIMEZONE('utc', NOW())
        WHERE id = v_game.id;
    END IF;

    -- Re-read the row to get both selections
    SELECT * INTO v_game
    FROM scrim_game_results
    WHERE id = v_game.id;

    -- Both teams have selected: check for agreement or dispute
    IF v_game.team_a_selected_winner_id IS NOT NULL AND v_game.team_b_selected_winner_id IS NOT NULL THEN
        IF v_game.team_a_selected_winner_id = v_game.team_b_selected_winner_id THEN
            -- Agreement: confirm the winner
            UPDATE scrim_game_results
            SET winner_team_id = v_game.team_a_selected_winner_id,
                status = 'Confirmed',
                updated_at = TIMEZONE('utc', NOW())
            WHERE id = v_game.id;
        ELSE
            -- Dispute: flag for admin review
            UPDATE scrim_game_results
            SET is_disputed = TRUE,
                status = 'Disputed',
                updated_at = TIMEZONE('utc', NOW())
            WHERE id = v_game.id;

            -- Notify admins (insert into a notifications table or similar)
            -- For now, the disputed flag is enough for the admin panel to pick it up
        END IF;
    ELSE
        -- Only one team has selected so far
        UPDATE scrim_game_results
        SET status = 'Winner Selected',
            updated_at = TIMEZONE('utc', NOW())
        WHERE id = v_game.id;
    END IF;

    -- Return updated game result
    SELECT to_jsonb(g) INTO v_result FROM scrim_game_results g WHERE g.id = v_game.id;
    RETURN json_build_object('success', true, 'game_result', v_result);
END;
$$;

-- 4. Update complete_scrim to allow admin-overridden winners and handle disputes
DROP FUNCTION IF EXISTS public.complete_scrim(UUID, UUID);

CREATE OR REPLACE FUNCTION public.complete_scrim(
    p_scrim_id UUID,
    p_winner_team_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_scrim RECORD;
    v_host_leader_id UUID;
    v_opponent_leader_id UUID;
    v_game_count INT;
    v_games_with_both_screenshots INT;
    v_games_resolved INT;
    v_disputed_count INT;
    v_result JSON;
BEGIN
    -- Lock the scrim row
    SELECT * INTO v_scrim
    FROM scrims
    WHERE id = p_scrim_id
    FOR UPDATE;

    IF v_scrim IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Scrim not found');
    END IF;

    IF v_scrim.status != 'In Progress' THEN
        RETURN json_build_object('success', false, 'error', 'Scrim is not in progress');
    END IF;

    -- Verify winner is a participant
    IF p_winner_team_id NOT IN (v_scrim.team_id, v_scrim.opponent_team_id) THEN
        RETURN json_build_object('success', false, 'error', 'Winner must be a participating team');
    END IF;

    -- Verify caller is a team leader
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    SELECT leader_id INTO v_opponent_leader_id FROM teams WHERE id = v_scrim.opponent_team_id;
    IF auth.uid() NOT IN (v_host_leader_id, v_opponent_leader_id) THEN
        RETURN json_build_object('success', false, 'error', 'Only team leaders can complete the scrim');
    END IF;

    -- Count game results
    SELECT COUNT(*) INTO v_game_count
    FROM scrim_game_results WHERE scrim_id = p_scrim_id;

    -- Count games with both screenshots
    SELECT COUNT(*) INTO v_games_with_both_screenshots
    FROM scrim_game_results
    WHERE scrim_id = p_scrim_id
      AND team_a_screenshot_url IS NOT NULL
      AND team_b_screenshot_url IS NOT NULL;

    -- Count resolved games (confirmed winner OR admin override)
    SELECT COUNT(*) INTO v_games_resolved
    FROM scrim_game_results
    WHERE scrim_id = p_scrim_id
      AND (
          status = 'Confirmed'
          OR admin_override_winner_id IS NOT NULL
      );

    -- Count disputed games without admin override
    SELECT COUNT(*) INTO v_disputed_count
    FROM scrim_game_results
    WHERE scrim_id = p_scrim_id
      AND is_disputed = TRUE
      AND admin_override_winner_id IS NULL;

    IF v_game_count != v_scrim.best_of THEN
        RETURN json_build_object('success', false, 'error', format('Expected %s game results, found %s', v_scrim.best_of, v_game_count));
    END IF;

    IF v_games_with_both_screenshots != v_scrim.best_of THEN
        RETURN json_build_object('success', false, 'error', format('All %s games must have screenshots from both teams', v_scrim.best_of));
    END IF;

    IF v_disputed_count > 0 THEN
        RETURN json_build_object('success', false, 'error', format('%s game(s) are disputed and require admin resolution', v_disputed_count));
    END IF;

    IF v_games_resolved != v_scrim.best_of THEN
        RETURN json_build_object('success', false, 'error', format('All %s games must have a confirmed winner', v_scrim.best_of));
    END IF;

    -- Mark scrim as completed
    UPDATE scrims
    SET status = 'Completed',
        winner_team_id = p_winner_team_id,
        result_submitted_at = TIMEZONE('utc', NOW()),
        updated_at = TIMEZONE('utc', NOW())
    WHERE id = p_scrim_id;

    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;

-- 5. Admin override function: allows admin to resolve a disputed game
CREATE OR REPLACE FUNCTION public.admin_resolve_game_winner(
    p_game_result_id UUID,
    p_winner_team_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_game RECORD;
    v_scrim RECORD;
    v_result JSON;
BEGIN
    -- Verify caller is an admin
    IF NOT EXISTS (
        SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = TRUE
    ) THEN
        RETURN json_build_object('success', false, 'error', 'Admin access required');
    END IF;

    -- Lock and fetch the game result
    SELECT * INTO v_game
    FROM scrim_game_results
    WHERE id = p_game_result_id
    FOR UPDATE;

    IF v_game IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Game result not found');
    END IF;

    -- Fetch the scrim to validate the winner is a participant
    SELECT * INTO v_scrim
    FROM scrims
    WHERE id = v_game.scrim_id;

    IF p_winner_team_id NOT IN (v_scrim.team_id, v_scrim.opponent_team_id) THEN
        RETURN json_build_object('success', false, 'error', 'Winner must be a participating team');
    END IF;

    -- Apply admin override
    UPDATE scrim_game_results
    SET admin_override_winner_id = p_winner_team_id,
        winner_team_id = p_winner_team_id,
        is_disputed = FALSE,
        status = 'Confirmed',
        updated_at = TIMEZONE('utc', NOW())
    WHERE id = p_game_result_id;

    SELECT to_jsonb(g) INTO v_result FROM scrim_game_results g WHERE g.id = p_game_result_id;
    RETURN json_build_object('success', true, 'game_result', v_result);
END;
$$;

-- 6. RLS policy for admin override (admins can update any game result)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'scrim_game_results'
          AND policyname = 'Admins can resolve disputed game results'
    ) THEN
        CREATE POLICY "Admins can resolve disputed game results"
            ON scrim_game_results FOR UPDATE
            USING (
                EXISTS (
                    SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = TRUE
                )
            )
            WITH CHECK (
                EXISTS (
                    SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = TRUE
                )
            );
    END IF;
END $$;

-- 7. Force PostgREST to reload its schema cache immediately
NOTIFY pgrst, 'reload schema';
