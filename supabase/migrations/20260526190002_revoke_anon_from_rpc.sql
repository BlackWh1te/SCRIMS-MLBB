-- Migration: Properly revoke anon access from admin/host-only RPC functions
-- Date: 2026-05-26
-- Context: Previous REVOKE FROM anon didn't fully work because functions owned by
--          postgres may have PUBLIC execute privileges by default in Supabase.
-- Fix: Revoke from PUBLIC first, then grant back only to authenticated.

DO $$
DECLARE
    revoke_list TEXT[] := ARRAY[
        'apply_for_tournament(uuid, uuid)',
        'auto_create_tournament_host_account()',
        'award_tournament_match_points(uuid, uuid, boolean)',
        'cancel_tournament(uuid)',
        'check_tournament_no_shows(uuid)',
        'complete_tournament(uuid)',
        'disqualify_tournament_team(uuid, uuid)',
        'generate_swiss_pairings(uuid)',
        'get_conversation_unread_count(uuid)',
        'get_conversations_for_user(uuid)',
        'handle_new_user()',
        'mark_conversation_as_read(uuid, uuid)',
        'recalculate_tiebreakers(uuid)',
        'review_tournament_application(uuid, uuid, text, text)',
        'set_match_auto_complete()',
        'set_tournament_match_roster(uuid, uuid, uuid[])',
        'submit_tournament_match_result(uuid, uuid, uuid, integer, integer, boolean)',
        'update_tournament_scores(uuid)'
    ];
    func_name TEXT;
BEGIN
    FOREACH func_name IN ARRAY revoke_list
    LOOP
        BEGIN
            -- Revoke from PUBLIC first (this is key — Supabase defaults may grant PUBLIC)
            EXECUTE format('REVOKE ALL ON FUNCTION public.%s FROM PUBLIC', func_name);
            -- Also explicitly revoke from anon
            EXECUTE format('REVOKE ALL ON FUNCTION public.%s FROM anon', func_name);
            -- Grant back to authenticated only (so signed-in users can still call via RPC)
            EXECUTE format('GRANT EXECUTE ON FUNCTION public.%s TO authenticated', func_name);
        EXCEPTION WHEN undefined_function THEN
            RAISE NOTICE 'Skipping missing function: %', func_name;
        END;
    END LOOP;
END $$;

NOTIFY pgrst, 'reload schema';
