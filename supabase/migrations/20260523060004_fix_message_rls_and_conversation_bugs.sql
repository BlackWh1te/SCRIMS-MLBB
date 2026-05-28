-- Migration: Fix message RLS policies and conversation pipeline bugs
-- Date: 2026-05-23
--
-- Fixes:
-- 1. Messages SELECT policy was checking messages.match_id instead of messages.conversation_id
-- 2. Messages INSERT policy was missing / broken (checked matches table, not conversations)
-- 3. Conversations INSERT policy was missing
-- 4. Direct conversations (PlayerFinder) failed because scrim_id was NOT NULL in some contexts
--    -- Actually scrim_id is nullable in schema; this migration ensures policies handle NULL.

-- ═══════════════════════════════════════════════════════════════
-- DROP all existing broken message/conversation policies
-- ═══════════════════════════════════════════════════════════════
DROP POLICY IF EXISTS "Users can view their messages" ON messages;
DROP POLICY IF EXISTS "Allow read messages" ON messages;
DROP POLICY IF EXISTS "Match participants can view messages" ON messages;
DROP POLICY IF EXISTS "Match participants can send messages" ON messages;
DROP POLICY IF EXISTS "Users can view their conversations" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can view" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can insert" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can update" ON conversations;
DROP POLICY IF EXISTS "Conversation members can view messages" ON messages;
DROP POLICY IF EXISTS "Conversation members can send messages" ON messages;
DROP POLICY IF EXISTS "Message sender can update" ON messages;
DROP POLICY IF EXISTS "Message sender can delete" ON messages;

-- ═══════════════════════════════════════════════════════════════
-- CONVERSATIONS POLICIES
-- ═══════════════════════════════════════════════════════════════

-- SELECT: participants can see conversations they're in
CREATE POLICY "Conversation participants can view" ON conversations
    FOR SELECT USING (
        participant_a_id = auth.uid() OR participant_b_id = auth.uid()
    );

-- INSERT: authenticated users can create conversations where they are a participant
CREATE POLICY "Conversation participants can insert" ON conversations
    FOR INSERT WITH CHECK (
        participant_a_id = auth.uid() OR participant_b_id = auth.uid()
    );

-- UPDATE: participants can update typing status / last_message
CREATE POLICY "Conversation participants can update" ON conversations
    FOR UPDATE USING (
        participant_a_id = auth.uid() OR participant_b_id = auth.uid()
    );

-- ═══════════════════════════════════════════════════════════════
-- MESSAGES POLICIES
-- ═══════════════════════════════════════════════════════════════

-- SELECT: sender OR any participant of the parent conversation can view messages
CREATE POLICY "Conversation members can view messages" ON messages
    FOR SELECT USING (
        sender_id = auth.uid()
        OR EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
        )
    );

-- INSERT: sender must be a participant of the parent conversation
CREATE POLICY "Conversation members can send messages" ON messages
    FOR INSERT WITH CHECK (
        sender_id = auth.uid()
        AND EXISTS (
            SELECT 1 FROM conversations c
            WHERE c.id = messages.conversation_id
            AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
        )
    );

-- UPDATE: only sender can update (e.g. read receipts, edits)
CREATE POLICY "Message sender can update" ON messages
    FOR UPDATE USING (sender_id = auth.uid());

-- DELETE: only sender can delete their own message
CREATE POLICY "Message sender can delete" ON messages
    FOR DELETE USING (sender_id = auth.uid());

-- Allow service_role / anon bypass for admin panel and edge functions if needed
-- (Supabase service_role key bypasses RLS by default, so no extra policy needed)

NOTIFY pgrst, 'reload schema';
