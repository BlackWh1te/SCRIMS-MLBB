-- ──────────────────────────────────────────────────────────────────────────────
-- Migration: add_team_chat_to_conversations
-- Date: 2026-05-27
--
-- Problem
-- -------
-- Conversations table only supports 1-on-1 scrim chats. Teams need a
-- dedicated group chat that is pinned to the top of the message list.
--
-- Fix
-- ---
-- 1. Add team_id, is_team_chat, is_pinned, group_name to conversations.
-- 2. Create get_or_create_team_conversation RPC.
-- 3. Update get_conversations_for_user to include team chats.
-- 4. Update RLS policies so team members can read/write messages and
--    view/update conversations for their team chats.
-- ──────────────────────────────────────────────────────────────────────────────

-- ── 1. Add columns ────────────────────────────────────────────────────────────

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS team_id UUID REFERENCES teams(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS is_team_chat BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS group_name TEXT;

-- Index for fast team-chat lookups
CREATE INDEX IF NOT EXISTS idx_conversations_team_id ON conversations(team_id);
CREATE INDEX IF NOT EXISTS idx_conversations_is_team_chat ON conversations(is_team_chat);

-- ── 2. Unique constraint: one team chat per team ──────────────────────────────
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_team_chat ON conversations(team_id)
    WHERE is_team_chat = TRUE;

-- ── 3. RPC: get or create team conversation ───────────────────────────────────
CREATE OR REPLACE FUNCTION get_or_create_team_conversation(
    p_team_id UUID,
    p_team_name TEXT,
    p_leader_id UUID,
    p_leader_name TEXT
)
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
    created_at TIMESTAMP WITH TIME ZONE,
    team_id UUID,
    is_team_chat BOOLEAN,
    is_pinned BOOLEAN,
    group_name TEXT
) AS $$
DECLARE
    v_conversation_id UUID;
BEGIN
    -- Try to find existing team conversation
    SELECT c.id INTO v_conversation_id
    FROM conversations c
    WHERE c.team_id = p_team_id AND c.is_team_chat = TRUE
    LIMIT 1;

    IF v_conversation_id IS NULL THEN
        -- Create new team conversation
        INSERT INTO conversations (
            team_id,
            is_team_chat,
            is_pinned,
            group_name,
            participant_a_id,
            participant_a_name,
            participant_a_team_id,
            participant_a_team_name,
            last_message,
            last_message_time,
            chat_opens_at,
            participant_a_typing,
            participant_b_typing,
            created_at
        ) VALUES (
            p_team_id,
            TRUE,
            TRUE,
            COALESCE(p_team_name, 'Team Chat'),
            p_leader_id,
            p_leader_name,
            p_team_id,
            p_team_name,
            '',
            TIMEZONE('utc', NOW()),
            TIMEZONE('utc', NOW()),
            FALSE,
            FALSE,
            TIMEZONE('utc', NOW())
        )
        RETURNING conversations.id INTO v_conversation_id;
    END IF;

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
        c.created_at,
        c.team_id,
        c.is_team_chat,
        c.is_pinned,
        c.group_name
    FROM conversations c
    WHERE c.id = v_conversation_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 4. Update get_conversations_for_user to include team chats ───────────────
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
    created_at TIMESTAMP WITH TIME ZONE,
    team_id UUID,
    is_team_chat BOOLEAN,
    is_pinned BOOLEAN,
    group_name TEXT
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
        c.created_at,
        c.team_id,
        c.is_team_chat,
        c.is_pinned,
        c.group_name
    FROM conversations c
    WHERE
        (c.participant_a_id = p_user_id OR c.participant_b_id = p_user_id)
        OR (
            c.is_team_chat = TRUE
            AND c.team_id IN (
                SELECT tm.team_id FROM team_members tm
                WHERE tm.user_id = p_user_id AND tm.status = 'ACTIVE'
            )
        )
    ORDER BY c.is_pinned DESC, c.last_message_time DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ── 5. Update conversations RLS policies for team chats ──────────────────────
DROP POLICY IF EXISTS "Conversation participants can view" ON conversations;
CREATE POLICY "Conversation participants can view" ON conversations
    FOR SELECT USING (
        participant_a_id = auth.uid()
        OR participant_b_id = auth.uid()
        OR (
            is_team_chat = TRUE
            AND team_id IN (
                SELECT tm.team_id FROM team_members tm
                WHERE tm.user_id = auth.uid() AND tm.status = 'ACTIVE'
            )
        )
    );

DROP POLICY IF EXISTS "Conversation participants can update" ON conversations;
CREATE POLICY "Conversation participants can update" ON conversations
    FOR UPDATE USING (
        participant_a_id = auth.uid()
        OR participant_b_id = auth.uid()
        OR (
            is_team_chat = TRUE
            AND team_id IN (
                SELECT tm.team_id FROM team_members tm
                WHERE tm.user_id = auth.uid() AND tm.status = 'ACTIVE'
            )
        )
    );

-- ── 6. Update messages RLS policies for team chats ─────────────────────────
DROP POLICY IF EXISTS "Conversation members can view messages" ON messages;
CREATE POLICY "Conversation members can view messages" ON messages
    FOR SELECT USING (
        sender_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (
                c.participant_a_id = auth.uid()
                OR c.participant_b_id = auth.uid()
                OR (
                    c.is_team_chat = TRUE
                    AND c.team_id IN (
                        SELECT tm.team_id FROM team_members tm
                        WHERE tm.user_id = auth.uid() AND tm.status = 'ACTIVE'
                    )
                )
            )
        )
    );

DROP POLICY IF EXISTS "Conversation members can send messages" ON messages;
CREATE POLICY "Conversation members can send messages" ON messages
    FOR INSERT WITH CHECK (
        sender_id = auth.uid()
        AND EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (
                c.participant_a_id = auth.uid()
                OR c.participant_b_id = auth.uid()
                OR (
                    c.is_team_chat = TRUE
                    AND c.team_id IN (
                        SELECT tm.team_id FROM team_members tm
                        WHERE tm.user_id = auth.uid() AND tm.status = 'ACTIVE'
                    )
                )
            )
            AND c.chat_opens_at <= TIMEZONE('utc', NOW())
        )
    );

-- Notify PostgREST to reload schema cache
NOTIFY pgrst, 'reload schema';
