-- Migration: Add team_name to scrims for self-contained display
-- Date: 2026-05-31

ALTER TABLE scrims ADD COLUMN IF NOT EXISTS team_name TEXT;
