-- Check publication flags and try to create a test replication slot
CREATE OR REPLACE FUNCTION public.diagnose_publication()
RETURNS JSONB AS $$
DECLARE
    result JSONB := '{}';
    pub_flags JSONB;
    has_slot BOOLEAN;
    can_create_slot BOOLEAN;
BEGIN
    -- Publication flags
    SELECT jsonb_build_object(
        'pubinsert', pubinsert,
        'pubupdate', pubupdate,
        'pubdelete', pubdelete,
        'pubtruncate', pubtruncate
    )
    INTO pub_flags
    FROM pg_publication
    WHERE pubname = 'supabase_realtime';
    result := result || jsonb_build_object('publication_flags', COALESCE(pub_flags, '{}'::jsonb));

    -- Check if any logical replication slots exist
    SELECT EXISTS(SELECT 1 FROM pg_replication_slots WHERE slot_type = 'logical')
    INTO has_slot;
    result := result || jsonb_build_object('has_logical_slot', has_slot);

    -- Check if we have replication privilege
    SELECT pg_has_role(current_user, 'pg_read_all_data', 'MEMBER')
    INTO can_create_slot;
    result := result || jsonb_build_object('can_read_all_data', can_create_slot);

    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
