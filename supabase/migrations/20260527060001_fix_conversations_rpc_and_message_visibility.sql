-- Migration: Fix conversations RPC to include tournament_match_id and participant_count
-- Date: 2026-05-27
-- Context:
--   1. get_conversations_for_user RPC was missing tournament_match_id and participant_count
--      fields, causing group chat conversations to not be properly identified.
--   2. The RPC only checked participant_a_id/participant_b_id, missing group participants
--      added via conversation_participants table.
--   3. Messages were not visible because the RPC returned incomplete data and the
--      conversation list didn't include group chats.

-- ═══════════════════════════════════════════════════════════════
-- 1. UPDATE get_conversations_for_user RPC
-- ═══════════════════════════════════════════════════════════════

DROP FUNCTION IF EXISTS get_conversations_for_user(UUID);
CREATE OR REPLACE FUNCTION get_conversations_for_user(p_user_id UUID)
RETURNS TABLE (
    id UUID,
    scrim_id UUID,
    tournament_match_id UUID,
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
    participant_count INT,
    created_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.scrim_id,
        c.tournament_match_id,
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
        COALESCE(
            (SELECT COUNT(*) FROM conversation_participants cp
             WHERE cp.conversation_id = c.id),
            0
        )::INT + 2 AS participant_count,
        c.created_at
    FROM conversations c
    WHERE c.participant_a_id = p_user_id
       OR c.participant_b_id = p_user_id
       OR EXISTS (
           SELECT 1 FROM conversation_participants cp
           WHERE cp.conversation_id = c.id AND cp.user_id = p_user_id
       )
    ORDER BY c.last_message_time DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
