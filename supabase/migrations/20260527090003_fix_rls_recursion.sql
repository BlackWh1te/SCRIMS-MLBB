-- ═══════════════════════════════════════════════════════════════
-- Migration: Fix RLS Infinite Recursion
-- Date: 2026-05-27
-- Author: Devin
-- Problem: conversation_participants policy self-references + mutual recursion with conversations
-- ═══════════════════════════════════════════════════════════════

-- Step 1: Create SECURITY DEFINER helper that bypasses RLS for membership checks
-- This breaks the recursion because RLS policies are NOT applied inside SECURITY DEFINER functions
CREATE OR REPLACE FUNCTION public.is_user_in_conversation(
    p_conv_id UUID,
    p_user_id UUID
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Direct participants (1:1 or scrim chats)
    IF EXISTS (
        SELECT 1 FROM conversations c
        WHERE c.id = p_conv_id
        AND (c.participant_a_id = p_user_id OR c.participant_b_id = p_user_id)
    ) THEN
        RETURN TRUE;
    END IF;

    -- Group chat participants (tournament match chats)
    IF EXISTS (
        SELECT 1 FROM conversation_participants cp
        WHERE cp.conversation_id = p_conv_id AND cp.user_id = p_user_id
    ) THEN
        RETURN TRUE;
    END IF;

    RETURN FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- Step 2: Drop all broken policies on conversation_participants
DROP POLICY IF EXISTS "Conversation participants can view members" ON conversation_participants;

-- Step 3: Recreate conversation_participants policy using the helper
-- No self-reference; function bypasses RLS internally
CREATE POLICY "Conversation participants can view members" ON conversation_participants
    FOR SELECT USING (is_user_in_conversation(conversation_id, auth.uid()));

-- Step 4: Drop all broken policies on conversations
DROP POLICY IF EXISTS "Conversation participants can view" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can insert" ON conversations;
DROP POLICY IF EXISTS "Conversation participants can update" ON conversations;

-- Step 5: Recreate conversation policies using the helper
CREATE POLICY "Conversation participants can view" ON conversations
    FOR SELECT USING (
        participant_a_id = auth.uid()
        OR participant_b_id = auth.uid()
        OR is_user_in_conversation(id, auth.uid())
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
        OR is_user_in_conversation(id, auth.uid())
    );

-- Step 6: Drop old broken policies on messages (they reference the recursive chain)
DROP POLICY IF EXISTS "Conversation members can view messages" ON messages;
DROP POLICY IF EXISTS "Conversation members can send messages" ON messages;
DROP POLICY IF EXISTS "messages_insert_policy" ON messages;

-- Step 7: Recreate message policies using the helper
-- SELECT: sender always sees their own; members see conversation messages
CREATE POLICY "Conversation members can view messages" ON messages
    FOR SELECT USING (
        sender_id = auth.uid()
        OR is_user_in_conversation(conversation_id, auth.uid())
    );

-- INSERT: sender must be a conversation member; chat gate enforced by trigger
CREATE POLICY "Conversation members can send messages" ON messages
    FOR INSERT WITH CHECK (
        sender_id = auth.uid()
        AND is_user_in_conversation(conversation_id, auth.uid())
    );

-- Step 8: Keep existing sender-only UPDATE/DELETE policies (they don't recurse)
-- These were already present and correct
