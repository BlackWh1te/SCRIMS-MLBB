-- Migration: Fix RLS infinite recursion in messaging tables
-- Date: 2026-05-31

CREATE OR REPLACE FUNCTION public.is_conversation_member(conv_id UUID, check_user_id UUID)
RETURNS BOOLEAN AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM conversations c WHERE c.id = conv_id
               AND (c.participant_a_id = check_user_id OR c.participant_b_id = check_user_id))
    THEN RETURN TRUE; END IF;
    IF EXISTS (SELECT 1 FROM conversation_participants cp
               WHERE cp.conversation_id = conv_id AND cp.user_id = check_user_id)
    THEN RETURN TRUE; END IF;
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP POLICY IF EXISTS "Conversation participants can view members" ON conversation_participants;
DROP POLICY IF EXISTS "Conversation participants can view" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can insert" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can update" ON conversations;
DROP POLICY IF EXISTS "Conversation members can view messages" ON messages;
DROP POLICY IF EXISTS "Conversation members can send messages" ON messages;
DROP POLICY IF EXISTS "Message sender can update" ON messages;
DROP POLICY IF EXISTS "Message sender can delete" ON messages;

CREATE POLICY "Conversation participants can view" ON conversations
    FOR SELECT USING (
        participant_a_id = auth.uid() 
        OR participant_b_id = auth.uid()
        OR is_conversation_member(id, auth.uid())
    );

CREATE POLICY "Conversation participants can insert" ON conversations
    FOR INSERT WITH CHECK (
        participant_a_id = auth.uid() 
        OR participant_b_id = auth.uid()
    );

CREATE POLICY "Conversation participants can update" ON conversations
    FOR UPDATE USING (
        participant_a_id = auth.uid() 
        OR participant_b_id = auth.uid()
        OR is_conversation_member(id, auth.uid())
    );

CREATE POLICY "Conversation participants can view members" ON conversation_participants
    FOR SELECT USING (
        user_id = auth.uid()
        OR is_conversation_member(conversation_id, auth.uid())
    );

CREATE POLICY "Conversation participants can insert self" ON conversation_participants
    FOR INSERT WITH CHECK (
        user_id = auth.uid()
        AND is_conversation_member(conversation_id, auth.uid())
    );

CREATE POLICY "Conversation participants can delete self" ON conversation_participants
    FOR DELETE USING (
        user_id = auth.uid()
    );

CREATE POLICY "Conversation members can view messages" ON messages
    FOR SELECT USING (
        sender_id = auth.uid()
        OR is_conversation_member(conversation_id, auth.uid())
    );

CREATE POLICY "Conversation members can send messages" ON messages
    FOR INSERT WITH CHECK (
        sender_id = auth.uid()
        AND is_conversation_member(conversation_id, auth.uid())
    );

CREATE POLICY "Message sender can update" ON messages
    FOR UPDATE USING (sender_id = auth.uid());

CREATE POLICY "Message sender can delete" ON messages
    FOR DELETE USING (sender_id = auth.uid());

NOTIFY pgrst, 'reload schema';
