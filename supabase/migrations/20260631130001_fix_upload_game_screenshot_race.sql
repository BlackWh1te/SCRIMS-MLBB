-- Fix upload_game_screenshot RPC to eliminate client-side pre-read race condition.
-- The old RPC required p_is_team_a (determined by client reading scrim first).
-- The new RPC accepts p_team_id and derives is_team_a from the locked DB row.

DROP FUNCTION IF EXISTS public.upload_game_screenshot(UUID, INTEGER, BOOLEAN, TEXT);

CREATE OR REPLACE FUNCTION public.upload_game_screenshot(
    p_scrim_id UUID,
    p_game_number INTEGER,
    p_team_id UUID,
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
    v_is_team_a BOOLEAN;
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

    -- Verify the team is a participant
    IF p_team_id NOT IN (v_scrim.team_id, v_scrim.opponent_team_id) THEN
        RETURN json_build_object('success', false, 'error', 'Team is not a participant in this scrim');
    END IF;

    -- Derive is_team_a from the locked DB row (eliminates client-side race)
    v_is_team_a := (p_team_id = v_scrim.team_id);

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
    IF v_is_team_a THEN
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
