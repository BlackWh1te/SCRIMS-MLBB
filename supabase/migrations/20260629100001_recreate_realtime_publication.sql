-- Migration: Recreate realtime publication to force reinitialization
-- Date: 2026-06-28
--
-- Problem: All DB-level checks pass (publication exists, flags correct,
--   tables included, REPLICA IDENTITY FULL set) but WebSocket subscribers
--   receive zero postgres_changes events. This suggests the Supabase
--   Realtime server's replication slot is stale or the publication cache
--   is out of sync.
--
-- Fix: Drop and recreate the publication. This forces Postgres to
--   generate a new replication slot and the Realtime server to resubscribe.
--   All tables are re-added idempotently.

-- ═══════════════════════════════════════════════════════════════
-- 1. DROP EXISTING PUBLICATION
-- ═══════════════════════════════════════════════════════════════
DROP PUBLICATION IF EXISTS supabase_realtime;

-- ═══════════════════════════════════════════════════════════════
-- 2. RECREATE PUBLICATION WITH ALL TABLES
-- ═══════════════════════════════════════════════════════════════
CREATE PUBLICATION supabase_realtime FOR TABLE
    messages,
    conversations,
    conversation_participants,
    scrims,
    scrim_applications,
    app_notifications,
    teams,
    team_invitations,
    team_members,
    lfg_posts,
    player_stats,
    tournaments,
    tournament_applications,
    tournament_teams,
    tournament_swiss_matches,
    tournament_match_rosters,
    tournament_host_requests,
    user_reports,
    team_ratings
WITH (publish = 'insert, update, delete, truncate');

-- ═══════════════════════════════════════════════════════════════
-- 3. ENSURE REPLICA IDENTITY FULL ON MESSAGING TABLES
-- ═══════════════════════════════════════════════════════════════
ALTER TABLE messages REPLICA IDENTITY FULL;
ALTER TABLE conversations REPLICA IDENTITY FULL;
ALTER TABLE conversation_participants REPLICA IDENTITY FULL;

-- ═══════════════════════════════════════════════════════════════
-- 4. DIAGNOSTIC FUNCTION (updated to reflect new state)
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
