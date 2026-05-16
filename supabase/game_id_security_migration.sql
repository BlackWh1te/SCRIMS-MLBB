-- Migration: Enforce 1-to-1 Game ID and account link, and prevent banned ID reuse.
-- 1. Add unique constraint to mlbb_id
-- 2. Add ban-related columns to profiles

-- First, ensure no duplicate mlbb_id exist (optional, but good practice)
-- DELETE FROM profiles WHERE id NOT IN (SELECT MIN(id) FROM profiles GROUP BY mlbb_id) AND mlbb_id IS NOT NULL;

-- Add UNIQUE constraint to mlbb_id
-- Note: UNIQUE in PostgreSQL allows multiple NULLs, which is fine for users who haven't set their ID yet.
ALTER TABLE public.profiles ADD CONSTRAINT profiles_mlbb_id_key UNIQUE (mlbb_id);

-- Add ban columns
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS is_banned BOOLEAN DEFAULT FALSE;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS ban_reason TEXT;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS banned_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS banned_by UUID REFERENCES public.profiles(id);

-- Update delete_user function to handle banned users differently if needed
-- Actually, the user's request suggests we should block the ID. 
-- By keeping the profile record with is_banned=true, the UNIQUE constraint on mlbb_id 
-- will automatically prevent anyone else (or the same user with a new account) from using it.

COMMENT ON COLUMN public.profiles.mlbb_id IS 'Unique MLBB Game ID. Linked 1-to-1 with the account to prevent multiple accounts or banned ID reuse.';
