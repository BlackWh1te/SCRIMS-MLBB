# Tournament Host Authentication

Implement the separate auth system for tournament hosts in the Admin Panel.

## Plan Reference
Read `tournamentwork.md` at the project root. Sections 5.3, 5.4, and 9.1 contain host auth architecture.

## Rules

### Before Writing Any Code
1. Read `tournamentwork.md` sections 5.3, 5.4, 9.1 completely.
2. Read the existing admin auth system:
   - `AdminPanel/src/contexts/AuthContext.tsx` — admin auth (hardcoded credentials)
   - `AdminPanel/src/lib/auth.ts` — hardcoded admin credentials
   - `AdminPanel/src/app/login/page.tsx` — admin login page
   - `AdminPanel/src/middleware.ts` — current middleware
3. Understand: Host auth is COMPLETELY SEPARATE from admin auth.

### Architecture Overview

```
Admin Auth (existing):
  - Hardcoded credentials (CAST/Flam) in auth.ts
  - Session in localStorage/sessionStorage/cookies/IndexedDB
  - Middleware: no server-side auth check, just security headers
  - Routes: /login → /dashboard/*

Host Auth (new):
  - Supabase Auth (real auth users created via admin API)
  - Session managed by Supabase Auth SDK
  - Middleware: verify Supabase Auth session for /host/* routes
  - Routes: /host/login → /host/[tournamentId]/*
```

### Implementation Steps

#### 1. Create `HostAuthContext.tsx`
```
- Uses @supabase/supabase-js client (NOT service role)
- signInWithPassword(email, password) for host login
- signOut() for host logout
- onAuthStateChange listener for session persistence
- Exposes: isHostAuthenticated, hostUser, hostProfile, hostLogin, hostLogout
```

#### 2. Create `/host/login/page.tsx`
```
- Email + password form (NOT username)
- Uses HostAuthContext for login
- On success: redirect to /host/[tournamentId]/dashboard
  (tournamentId comes from tournament_host_accounts table)
- Styled same as admin login but with different branding
  "Tournament Host Panel" instead of "Admin Panel"
```

#### 3. Create `/host/layout.tsx`
```
- Wraps children with HostAuthContext provider
- If not authenticated: redirect to /host/login
- If authenticated but no tournament: show "No tournament assigned"
- Separate sidebar with host-specific nav items
- NO access to /dashboard/* admin routes
```

#### 4. Create API Route for Host Account Creation
`src/app/api/tournament-host/create-account/route.ts`
```
- Server-only route (uses SUPABASE_SERVICE_ROLE_KEY from env, NOT NEXT_PUBLIC_)
- POST handler:
  1. Verify caller is admin (check admin session)
  2. Get tournament_id from request body
  3. Generate email: tournamenthostNNN@mlbbhost.com
  4. Generate random 10-char password
  5. Call Supabase Auth admin.createUser({ email, password, email_confirm: true })
  6. Store auth_user_id in tournament_host_accounts
  7. Return { email, password } to caller (shown to host ONCE)
  8. NEVER store password_plain in the database
```

#### 5. Update Middleware
```typescript
// In middleware.ts, add host route handling:
if (request.nextUrl.pathname.startsWith('/host')) {
  // Skip /host/login (public)
  if (request.nextUrl.pathname === '/host/login') {
    return response; // Allow through
  }

  // Check for Supabase Auth session cookie
  const supabaseSessionCookie = request.cookies.get('sb-efhbyrhxtsadbqjsfogc-auth-token');
  if (!supabaseSessionCookie) {
    return NextResponse.redirect(new URL('/host/login', request.url));
  }

  // Add security headers and continue
  return response;
}
```

### Security Rules (CRITICAL)
- NEVER use `SUPABASE_SERVICE_ROLE_KEY` in client-side code
- NEVER store host passwords in the database
- Host accounts use Supabase Auth, NOT the hardcoded admin system
- Host can ONLY access their own tournament data (enforced by RLS)
- Host session cookies are separate from admin session cookies
- Rate limit host login attempts (5 per minute max)

### Host Account Lifecycle
1. Host creates tournament in Android app
2. Android app calls API to create host account
3. API creates Supabase Auth user + stores metadata
4. API returns email + password to Android app
5. Android app shows credentials to host (in TournamentHostPanelScreen)
6. Host uses credentials to login at /host/login in browser
7. Password is NEVER stored — if host forgets, admin must reset via API

## Voice Triggers
"host auth", "tournament host login", "host authentication"
