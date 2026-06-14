-- Migration: Fix message delivery_status trigger to fire on all inserts
-- Date: 2026-06-29
--
-- Problem: The messages table has a DEFAULT 'SENT' on delivery_status,
--   so the WHEN (NEW.delivery_status = 'pending') clause on the trigger
--   never matches for normal app inserts. Messages stay as 'SENT'.
--
-- Fix: Remove the WHEN clause so the trigger fires for every insert
--   and unconditionally sets delivery_status to 'delivered'.

-- Drop and recreate trigger without the WHEN clause
DROP TRIGGER IF EXISTS on_message_insert_set_delivered ON messages;

CREATE TRIGGER on_message_insert_set_delivered
    BEFORE INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_message_inserted();

-- Backfill existing messages that are still 'SENT' to 'delivered'
UPDATE messages
SET delivery_status = 'delivered'
WHERE delivery_status = 'SENT';
