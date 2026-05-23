-- Migration: Create conversations table and fix messages schema for full messaging pipeline
-- Date: 2026-05-24
-- Context: Live DB had messages table linked to matches, but NO conversations table.
--          App code expects conversations architecture. messages had 0 rows so migration is safe.

-- ═══════════════════════════════════════════════════════════════
-- 1. CREATE conversations table FIRST (needed for FK below)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scrim_id UUID REFERENCES scrims(id) ON DELETE CASCADE,
    participant_a_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    participant_a_name TEXT,
    participant_a_team_id UUID REFERENCES teams(id),
    participant_a_team_name TEXT,
    participant_b_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    participant_b_name TEXT,
    participant_b_team_id UUID REFERENCES teams(id),
    participant_b_team_name TEXT,
    last_message TEXT DEFAULT '',
    last_message_time TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    chat_opens_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    participant_a_typing BOOLEAN DEFAULT FALSE,
    participant_b_typing BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- ═══════════════════════════════════════════════════════════════
-- 2. ALTER messages: add missing columns that the app expects
-- ═══════════════════════════════════════════════════════════════

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'messages' AND column_name = 'conversation_id'
    ) THEN
        ALTER TABLE messages ADD COLUMN conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'messages' AND column_name = 'image_url'
    ) THEN
        ALTER TABLE messages ADD COLUMN image_url TEXT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'messages' AND column_name = 'voice_url'
    ) THEN
        ALTER TABLE messages ADD COLUMN voice_url TEXT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'messages' AND column_name = 'voice_duration'
    ) THEN
        ALTER TABLE messages ADD COLUMN voice_duration INTEGER;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'messages' AND column_name = 'read_at'
    ) THEN
        ALTER TABLE messages ADD COLUMN read_at TIMESTAMP WITH TIME ZONE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_conversations_participant_a ON conversations(participant_a_id);
CREATE INDEX IF NOT EXISTS idx_conversations_participant_b ON conversations(participant_b_id);
CREATE INDEX IF NOT EXISTS idx_conversations_scrim ON conversations(scrim_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);

-- ═══════════════════════════════════════════════════════════════
-- 3. RLS: enable and drop old broken policies
-- ═══════════════════════════════════════════════════════════════

ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their conversations" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can view" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can insert" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can update" ON conversations;

DROP POLICY IF EXISTS "Users can view their messages" ON messages;
DROP POLICY IF EXISTS "Allow read messages" ON messages;
DROP POLICY IF EXISTS "Match participants can view messages" ON messages;
DROP POLICY IF EXISTS "Match participants can send messages" ON messages;
DROP POLICY IF EXISTS "Conversation members can view messages" ON messages;
DROP POLICY IF EXISTS "Conversation members can send messages" ON messages;
DROP POLICY IF EXISTS "Message sender can update" ON messages;
DROP POLICY IF EXISTS "Message sender can delete" ON messages;

-- ═══════════════════════════════════════════════════════════════
-- 4. RLS: conversations policies
-- ═══════════════════════════════════════════════════════════════

CREATE POLICY "Conversation participants can view" ON conversations
    FOR SELECT USING (
        participant_a_id = auth.uid() OR participant_b_id = auth.uid()
    );

CREATE POLICY "Conversation participants can insert" ON conversations
    FOR INSERT WITH CHECK (
        participant_a_id = auth.uid() OR participant_b_id = auth.uid()
    );

CREATE POLICY "Conversation participants can update" ON conversations
    FOR UPDATE USING (
        participant_a_id = auth.uid() OR participant_b_id = auth.uid()
    );

-- ═══════════════════════════════════════════════════════════════
-- 5. RLS: messages policies
-- ═══════════════════════════════════════════════════════════════

CREATE POLICY "Conversation members can view messages" ON messages
    FOR SELECT USING (
        sender_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
        )
    );

CREATE POLICY "Conversation members can send messages" ON messages
    FOR INSERT WITH CHECK (
        sender_id = auth.uid()
        AND EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
        )
    );

CREATE POLICY "Message sender can update" ON messages
    FOR UPDATE USING (sender_id = auth.uid());

CREATE POLICY "Message sender can delete" ON messages
    FOR DELETE USING (sender_id = auth.uid());

-- ═══════════════════════════════════════════════════════════════
-- 6. FUNCTION: count unread messages per conversation for a user
-- ═══════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION get_conversation_unread_count(
    p_conversation_id UUID,
    p_user_id UUID
)
RETURNS INTEGER AS $$
DECLARE
    unread_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO unread_count
    FROM messages
    WHERE conversation_id = p_conversation_id
      AND sender_id != p_user_id
      AND is_read = FALSE;
    RETURN COALESCE(unread_count, 0);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════
-- 7. FUNCTION: get conversations with unread counts for a user
-- ═══════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION get_conversations_for_user(p_user_id UUID)
RETURNS TABLE (
    id UUID,
    scrim_id UUID,
    participant_a_id UUID,
    participant_a_name TEXT,
    participant_a_team_id UUID,
    participant_a_team_name TEXT,
    participant_b_id UUID,
    participant_b_name TEXT,
    participant_b_team_id UUID,
    participant_b_team_name TEXT,
    last_message TEXT,
    last_message_time TIMESTAMP WITH TIME ZONE,
    chat_opens_at TIMESTAMP WITH TIME ZONE,
    participant_a_typing BOOLEAN,
    participant_b_typing BOOLEAN,
    unread_count BIGINT,
    created_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.scrim_id,
        c.participant_a_id,
        c.participant_a_name,
        c.participant_a_team_id,
        c.participant_a_team_name,
        c.participant_b_id,
        c.participant_b_name,
        c.participant_b_team_id,
        c.participant_b_team_name,
        c.last_message,
        c.last_message_time,
        c.chat_opens_at,
        c.participant_a_typing,
        c.participant_b_typing,
        COALESCE(
            (SELECT COUNT(*) FROM messages m
             WHERE m.conversation_id = c.id
               AND m.sender_id != p_user_id
               AND m.is_read = FALSE),
            0
        )::BIGINT AS unread_count,
        c.created_at
    FROM conversations c
    WHERE c.participant_a_id = p_user_id OR c.participant_b_id = p_user_id
    ORDER BY c.last_message_time DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════
-- 8. TRIGGER: enforce chat gate (server-side lock before chatOpensAt)
-- ═══════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION enforce_chat_gate()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM conversations c
        WHERE c.id = NEW.conversation_id
          AND c.chat_opens_at > TIMEZONE('utc', NOW())
    ) THEN
        RAISE EXCEPTION 'Chat is locked until %', (
            SELECT c.chat_opens_at FROM conversations c WHERE c.id = NEW.conversation_id
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_enforce_chat_gate ON messages;
CREATE TRIGGER trg_enforce_chat_gate
    BEFORE INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION enforce_chat_gate();

-- ═══════════════════════════════════════════════════════════════
-- 10. TRIGGER: auto-update conversations.last_message on new message
-- ═══════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION update_conversation_last_message()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE conversations
    SET
        last_message = NEW.content,
        last_message_time = NEW.created_at
    WHERE id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_conversation_last_message ON messages;
CREATE TRIGGER trg_update_conversation_last_message
    AFTER INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION update_conversation_last_message();

-- ═══════════════════════════════════════════════════════════════
-- 11. FIX: mark_conversation_as_read uses conversation_id (not match_id)
-- ═══════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION mark_conversation_as_read(
    p_conversation_id UUID,
    p_user_id UUID
)
RETURNS VOID AS $$
BEGIN
    UPDATE messages
    SET is_read = TRUE,
        read_at = TIMEZONE('utc', NOW())
    WHERE conversation_id = p_conversation_id
      AND sender_id != p_user_id
      AND is_read = FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ═══════════════════════════════════════════════════════════════
-- 12. REALTIME: ensure conversations and messages are published
-- ═══════════════════════════════════════════════════════════════

ALTER PUBLICATION supabase_realtime ADD TABLE conversations;
ALTER PUBLICATION supabase_realtime ADD TABLE messages;

-- ═══════════════════════════════════════════════════════════════
-- 13. ADD avatar_url to profiles (if not exists)
-- ═══════════════════════════════════════════════════════════════

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'avatar_url'
    ) THEN
        ALTER TABLE profiles ADD COLUMN avatar_url TEXT;
    END IF;
END $$;

NOTIFY pgrst, 'reload schema';
