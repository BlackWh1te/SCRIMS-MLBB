# Tournament Database Schema & Migrations

Build the Supabase database layer for the tournament system.

## Plan Reference
Read `tournamentwork.md` at the project root before doing anything. Sections 1-4 contain the complete schema, RLS policies, RPC functions, triggers, and constraints.

## Rules

### Before Writing Any SQL
1. Read the FULL existing schema at `supabase/schema.sql` to understand current tables, indexes, and constraints.
2. Read `tournamentwork.md` sections 1-4 completely.
3. Check Section 9 "SECURITY & OPTIMIZATION REQUIREMENTS" — it overrides older sections.

### Migration File
- Write a SINGLE migration file: `supabase/migrations/20240601_tournament_system.sql`
- Include ALL tables, RLS policies, RPC functions, triggers, constraints, and indexes in one file.
- Use `IF NOT EXISTS` / `DO $$ ... IF NOT EXISTS` guards so the migration is idempotent.
- End with `NOTIFY pgrst, 'reload schema';` so PostgREST picks up changes.

### Table Creation Order (respects foreign keys)
1. `profiles` ALTER (add columns) — no FK deps
2. `tournament_host_requests` — depends on profiles
3. `tournaments` — depends on profiles
4. `tournament_requirements` — depends on tournaments
5. `tournament_applications` — depends on tournaments, teams
6. `tournament_teams` — depends on tournaments, teams
7. `tournament_swiss_matches` — depends on tournaments, teams, conversations
8. `tournament_match_rosters` — depends on tournament_swiss_matches, teams, profiles
9. `tournament_host_accounts` — depends on tournaments, profiles, auth.users
10. `tournament_player_stats` — depends on tournaments, profiles, teams
11. `conversation_participants` — depends on conversations, profiles
12. `tournament_match_room_secrets` — depends on tournament_swiss_matches
13. RLS policies — after all tables
14. RPC functions — after all tables + RLS
15. Triggers — after all tables + functions
16. Indexes — last
17. Realtime publication — last

### Critical Security Rules (from tournamentwork.md Section 9)
- Do NOT store generated host passwords in any database table. Host account creation happens server-side only.
- Do NOT expose `SUPABASE_SERVICE_ROLE_KEY` through `NEXT_PUBLIC_*` env vars.
- Use `conversation_participants` table (NOT participant_c columns on conversations).
- Sensitive fields (`room_password`, dispute notes, admin overrides) go in `tournament_match_room_secrets` with strict RLS.
- All mutations go through RPCs with explicit caller checks.

### RPC Function Standards
- Every RPC that mutates data must use `SECURITY DEFINER` and validate `auth.uid()` inside the function body.
- Return `json_build_object('success', boolean, 'error', text)` for all RPCs.
- Never do broad UPDATE/DELETE inside RPCs — always filter by IDs passed as parameters.

### Testing
After writing the migration, verify it by:
1. Reading the SQL file end-to-end for syntax errors
2. Checking all FK references point to existing tables
3. Checking all RLS policies reference correct column names
4. Checking all trigger functions match their trigger declarations

## Voice Triggers
"tournament database", "tournament migration", "tournament schema"
