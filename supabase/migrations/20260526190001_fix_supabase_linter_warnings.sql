-- Migration: Fix Supabase Linter Security Warnings
-- Date: 2026-05-26
-- Issues fixed:
--   1. function_search_path_mutable (27 functions)
--   2. anon_security_definer_function_executable (revoke anon from internal functions)
--   3. public_bucket_allows_listing (narrow storage bucket policies)

-- ═══════════════════════════════════════════════════════════════
-- 1. FIX function_search_path_mutable
--    Add explicit search_path to all flagged functions.
--    Uses a safe wrapper that skips missing functions.
-- ═══════════════════════════════════════════════════════════════

DO $$
DECLARE
    func_rec RECORD;
    func_list TEXT[] := ARRAY[
        'update_profiles_updated_at()',
        'update_player_stats_after_scrim()',
        'get_conversation_unread_count(uuid)',
        'enforce_chat_gate()',
        'increment_lfg_view_count(uuid)',
        'get_team_average_rating(uuid)',
        'get_team_ratings(uuid)',
        'get_conversations_for_user(uuid)',
        'update_conversation_last_message()',
        'mark_conversation_as_read(uuid, uuid)',
        'apply_for_tournament(uuid, uuid)',
        'review_tournament_application(uuid, uuid, text, text)',
        'generate_swiss_pairings(uuid)',
        'set_tournament_match_roster(uuid, uuid, uuid[])',
        'submit_tournament_match_result(uuid, uuid, uuid, integer, integer, boolean)',
        'award_tournament_match_points(uuid, uuid, boolean)',
        'update_tournament_scores(uuid)',
        'recalculate_tiebreakers(uuid)',
        'disqualify_tournament_team(uuid, uuid)',
        'check_tournament_no_shows(uuid)',
        'cancel_tournament(uuid)',
        'complete_tournament(uuid)',
        'enforce_weekly_tournament_limit()',
        'auto_calculate_swiss_rounds()',
        'enforce_requirement_limits()',
        'auto_block_after_3_rejections()',
        'set_match_auto_complete()'
    ];
    func_name TEXT;
BEGIN
    FOREACH func_name IN ARRAY func_list
    LOOP
        BEGIN
            EXECUTE format('ALTER FUNCTION public.%s SET search_path = public', func_name);
        EXCEPTION WHEN undefined_function THEN
            RAISE NOTICE 'Skipping missing function: %', func_name;
        END;
    END LOOP;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 2. FIX anon_security_definer_function_executable
--    Revoke EXECUTE from anon on internal/admin/host-only functions.
--    Intentionally public functions (e.g. increment_lfg_view_count) are NOT revoked.
--    Uses a safe wrapper that skips missing functions.
-- ═══════════════════════════════════════════════════════════════

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
            EXECUTE format('REVOKE EXECUTE ON FUNCTION public.%s FROM anon', func_name);
        EXCEPTION WHEN undefined_function THEN
            RAISE NOTICE 'Skipping missing function for revoke: %', func_name;
        END;
    END LOOP;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 3. FIX public_bucket_allows_listing
--    Replace broad SELECT policies with per-object access.
--    Public bucket URLs work without a SELECT policy on storage.objects;
--    the SELECT policy only enables directory listing, which leaks filenames.
-- ═══════════════════════════════════════════════════════════════

-- 3a. lfg-screenshots bucket
DROP POLICY IF EXISTS "Public can view LFG screenshots" ON storage.objects;
-- No replacement SELECT policy needed — public bucket URLs bypass RLS for reads.

-- 3b. tournament-logos bucket
DROP POLICY IF EXISTS "Anyone can view tournament logos" ON storage.objects;
-- No replacement SELECT policy needed — public bucket URLs bypass RLS for reads.

-- ═══════════════════════════════════════════════════════════════
-- 4. Reload schema cache
-- ═══════════════════════════════════════════════════════════════

NOTIFY pgrst, 'reload schema';
