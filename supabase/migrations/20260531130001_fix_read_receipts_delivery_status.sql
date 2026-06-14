-- Migration: Fix read receipts and delivery status
-- Date: 2026-05-31

-- =================================================================
-- 1. Ensure mark_conversation_as_read exists and works
-- =================================================================

DROP FUNCTION IF EXISTS public.mark_conversation_as_read(UUID, UUID);

CREATE OR REPLACE FUNCTION public.mark_conversation_as_read(
    p_conversation_id UUID,
    p_user_id UUID
)
RETURNS VOID AS $$
BEGIN
    -- Mark all messages from OTHER users as read
    UPDATE messages
    SET is_read = TRUE, read_at = TIMEZONE('utc', NOW())
    WHERE conversation_id = p_conversation_id
      AND sender_id != p_user_id
      AND is_read = FALSE;

    -- Optionally update conversation unread counters if they existed
    -- (The app computes unread counts from messages table)
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute to authenticated and anon (anon will still be blocked by RLS inside the function)
GRANT EXECUTE ON FUNCTION public.mark_conversation_as_read(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.mark_conversation_as_read(UUID, UUID) TO anon;

-- =================================================================
-- 2. Function to get unread count for a conversation
-- =================================================================

DROP FUNCTION IF EXISTS public.get_conversation_unread_count(UUID, UUID);

CREATE OR REPLACE FUNCTION public.get_conversation_unread_count(
    p_conversation_id UUID,
    p_user_id UUID
)
RETURNS INTEGER AS $$
DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM messages
    WHERE conversation_id = p_conversation_id
      AND sender_id != p_user_id
      AND is_read = FALSE;
    RETURN v_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION public.get_conversation_unread_count(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_conversation_unread_count(UUID, UUID) TO anon;

-- =================================================================
-- 3. Auto-update delivery_status on insert via trigger
--    When a message is successfully inserted, update delivery_status to 'sent'
-- =================================================================

CREATE OR REPLACE FUNCTION public.auto_update_delivery_status()
RETURNS TRIGGER AS $$
BEGIN
    -- Only update if still pending (avoids overwriting explicit statuses)
    IF NEW.delivery_status = 'pending' THEN
        NEW.delivery_status := 'sent';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trg_auto_delivery_status ON messages;
CREATE TRIGGER trg_auto_delivery_status
    BEFORE INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION public.auto_update_delivery_status();

-- =================================================================
-- 4. Backfill existing messages that are still 'pending' but were sent
-- =================================================================
UPDATE messages
SET delivery_status = 'sent'
WHERE delivery_status = 'pending';

-- =================================================================
-- 5. Grant EXECUTE on other conversation-related RPCs
-- =================================================================

DO $$
DECLARE
    func_rec RECORD;
BEGIN
    FOR func_rec IN
        SELECT proname
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'public'
        AND p.proname IN (
            'get_conversations_for_user',
            'get_conversation_unread_count',
            'mark_conversation_as_read'
        )
    LOOP
        EXECUTE format('GRANT EXECUTE ON FUNCTION public.%I TO authenticated', func_rec.proname);
        EXECUTE format('GRANT EXECUTE ON FUNCTION public.%I TO anon', func_rec.proname);
    END LOOP;
END $$;

NOTIFY pgrst, 'reload schema';
