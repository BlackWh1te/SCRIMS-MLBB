-- Fix completed_requires_winner constraint to allow NULL winner for BO2 ties
-- The complete_scrim RPC already validates tie legitimacy; this just allows the DB to accept it

ALTER TABLE scrims DROP CONSTRAINT IF EXISTS completed_requires_winner;

ALTER TABLE scrims ADD CONSTRAINT completed_requires_winner
    CHECK (
        (status <> 'Completed')
        OR (
            (status = 'Completed')
            AND (
                winner_team_id IS NOT NULL
                OR best_of = 2  -- BO2 ties can have NULL winner
            )
        )
    );

-- Force PostgREST schema cache refresh
NOTIFY pgrst, 'reload schema';
