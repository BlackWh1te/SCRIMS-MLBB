-- Migration: Fix Realtime broadcast by setting REPLICA IDENTITY FULL
-- Date: 2026-05-31
-- Root cause: Supabase Realtime uses PostgreSQL logical replication to capture
--   row changes. Without REPLICA IDENTITY FULL, the WAL (Write-Ahead Log)
--   may not contain enough information to broadcast INSERT/UPDATE/DELETE
--   events to WebSocket subscribers.
-- Tables affected: messages, conversations, conversation_participants

ALTER TABLE messages REPLICA IDENTITY FULL;
ALTER TABLE conversations REPLICA IDENTITY FULL;
ALTER TABLE conversation_participants REPLICA IDENTITY FULL;

-- Also ensure these tables are in the realtime publication (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'messages'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE messages;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'conversations'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE conversations;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime' AND tablename = 'conversation_participants'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE conversation_participants;
    END IF;
END $$;

NOTIFY pgrst, 'reload schema';
