-- Migration: Add is_open_for_applications column to teams table
-- Fixes: PGRST204 "could not find the is_open_for_applications column of teams in the schema cache"
-- Date: 2026-05-23
-- Run this in Supabase SQL Editor if the column is missing from your database.

-- Add is_open_for_applications column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'teams' AND column_name = 'is_open_for_applications'
    ) THEN
        ALTER TABLE teams ADD COLUMN is_open_for_applications BOOLEAN DEFAULT FALSE;
        RAISE NOTICE 'Added is_open_for_applications column to teams table';
    ELSE
        RAISE NOTICE 'Column is_open_for_applications already exists in teams table';
    END IF;
END $$;

-- Add logo_url column if it doesn't exist (also referenced by the app)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'teams' AND column_name = 'logo_url'
    ) THEN
        ALTER TABLE teams ADD COLUMN logo_url TEXT;
        RAISE NOTICE 'Added logo_url column to teams table';
    ELSE
        RAISE NOTICE 'Column logo_url already exists in teams table';
    END IF;
END $$;

-- Notify PostgREST to reload schema cache after adding columns
NOTIFY pgrst, 'reload schema';
