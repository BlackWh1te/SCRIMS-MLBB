package com.scrimslegends.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 5 to 6.
 * Adds new columns to cached_scrims and messages tables introduced in schema fixes:
 * - ScrimEntity: teamAReadyAt, teamBReadyAt, gameMode, region, skillLevel, maxPlayers, currentPlayers
 * - MessageEntity: matchId, senderTeamId
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // cached_scrims columns
        db.execSQL("ALTER TABLE cached_scrims ADD COLUMN teamAReadyAt TEXT")
        db.execSQL("ALTER TABLE cached_scrims ADD COLUMN teamBReadyAt TEXT")
        db.execSQL("ALTER TABLE cached_scrims ADD COLUMN gameMode TEXT NOT NULL DEFAULT 'RANKED'")
        db.execSQL("ALTER TABLE cached_scrims ADD COLUMN region TEXT NOT NULL DEFAULT 'EU'")
        db.execSQL("ALTER TABLE cached_scrims ADD COLUMN skillLevel TEXT NOT NULL DEFAULT 'ALL'")
        db.execSQL("ALTER TABLE cached_scrims ADD COLUMN maxPlayers INTEGER NOT NULL DEFAULT 10")
        db.execSQL("ALTER TABLE cached_scrims ADD COLUMN currentPlayers INTEGER NOT NULL DEFAULT 0")

        // messages columns
        db.execSQL("ALTER TABLE messages ADD COLUMN matchId TEXT")
        db.execSQL("ALTER TABLE messages ADD COLUMN senderTeamId TEXT")
    }
}

/**
 * Migration from version 6 to 7.
 * Adds tournament match chat columns to conversations table:
 * - tournamentMatchId, participantCount, isGroupChat
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversations ADD COLUMN tournamentMatchId TEXT")
        db.execSQL("ALTER TABLE conversations ADD COLUMN participantCount INTEGER NOT NULL DEFAULT 2")
        db.execSQL("ALTER TABLE conversations ADD COLUMN isGroupChat INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Migration from version 7 to 8.
 * Adds wins, losses, pts columns to cached_lfg_posts table.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cached_lfg_posts ADD COLUMN wins INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE cached_lfg_posts ADD COLUMN losses INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE cached_lfg_posts ADD COLUMN pts INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Migration from version 8 to 9.
 * Adds viewCount column to cached_lfg_posts table.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cached_lfg_posts ADD COLUMN viewCount INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Migration from version 9 to 10.
 * Adds unreadCount column to conversations table.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversations ADD COLUMN unreadCount INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * Migration from version 10 to 11.
 * Production messaging hardening:
 * - pending_messages outbox table
 * - messages.deliveryStatus + messages.clientMessageId
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Outbox table
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS pending_messages (
                clientMessageId TEXT PRIMARY KEY NOT NULL,
                conversationId TEXT NOT NULL,
                senderId TEXT NOT NULL,
                senderName TEXT NOT NULL,
                content TEXT NOT NULL,
                type TEXT NOT NULL,
                imageUrl TEXT,
                voiceUrl TEXT,
                voiceDuration INTEGER,
                createdAt INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'PENDING',
                retryCount INTEGER NOT NULL DEFAULT 0,
                nextRetryAt INTEGER NOT NULL DEFAULT 0,
                errorReason TEXT,
                failedAt INTEGER
            )"""
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_pending_status_retry ON pending_messages(status, nextRetryAt)")

        // Message delivery tracking
        db.execSQL("ALTER TABLE messages ADD COLUMN deliveryStatus TEXT NOT NULL DEFAULT 'SENT'")
        db.execSQL("ALTER TABLE messages ADD COLUMN clientMessageId TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_client_id ON messages(clientMessageId)")
    }
}

/**
 * Migration from version 11 to 12.
 * Adds team group chat fields to cached conversations.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversations ADD COLUMN teamId TEXT")
        db.execSQL("ALTER TABLE conversations ADD COLUMN isTeamChat INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE conversations ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE conversations ADD COLUMN groupName TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_conversations_team_chat ON conversations(teamId, isTeamChat)")
    }
}

/**
 * Migration from version 12 to 13.
 * Adds participant avatar URL columns to cached conversations so real
 * profile pictures can be shown in the chat list and chat header without
 * an extra network fetch.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversations ADD COLUMN participantAAvatarUrl TEXT")
        db.execSQL("ALTER TABLE conversations ADD COLUMN participantBAvatarUrl TEXT")
    }
}

/**
 * Migration from version 13 to 14.
 * Message feature redesign:
 * - Reply-to support: replyToId, replyToSnippet, replyToSenderName
 * - Soft delete: isDeleted flag
 * - Performance: index on messages.conversationId
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Reply support
        db.execSQL("ALTER TABLE messages ADD COLUMN replyToId TEXT")
        db.execSQL("ALTER TABLE messages ADD COLUMN replyToSnippet TEXT")
        db.execSQL("ALTER TABLE messages ADD COLUMN replyToSenderName TEXT")
        // Soft delete
        db.execSQL("ALTER TABLE messages ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        // Performance index
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_conv_id ON messages(conversationId)")
    }
}
