-- Migration: Remove unused variables in generate_swiss_pairings to clean up linter warnings
-- Date: 2026-05-26

CREATE OR REPLACE FUNCTION public.generate_swiss_pairings(p_tournament_id uuid)
 RETURNS json
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_tournament RECORD;
    v_new_round INTEGER;
    v_team_ids UUID[];
    v_paired BOOLEAN[];
    v_match_num INTEGER;
    v_match_id UUID;
    v_conversation_id UUID;
    v_team_a_leader UUID;
    v_team_b_leader UUID;
    v_found_match BOOLEAN;
    v_matches_created INTEGER := 0;
    v_byes_created INTEGER := 0;
BEGIN
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can generate pairings');
    END IF;

    IF v_tournament.status NOT IN ('check_in', 'in_progress') THEN
        RETURN json_build_object('success', false, 'error', 'Tournament must be in check-in or in-progress phase');
    END IF;

    SELECT array_agg(tt.team_id ORDER BY
        CASE WHEN v_tournament.current_round = 0 THEN RANDOM() ELSE 0 END,
        tt.swiss_points DESC,
        tt.buchholz_score DESC
    ) INTO v_team_ids
    FROM tournament_teams tt
    WHERE tt.tournament_id = p_tournament_id AND tt.checked_in = TRUE AND tt.is_disqualified = FALSE;

    IF v_team_ids IS NULL OR array_length(v_team_ids, 1) < 4 THEN
        RETURN json_build_object('success', false, 'error', 'Need at least 4 checked-in teams for Swiss pairings');
    END IF;

    v_new_round := v_tournament.current_round + 1;

    IF v_new_round > COALESCE(v_tournament.swiss_rounds, CEIL(LOG(2, v_tournament.max_teams))::INTEGER) THEN
        RETURN json_build_object('success', false, 'error', 'All Swiss rounds have been played');
    END IF;

    v_paired := array_fill(FALSE, ARRAY[array_length(v_team_ids, 1)]);
    v_match_num := 0;

    FOR i IN 1..array_length(v_team_ids, 1) LOOP
        IF v_paired[i] THEN CONTINUE; END IF;

        v_found_match := FALSE;

        FOR j IN i+1..array_length(v_team_ids, 1) LOOP
            IF v_paired[j] THEN CONTINUE; END IF;

            IF NOT EXISTS (
                SELECT 1 FROM tournament_swiss_matches m
                WHERE m.tournament_id = p_tournament_id
                  AND (
                    (m.team_a_id = v_team_ids[i] AND m.team_b_id = v_team_ids[j])
                    OR (m.team_a_id = v_team_ids[j] AND m.team_b_id = v_team_ids[i])
                  )
            ) THEN
                v_match_num := v_match_num + 1;

                SELECT t.leader_id INTO v_team_a_leader FROM teams t WHERE t.id = v_team_ids[i];
                SELECT t.leader_id INTO v_team_b_leader FROM teams t WHERE t.id = v_team_ids[j];

                INSERT INTO conversations (tournament_match_id, participant_a_id, participant_a_name,
                    participant_a_team_id, participant_b_id, participant_b_name, participant_b_team_id)
                VALUES (NULL, v_team_a_leader,
                    (SELECT username FROM profiles WHERE id = v_team_a_leader),
                    v_team_ids[i],
                    v_team_b_leader,
                    (SELECT username FROM profiles WHERE id = v_team_b_leader),
                    v_team_ids[j]
                ) RETURNING id INTO v_conversation_id;

                INSERT INTO conversation_participants (conversation_id, user_id, role) VALUES
                    (v_conversation_id, v_team_a_leader, 'team_a_leader'),
                    (v_conversation_id, v_team_b_leader, 'team_b_leader'),
                    (v_conversation_id, v_tournament.host_user_id, 'host');

                INSERT INTO tournament_swiss_matches (
                    tournament_id, round_number, match_number,
                    team_a_id, team_b_id, conversation_id, status
                ) VALUES (
                    p_tournament_id, v_new_round, v_match_num,
                    v_team_ids[i], v_team_ids[j], v_conversation_id, 'scheduled'
                ) RETURNING id INTO v_match_id;

                UPDATE conversations SET tournament_match_id = v_match_id WHERE id = v_conversation_id;

                v_paired[i] := TRUE;
                v_paired[j] := TRUE;
                v_found_match := TRUE;
                v_matches_created := v_matches_created + 1;
                EXIT;
            END IF;
        END LOOP;

        IF NOT v_found_match THEN
            v_match_num := v_match_num + 1;

            INSERT INTO tournament_swiss_matches (
                tournament_id, round_number, match_number,
                team_a_id, team_b_id, status, is_draw, winner_team_id
            ) VALUES (
                p_tournament_id, v_new_round, v_match_num,
                v_team_ids[i], NULL, 'bye', FALSE, v_team_ids[i]
            );

            UPDATE tournament_teams SET
                swiss_wins = swiss_wins + 1,
                swiss_points = swiss_points + 3
                WHERE tournament_id = p_tournament_id AND team_id = v_team_ids[i];

            v_paired[i] := TRUE;
            v_byes_created := v_byes_created + 1;
        END IF;
    END LOOP;

    UPDATE tournaments SET
        current_round = v_new_round,
        status = 'in_progress',
        updated_at = TIMEZONE('utc', NOW())
        WHERE id = p_tournament_id;

    INSERT INTO app_notifications (user_id, type, title, body, data)
        SELECT t.leader_id, 'TOURNAMENT_ROUND_ADVANCED',
            'New Round Generated!',
            format('Round %s of %s has been generated. Check your match!', v_new_round, v_tournament.title),
            jsonb_build_object('tournament_id', p_tournament_id::TEXT)
        FROM tournament_teams tt
        JOIN teams t ON t.id = tt.team_id
        WHERE tt.tournament_id = p_tournament_id AND tt.checked_in = TRUE AND tt.is_disqualified = FALSE;

    RETURN json_build_object(
        'success', true,
        'round', v_new_round,
        'matches_created', v_matches_created,
        'byes_created', v_byes_created
    );
END;
$function$;
