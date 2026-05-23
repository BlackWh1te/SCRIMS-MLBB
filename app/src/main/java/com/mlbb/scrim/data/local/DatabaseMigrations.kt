package com.mlbb.scrim.data.local

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
