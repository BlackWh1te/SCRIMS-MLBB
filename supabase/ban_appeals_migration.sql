-- ═══════════════════════════════════════════════════════════════
-- Ban Appeals System Migration
-- Adds: ban_appeals table, RLS policies, RPC functions
-- ═══════════════════════════════════════════════════════════════

-- ─── Step 1: Ensure profiles has ban metadata columns ───

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'ban_reason'
    ) THEN
        ALTER TABLE profiles ADD COLUMN ban_reason TEXT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'banned_at'
    ) THEN
        ALTER TABLE profiles ADD COLUMN banned_at TIMESTAMP WITH TIME ZONE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'profiles' AND column_name = 'banned_by'
    ) THEN
        ALTER TABLE profiles ADD COLUMN banned_by UUID REFERENCES profiles(id);
    END IF;
END $$;

-- ─── Step 2: Create ban_appeals table ───

CREATE TABLE IF NOT EXISTS ban_appeals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    ban_reason TEXT,                          -- the admin's reason for the ban (copied from profiles)
    appeal_message TEXT NOT NULL,             -- user's appeal text
    status TEXT NOT NULL DEFAULT 'pending'    -- pending / under_review / approved / rejected
        CHECK (status IN ('pending', 'under_review', 'approved', 'rejected')),
    admin_notes TEXT,                         -- admin's notes during review
    reviewed_by UUID REFERENCES profiles(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_ban_appeals_user_id ON ban_appeals(user_id);
CREATE INDEX IF NOT EXISTS idx_ban_appeals_status ON ban_appeals(status);
CREATE INDEX IF NOT EXISTS idx_ban_appeals_created_at ON ban_appeals(created_at DESC);

-- ─── Step 3: RLS Policies ───

ALTER TABLE ban_appeals ENABLE ROW LEVEL SECURITY;

-- Users can submit their own appeal (INSERT) only if they are banned
CREATE POLICY "Users can submit own appeal"
    ON ban_appeals FOR INSERT
    WITH CHECK (
        auth.uid() = user_id
        AND EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND is_banned = TRUE)
    );

-- Users can read their own appeals
CREATE POLICY "Users can read own appeals"
    ON ban_appeals FOR SELECT
    USING (auth.uid() = user_id);

-- Admins can read all appeals
CREATE POLICY "Admins can read all appeals"
    ON ban_appeals FOR SELECT
    USING (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = TRUE)
    );

-- Admins can update any appeal (review)
CREATE POLICY "Admins can update any appeal"
    ON ban_appeals FOR UPDATE
    USING (
        EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = TRUE)
    );

-- ─── Step 4: RPC — submit_ban_appeal ───

CREATE OR REPLACE FUNCTION submit_ban_appeal(
    p_user_id UUID,
    p_appeal_message TEXT
)
RETURNS UUID AS $$
DECLARE
    v_appeal_id UUID;
    v_ban_reason TEXT;
    v_existing_count INTEGER;
BEGIN
    -- Validate: user must be banned
    IF NOT EXISTS (SELECT 1 FROM profiles WHERE id = p_user_id AND is_banned = TRUE) THEN
        RAISE EXCEPTION 'You are not currently banned. Only banned users can submit appeals.';
    END IF;

    -- Rate limit: max 1 pending/under_review appeal per user
    SELECT COUNT(*) INTO v_existing_count
    FROM ban_appeals
    WHERE user_id = p_user_id
      AND status IN ('pending', 'under_review');

    IF v_existing_count > 0 THEN
        RAISE EXCEPTION 'You already have a pending appeal. Please wait for it to be reviewed.';
    END IF;

    -- Get the ban reason from profiles
    SELECT ban_reason INTO v_ban_reason FROM profiles WHERE id = p_user_id;

    -- Insert the appeal
    INSERT INTO ban_appeals (user_id, ban_reason, appeal_message, status)
    VALUES (p_user_id, v_ban_reason, p_appeal_message, 'pending')
    RETURNING id INTO v_appeal_id;

    RETURN v_appeal_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ─── Step 5: RPC — review_ban_appeal ───

CREATE OR REPLACE FUNCTION review_ban_appeal(
    p_appeal_id UUID,
    p_status TEXT,
    p_admin_notes TEXT DEFAULT NULL,
    p_reviewer_id UUID DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
    v_user_id UUID;
BEGIN
    -- Validate status
    IF p_status NOT IN ('under_review', 'approved', 'rejected') THEN
        RAISE EXCEPTION 'Invalid appeal status. Must be under_review, approved, or rejected.';
    END IF;

    -- Get the user_id from the appeal
    SELECT user_id INTO v_user_id FROM ban_appeals WHERE id = p_appeal_id;

    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Appeal not found.';
    END IF;

    -- Update the appeal
    UPDATE ban_appeals
    SET
        status = p_status,
        admin_notes = COALESCE(p_admin_notes, admin_notes),
        reviewed_by = p_reviewer_id,
        reviewed_at = TIMEZONE('utc', NOW()),
        updated_at = TIMEZONE('utc', NOW())
    WHERE id = p_appeal_id;

    -- If approved, unban the user automatically
    IF p_status = 'approved' THEN
        UPDATE profiles
        SET
            is_banned = FALSE,
            ban_reason = NULL,
            banned_at = NULL,
            banned_by = NULL
        WHERE id = v_user_id;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ─── Step 6: RPC — get_user_appeal_status ───
-- For the Android app to check if user has a pending appeal

CREATE OR REPLACE FUNCTION get_user_appeal_status(p_user_id UUID)
RETURNS TABLE (
    id UUID,
    status TEXT,
    ban_reason TEXT,
    appeal_message TEXT,
    admin_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    reviewed_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        ba.id,
        ba.status,
        ba.ban_reason,
        ba.appeal_message,
        ba.admin_notes,
        ba.created_at,
        ba.reviewed_at
    FROM ban_appeals ba
    WHERE ba.user_id = p_user_id
    ORDER BY ba.created_at DESC
    LIMIT 1;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ─── Step 7: Update the ban_user helper (admin sets ban metadata) ───

CREATE OR REPLACE FUNCTION ban_user(
    p_user_id UUID,
    p_banned_by UUID DEFAULT NULL,
    p_ban_reason TEXT
)
RETURNS VOID AS $$
BEGIN
    UPDATE profiles
    SET
        is_banned = TRUE,
        ban_reason = p_ban_reason,
        banned_at = TIMEZONE('utc', NOW()),
        banned_by = p_banned_by
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION unban_user(
    p_user_id UUID
)
RETURNS VOID AS $$
BEGIN
    UPDATE profiles
    SET
        is_banned = FALSE,
        ban_reason = NULL,
        banned_at = NULL,
        banned_by = NULL
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Notify PostgREST to reload schema cache
NOTIFY pgrst, 'reload schema';
