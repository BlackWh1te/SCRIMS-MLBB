-- Fix BO2 tie completion + atomic roster updates

-- ═══════════════════════════════════════════════════════════════════════
-- 1. complete_scrim: allow NULL winner for BO2 ties
-- ═══════════════════════════════════════════════════════════════════════
DROP FUNCTION IF EXISTS public.complete_scrim(UUID, UUID);

CREATE OR REPLACE FUNCTION public.complete_scrim(
    p_scrim_id UUID,
    p_winner_team_id UUID DEFAULT NULL
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
    v_a_wins INT;
    v_b_wins INT;
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

    -- Calculate series wins
    SELECT COUNT(*) INTO v_a_wins
    FROM scrim_game_results
    WHERE scrim_id = p_scrim_id
      AND COALESCE(admin_override_winner_id, winner_team_id) = v_scrim.team_id;

    SELECT COUNT(*) INTO v_b_wins
    FROM scrim_game_results
    WHERE scrim_id = p_scrim_id
      AND COALESCE(admin_override_winner_id, winner_team_id) = v_scrim.opponent_team_id;

    -- If no winner provided, verify it's a legitimate tie (only possible in BO2)
    IF p_winner_team_id IS NULL THEN
        IF v_scrim.best_of != 2 THEN
            RETURN json_build_object('success', false, 'error', 'A winner is required for this format');
        END IF;
        IF v_a_wins != v_b_wins THEN
            RETURN json_build_object('success', false, 'error', 'Series is not tied — a winner must be provided');
        END IF;
    ELSE
        -- Verify winner is a participant
        IF p_winner_team_id NOT IN (v_scrim.team_id, v_scrim.opponent_team_id) THEN
            RETURN json_build_object('success', false, 'error', 'Winner must be a participating team');
        END IF;
        -- Verify the provided winner actually won the series
        IF p_winner_team_id = v_scrim.team_id AND v_a_wins <= v_b_wins THEN
            RETURN json_build_object('success', false, 'error', 'Provided winner does not have more wins');
        END IF;
        IF p_winner_team_id = v_scrim.opponent_team_id AND v_b_wins <= v_a_wins THEN
            RETURN json_build_object('success', false, 'error', 'Provided winner does not have more wins');
        END IF;
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

-- ═══════════════════════════════════════════════════════════════════════
-- 2. Atomic set_scrim_roster RPC
-- ═══════════════════════════════════════════════════════════════════════
DROP FUNCTION IF EXISTS public.set_scrim_roster(UUID, UUID, UUID[]);

CREATE OR REPLACE FUNCTION public.set_scrim_roster(
    p_scrim_id UUID,
    p_team_id UUID,
    p_player_ids UUID[]
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_scrim RECORD;
    v_team RECORD;
    v_leader_id UUID;
    v_existing_count INT;
    v_result JSON;
    v_player_id UUID;
BEGIN
    -- Lock the scrim row
    SELECT * INTO v_scrim
    FROM scrims
    WHERE id = p_scrim_id
    FOR UPDATE;

    IF v_scrim IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Scrim not found');
    END IF;

    -- Verify team is a participant
    IF p_team_id NOT IN (v_scrim.team_id, v_scrim.opponent_team_id) THEN
        RETURN json_build_object('success', false, 'error', 'Team is not a participant in this scrim');
    END IF;

    -- Verify caller is the leader of this team
    SELECT * INTO v_team FROM teams WHERE id = p_team_id;
    IF v_team IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Team not found');
    END IF;
    IF v_team.leader_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only the team leader can set the roster');
    END IF;

    -- Verify all players are active members of the team
    FOREACH v_player_id IN ARRAY p_player_ids LOOP
        IF NOT EXISTS (
            SELECT 1 FROM team_members
            WHERE team_id = p_team_id AND user_id = v_player_id AND role != 'invited'
        ) THEN
            RETURN json_build_object('success', false, 'error', format('Player %s is not an active member of this team', v_player_id));
        END IF;
    END LOOP;

    -- Delete existing roster entries for this team+scrim
    DELETE FROM scrim_rosters WHERE scrim_id = p_scrim_id AND team_id = p_team_id;

    -- Insert new roster entries (all active)
    FOREACH v_player_id IN ARRAY p_player_ids LOOP
        INSERT INTO scrim_rosters (scrim_id, team_id, user_id, is_active)
        VALUES (p_scrim_id, p_team_id, v_player_id, TRUE);
    END LOOP;

    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;

-- Force PostgREST schema cache refresh
NOTIFY pgrst, 'reload schema';
