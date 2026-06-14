-- Migration: Add avatar URLs to get_conversations_for_user RPC
-- Joins profiles table to expose participant avatar URLs directly in
-- the conversations query, so the Android client can display real avatars
-- without a separate profile fetch per conversation.

-- Drop first because return type changes (adds avatar_url columns)
DROP FUNCTION IF EXISTS get_conversations_for_user(UUID);

CREATE OR REPLACE FUNCTION get_conversations_for_user(p_user_id UUID)
RETURNS TABLE (
    id UUID,
    scrim_id UUID,
    participant_a_id UUID,
    participant_a_name TEXT,
    participant_a_team_id UUID,
    participant_a_team_name TEXT,
    participant_a_avatar_url TEXT,
    participant_b_id UUID,
    participant_b_name TEXT,
    participant_b_team_id UUID,
    participant_b_team_name TEXT,
    participant_b_avatar_url TEXT,
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
        pa.avatar_url AS participant_a_avatar_url,
        c.participant_b_id,
        c.participant_b_name,
        c.participant_b_team_id,
        c.participant_b_team_name,
        pb.avatar_url AS participant_b_avatar_url,
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
    LEFT JOIN profiles pa ON pa.id = c.participant_a_id
    LEFT JOIN profiles pb ON pb.id = c.participant_b_id
    WHERE
        (c.participant_a_id = p_user_id OR c.participant_b_id = p_user_id)
        OR (
            c.is_team_chat = TRUE
            AND c.team_id IN (
                SELECT tm.team_id FROM team_members tm
                WHERE tm.user_id = p_user_id
            )
        )
    ORDER BY c.is_pinned DESC, c.last_message_time DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
