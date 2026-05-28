-- Migration: Rename mlbb_id column to game_id across the database
-- This aligns the database schema with the rebrand to Scrims Legends.
-- Run AFTER deploying the app update that sends "game_id" instead of "mlbb_id".

BEGIN;

-- 1. Rename the column on the profiles table
ALTER TABLE profiles RENAME COLUMN mlbb_id TO game_id;

-- 2. Recreate the unique index (dropped automatically when column is renamed)
CREATE UNIQUE INDEX IF NOT EXISTS idx_profiles_game_id ON profiles(game_id);

-- 3. Update any triggers or functions that reference the old column name
-- The auto-create-profile trigger references user_metadata->>'mlbb_id' via raw_user_meta_data
-- Check and update if needed:
DO $$
BEGIN
    -- Update the auto-profile trigger if it exists and references mlbb_id
    IF EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'on_auth_user_created'
    ) THEN
        -- The trigger body is in a function; we need to check the function definition
        -- and recreate it if it references mlbb_id
        RAISE NOTICE 'Please manually verify the auth trigger function references game_id instead of mlbb_id';
    END IF;
END $$;

COMMIT;
