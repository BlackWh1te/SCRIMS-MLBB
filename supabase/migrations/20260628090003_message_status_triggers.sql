-- Migration: Message delivery and read-status triggers
-- Date: 2026-05-28
-- Fixes: delivery_status stuck at 'pending'; is_read never updated

-- ═══════════════════════════════════════════════════════════════
-- 1. AUTO-MARK DELIVERED ON INSERT
-- ═══════════════════════════════════════════════════════════════
-- When a message is successfully inserted, it is by definition delivered.
-- This trigger flips delivery_status from 'pending' to 'delivered'.
CREATE OR REPLACE FUNCTION public.handle_message_inserted()
RETURNS TRIGGER AS $$
BEGIN
    NEW.delivery_status := 'delivered';
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_message_insert_set_delivered ON messages;
CREATE TRIGGER on_message_insert_set_delivered
    BEFORE INSERT ON messages
    FOR EACH ROW
    WHEN (NEW.delivery_status = 'pending')
    EXECUTE FUNCTION public.handle_message_inserted();

-- ═══════════════════════════════════════════════════════════════
-- 2. RPC TO MARK MESSAGES AS READ
-- ═══════════════════════════════════════════════════════════════
-- Call this from the Android client when the recipient opens the conversation.
-- Returns the number of messages marked as read.
CREATE OR REPLACE FUNCTION public.mark_messages_as_read(
    p_conversation_id UUID,
    p_reader_id UUID
)
RETURNS INTEGER AS $$
DECLARE
    updated_count INTEGER;
BEGIN
    UPDATE messages
    SET is_read = true,
        delivery_status = COALESCE(delivery_status, 'delivered')
    WHERE conversation_id = p_conversation_id
      AND sender_id != p_reader_id
      AND is_read = false;

    GET DIAGNOSTICS updated_count = ROW_COUNT;
    RETURN updated_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.mark_messages_as_read(UUID, UUID) TO authenticated;

-- ═══════════════════════════════════════════════════════════════
-- 3. BACKFILL EXISTING MESSAGES (optional, safe to run on large tables)
-- ═══════════════════════════════════════════════════════════════
-- Only updates rows that are still 'pending', avoiding a full table rewrite.
UPDATE messages
SET delivery_status = 'delivered'
WHERE delivery_status = 'pending';
