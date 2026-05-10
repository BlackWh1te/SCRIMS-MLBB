-- MLBB Scrim Host - Row Level Security Policies
-- PostgreSQL with Supabase

-- Enable RLS on all tables
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE teams ENABLE ROW LEVEL SECURITY;
ALTER TABLE team_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE scrims ENABLE ROW LEVEL SECURITY;
ALTER TABLE scrim_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE match_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE team_invitations ENABLE ROW LEVEL SECURITY;

-- Profiles policies
CREATE POLICY "Users can view all profiles"
  ON profiles FOR SELECT
  USING (true);

CREATE POLICY "Users can update own profile"
  ON profiles FOR UPDATE
  USING (auth.uid() = id);

CREATE POLICY "Admins can update any profile"
  ON profiles FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = true
    )
  );

-- Teams policies
CREATE POLICY "Users can view all teams"
  ON teams FOR SELECT
  USING (true);

CREATE POLICY "Users can create teams"
  ON teams FOR INSERT
  WITH CHECK (auth.uid() = leader_id);

CREATE POLICY "Team leaders can update own team"
  ON teams FOR UPDATE
  USING (auth.uid() = leader_id);

CREATE POLICY "Admins can update any team"
  ON teams FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = true
    )
  );

CREATE POLICY "Team leaders can delete own team"
  ON teams FOR DELETE
  USING (auth.uid() = leader_id);

-- Team members policies
CREATE POLICY "Users can view team members"
  ON team_members FOR SELECT
  USING (true);

CREATE POLICY "Team leaders can add members"
  ON team_members FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM teams WHERE id = team_id AND leader_id = auth.uid()
    )
  );

CREATE POLICY "Team leaders can update members"
  ON team_members FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM teams WHERE id = team_id AND leader_id = auth.uid()
    )
  );

CREATE POLICY "Team leaders can remove members"
  ON team_members FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM teams WHERE id = team_id AND leader_id = auth.uid()
    ) OR user_id = auth.uid()
  );

-- Scrims policies
CREATE POLICY "Users can view all scrims"
  ON scrims FOR SELECT
  USING (true);

CREATE POLICY "Team leaders can create scrims"
  ON scrims FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM teams WHERE id = team_id AND leader_id = auth.uid()
    )
  );

CREATE POLICY "Team leaders can update own scrims"
  ON scrims FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM teams WHERE id = team_id AND leader_id = auth.uid()
    )
  );

CREATE POLICY "Team leaders can delete own scrims"
  ON scrims FOR DELETE
  USING (
    EXISTS (
      SELECT 1 FROM teams WHERE id = team_id AND leader_id = auth.uid()
    )
  );

-- Scrim applications policies
CREATE POLICY "Users can view scrim applications"
  ON scrim_applications FOR SELECT
  USING (true);

CREATE POLICY "Team leaders can apply to scrims"
  ON scrim_applications FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM teams WHERE id = applicant_team_id AND leader_id = auth.uid()
    )
  );

CREATE POLICY "Scrim owners can accept/reject applications"
  ON scrim_applications FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM scrims
      WHERE id = scrim_id
      AND team_id IN (
        SELECT id FROM teams WHERE leader_id = auth.uid()
      )
    )
  );

-- Matches policies
CREATE POLICY "Users can view matches they participate in"
  ON matches FOR SELECT
  USING (
    team_a_id IN (SELECT team_id FROM team_members WHERE user_id = auth.uid())
    OR team_b_id IN (SELECT team_id FROM team_members WHERE user_id = auth.uid())
    OR EXISTS (SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = true)
  );

CREATE POLICY "Team leaders can update match details"
  ON matches FOR UPDATE
  USING (
    team_a_id IN (SELECT id FROM teams WHERE leader_id = auth.uid())
    OR team_b_id IN (SELECT id FROM teams WHERE leader_id = auth.uid())
  );

-- Messages policies
CREATE POLICY "Match participants can view messages"
  ON messages FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM matches
      WHERE id = match_id
      AND (
        team_a_id IN (SELECT team_id FROM team_members WHERE user_id = auth.uid())
        OR team_b_id IN (SELECT team_id FROM team_members WHERE user_id = auth.uid())
      )
    )
  );

CREATE POLICY "Match participants can send messages"
  ON messages FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM matches
      WHERE id = match_id
      AND (
        team_a_id IN (SELECT team_id FROM team_members WHERE user_id = auth.uid())
        OR team_b_id IN (SELECT team_id FROM team_members WHERE user_id = auth.uid())
      )
    )
    AND sender_id = auth.uid()
  );

-- Match results policies
CREATE POLICY "Match participants can view results"
  ON match_results FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM matches
      WHERE id = match_id
      AND (
        team_a_id IN (SELECT team_id FROM team_members WHERE user_id = auth.uid())
        OR team_b_id IN (SELECT team_id FROM team_members WHERE user_id = auth.uid())
      )
    )
  );

CREATE POLICY "Match participants can upload screenshots"
  ON match_results FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM matches
      WHERE id = match_id
      AND (
        team_a_id IN (SELECT id FROM teams WHERE leader_id = auth.uid())
        OR team_b_id IN (SELECT id FROM teams WHERE leader_id = auth.uid())
      )
    )
  );

CREATE POLICY "Admins can verify results"
  ON match_results FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM profiles WHERE id = auth.uid() AND is_admin = true
    )
  );

-- Team invitations policies
CREATE POLICY "Users can view invitations sent to them"
  ON team_invitations FOR SELECT
  USING (invited_user_id = auth.uid());

CREATE POLICY "Team leaders can invite players"
  ON team_invitations FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM teams WHERE id = team_id AND leader_id = auth.uid()
    )
    AND invited_by = auth.uid()
  );

CREATE POLICY "Invited users can accept/reject invitations"
  ON team_invitations FOR UPDATE
  USING (invited_user_id = auth.uid());