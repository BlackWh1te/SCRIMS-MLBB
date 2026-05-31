-- Migration: Rename mlbb_id column to game_id across the database
-- This aligns the database schema with the rebrand to Scrims Legends.
-- Run AFTER deploying the app update that sends "game_id" instead of "mlbb_id".

BEGIN;

-- 1. Rename the column on the profiles table (idempotent)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'mlbb_id'
    ) THEN
        ALTER TABLE profiles RENAME COLUMN mlbb_id TO game_id;
    END IF;
END $$;

-- 2. Recreate the unique index on the new column name
CREATE UNIQUE INDEX IF NOT EXISTS idx_profiles_game_id ON profiles(game_id);

-- 3. Update any triggers or functions that reference the old column name
-- The auto-create-profile trigger references user_metadata->>'mlbb_id' via raw_user_meta_data
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'on_auth_user_created'
    ) THEN
        RAISE NOTICE 'Please manually verify the auth trigger function references game_id instead of mlbb_id';
    END IF;
END $$;

COMMIT;
