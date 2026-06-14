-- ═══════════════════════════════════════════════════════════════
-- Migration: Production Messaging Hardening
-- Date: 2026-05-27
-- Author: Devin
-- ═══════════════════════════════════════════════════════════════

-- 1. Add client_message_id to messages for idempotent delivery
ALTER TABLE messages ADD COLUMN IF NOT EXISTS client_message_id TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS delivery_status TEXT NOT NULL DEFAULT 'SENT';

-- 2. Index for idempotency lookups + pagination performance
CREATE INDEX IF NOT EXISTS idx_messages_client_id ON messages(client_message_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_created ON messages(conversation_id, created_at DESC);

-- 3. Index for unread count optimization (avoids seq scan)
CREATE INDEX IF NOT EXISTS idx_messages_unread ON messages(conversation_id, sender_id, is_read) WHERE is_read = FALSE;

-- 4. Index for conversation list ordering (already exists, verify)
CREATE INDEX IF NOT EXISTS idx_conversations_last_message_time ON conversations(last_message_time DESC);

-- 5. Rate limit table (lightweight, no external dependencies)
CREATE TABLE IF NOT EXISTS message_rate_limits (
    user_id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
    window_start TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    message_count INTEGER NOT NULL DEFAULT 0
);

-- 6. Rate limit enforcement function
CREATE OR REPLACE FUNCTION enforce_message_rate_limit()
RETURNS TRIGGER AS $$
DECLARE
    v_user_id UUID;
    v_count INTEGER;
    v_window TIMESTAMP WITH TIME ZONE;
    v_max_per_minute INTEGER := 30;  -- Configurable
BEGIN
    v_user_id := NEW.sender_id;

    SELECT message_count, window_start INTO v_count, v_window
    FROM message_rate_limits
    WHERE user_id = v_user_id
    FOR UPDATE;

    IF v_window IS NULL THEN
        INSERT INTO message_rate_limits(user_id, window_start, message_count)
        VALUES (v_user_id, TIMEZONE('utc', NOW()), 1);
    ELSIF v_window < TIMEZONE('utc', NOW()) - INTERVAL '1 minute' THEN
        -- Window expired, reset
        UPDATE message_rate_limits
        SET window_start = TIMEZONE('utc', NOW()), message_count = 1
        WHERE user_id = v_user_id;
    ELSE
        IF v_count >= v_max_per_minute THEN
            RAISE EXCEPTION 'Rate limit exceeded: % messages per minute', v_max_per_minute;
        END IF;
        UPDATE message_rate_limits
        SET message_count = message_count + 1
        WHERE user_id = v_user_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 7. Attach rate limit trigger
DROP TRIGGER IF EXISTS trg_message_rate_limit ON messages;
CREATE TRIGGER trg_message_rate_limit
    BEFORE INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION enforce_message_rate_limit();

-- 8. Unique constraint on client_message_id per conversation (idempotency)
-- NULL values are exempt from unique constraints, so we use partial index
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_client_message
ON messages(conversation_id, client_message_id)
WHERE client_message_id IS NOT NULL;

-- 9. Function to upsert message with idempotency (for RPC usage if needed)
CREATE OR REPLACE FUNCTION upsert_message_with_idempotency(
    p_conversation_id UUID,
    p_sender_id UUID,
    p_sender_name TEXT,
    p_content TEXT,
    p_type TEXT DEFAULT 'TEXT',
    p_client_message_id TEXT DEFAULT NULL,
    p_image_url TEXT DEFAULT NULL,
    p_voice_url TEXT DEFAULT NULL,
    p_voice_duration INTEGER DEFAULT NULL
)
RETURNS TABLE(id UUID, created_at TIMESTAMP WITH TIME ZONE) AS $$
DECLARE
    v_existing_id UUID;
BEGIN
    -- Idempotency check
    IF p_client_message_id IS NOT NULL THEN
        SELECT m.id INTO v_existing_id
        FROM messages m
        WHERE m.conversation_id = p_conversation_id
          AND m.client_message_id = p_client_message_id
        LIMIT 1;

        IF v_existing_id IS NOT NULL THEN
            RETURN QUERY SELECT m.id, m.created_at FROM messages m WHERE m.id = v_existing_id;
            RETURN;
        END IF;
    END IF;

    INSERT INTO messages (
        conversation_id, sender_id, sender_name, content, type,
        client_message_id, image_url, voice_url, voice_duration
    ) VALUES (
        p_conversation_id, p_sender_id, p_sender_name, p_content, p_type,
        p_client_message_id, p_image_url, p_voice_url, p_voice_duration
    )
    RETURNING messages.id, messages.created_at INTO id, created_at;

    RETURN NEXT;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 10. Harder RLS on messages: enforce sender_id = auth.uid()
DROP POLICY IF EXISTS messages_insert_policy ON messages;
CREATE POLICY messages_insert_policy ON messages FOR INSERT
WITH CHECK (
    auth.uid() = sender_id AND
    EXISTS (
        SELECT 1 FROM conversations c
        WHERE c.id = messages.conversation_id
        AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
        AND c.chat_opens_at <= TIMEZONE('utc', NOW())
    )
);

-- 11. View for conversation unread counts (materialized via trigger or computed)
-- This RPC-compatible function replaces heavy client-side counting
DROP FUNCTION IF EXISTS get_conversation_unread_count(UUID, UUID);
CREATE OR REPLACE FUNCTION get_conversation_unread_count(
    p_conversation_id UUID,
    p_user_id UUID
)
RETURNS BIGINT AS $$
    SELECT COUNT(*)::BIGINT
    FROM messages m
    WHERE m.conversation_id = p_conversation_id
      AND m.sender_id != p_user_id
      AND m.is_read = FALSE;
$$ LANGUAGE sql SECURITY DEFINER STABLE;
