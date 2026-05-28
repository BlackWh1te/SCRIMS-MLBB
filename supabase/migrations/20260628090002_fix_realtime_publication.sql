-- Migration: Fix Realtime publication for messages and conversations
-- Date: 2026-05-28
-- Root cause: Supabase Realtime requires the publication to exist with
--   pubinsert = true. Hosted Supabase instances sometimes lose table
--   membership or have the publication recreated without all flags.
--
-- This migration is idempotent and safe to run multiple times.

-- ═══════════════════════════════════════════════════════════════
-- 1. ENSURE PUBLICATION EXISTS WITH ALL REQUIRED FLAGS
-- ═══════════════════════════════════════════════════════════════
DO $$
DECLARE
    pub_exists BOOLEAN;
BEGIN
    SELECT EXISTS(SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime')
    INTO pub_exists;

    IF NOT pub_exists THEN
        CREATE PUBLICATION supabase_realtime
            FOR TABLE messages, conversations, conversation_participants, scrims,
                        scrim_applications, app_notifications, teams,
                        team_invitations, team_members, lfg_posts, player_stats,
                        tournaments, tournament_applications, tournament_teams,
                        tournament_host_requests, user_reports
            WITH (publish = 'insert, update, delete, truncate');
    END IF;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 2. PUBLICATION FLAGS NOTE
-- ═══════════════════════════════════════════════════════════════
-- On hosted Supabase, the supabase_realtime publication flags
-- (pubinsert, pubupdate, pubdelete, pubtruncate) are managed by
-- the platform and typically already enabled. If you need to
-- verify them, run: SELECT * FROM pg_publication WHERE pubname = 'supabase_realtime';
-- Changing these via ALTER PUBLICATION SET is not supported on
-- all Postgres versions, so we skip it here and rely on the
-- diagnostic function (section 5) for verification.

-- ═══════════════════════════════════════════════════════════════
-- 3. ENSURE MESSAGING TABLES HAVE REPLICA IDENTITY FULL
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE messages REPLICA IDENTITY FULL;
ALTER TABLE conversations REPLICA IDENTITY FULL;
ALTER TABLE conversation_participants REPLICA IDENTITY FULL;

-- ═══════════════════════════════════════════════════════════════
-- 4. IDEMPOTENT TABLE ADDITIONS TO PUBLICATION
-- ═══════════════════════════════════════════════════════════════
DO $$
DECLARE
    tables TEXT[] := ARRAY[
        'messages', 'conversations', 'conversation_participants',
        'scrims', 'scrim_applications', 'app_notifications',
        'teams', 'team_invitations', 'team_members',
        'lfg_posts', 'player_stats', 'tournaments',
        'tournament_applications', 'tournament_teams',
        'tournament_host_requests', 'user_reports'
    ];
    t TEXT;
BEGIN
    FOREACH t IN ARRAY tables
    LOOP
        BEGIN
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE %I', t);
        EXCEPTION WHEN duplicate_object THEN
            -- Table already in publication, safe to ignore
            NULL;
        END;
    END LOOP;
END $$;

-- ═══════════════════════════════════════════════════════════════
-- 5. DIAGNOSTIC FUNCTION (updated)
-- ═══════════════════════════════════════════════════════════════
CREATE OR REPLACE FUNCTION public.diagnose_realtime_publication()
RETURNS JSONB AS $$
DECLARE
    result JSONB := '{}';
    pub_info JSONB;
    pub_tables JSONB;
    replica_ids JSONB;
BEGIN
    SELECT jsonb_build_object(
        'exists', true,
        'pubinsert', pubinsert,
        'pubupdate', pubupdate,
        'pubdelete', pubdelete,
        'pubtruncate', pubtruncate
    )
    INTO pub_info
    FROM pg_publication
    WHERE pubname = 'supabase_realtime';

    result := result || jsonb_build_object('publication', COALESCE(pub_info, '{"exists":false}'::jsonb));

    SELECT jsonb_agg(jsonb_build_object('table', tablename, 'schema', schemaname))
    INTO pub_tables
    FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime';

    result := result || jsonb_build_object('publication_tables', COALESCE(pub_tables, '[]'::jsonb));

    SELECT jsonb_agg(jsonb_build_object('table', c.relname, 'identity', CASE c.relreplident
        WHEN 'd' THEN 'default'
        WHEN 'n' THEN 'nothing'
        WHEN 'f' THEN 'full'
        WHEN 'i' THEN 'index'
        ELSE c.relreplident::text
    END))
    INTO replica_ids
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relname IN ('messages', 'conversations', 'conversation_participants')
      AND n.nspname = 'public';

    result := result || jsonb_build_object('replica_identities', COALESCE(replica_ids, '[]'::jsonb));

    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.diagnose_realtime_publication() TO authenticated;
GRANT EXECUTE ON FUNCTION public.diagnose_realtime_publication() TO anon;
