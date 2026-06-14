-- Migration: Fix Messaging 500 Errors and RLS Policies
-- Date: 2026-05-31

-- 1. Ensure conversation triggers are SECURITY DEFINER
-- This prevents 500 errors if the sender doesn't have UPDATE permissions on conversations table
CREATE OR REPLACE FUNCTION public.update_conversation_last_message()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE conversations
    SET
        last_message = NEW.content,
        last_message_time = COALESCE(NEW.created_at, TIMEZONE('utc', NOW()))
    WHERE id = NEW.conversation_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Fix Broken RLS Policies for Messages (Reverting bad "Security Fixes All" changes)
-- The "Security Fixes All" migration broke direct chats by requiring a match_id.
DROP POLICY IF EXISTS "Match participants can view messages" ON messages;
DROP POLICY IF EXISTS "Match participants can send messages" ON messages;
DROP POLICY IF EXISTS "Allow read messages" ON messages;
DROP POLICY IF EXISTS "Conversation members can view messages" ON messages;
DROP POLICY IF EXISTS "Conversation members can send messages" ON messages;

-- SELECT: Anyone in the conversation can view
CREATE POLICY "Conversation members can view messages" ON messages
    FOR SELECT USING (
        sender_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (
                c.participant_a_id = auth.uid() 
                OR c.participant_b_id = auth.uid()
                OR EXISTS (
                    SELECT 1 FROM conversation_participants cp 
                    WHERE cp.conversation_id = c.id AND cp.user_id = auth.uid()
                )
            )
        )
    );

-- INSERT: Sender must be in the conversation
CREATE POLICY "Conversation members can send messages" ON messages
    FOR INSERT WITH CHECK (
        sender_id = auth.uid()
        AND EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = conversation_id
            AND (
                c.participant_a_id = auth.uid() 
                OR c.participant_b_id = auth.uid()
                OR EXISTS (
                    SELECT 1 FROM conversation_participants cp 
                    WHERE cp.conversation_id = c.id AND cp.user_id = auth.uid()
                )
            )
        )
    );

-- 3. Fix Conversation Policies (Ensure they are present and correct)
DROP POLICY IF EXISTS "Conversation participants can view" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can insert" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can update" ON conversations;

CREATE POLICY "Conversation participants can view" ON conversations
    FOR SELECT USING (
        participant_a_id = auth.uid() 
        OR participant_b_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversation_participants cp 
            WHERE cp.conversation_id = id AND cp.user_id = auth.uid()
        )
    );

CREATE POLICY "Conversation participants can insert" ON conversations
    FOR INSERT WITH CHECK (
        participant_a_id = auth.uid() 
        OR participant_b_id = auth.uid()
    );

-- Crucial: Users MUST be able to update conversation metadata (like typing status)
-- or the update_conversation_last_message trigger might still cause friction 
-- if not defined as SECURITY DEFINER (which we already fixed in step 1).
CREATE POLICY "Conversation participants can update" ON conversations
    FOR UPDATE USING (
        participant_a_id = auth.uid() 
        OR participant_b_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversation_participants cp 
            WHERE cp.conversation_id = id AND cp.user_id = auth.uid()
        )
    );

-- 4. Fix the chat gate trigger to be more robust
CREATE OR REPLACE FUNCTION public.enforce_chat_gate()
RETURNS TRIGGER AS $$
BEGIN
    -- Only enforce gate for SCRIM chats (not direct user-to-user LFG chats)
    -- We detect scrim chats by checking if scrim_id is present
    IF EXISTS (
        SELECT 1 FROM conversations c
        WHERE c.id = NEW.conversation_id
          AND c.scrim_id IS NOT NULL
          AND c.chat_opens_at > TIMEZONE('utc', NOW())
    ) THEN
        RAISE EXCEPTION 'Chat is locked until %', (
            SELECT c.chat_opens_at FROM conversations c WHERE c.id = NEW.conversation_id
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. Ensure sender_team_id and match_id are truly optional in the DB
ALTER TABLE messages ALTER COLUMN sender_team_id DROP NOT NULL;
ALTER TABLE messages ALTER COLUMN match_id DROP NOT NULL;

NOTIFY pgrst, 'reload schema';
