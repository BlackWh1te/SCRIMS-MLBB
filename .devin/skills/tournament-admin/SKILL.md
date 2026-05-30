# Tournament Admin Panel (Next.js)

Build the admin panel pages for tournament management in the AdminPanel project.

## Plan Reference
Read `tournamentwork.md` at the Android project root. Sections 5 and 7 contain admin panel changes and notification types.

## Project Location
The AdminPanel is at `C:\Users\Shukhrat\Desktop\New folder\git\AdminPanel`.

## Rules

### Before Writing Any Code
1. Read `tournamentwork.md` sections 5 and 7 completely.
2. Read the AdminPanel's existing patterns:
   - `src/app/dashboard/users/page.tsx` — reference for list pages
   - `src/app/dashboard/scrims/page.tsx` — reference for review/approval pages
   - `src/components/DashboardLayout.tsx` — navigation structure
   - `src/lib/security.ts` — SecureQueryBuilder whitelist
   - `src/locales/en.ts` and `src/locales/ru.ts` — i18n pattern
   - `src/types/database.ts` — type definitions
3. Read `AGENTS.md` at the AdminPanel root — it warns about Next.js 16 breaking changes. Read `node_modules/next/dist/docs/` before writing code.

### Pages to Build

#### Admin Pages (under `/dashboard/`)
1. **`/dashboard/tournament-requests/page.tsx`** — Review host requests
   - List all `tournament_host_requests` with status badges (pending/approved/rejected)
   - Approve/reject with admin notes textarea
   - On approve: call RPC to set `profiles.is_tournament_host = TRUE`, send notification
   - On reject: send notification with reason
   - Follow existing pattern from `appeals/page.tsx`

2. **`/dashboard/tournaments/page.tsx`** — Admin oversight of ALL tournaments
   - List all tournaments with status, host info, team count
   - Flag/unflag tournaments
   - Force-cancel abusive tournaments
   - View Swiss bracket for any tournament
   - Admin override on disputed matches

#### Host Pages (under `/host/` — SEPARATE auth system)
3. **`/host/login/page.tsx`** — Host login (Supabase Auth, NOT admin credentials)
4. **`/host/layout.tsx`** — Host layout with sidebar (different from admin)
5. **`/host/[tournamentId]/dashboard/page.tsx`** — Main host dashboard
6. **`/host/[tournamentId]/applications/page.tsx`** — Review team applications
7. **`/host/[tournamentId]/bracket/page.tsx`** — Swiss bracket view + management
8. **`/host/[tournamentId]/matches/page.tsx`** — Match scheduling + results
9. **`/host/[tournamentId]/settings/page.tsx`** — Edit tournament details

### Architecture for Host Auth
- Create `src/contexts/HostAuthContext.tsx` — uses Supabase Auth (NOT `validateAdminCredentials`)
- Create `src/components/HostLayout.tsx` — separate layout for host pages
- Host login authenticates via Supabase Auth `signInWithPassword` with the auto-generated email
- Host session stored separately from admin session
- Middleware update: `/host/*` routes check Supabase Auth session, NOT admin cookies

### API Route for Host Account Creation
- `src/app/api/tournament-host/create-account/route.ts`
- Server-only route (uses `SUPABASE_SERVICE_ROLE_KEY` — NOT `NEXT_PUBLIC_`)
- Creates Supabase Auth user via admin API
- Returns credentials to host ONCE (never stored in DB)
- Follows security rules in tournamentwork.md Section 9

### Security Updates
- Update `src/lib/security.ts` SecureQueryBuilder whitelist to include all tournament tables
- Update `src/middleware.ts` to handle `/host/*` routes with separate auth
- NEVER use service role key client-side

### i18n
- Add all tournament strings to `src/locales/en.ts` and `src/locales/ru.ts`
- Follow existing key naming pattern

### Types
- Add tournament types to `src/types/database.ts`
- Follow existing interface pattern (Profile, Team, etc.)

### UI Style
- Follow existing glassmorphic dark theme (`glass`, `premium-card`, `premium-button` CSS classes)
- Use Framer Motion for animations (existing pattern)
- Use Lucide icons (existing pattern)
- Use Radix UI primitives (existing pattern)

## Voice Triggers
"tournament admin", "tournament admin panel", "host dashboard"
