-- Allow teams to retroactively change series format when they can't finish all games
-- e.g., BO5 with 3 games played → convert to BO3

CREATE OR REPLACE FUNCTION public.change_series_format(
    p_scrim_id UUID,
    p_new_best_of INTEGER
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
    v_games_played INT;
    v_games_with_winner INT;
    v_result JSON;
BEGIN
    -- Validate new best_of is in allowed values
    IF p_new_best_of NOT IN (1, 2, 3, 5) THEN
        RETURN json_build_object('success', false, 'error', 'Invalid format. Allowed: 1, 2, 3, 5');
    END IF;

    -- Lock the scrim row
    SELECT * INTO v_scrim
    FROM scrims
    WHERE id = p_scrim_id
    FOR UPDATE;

    IF v_scrim IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Scrim not found');
    END IF;

    -- Only allow format changes when scrim is In Progress
    IF v_scrim.status != 'In Progress' THEN
        RETURN json_build_object('success', false, 'error', 'Can only change format while scrim is in progress');
    END IF;

    -- Verify caller is a team leader of either team
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    SELECT leader_id INTO v_opponent_leader_id FROM teams WHERE id = v_scrim.opponent_team_id;
    IF auth.uid() NOT IN (v_host_leader_id, v_opponent_leader_id) THEN
        RETURN json_build_object('success', false, 'error', 'Only team leaders can change the series format');
    END IF;

    -- Can't make format larger (no point)
    IF p_new_best_of >= v_scrim.best_of THEN
        RETURN json_build_object('success', false, 'error', format('New format (%s) must be smaller than current (%s)', p_new_best_of, v_scrim.best_of));
    END IF;

    -- Count games that already have a confirmed winner or admin override
    SELECT COUNT(*) INTO v_games_with_winner
    FROM scrim_game_results
    WHERE scrim_id = p_scrim_id
      AND (status = 'Confirmed' OR admin_override_winner_id IS NOT NULL);

    -- Can't shrink below games that already have winners
    IF v_games_with_winner > p_new_best_of THEN
        RETURN json_build_object('success', false, 'error', format('Cannot shrink to %s games — %s games already have confirmed winners', p_new_best_of, v_games_with_winner));
    END IF;

    -- Count all game result rows that exist
    SELECT COUNT(*) INTO v_games_played
    FROM scrim_game_results
    WHERE scrim_id = p_scrim_id;

    -- Delete orphaned game results beyond the new format
    DELETE FROM scrim_game_results
    WHERE scrim_id = p_scrim_id
      AND game_number > p_new_best_of;

    -- Update scrim best_of
    UPDATE scrims
    SET best_of = p_new_best_of,
        updated_at = TIMEZONE('utc', NOW())
    WHERE id = p_scrim_id;

    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object(
        'success', true,
        'scrim', v_result,
        'deleted_games', v_games_played - p_new_best_of
    );
END;
$$;

-- Force PostgREST schema cache refresh
NOTIFY pgrst, 'reload schema';
