-- Migration: Add team_ratings table for peer feedback
-- Date: 2026-05-23

CREATE TABLE IF NOT EXISTS team_ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID REFERENCES teams(id) ON DELETE CASCADE NOT NULL,
    rater_team_id UUID REFERENCES teams(id) ON DELETE CASCADE NOT NULL,
    rater_user_id UUID REFERENCES profiles(id) ON DELETE CASCADE NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    feedback TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    UNIQUE(team_id, rater_team_id)
);

CREATE INDEX IF NOT EXISTS idx_team_ratings_team ON team_ratings(team_id);
CREATE INDEX IF NOT EXISTS idx_team_ratings_rater_team ON team_ratings(rater_team_id);

-- Function to get average rating for a team
CREATE OR REPLACE FUNCTION get_team_average_rating(p_team_id UUID)
RETURNS NUMERIC AS $$
DECLARE
    avg_rating NUMERIC;
BEGIN
    SELECT AVG(rating)::NUMERIC(3,1)
    INTO avg_rating
    FROM team_ratings
    WHERE team_id = p_team_id;
    RETURN COALESCE(avg_rating, 0);
END;
$$ LANGUAGE plpgsql;

-- Function to get team ratings with rater info
CREATE OR REPLACE FUNCTION get_team_ratings(p_team_id UUID)
RETURNS TABLE (
    id UUID,
    rater_team_id UUID,
    rater_team_name TEXT,
    rater_user_name TEXT,
    rating INTEGER,
    feedback TEXT,
    created_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        tr.id,
        tr.rater_team_id,
        t.name AS rater_team_name,
        p.username AS rater_user_name,
        tr.rating,
        tr.feedback,
        tr.created_at
    FROM team_ratings tr
    JOIN teams t ON t.id = tr.rater_team_id
    JOIN profiles p ON p.id = tr.rater_user_id
    WHERE tr.team_id = p_team_id
    ORDER BY tr.created_at DESC;
END;
$$ LANGUAGE plpgsql;

-- Enable realtime for team_ratings
ALTER PUBLICATION supabase_realtime ADD TABLE team_ratings;

NOTIFY pgrst, 'reload schema';
