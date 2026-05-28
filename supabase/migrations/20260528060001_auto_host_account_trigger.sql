-- Migration: Auto-create tournament_host_accounts on tournament insert
-- Date: 2026-05-28
-- Problem: Hosts can't log into /host/login because tournament_host_accounts
--          rows are never created when they create tournaments in the Android app.
-- Fix: Trigger auto-creates the host account mapping using the host's auth credentials.

-- ═══════════════════════════════════════════════════════════════
-- 1. FUNCTION: Auto-create host account when tournament is inserted
-- ═══════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION auto_create_tournament_host_account()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_email TEXT;
BEGIN
    -- Look up the host's email from auth.users (same UUID as profiles.id)
    SELECT email INTO v_email
    FROM auth.users
    WHERE id = NEW.host_user_id;

    -- If found, create the host account mapping
    IF v_email IS NOT NULL THEN
        INSERT INTO tournament_host_accounts (
            tournament_id,
            host_user_id,
            auth_user_id,
            email
        )
        VALUES (
            NEW.id,
            NEW.host_user_id,
            NEW.host_user_id,  -- Supabase: profiles.id = auth.users.id
            v_email
        )
        ON CONFLICT (tournament_id) DO NOTHING;
    END IF;

    RETURN NEW;
END;
$$;

-- ═══════════════════════════════════════════════════════════════
-- 2. TRIGGER: Fire after each tournament insert
-- ═══════════════════════════════════════════════════════════════

DROP TRIGGER IF EXISTS tr_auto_create_host_account ON tournaments;

CREATE TRIGGER tr_auto_create_host_account
    AFTER INSERT ON tournaments
    FOR EACH ROW
    EXECUTE FUNCTION auto_create_tournament_host_account();

-- ═══════════════════════════════════════════════════════════════
-- 3. BACKFILL: Create host accounts for existing tournaments
--    (Run once if you already have tournaments without host accounts)
-- ═══════════════════════════════════════════════════════════════

INSERT INTO tournament_host_accounts (tournament_id, host_user_id, auth_user_id, email)
SELECT
    t.id AS tournament_id,
    t.host_user_id,
    t.host_user_id AS auth_user_id,
    COALESCE(au.email, 'unknown@mlbbhost.com')
FROM tournaments t
LEFT JOIN tournament_host_accounts tha ON tha.tournament_id = t.id
LEFT JOIN auth.users au ON au.id = t.host_user_id
WHERE tha.id IS NULL
ON CONFLICT (tournament_id) DO NOTHING;
