-- User Reports table
-- Stores reports submitted by users against other users (avatars, chat behavior, etc.)
-- Admin can view these in the support dashboard

CREATE TABLE IF NOT EXISTS user_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID REFERENCES profiles(id) ON DELETE SET NULL,
    reporter_name TEXT,
    reported_user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    reported_user_name TEXT,
    reported_avatar_url TEXT,
    reason TEXT NOT NULL,
    description TEXT,
    status TEXT DEFAULT 'open', -- open, reviewing, resolved, dismissed
    admin_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Enable RLS
ALTER TABLE user_reports ENABLE ROW LEVEL SECURITY;

-- Users can only view their own reports
CREATE POLICY "Users can view own reports" ON user_reports
    FOR SELECT USING (reporter_id = auth.uid());

-- Users can create reports
CREATE POLICY "Users can create reports" ON user_reports
    FOR INSERT WITH CHECK (reporter_id = auth.uid());

-- Admins can view all reports
CREATE POLICY "Admins can view all reports" ON user_reports
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = TRUE)
    );

-- Admins can update reports
CREATE POLICY "Admins can update reports" ON user_reports
    FOR UPDATE USING (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = TRUE)
    );

-- Add to realtime publication so admin panel can subscribe
ALTER PUBLICATION supabase_realtime ADD TABLE user_reports;
