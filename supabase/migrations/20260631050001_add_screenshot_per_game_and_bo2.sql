-- Migration: Add per-game screenshot support and BO2 to scrims
-- Date: 2026-05-31

-- 1. Update best_of constraint to allow BO2 (user-requested feature)
ALTER TABLE scrims DROP CONSTRAINT IF EXISTS valid_best_of;
ALTER TABLE scrims ADD CONSTRAINT valid_best_of CHECK (best_of IN (1, 2, 3, 5));

-- 2. Create per-game results table
-- Each game in a best-of series gets its own row with screenshots from both teams.
CREATE TABLE IF NOT EXISTS scrim_game_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scrim_id UUID NOT NULL REFERENCES scrims(id) ON DELETE CASCADE,
    game_number INTEGER NOT NULL,
    team_a_screenshot_url TEXT,
    team_b_screenshot_url TEXT,
    team_a_screenshot_uploaded_at TIMESTAMP WITH TIME ZONE,
    team_b_screenshot_uploaded_at TIMESTAMP WITH TIME ZONE,
    winner_team_id UUID REFERENCES teams(id),
    -- Status tracks the progress of this individual game result
    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'AWAITING_OPPONENT', 'BOTH_UPLOADED', 'WINNER_SELECTED', 'CONFIRMED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(scrim_id, game_number)
);

-- 3. Enable RLS on new table
ALTER TABLE scrim_game_results ENABLE ROW LEVEL SECURITY;

-- 4. RLS Policies
-- Anyone can read game results (needed for viewing scrim details)
CREATE POLICY "Anyone can read scrim game results"
    ON scrim_game_results FOR SELECT
    USING (true);

-- Team leaders of either participating team can update game results
CREATE POLICY "Team leaders can update game results"
    ON scrim_game_results FOR UPDATE
    USING (
        EXISTS (
            SELECT 1 FROM scrims s
            WHERE s.id = scrim_game_results.scrim_id
            AND (
                EXISTS (SELECT 1 FROM teams t WHERE t.id = s.team_id AND t.leader_id = auth.uid())
                OR EXISTS (SELECT 1 FROM teams t WHERE t.id = s.opponent_team_id AND t.leader_id = auth.uid())
            )
        )
    );

-- Team leaders can insert game results for their scrims
CREATE POLICY "Team leaders can insert game results"
    ON scrim_game_results FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM scrims s
            WHERE s.id = scrim_game_results.scrim_id
            AND (
                EXISTS (SELECT 1 FROM teams t WHERE t.id = s.team_id AND t.leader_id = auth.uid())
                OR EXISTS (SELECT 1 FROM teams t WHERE t.id = s.opponent_team_id AND t.leader_id = auth.uid())
            )
        )
    );

-- 5. Indexes for performance
CREATE INDEX IF NOT EXISTS idx_scrim_game_results_scrim ON scrim_game_results(scrim_id);
CREATE INDEX IF NOT EXISTS idx_scrim_game_results_status ON scrim_game_results(status);

-- 6. Trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_scrim_game_results_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = TIMEZONE('utc', NOW());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_scrim_game_results_updated_at ON scrim_game_results;
CREATE TRIGGER trg_scrim_game_results_updated_at
    BEFORE UPDATE ON scrim_game_results
    FOR EACH ROW
    EXECUTE FUNCTION update_scrim_game_results_updated_at();
