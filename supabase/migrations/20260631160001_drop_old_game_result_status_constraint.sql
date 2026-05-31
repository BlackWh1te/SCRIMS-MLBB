-- Drop the old scrim_game_results_status_check constraint which conflicts with valid_game_result_status.
-- The old constraint uses ALL_CAPS (PENDING, AWAITING_OPPONENT, etc.)
-- while valid_game_result_status uses Title Case (Pending, Awaiting Opponent, etc.)
-- and also includes 'Disputed'. The app code sends Title Case values.

ALTER TABLE scrim_game_results DROP CONSTRAINT IF EXISTS scrim_game_results_status_check;
