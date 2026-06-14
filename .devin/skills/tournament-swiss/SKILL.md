# Tournament Swiss Pairing Engine

Implement the Swiss tournament pairing algorithm and bracket management.

## Plan Reference
Read `tournamentwork.md` at the project root. Section 3.3 (`generate_swiss_pairings`) and Section 3.5-3.6 (score/tiebreaker calculations) contain the complete algorithm.

## Rules

### Before Writing Any Code
1. Read `tournamentwork.md` sections 3.3, 3.5, 3.6 completely.
2. Read the existing `award_scrim_points()` function in `supabase/schema.sql` for scoring patterns.
3. Understand the Swiss system constraints below.

### Swiss Tournament Rules

#### Scoring (per match)
| Result | Points |
|--------|--------|
| Win (BO1) | 3 |
| Draw (BO2: 1-1) | 1 each |
| Loss | 0 |
| Bye (no opponent) | 3 (auto-win) |

#### Pairing Rules
1. **Round 1**: Random pairing of all checked-in teams
2. **Round 2+**: Sort by `swiss_points DESC`, then `buchholz_score DESC`
3. **No rematches**: Teams that already played each other cannot be paired again
4. **Odd teams**: One team gets a bye (auto-win) — choose the team with lowest points
5. **Minimum 4 teams**: Swiss requires at least 4 checked-in teams

#### Tiebreakers (applied after each round)
1. **Buchholz score**: Sum of all opponents' `swiss_points`. Higher = faced stronger opponents.
2. **Sonneborn-Berger**: Sum of (defeated opponents' points * 1) + (drawn opponents' points * 0.5). Rewards beating strong opponents.

#### Swiss Rounds Calculation
`swiss_rounds = CEIL(LOG(2, max_teams))`
- 4 teams = 2 rounds
- 8 teams = 3 rounds
- 16 teams = 4 rounds
- 32 teams = 5 rounds
- 64 teams = 6 rounds

#### Elimination
A team is NOT eliminated in standard Swiss — all teams play all rounds. However, the host can configure elimination after X losses if desired (future feature). For v1, all teams play all rounds.

### Implementation

The Swiss pairing is implemented as a Supabase RPC function `generate_swiss_pairings(p_tournament_id UUID)`.

#### Algorithm Pseudocode
```
1. Get all tournament_teams where checked_in = TRUE
2. Get current_round from tournaments table
3. new_round = current_round + 1

4. IF new_round = 1:
     Sort teams RANDOM()
   ELSE:
     Sort teams by swiss_points DESC, buchholz_score DESC

5. Initialize paired[] = [false, false, ...]

6. FOR i = 1 TO length(teams):
     IF paired[i]: SKIP

     Find j > i where:
       - NOT paired[j]
       - teams[i] and teams[j] have NOT played each other before
       (check tournament_swiss_matches for existing pairings)

     IF found:
       Create match: team_a = teams[i], team_b = teams[j]
       Create conversation with 3 participants:
         - Team A leader
         - Team B leader
         - Tournament host
       Mark paired[i] = paired[j] = true
     ELSE:
       Create BYE match: team_a = teams[i], team_b = NULL
       Auto-award win (3 points)
       Mark paired[i] = true

7. Update tournaments.current_round = new_round
8. Update tournaments.status = 'in_progress'
9. Notify all team leaders of their match
```

### Conversation Creation for Match Chat
When creating a match, also create a conversation:
```sql
INSERT INTO conversations (scrim_id, tournament_match_id) VALUES (NULL, v_match_id);
-- Then add 3 participants via conversation_participants:
-- 1. Team A leader (role = 'team_a_leader')
-- 2. Team B leader (role = 'team_b_leader')
-- 3. Tournament host (role = 'host')
```

### After Each Match Result
1. Call `update_tournament_scores(match_id)` — updates wins/losses/points on `tournament_teams`
2. Call `recalculate_tiebreakers(tournament_id)` — updates buchholz and sonneborn_berger
3. These are called automatically by `submit_tournament_match_result` RPC

### Edge Cases
- **Odd number of teams**: Bye goes to team with lowest points (or random in round 1)
- **All teams paired but one left with no valid opponent**: Give bye
- **Tournament has fewer than 4 checked-in teams**: Return error, cannot generate Swiss
- **Disputed match**: Do NOT advance to next round until dispute resolved
- **All rounds completed**: Host calls `complete_tournament()` to finalize

### Testing the Algorithm
After implementation, test with:
1. 4 teams → 2 rounds, verify all teams play each other exactly once
2. 8 teams → 3 rounds, verify no rematches
3. Odd team count → verify bye assignment
4. BO2 draw → verify 1 point each
5. Tiebreaker calculation → verify Buchholz and Sonneborn-Berger scores

## Voice Triggers
"swiss pairing", "tournament bracket", "swiss algorithm"
