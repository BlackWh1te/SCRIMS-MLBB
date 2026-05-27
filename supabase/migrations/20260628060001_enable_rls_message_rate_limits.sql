-- Migration: Enable RLS on message_rate_limits
-- Date: 2026-06-28
-- Context: Supabase linter reported message_rate_limits is in public schema
--          but RLS is not enabled. This table tracks per-user message rate
--          limits and should only be accessible to the owning user.

-- Enable RLS
ALTER TABLE message_rate_limits ENABLE ROW LEVEL SECURITY;

-- Users can only see their own rate limit row
DROP POLICY IF EXISTS "Users can read own rate limit" ON message_rate_limits;
CREATE POLICY "Users can read own rate limit" ON message_rate_limits
    FOR SELECT USING (user_id = auth.uid());

-- Users can only modify their own rate limit row
DROP POLICY IF EXISTS "Users can update own rate limit" ON message_rate_limits;
CREATE POLICY "Users can update own rate limit" ON message_rate_limits
    FOR UPDATE USING (user_id = auth.uid()) WITH CHECK (user_id = auth.uid());

-- Users can insert only for themselves
DROP POLICY IF EXISTS "Users can insert own rate limit" ON message_rate_limits;
CREATE POLICY "Users can insert own rate limit" ON message_rate_limits
    FOR INSERT WITH CHECK (user_id = auth.uid());

-- No DELETE policy needed; rows are managed by the trigger and cascade on profile deletion
