-- Migration: Add resolve_tournament_dispute RPC
-- Allows tournament hosts to resolve a disputed match by declaring
-- a winner, a draw, and recording their resolution notes.

CREATE OR REPLACE FUNCTION resolve_tournament_dispute(
    p_match_id       UUID,
    p_winner_team_id UUID DEFAULT NULL,
    p_is_draw        BOOLEAN DEFAULT FALSE,
    p_resolution     TEXT DEFAULT ''
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_tournament_id UUID;
    v_host_id       UUID;
BEGIN
    -- Fetch tournament ID and host
    SELECT tsm.tournament_id, t.host_user_id
    INTO   v_tournament_id, v_host_id
    FROM   tournament_swiss_matches tsm
    JOIN   tournaments t ON t.id = tsm.tournament_id
    WHERE  tsm.id = p_match_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Match not found';
    END IF;

    -- Host-only action
    IF v_host_id IS DISTINCT FROM auth.uid() THEN
        RAISE EXCEPTION 'Only the tournament host can resolve a dispute';
    END IF;

    -- Validate: either winner or draw
    IF NOT p_is_draw AND p_winner_team_id IS NULL THEN
        RAISE EXCEPTION 'Must provide winner or declare a draw';
    END IF;

    -- Validate resolution notes
    IF p_resolution IS NULL OR length(trim(p_resolution)) = 0 THEN
        RAISE EXCEPTION 'Resolution notes are required';
    END IF;

    -- Apply resolution
    UPDATE tournament_swiss_matches
    SET
        status                = 'completed',
        winner_team_id        = CASE WHEN p_is_draw THEN NULL ELSE p_winner_team_id END,
        is_draw               = p_is_draw,
        dispute_resolution    = p_resolution,
        dispute_resolved_by   = auth.uid(),
        result_submitted_at   = NOW(),
        updated_at            = NOW()
    WHERE id = p_match_id;

    -- Award points (re-use existing function if available, else inline)
    PERFORM award_tournament_match_points(p_match_id);

    -- Update standings
    PERFORM update_tournament_scores(v_tournament_id);

    RETURN json_build_object('success', true, 'match_id', p_match_id);
END;
$$;
