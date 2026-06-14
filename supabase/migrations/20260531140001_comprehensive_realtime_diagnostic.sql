-- Comprehensive realtime diagnostic
-- Date: 2026-05-31

CREATE OR REPLACE FUNCTION public.diagnose_realtime_comprehensive()
RETURNS JSONB AS $$
DECLARE
    result JSONB := '{}';
    slots JSONB;
    wal JSONB;
    pub JSONB;
    realtime_cfg JSONB;
BEGIN
    -- Replication slots detail
    SELECT jsonb_agg(jsonb_build_object(
        'slot_name', slot_name,
        'plugin', plugin,
        'slot_type', slot_type,
        'database', database,
        'active', active,
        'restart_lsn', restart_lsn::text,
        'confirmed_flush_lsn', confirmed_flush_lsn::text,
        'pg_wal_lsn_diff', pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)::text
    ))
    INTO slots
    FROM pg_replication_slots;
    result := result || jsonb_build_object('slots', COALESCE(slots, '[]'::jsonb));

    -- WAL level and settings
    SELECT jsonb_build_object(
        'wal_level', current_setting('wal_level'),
        'max_replication_slots', current_setting('max_replication_slots'),
        'max_wal_senders', current_setting('max_wal_senders')
    )
    INTO wal;
    result := result || jsonb_build_object('wal_settings', wal);

    -- Publication detail
    SELECT jsonb_build_object(
        'pubname', pubname,
        'pubinsert', pubinsert,
        'pubupdate', pubupdate,
        'pubdelete', pubdelete,
        'pubtruncate', pubtruncate
    )
    INTO pub
    FROM pg_publication
    WHERE pubname = 'supabase_realtime';
    result := result || jsonb_build_object('publication', COALESCE(pub, '{}'::jsonb));

    -- Realtime config if table exists
    BEGIN
        SELECT jsonb_agg(jsonb_build_object('key', key, 'value', value))
        INTO realtime_cfg
        FROM realtime.channels;
        result := result || jsonb_build_object('realtime_channels', COALESCE(realtime_cfg, '[]'::jsonb));
    EXCEPTION WHEN OTHERS THEN
        result := result || jsonb_build_object('realtime_channels_error', SQLERRM);
    END;

    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Also create a test function that inserts and returns current WAL position
CREATE OR REPLACE FUNCTION public.test_insert_and_wal(p_conversation_id UUID, p_sender_id UUID, p_content TEXT)
RETURNS JSONB AS $$
DECLARE
    msg_id UUID;
    wal_before TEXT;
    wal_after TEXT;
BEGIN
    wal_before := pg_current_wal_lsn()::text;
    
    INSERT INTO messages (conversation_id, sender_id, content, delivery_status, client_message_id)
    VALUES (p_conversation_id, p_sender_id, p_content, 'pending', 'diag-' || extract(epoch from now())::text)
    RETURNING id INTO msg_id;
    
    wal_after := pg_current_wal_lsn()::text;
    
    RETURN jsonb_build_object(
        'message_id', msg_id,
        'wal_before', wal_before,
        'wal_after', wal_after,
        'wal_advanced', wal_before != wal_after
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.diagnose_realtime_comprehensive() TO authenticated;
GRANT EXECUTE ON FUNCTION public.diagnose_realtime_comprehensive() TO anon;
GRANT EXECUTE ON FUNCTION public.test_insert_and_wal(UUID, UUID, TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION public.test_insert_and_wal(UUID, UUID, TEXT) TO anon;
