-- Diagnostic migration for realtime troubleshooting
-- This migration creates a function to inspect publication/replication state.

CREATE OR REPLACE FUNCTION public.diagnose_realtime()
RETURNS JSONB AS $$
DECLARE
    result JSONB := '{}';
    pub_tables JSONB;
    replica_idents JSONB;
    slots JSONB;
    extensions JSONB;
BEGIN
    -- Publication tables
    SELECT jsonb_agg(jsonb_build_object('table', tablename, 'schema', schemaname))
    INTO pub_tables
    FROM pg_publication_tables
    WHERE pubname = 'supabase_realtime';
    result := result || jsonb_build_object('publication_tables', COALESCE(pub_tables, '[]'::jsonb));

    -- Replica identities
    SELECT jsonb_agg(jsonb_build_object('table', c.relname, 'identity', CASE c.relreplident
        WHEN 'd' THEN 'default' WHEN 'n' THEN 'nothing'
        WHEN 'f' THEN 'full' WHEN 'i' THEN 'index' END))
    INTO replica_idents
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relname IN ('messages', 'conversations', 'conversation_participants')
    AND n.nspname = 'public';
    result := result || jsonb_build_object('replica_identities', COALESCE(replica_idents, '[]'::jsonb));

    -- Replication slots
    SELECT jsonb_agg(jsonb_build_object(
        'slot_name', slot_name, 'plugin', plugin,
        'slot_type', slot_type, 'active', active,
        'restart_lsn', restart_lsn::text
    ))
    INTO slots
    FROM pg_replication_slots;
    result := result || jsonb_build_object('replication_slots', COALESCE(slots, '[]'::jsonb));

    -- Extensions
    SELECT jsonb_agg(jsonb_build_object('name', extname, 'version', extversion))
    INTO extensions
    FROM pg_extension;
    result := result || jsonb_build_object('extensions', COALESCE(extensions, '[]'::jsonb));

    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Also create a function to test realtime by inserting and returning WAL info
CREATE OR REPLACE FUNCTION public.test_realtime_insert(p_conversation_id UUID, p_sender_id UUID, p_content TEXT)
RETURNS JSONB AS $$
DECLARE
    msg_id UUID;
    result JSONB;
BEGIN
    INSERT INTO messages (conversation_id, sender_id, content, delivery_status, client_message_id)
    VALUES (p_conversation_id, p_sender_id, p_content, 'pending', 'test-' || extract(epoch from now())::text)
    RETURNING id INTO msg_id;

    result := jsonb_build_object(
        'inserted', true,
        'message_id', msg_id,
        'timestamp', now()
    );
    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
