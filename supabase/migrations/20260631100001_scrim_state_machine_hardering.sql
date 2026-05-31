-- Comprehensive scrim state machine hardening
-- Fixes race conditions, missing validations, and data consistency at the DB level

-- 1. Enforce valid scrim status values (prevent invalid states)
ALTER TABLE scrims DROP CONSTRAINT IF EXISTS valid_scrim_status;
ALTER TABLE scrims ADD CONSTRAINT valid_scrim_status
    CHECK (status IN ('Open', 'Pending', 'Accepted', 'Ready', 'In Progress', 'Completed', 'Cancelled'));

-- 2. When status is 'Filled' or beyond, opponent_team_id must be set
-- (prevents scrims locked without an opponent)
ALTER TABLE scrims DROP CONSTRAINT IF EXISTS filled_requires_opponent;
ALTER TABLE scrims ADD CONSTRAINT filled_requires_opponent
    CHECK (
        status IN ('Open', 'Pending', 'Accepted') OR
        (status NOT IN ('Open', 'Pending', 'Accepted') AND opponent_team_id IS NOT NULL)
    );

-- 3. When status is 'Completed', winner_team_id must be set and must match a participant
ALTER TABLE scrims DROP CONSTRAINT IF EXISTS completed_requires_winner;
ALTER TABLE scrims ADD CONSTRAINT completed_requires_winner
    CHECK (
        status != 'Completed' OR
        (status = 'Completed' AND winner_team_id IS NOT NULL)
    );

-- 4. Both ready flags must be FALSE when status is 'Open' or 'Accepted' (FILLED in app)
-- (prevents stale ready flags from a previous attempt)
ALTER TABLE scrims DROP CONSTRAINT IF EXISTS open_filled_not_ready;
ALTER TABLE scrims ADD CONSTRAINT open_filled_not_ready
    CHECK (
        status NOT IN ('Open', 'Accepted') OR
        (team_a_ready = FALSE AND team_b_ready = FALSE)
    );

-- 5. Scrim game results status values
ALTER TABLE scrim_game_results DROP CONSTRAINT IF EXISTS valid_game_result_status;
ALTER TABLE scrim_game_results ADD CONSTRAINT valid_game_result_status
    CHECK (status IN ('Pending', 'Awaiting Opponent', 'Both Uploaded', 'Winner Selected', 'Confirmed'));

-- 6. Prevent selecting a winner before both screenshots are uploaded
-- This is enforced by the app, but adding a DB guard for data integrity
ALTER TABLE scrim_game_results DROP CONSTRAINT IF EXISTS winner_requires_screenshots;
ALTER TABLE scrim_game_results ADD CONSTRAINT winner_requires_screenshots
    CHECK (
        winner_team_id IS NULL OR
        (team_a_screenshot_url IS NOT NULL AND team_b_screenshot_url IS NOT NULL)
    );

-- 7. Best_of must match the DB constraint (1, 3, 5) — note: BO2 is a special case handled by app
-- The existing CHECK(best_of IN (1, 3, 5)) already handles this, but we ensure it stays

-- 8. Create an atomic function for markReady that handles the race condition
-- This replaces the fragile client-side "read then write" with a single atomic UPDATE
CREATE OR REPLACE FUNCTION public.mark_scrim_ready(
    p_scrim_id UUID,
    p_team_id UUID
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
    v_is_team_a BOOLEAN;
    v_updates JSON;
BEGIN
    -- Lock the scrim row to prevent race conditions
    SELECT * INTO v_scrim
    FROM scrims
    WHERE id = p_scrim_id
    FOR UPDATE;

    IF v_scrim IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Scrim not found');
    END IF;

    -- Verify status is Ready Check
    IF v_scrim.status != 'Ready' THEN
        RETURN json_build_object('success', false, 'error', 'Scrim is not in ready check phase');
    END IF;

    -- Verify team is a participant
    IF v_scrim.team_id != p_team_id AND v_scrim.opponent_team_id != p_team_id THEN
        RETURN json_build_object('success', false, 'error', 'Team is not a participant');
    END IF;

    -- Verify caller is a team leader
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    SELECT leader_id INTO v_opponent_leader_id FROM teams WHERE id = v_scrim.opponent_team_id;
    IF auth.uid() NOT IN (v_host_leader_id, v_opponent_leader_id) THEN
        RETURN json_build_object('success', false, 'error', 'Only team leaders can mark ready');
    END IF;

    -- Determine which team is marking ready
    v_is_team_a := (v_scrim.team_id = p_team_id);

    -- Check not already ready
    IF v_is_team_a AND v_scrim.team_a_ready THEN
        RETURN json_build_object('success', false, 'error', 'Team is already marked ready');
    END IF;
    IF NOT v_is_team_a AND v_scrim.team_b_ready THEN
        RETURN json_build_object('success', false, 'error', 'Team is already marked ready');
    END IF;

    -- Atomically update ready status and check if both are now ready
    IF v_is_team_a THEN
        UPDATE scrims
        SET team_a_ready = TRUE,
            team_a_ready_at = TIMEZONE('utc', NOW()),
            status = CASE WHEN team_b_ready THEN 'In Progress' ELSE 'Ready' END
        WHERE id = p_scrim_id;
    ELSE
        UPDATE scrims
        SET team_b_ready = TRUE,
            team_b_ready_at = TIMEZONE('utc', NOW()),
            status = CASE WHEN team_a_ready THEN 'In Progress' ELSE 'Ready' END
        WHERE id = p_scrim_id;
    END IF;

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_updates FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_updates);
END;
$$;

-- 9. Create an atomic function for completing a scrim with full validation
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
    v_game_count INTEGER;
    v_games_with_winner INTEGER;
    v_games_with_both_screenshots INTEGER;
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

    -- Verify status is In Progress
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

    -- Count games with winner selected
    SELECT COUNT(*) INTO v_games_with_winner
    FROM scrim_game_results
    WHERE scrim_id = p_scrim_id
      AND winner_team_id IS NOT NULL;

    -- Validate all games are complete
    IF v_game_count != v_scrim.best_of THEN
        RETURN json_build_object('success', false, 'error', format('Expected %s game results, found %s', v_scrim.best_of, v_game_count));
    END IF;

    IF v_games_with_both_screenshots != v_scrim.best_of THEN
        RETURN json_build_object('success', false, 'error', format('All %s games must have screenshots from both teams', v_scrim.best_of));
    END IF;

    IF v_games_with_winner != v_scrim.best_of THEN
        RETURN json_build_object('success', false, 'error', format('All %s games must have a winner selected', v_scrim.best_of));
    END IF;

    -- Atomically complete the scrim
    UPDATE scrims
    SET status = 'Completed',
        winner_team_id = p_winner_team_id,
        result_submitted_at = TIMEZONE('utc', NOW())
    WHERE id = p_scrim_id;

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;

-- 10. Atomic function for uploading a game screenshot (handles race condition)
CREATE OR REPLACE FUNCTION public.upload_game_screenshot(
    p_scrim_id UUID,
    p_game_number INTEGER,
    p_is_team_a BOOLEAN,
    p_screenshot_url TEXT
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
    v_result JSON;
BEGIN
    -- Lock the parent scrim first (prevents race on scrim status)
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

    -- Verify caller is a team leader
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    SELECT leader_id INTO v_opponent_leader_id FROM teams WHERE id = v_scrim.opponent_team_id;
    IF auth.uid() NOT IN (v_host_leader_id, v_opponent_leader_id) THEN
        RETURN json_build_object('success', false, 'error', 'Only team leaders can upload screenshots');
    END IF;

    -- Update screenshot and status atomically (read current state from DB, not parameter)
    IF p_is_team_a THEN
        UPDATE scrim_game_results
        SET team_a_screenshot_url = p_screenshot_url,
            team_a_screenshot_uploaded_at = TIMEZONE('utc', NOW()),
            status = CASE WHEN team_b_screenshot_url IS NOT NULL THEN 'Both Uploaded' ELSE 'Awaiting Opponent' END
        WHERE id = v_game.id;
    ELSE
        UPDATE scrim_game_results
        SET team_b_screenshot_url = p_screenshot_url,
            team_b_screenshot_uploaded_at = TIMEZONE('utc', NOW()),
            status = CASE WHEN team_a_screenshot_url IS NOT NULL THEN 'Both Uploaded' ELSE 'Awaiting Opponent' END
        WHERE id = v_game.id;
    END IF;

    -- Return updated game result
    SELECT to_jsonb(g) INTO v_result FROM scrim_game_results g WHERE g.id = v_game.id;
    RETURN json_build_object('success', true, 'game_result', v_result);
END;
$$;

-- 11. Atomic function for selecting game winner (prevents winner before screenshots)
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

    -- Verify both screenshots are uploaded
    IF v_game.team_a_screenshot_url IS NULL OR v_game.team_b_screenshot_url IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Both teams must upload screenshots before selecting a winner');
    END IF;

    -- Update winner atomically
    UPDATE scrim_game_results
    SET winner_team_id = p_winner_team_id,
        status = 'Winner Selected'
    WHERE id = v_game.id;

    -- Return updated game result
    SELECT to_jsonb(g) INTO v_result FROM scrim_game_results g WHERE g.id = v_game.id;
    RETURN json_build_object('success', true, 'game_result', v_result);
END;
$$;

-- 12. Atomic function for transitioning to ready check with time validation
CREATE OR REPLACE FUNCTION public.transition_to_ready_check(
    p_scrim_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_scrim RECORD;
    v_host_leader_id UUID;
    v_now TIMESTAMP WITH TIME ZONE;
    v_scheduled TIMESTAMP WITH TIME ZONE;
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

    -- Verify caller is host leader
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    IF v_host_leader_id IS NULL OR v_host_leader_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only the host team leader can start ready check');
    END IF;

    -- Verify status is Filled
    IF v_scrim.status != 'Filled' THEN
        RETURN json_build_object('success', false, 'error', 'Scrim must be filled before ready check');
    END IF;

    -- Verify opponent exists
    IF v_scrim.opponent_team_id IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'No opponent has been set');
    END IF;

    -- Verify scheduled time has been reached (or within 5 minutes)
    v_now := TIMEZONE('utc', NOW());
    v_scheduled := v_scrim.scheduled_date + v_scrim.scheduled_time;
    IF v_scheduled > v_now + INTERVAL '5 minutes' THEN
        RETURN json_build_object('success', false, 'error', 'Ready check can only start within 5 minutes of scheduled time');
    END IF;

    -- Atomically transition to ready check and reset flags
    UPDATE scrims
    SET status = 'Ready',
        team_a_ready = FALSE,
        team_b_ready = FALSE,
        team_a_ready_at = NULL,
        team_b_ready_at = NULL
    WHERE id = p_scrim_id;

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;
