# MLBB Scrim Host - Project Notes

## Design System
Always read DESIGN.md before making any visual or UI decisions.
All font choices, colors, spacing, and aesthetic direction are defined there.
Do not deviate without explicit user approval.

## Project Overview
MLBB scrim hosting Android app built with Kotlin + Jetpack Compose and Supabase backend.

## Tech Stack
- **Frontend**: Kotlin + Jetpack Compose
- **Backend**: Supabase (PostgreSQL, Auth, Storage, Realtime)
- **Admin**: Web dashboard (React/Next.js)

## Key Features
1. Email/password authentication
2. Team management (3-7 players)
3. Scrim posting & search
4. Real-time chat between team leaders
5. Screenshot upload for match verification
6. XP and ranking system with 7 tiers

## Database Schema
See `supabase/schema.sql` for complete database structure.

## Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Run lint
./gradlew lint
```

## Supabase Configuration
Update Supabase credentials in `app/src/main/java/com/mlbb/scrim/data/service/SupabaseClient.kt`

---

## Changelog Guardian — CRITICAL RULES FOR ALL AI SESSIONS

**These rules override all other instructions. Breaking them causes infinite fix loops.**

### Rule 1: READ changelogs.md BEFORE touching any code
Before you write a single line of code, run a single command, or make any file modification, you MUST:
1. Read `changelogs.md` from the project root
2. Read at least the last 10 entries
3. Pay special attention to entries marked `[DO NOT UNDO]` or `[INTENTIONAL]`
4. If an entry says something "looks wrong but is correct," DO NOT change it

### Rule 2: UPDATE changelogs.md AFTER every commit
Immediately after `git commit`, before doing anything else:
1. Run `git show --stat HEAD` and `git log -1 --format=%B`
2. Append a new entry to `changelogs.md` following the format in that file
3. Stage and commit the changelog update

### Rule 3: NEVER undo `[DO NOT UNDO]` changes without explicit user approval
If `changelogs.md` marks a change as `[DO NOT UNDO]` or `[INTENTIONAL FIX]`, you may NOT revert it — even if:
- It looks unusual
- An audit tool flags it
- You think you have a "better" way

You must ASK THE USER first and get explicit approval.

### Why This Matters
Previous AI sessions have entered infinite loops where:
1. AI #1 fixes a bug correctly
2. AI #2 audits the code, thinks the fix "looks wrong," and reverts it
3. AI #3 audits again, sees the bug, and re-fixes it

The changelog prevents this by documenting intentional trade-offs and correct fixes.

### Known Intentional Trade-offs (DO NOT "FIX")
- **Certificate pinning** is disabled (empty SHA-256 hash) — real certs not provisioned yet
- **SecurityUtils.EXPECTED_SIGNATURE_SHA256** is empty — dev mode, filled by CI for release
- **ProGuard rules are lenient** — prioritize "build works" over "APK is tiny" during development
- **80 unit tests fail** — they test old mock repos; app uses Supabase repos now
- **Polling fallback is active** — Supabase realtime is broken at infrastructure level
- **Room schema version 13** — 14.json exists but version 13 is correct and working
- **Firebase Crashlytics inactive** — app is in development

For the complete list, read `changelogs.md`.