-- Migration: Fix app_notifications column references (message -> body, action_id -> data)
-- and is_team_leader immutability flag
-- Date: 2026-05-26

-- 1. Fix is_team_leader: marked IMMUTABLE but queries a table, should be STABLE
CREATE OR REPLACE FUNCTION public.is_team_leader(user_id uuid, team_id uuid)
 RETURNS boolean
 LANGUAGE plpgsql
 STABLE
 SET search_path TO 'public'
AS $function$
BEGIN
  RETURN EXISTS (SELECT 1 FROM teams WHERE id = is_team_leader.team_id AND leader_id = is_team_leader.user_id);
END;
$function$;

-- 2. Fix apply_for_tournament: message -> body, action_id -> data
CREATE OR REPLACE FUNCTION public.apply_for_tournament(p_tournament_id uuid, p_team_id uuid)
 RETURNS json
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_tournament RECORD;
    v_team RECORD;
    v_member_count INTEGER;
    v_missing_telegram TEXT[];
    v_existing_status TEXT;
    v_rejection_count INTEGER;
    v_application_id UUID;
BEGIN
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    IF v_tournament.status != 'registration' THEN
        RETURN json_build_object('success', false, 'error', 'Tournament is not accepting applications');
    END IF;

    SELECT * INTO v_team FROM teams WHERE id = p_team_id AND leader_id = auth.uid();
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Only team leader can apply');
    END IF;

    SELECT COUNT(*) INTO v_member_count FROM team_members WHERE team_id = p_team_id;
    IF v_member_count < v_tournament.min_team_size THEN
        RETURN json_build_object('success', false, 'error',
            format('Team needs at least %s members (currently %s)', v_tournament.min_team_size, v_member_count));
    END IF;

    SELECT array_agg(p.username) INTO v_missing_telegram
        FROM team_members tm
        JOIN profiles p ON p.id = tm.user_id
        WHERE tm.team_id = p_team_id AND (p.telegram_username IS NULL OR p.telegram_username = '');
    IF v_missing_telegram IS NOT NULL AND array_length(v_missing_telegram, 1) > 0 THEN
        RETURN json_build_object('success', false, 'error', 'missing_telegram_users',
            'missing_telegram_users', v_missing_telegram);
    END IF;

    SELECT status INTO v_existing_status
        FROM tournament_applications
        WHERE tournament_id = p_tournament_id AND team_id = p_team_id;

    IF v_existing_status = 'pending' THEN
        RETURN json_build_object('success', false, 'error', 'Application already pending');
    END IF;

    IF v_existing_status = 'accepted' THEN
        RETURN json_build_object('success', false, 'error', 'Team already accepted');
    END IF;

    IF v_existing_status = 'blocked' THEN
        RETURN json_build_object('success', false, 'error', 'Team is blocked from this tournament');
    END IF;

    IF v_existing_status = 'rejected' THEN
        SELECT COUNT(*) INTO v_rejection_count
            FROM tournament_applications
            WHERE tournament_id = p_tournament_id AND team_id = p_team_id
              AND status IN ('rejected', 'blocked');
        IF v_rejection_count >= 3 THEN
            RETURN json_build_object('success', false, 'error', 'Team is blocked after 3 rejections');
        END IF;
    END IF;

    IF (SELECT COUNT(*) FROM tournament_teams WHERE tournament_id = p_tournament_id) >= v_tournament.max_teams THEN
        RETURN json_build_object('success', false, 'error', 'Tournament is full');
    END IF;

    INSERT INTO tournament_applications (tournament_id, team_id, status, attempt_number)
        VALUES (p_tournament_id, p_team_id, 'pending',
            COALESCE((SELECT MAX(attempt_number) FROM tournament_applications
                      WHERE tournament_id = p_tournament_id AND team_id = p_team_id), 0) + 1)
        ON CONFLICT (tournament_id, team_id) DO UPDATE SET
            status = 'pending',
            rejection_reason = NULL,
            attempt_number = tournament_applications.attempt_number + 1,
            applied_at = TIMEZONE('utc', NOW()),
            reviewed_at = NULL,
            reviewed_by = NULL
        RETURNING id INTO v_application_id;

    INSERT INTO app_notifications (user_id, type, title, body, data)
        VALUES (v_tournament.host_user_id, 'TOURNAMENT_APPLICATION_NEW',
            'New Tournament Application',
            format('Team %s applied to your tournament %s', v_team.name, v_tournament.title),
            jsonb_build_object('tournament_id', p_tournament_id::TEXT));

    RETURN json_build_object('success', true, 'application_id', v_application_id);
END;
$function$;

-- 3. Fix cancel_tournament: message -> body, action_id -> data, add search_path
CREATE OR REPLACE FUNCTION public.cancel_tournament(p_tournament_id uuid, p_cancellation_reason text DEFAULT NULL::text)
 RETURNS json
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_tournament RECORD;
BEGIN
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can cancel');
    END IF;

    IF v_tournament.status = 'completed' THEN
        RETURN json_build_object('success', false, 'error', 'Cannot cancel a completed tournament');
    END IF;

    UPDATE tournaments SET
        status = 'cancelled',
        cancelled_at = TIMEZONE('utc', NOW()),
        cancellation_reason = p_cancellation_reason,
        updated_at = TIMEZONE('utc', NOW())
        WHERE id = p_tournament_id;

    UPDATE profiles SET
        tournaments_cancelled = tournaments_cancelled + 1,
        host_trust_score = GREATEST(host_trust_score - 0.5, 1.0)
        WHERE id = v_tournament.host_user_id;

    INSERT INTO app_notifications (user_id, type, title, body, data)
        SELECT t.leader_id, 'TOURNAMENT_CANCELLED',
            'Tournament Cancelled',
            format('%s has been cancelled. Reason: %s', v_tournament.title, COALESCE(p_cancellation_reason, 'Not specified')),
            jsonb_build_object('tournament_id', p_tournament_id::TEXT)
        FROM tournament_teams tt
        JOIN teams t ON t.id = tt.team_id
        WHERE tt.tournament_id = p_tournament_id;

    RETURN json_build_object('success', true);
END;
$function$;

-- 4. Fix complete_tournament: message -> body, action_id -> data
CREATE OR REPLACE FUNCTION public.complete_tournament(p_tournament_id uuid)
 RETURNS json
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_tournament RECORD;
    v_placement INTEGER := 1;
    v_team_record RECORD;
BEGIN
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can complete tournament');
    END IF;

    IF EXISTS (
        SELECT 1 FROM tournament_swiss_matches
        WHERE tournament_id = p_tournament_id
          AND status IN ('scheduled', 'in_progress', 'disputed')
    ) THEN
        RETURN json_build_object('success', false, 'error', 'All matches must be completed first');
    END IF;

    FOR v_team_record IN
        SELECT tt.team_id, tt.swiss_points, tt.buchholz_score, tt.sonneborn_berger,
               tt.swiss_wins, tt.swiss_losses, tt.swiss_draws
            FROM tournament_teams tt
            WHERE tt.tournament_id = p_tournament_id AND tt.is_disqualified = FALSE
            ORDER BY tt.swiss_points DESC, tt.buchholz_score DESC, tt.sonneborn_berger DESC
    LOOP
        UPDATE tournament_teams SET final_placement = v_placement
            WHERE tournament_id = p_tournament_id AND team_id = v_team_record.team_id;

        INSERT INTO tournament_player_stats (tournament_id, user_id, team_id, placement,
            matches_won, matches_lost, matches_drawn)
            SELECT p_tournament_id, tm.user_id, v_team_record.team_id, v_placement,
                v_team_record.swiss_wins, v_team_record.swiss_losses, v_team_record.swiss_draws
            FROM team_members tm WHERE tm.team_id = v_team_record.team_id
            ON CONFLICT (tournament_id, user_id) DO UPDATE SET
                placement = v_placement,
                matches_won = v_team_record.swiss_wins,
                matches_lost = v_team_record.swiss_losses,
                matches_drawn = v_team_record.swiss_draws;

        v_placement := v_placement + 1;
    END LOOP;

    UPDATE tournament_teams SET final_placement = v_placement
        WHERE tournament_id = p_tournament_id AND is_disqualified = TRUE AND final_placement IS NULL;

    UPDATE tournaments SET
        status = 'completed',
        completed_at = TIMEZONE('utc', NOW()),
        updated_at = TIMEZONE('utc', NOW())
        WHERE id = p_tournament_id;

    UPDATE profiles SET
        tournaments_completed = tournaments_completed + 1,
        tournaments_hosted = tournaments_hosted + 1,
        host_trust_score = LEAST(host_trust_score + 0.3, 10.0)
        WHERE id = v_tournament.host_user_id;

    INSERT INTO app_notifications (user_id, type, title, body, data)
        SELECT t.leader_id, 'TOURNAMENT_COMPLETED',
            'Tournament Completed!',
            format('%s has finished! Check final standings.', v_tournament.title),
            jsonb_build_object('tournament_id', p_tournament_id::TEXT)
        FROM tournament_teams tt
        JOIN teams t ON t.id = tt.team_id
        WHERE tt.tournament_id = p_tournament_id;

    RETURN json_build_object('success', true, 'placements_set', v_placement - 1);
END;
$function$;

-- 5. Fix disqualify_tournament_team: message -> body, action_id -> data, add search_path
CREATE OR REPLACE FUNCTION public.disqualify_tournament_team(p_tournament_id uuid, p_team_id uuid, p_reason text DEFAULT NULL::text)
 RETURNS json
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_tournament RECORD;
    v_team RECORD;
    v_active_match_id UUID;
    v_opponent_team_id UUID;
BEGIN
    SELECT * INTO v_tournament FROM tournaments WHERE id = p_tournament_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Tournament not found');
    END IF;

    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can disqualify teams');
    END IF;

    SELECT * INTO v_team FROM teams WHERE id = p_team_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Team not found');
    END IF;

    UPDATE tournament_teams SET
        is_disqualified = TRUE,
        disqualification_reason = p_reason,
        disqualified_at = TIMEZONE('utc', NOW()),
        disqualified_by = auth.uid()
        WHERE tournament_id = p_tournament_id AND team_id = p_team_id;

    SELECT id, CASE WHEN team_a_id = p_team_id THEN team_b_id ELSE team_a_id END
        INTO v_active_match_id, v_opponent_team_id
        FROM tournament_swiss_matches
        WHERE tournament_id = p_tournament_id
          AND (team_a_id = p_team_id OR team_b_id = p_team_id)
          AND status IN ('scheduled', 'in_progress')
        LIMIT 1;

    IF v_active_match_id IS NOT NULL AND v_opponent_team_id IS NOT NULL THEN
        UPDATE tournament_swiss_matches SET
            status = 'completed',
            winner_team_id = v_opponent_team_id,
            result_submitted_at = TIMEZONE('utc', NOW()),
            result_submitted_by = auth.uid(),
            updated_at = TIMEZONE('utc', NOW())
            WHERE id = v_active_match_id;

        UPDATE tournament_teams SET
            swiss_wins = swiss_wins + 1,
            swiss_points = swiss_points + 3
            WHERE tournament_id = p_tournament_id AND team_id = v_opponent_team_id;

        PERFORM recalculate_tiebreakers(p_tournament_id);
    END IF;

    INSERT INTO app_notifications (user_id, type, title, body, data)
        VALUES (v_team.leader_id, 'TOURNAMENT_TEAM_DISQUALIFIED',
            'Team Disqualified',
            format('Your team %s has been disqualified from %s. Reason: %s',
                v_team.name, v_tournament.title, COALESCE(p_reason, 'Not specified')),
            jsonb_build_object('tournament_id', p_tournament_id::TEXT));

    RETURN json_build_object('success', true);
END;
$function$;

-- 6. Fix generate_swiss_pairings: message -> body, action_id -> data
CREATE OR REPLACE FUNCTION public.generate_swiss_pairings(p_tournament_id uuid)
 RETURNS json
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_tournament RECORD;
    v_new_round INTEGER;
    v_team_record RECORD;
    v_team_ids UUID[];
    v_paired BOOLEAN[];
    v_match_num INTEGER;
    v_match_id UUID;
    v_conversation_id UUID;
    v_team_a_leader UUID;
    v_team_b_leader UUID;
    v_found_match BOOLEAN;
    v_j INTEGER;
    v_bye_team_idx INTEGER;
    v_bye_team_id UUID;
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

-- 7. Fix review_tournament_application: message -> body, action_id -> data, add search_path
CREATE OR REPLACE FUNCTION public.review_tournament_application(p_application_id uuid, p_decision text, p_rejection_reason text DEFAULT NULL::text)
 RETURNS json
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_application RECORD;
    v_tournament RECORD;
    v_team RECORD;
    v_rejection_count INTEGER;
BEGIN
    IF p_decision NOT IN ('accepted', 'rejected') THEN
        RETURN json_build_object('success', false, 'error', 'Invalid decision');
    END IF;

    SELECT * INTO v_application FROM tournament_applications WHERE id = p_application_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Application not found');
    END IF;

    SELECT * INTO v_tournament FROM tournaments WHERE id = v_application.tournament_id;
    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can review applications');
    END IF;

    IF v_application.status != 'pending' THEN
        RETURN json_build_object('success', false, 'error', 'Application is not pending');
    END IF;

    SELECT * INTO v_team FROM teams WHERE id = v_application.team_id;

    IF p_decision = 'accepted' THEN
        UPDATE tournament_applications SET
            status = 'accepted',
            reviewed_at = TIMEZONE('utc', NOW()),
            reviewed_by = auth.uid()
            WHERE id = p_application_id;

        INSERT INTO tournament_teams (tournament_id, team_id)
            VALUES (v_application.tournament_id, v_application.team_id)
            ON CONFLICT (tournament_id, team_id) DO NOTHING;

        INSERT INTO app_notifications (user_id, type, title, body, data)
            VALUES (v_team.leader_id, 'TOURNAMENT_APPLICATION_ACCEPTED',
                'Application Accepted!',
                format('Your team %s has been accepted to %s!', v_team.name, v_tournament.title),
                jsonb_build_object('tournament_id', v_tournament.id::TEXT));

    ELSE
        UPDATE tournament_applications SET
            status = 'rejected',
            rejection_reason = p_rejection_reason,
            reviewed_at = TIMEZONE('utc', NOW()),
            reviewed_by = auth.uid()
            WHERE id = p_application_id;

        SELECT COUNT(*) INTO v_rejection_count
            FROM tournament_applications
            WHERE tournament_id = v_application.tournament_id
              AND team_id = v_application.team_id
              AND status IN ('rejected', 'blocked');

        IF v_rejection_count >= 3 THEN
            INSERT INTO app_notifications (user_id, type, title, body, data)
                VALUES (v_team.leader_id, 'TOURNAMENT_APPLICATION_BLOCKED',
                    'Application Blocked',
                    format('Your team %s has been blocked from %s after 3 rejections.', v_team.name, v_tournament.title),
                    jsonb_build_object('tournament_id', v_tournament.id::TEXT));
        ELSE
            INSERT INTO app_notifications (user_id, type, title, body, data)
                VALUES (v_team.leader_id, 'TOURNAMENT_APPLICATION_REJECTED',
                    'Application Rejected',
                    format('Your team %s was rejected from %s. Reason: %s',
                        v_team.name, v_tournament.title, COALESCE(p_rejection_reason, 'Not specified')),
                    jsonb_build_object('tournament_id', v_tournament.id::TEXT));
        END IF;
    END IF;

    RETURN json_build_object('success', true, 'decision', p_decision);
END;
$function$;

-- 8. Fix set_tournament_match_roster: message -> body, action_id -> data, add search_path
CREATE OR REPLACE FUNCTION public.set_tournament_match_roster(p_match_id uuid, p_team_id uuid, p_game_number integer, p_player_ids uuid[])
 RETURNS json
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_match RECORD;
    v_best_of INTEGER;
    v_min_team_size INTEGER;
    v_roster_size INTEGER;
BEGIN
    SELECT * INTO v_match FROM tournament_swiss_matches WHERE id = p_match_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Match not found');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM teams WHERE id = p_team_id AND leader_id = auth.uid()) THEN
        RETURN json_build_object('success', false, 'error', 'Only team leader can set roster');
    END IF;

    IF v_match.team_a_id != p_team_id AND v_match.team_b_id != p_team_id THEN
        RETURN json_build_object('success', false, 'error', 'Team is not in this match');
    END IF;

    SELECT best_of, min_team_size INTO v_best_of, v_min_team_size
        FROM tournaments WHERE id = v_match.tournament_id;

    IF p_game_number < 1 OR p_game_number > COALESCE(v_best_of, 1) THEN
        RETURN json_build_object('success', false, 'error', 'Invalid game number');
    END IF;

    v_roster_size := array_length(p_player_ids, 1);
    IF v_roster_size IS NULL OR v_roster_size < v_min_team_size THEN
        RETURN json_build_object('success', false, 'error',
            format('Roster must have at least %s players', v_min_team_size));
    END IF;

    IF EXISTS (
        SELECT unnest(p_player_ids) AS pid
        EXCEPT
        SELECT user_id FROM team_members WHERE team_id = p_team_id
    ) THEN
        RETURN json_build_object('success', false, 'error', 'All roster players must be team members');
    END IF;

    DELETE FROM tournament_match_rosters
        WHERE match_id = p_match_id AND team_id = p_team_id AND game_number = p_game_number;

    INSERT INTO tournament_match_rosters (match_id, team_id, user_id, game_number, is_active, assigned_by)
        SELECT p_match_id, p_team_id, unnest(p_player_ids), p_game_number, TRUE, auth.uid();

    INSERT INTO app_notifications (user_id, type, title, body, data)
        SELECT unnest(p_player_ids), 'TOURNAMENT_ROSTER_LOCKED',
            'Roster Locked',
            'You have been selected for the tournament match roster.',
            jsonb_build_object('match_id', p_match_id::TEXT);

    RETURN json_build_object('success', true, 'roster_size', v_roster_size);
END;
$function$;

-- 9. Fix submit_tournament_match_result: message -> body, action_id -> data, add search_path
CREATE OR REPLACE FUNCTION public.submit_tournament_match_result(p_match_id uuid, p_winner_team_id uuid, p_is_draw boolean DEFAULT false, p_game_a_score integer DEFAULT 0, p_game_b_score integer DEFAULT 0)
 RETURNS json
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public'
AS $function$
DECLARE
    v_match RECORD;
    v_tournament RECORD;
    v_team_a_name TEXT;
    v_team_b_name TEXT;
BEGIN
    SELECT * INTO v_match FROM tournament_swiss_matches WHERE id = p_match_id;
    IF NOT FOUND THEN
        RETURN json_build_object('success', false, 'error', 'Match not found');
    END IF;

    SELECT * INTO v_tournament FROM tournaments WHERE id = v_match.tournament_id;
    IF v_tournament.host_user_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only tournament host can submit results');
    END IF;

    IF v_match.status NOT IN ('scheduled', 'in_progress') THEN
        RETURN json_build_object('success', false, 'error', 'Match is not in a submittable state');
    END IF;

    IF NOT p_is_draw THEN
        IF p_winner_team_id IS NULL THEN
            RETURN json_build_object('success', false, 'error', 'Winner team ID required for non-draw result');
        END IF;
        IF p_winner_team_id != v_match.team_a_id AND p_winner_team_id != v_match.team_b_id THEN
            RETURN json_build_object('success', false, 'error', 'Winner must be one of the match teams');
        END IF;
    END IF;

    UPDATE tournament_swiss_matches SET
        status = 'completed',
        winner_team_id = p_winner_team_id,
        is_draw = p_is_draw,
        game_a_score = p_game_a_score,
        game_b_score = p_game_b_score,
        result_submitted_at = TIMEZONE('utc', NOW()),
        result_submitted_by = auth.uid(),
        updated_at = TIMEZONE('utc', NOW())
        WHERE id = p_match_id;

    IF p_is_draw THEN
        UPDATE tournament_teams SET
            swiss_draws = swiss_draws + 1,
            swiss_points = swiss_points + 1
            WHERE tournament_id = v_match.tournament_id AND team_id = v_match.team_a_id;
        UPDATE tournament_teams SET
            swiss_draws = swiss_draws + 1,
            swiss_points = swiss_points + 1
            WHERE tournament_id = v_match.tournament_id AND team_id = v_match.team_b_id;
    ELSE
        UPDATE tournament_teams SET
            swiss_wins = swiss_wins + 1,
            swiss_points = swiss_points + 3
            WHERE tournament_id = v_match.tournament_id AND team_id = p_winner_team_id;

        UPDATE tournament_teams SET
            swiss_losses = swiss_losses + 1
            WHERE tournament_id = v_match.tournament_id
              AND team_id IN (v_match.team_a_id, v_match.team_b_id)
              AND team_id != p_winner_team_id;
    END IF;

    PERFORM recalculate_tiebreakers(v_match.tournament_id);
    PERFORM award_tournament_match_points(p_match_id, p_winner_team_id, p_is_draw);

    SELECT name INTO v_team_a_name FROM teams WHERE id = v_match.team_a_id;
    SELECT name INTO v_team_b_name FROM teams WHERE id = v_match.team_b_id;

    INSERT INTO app_notifications (user_id, type, title, body, data)
        SELECT t.leader_id, 'TOURNAMENT_MATCH_RESULT',
            'Match Result',
            format('Match %s vs %s: %s',
                v_team_a_name, v_team_b_name,
                CASE WHEN p_is_draw THEN 'Draw!'
                     ELSE format('%s wins!', (SELECT name FROM teams WHERE id = p_winner_team_id))
                END),
            jsonb_build_object('tournament_id', v_match.tournament_id::TEXT)
        FROM teams t
        WHERE t.id IN (v_match.team_a_id, v_match.team_b_id);

    RETURN json_build_object('success', true);
END;
$function$;

NOTIFY pgrst, 'reload schema';
