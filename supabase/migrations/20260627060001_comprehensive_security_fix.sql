-- Migration: Comprehensive Security Fix — Correct RPC Grants, Restrict Diagnostic Functions
-- Date: 2026-06-27
-- Context:
--   1. Previous revoke migration (20260526190002) used wrong function signatures, so
--      anon access was never fully revoked from host/admin-only RPCs.
--   2. Some diagnostic/debug functions were granted to anon, which leaks info.
--   3. This migration re-applies correct revokes + grants using actual deployed signatures.

-- ═══════════════════════════════════════════════════════════════
-- 1. REVOKE ANON FROM HOST/ADMIN-ONLY FUNCTIONS (correct signatures)
-- ═══════════════════════════════════════════════════════════════

DO $$
DECLARE
    func_record RECORD;
BEGIN
    -- Host/admin-only functions that authenticated users should NOT call either
    -- (they have internal auth.uid() checks, but defense-in-depth: revoke anon)
    FOR func_record IN
        SELECT proname, pg_get_function_identity_arguments(oid) AS args
        FROM pg_proc
        WHERE pronamespace = 'public'::regnamespace
          AND prokind = 'f'
          AND proname IN (
              'cancel_tournament',
              'complete_tournament',
              'disqualify_tournament_team',
              'generate_swiss_pairings',
              'review_tournament_application',
              'submit_tournament_match_result'
          )
    LOOP
        BEGIN
            EXECUTE format('REVOKE ALL ON FUNCTION public.%I(%s) FROM PUBLIC',
                func_record.proname, func_record.args);
            EXECUTE format('REVOKE ALL ON FUNCTION public.%I(%s) FROM anon',
                func_record.proname, func_record.args);
            RAISE NOTICE 'Revoked PUBLIC/anon from: %', func_record.proname;
        EXCEPTION WHEN undefined_function THEN
            RAISE NOTICE 'Skipping missing function: %', func_record.proname;
        END;
    END LOOP;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 2. REVOKE ANON FROM TEAM-LEADER FUNCTIONS (correct signatures)
-- ═══════════════════════════════════════════════════════════════

DO $$
DECLARE
    func_record RECORD;
BEGIN
    FOR func_record IN
        SELECT proname, pg_get_function_identity_arguments(oid) AS args
        FROM pg_proc
        WHERE pronamespace = 'public'::regnamespace
          AND prokind = 'f'
          AND proname IN (
              'apply_for_tournament',
              'set_tournament_match_roster'
          )
    LOOP
        BEGIN
            EXECUTE format('REVOKE ALL ON FUNCTION public.%I(%s) FROM PUBLIC',
                func_record.proname, func_record.args);
            EXECUTE format('REVOKE ALL ON FUNCTION public.%I(%s) FROM anon',
                func_record.proname, func_record.args);
            -- These are safe for authenticated (they check leader_id = auth.uid() internally)
            EXECUTE format('GRANT EXECUTE ON FUNCTION public.%I(%s) TO authenticated',
                func_record.proname, func_record.args);
            RAISE NOTICE 'Fixed grants for: %', func_record.proname;
        EXCEPTION WHEN undefined_function THEN
            RAISE NOTICE 'Skipping missing function: %', func_record.proname;
        END;
    END LOOP;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 3. RESTRICT DIAGNOSTIC / DEBUG FUNCTIONS
--    These were previously granted to anon — they leak table structure and
--    internal state. Remove anon access, keep authenticated only.
-- ═══════════════════════════════════════════════════════════════

DO $$
DECLARE
    func_record RECORD;
BEGIN
    FOR func_record IN
        SELECT proname, pg_get_function_identity_arguments(oid) AS args
        FROM pg_proc
        WHERE pronamespace = 'public'::regnamespace
          AND prokind = 'f'
          AND proname IN (
              'diagnose_realtime',
              'diagnose_realtime_comprehensive',
              'diagnose_publication',
              'debug_list_policies',
              'test_insert_and_wal',
              'test_realtime_insert'
          )
    LOOP
        BEGIN
            EXECUTE format('REVOKE ALL ON FUNCTION public.%I(%s) FROM PUBLIC',
                func_record.proname, func_record.args);
            EXECUTE format('REVOKE ALL ON FUNCTION public.%I(%s) FROM anon',
                func_record.proname, func_record.args);
            EXECUTE format('REVOKE ALL ON FUNCTION public.%I(%s) FROM authenticated',
                func_record.proname, func_record.args);
            RAISE NOTICE 'Restricted diagnostic function: %', func_record.proname;
        EXCEPTION WHEN undefined_function THEN
            RAISE NOTICE 'Skipping missing function: %', func_record.proname;
        END;
    END LOOP;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 4. GRANT SAFE UTILITY FUNCTIONS TO AUTHENTICATED
--    Functions that all logged-in users legitimately need.
-- ═══════════════════════════════════════════════════════════════

DO $$
DECLARE
    func_record RECORD;
BEGIN
    FOR func_record IN
        SELECT proname, pg_get_function_identity_arguments(oid) AS args
        FROM pg_proc
        WHERE pronamespace = 'public'::regnamespace
          AND prokind = 'f'
          AND proname IN (
              'get_conversation_unread_count',
              'mark_conversation_as_read',
              'get_conversations_for_user',
              'is_conversation_member',
              'is_user_in_conversation',
              'enforce_message_rate_limit',
              'upsert_message_with_idempotency',
              'handle_new_user',
              'calculate_tier',
              'calculate_division',
              'update_team_tier',
              'update_conversation_last_message',
              'enforce_chat_gate',
              'delete_user',
              'can_post_lfg',
              'increment_lfg_view_count',
              'is_team_leader',
              'auto_update_delivery_status'
          )
    LOOP
        BEGIN
            EXECUTE format('REVOKE ALL ON FUNCTION public.%I(%s) FROM PUBLIC',
                func_record.proname, func_record.args);
            EXECUTE format('REVOKE ALL ON FUNCTION public.%I(%s) FROM anon',
                func_record.proname, func_record.args);
            EXECUTE format('GRANT EXECUTE ON FUNCTION public.%I(%s) TO authenticated',
                func_record.proname, func_record.args);
            RAISE NOTICE 'Granted to authenticated: %', func_record.proname;
        EXCEPTION WHEN undefined_function THEN
            RAISE NOTICE 'Skipping missing function: %', func_record.proname;
        END;
    END LOOP;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 5. ADD NULL-AUTH CHECK TO SECURITY DEFINER FUNCTIONS MISSING IT
--    Some helper functions run as SECURITY DEFINER but don't verify
--    that the caller is actually authenticated.
-- ═══════════════════════════════════════════════════════════════

-- is_team_leader: add explicit auth check at entry
CREATE OR REPLACE FUNCTION public.is_team_leader(user_id UUID, team_id UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RETURN FALSE;
    END IF;
    RETURN EXISTS (
        SELECT 1 FROM teams WHERE id = team_id AND leader_id = user_id
    );
END;
$$;

-- can_post_lfg: add explicit auth check
CREATE OR REPLACE FUNCTION public.can_post_lfg(p_user_id UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RETURN FALSE;
    END IF;
    RETURN NOT EXISTS (
        SELECT 1 FROM profiles
        WHERE id = p_user_id
          AND lfg_posting_banned_until IS NOT NULL
          AND lfg_posting_banned_until > NOW()
    );
END;
$$;

-- increment_lfg_view_count: add explicit auth check
CREATE OR REPLACE FUNCTION public.increment_lfg_view_count(p_post_id UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RETURN;
    END IF;
    UPDATE lfg_posts SET view_count = view_count + 1 WHERE id = p_post_id;
END;
$$;

-- ═══════════════════════════════════════════════════════════════
-- 6. RESTRICT admin_activity TABLE
-- ═══════════════════════════════════════════════════════════════

-- Ensure RLS is enabled
ALTER TABLE IF EXISTS admin_activity ENABLE ROW LEVEL SECURITY;

-- Drop any overly permissive policies
DROP POLICY IF EXISTS "Allow read admin_activity" ON admin_activity;
DROP POLICY IF EXISTS "Allow all admin_activity" ON admin_activity;
DROP POLICY IF EXISTS "Admin can read admin_activity" ON admin_activity;

-- Only admins can read
CREATE POLICY "Admin can read admin_activity" ON admin_activity
    FOR SELECT USING (EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = TRUE));

-- Only service role / admin can insert (no INSERT policy for authenticated)
-- AdminPanel uses service-role key for this, which bypasses RLS

-- ═══════════════════════════════════════════════════════════════
-- 7. RELOAD SCHEMA CACHE
-- ═══════════════════════════════════════════════════════════════

NOTIFY pgrst, 'reload schema';
