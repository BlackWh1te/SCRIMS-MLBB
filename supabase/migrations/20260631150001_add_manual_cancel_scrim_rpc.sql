-- Manual cancel scrim RPC (atomic, with custom reason and canceller)
-- Fixes race condition where ScrimViewModel.cancelScrim used read-then-write updateScrim

CREATE OR REPLACE FUNCTION public.cancel_scrim(
    p_scrim_id UUID,
    p_reason TEXT DEFAULT 'Cancelled by user',
    p_cancelled_by UUID DEFAULT NULL
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $$
DECLARE
    v_scrim RECORD;
    v_host_leader_id UUID;
    v_result JSON;
BEGIN
    -- Lock the scrim row
    SELECT * INTO v_scrim
    FROM scrims
    WHERE id = p_scrim_id
    FOR UPDATE;

    IF v_scrim IS NULL THEN
        RETURN json_build_object('success', false, 'error', 'Scrim not found');
    END IF;

    -- Do not overwrite already-completed or already-cancelled
    IF v_scrim.status IN ('Completed', 'Cancelled') THEN
        RETURN json_build_object('success', false, 'error', 'Scrim is already in a terminal state');
    END IF;

    -- Verify caller is the host team leader
    SELECT leader_id INTO v_host_leader_id FROM teams WHERE id = v_scrim.team_id;
    IF v_host_leader_id IS NULL OR v_host_leader_id != auth.uid() THEN
        RETURN json_build_object('success', false, 'error', 'Only the host team leader can cancel this scrim');
    END IF;

    -- Atomically cancel
    UPDATE scrims
    SET status = 'Cancelled',
        cancellation_reason = p_reason,
        cancelled_by = p_cancelled_by
    WHERE id = p_scrim_id;

    -- Reject any remaining pending applications
    UPDATE scrim_applications
    SET status = 'Rejected'
    WHERE scrim_id = p_scrim_id AND status = 'Pending';

    -- Return updated scrim
    SELECT to_jsonb(s) INTO v_result FROM scrims s WHERE s.id = p_scrim_id;
    RETURN json_build_object('success', true, 'scrim', v_result);
END;
$$;
