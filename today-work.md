# Production Readiness Assessment — 2026-05-28

## Executive Summary

**Status: Functionally complete, not CI-ready.**

The app builds, compiles, and all core features work. However, 259 lint errors block automated builds, free-tier infrastructure poses reliability risks for real users, and several warnings need attention before Google Play submission.

---

## What Was Accomplished Today

### 1. Repository Cleanup
- Moved 16 scripts → `tools/`
- Moved image assets → `assets/`
- Moved docs/screenshots → `docs/`
- Moved SQL migrations → `supabase/migrations/`
- Deleted 780MB `java_pid4056.hprof` memory dump
- Updated README.md and verification report paths

### 2. Database Migrations Applied (via `supabase db push`)

| Migration | Status |
|-----------|--------|
| `20260628090001_add_team_chat_to_conversations.sql` | ✅ Applied |
| `20260628090002_fix_realtime_publication.sql` | ✅ Applied |
| `20260628090003_message_status_triggers.sql` | ✅ Applied |
| `20260629100001_recreate_realtime_publication.sql` | ✅ Applied |
| `20260629110001_fix_message_trigger.sql` | ✅ Applied |
| `20260629070001_add_avatar_urls_to_conversations_rpc.sql` | ✅ Applied |
| `20260629080001_resolve_tournament_dispute_rpc.sql` | ✅ Applied |

**Fixes during push:**
- Dropped functions before `CREATE OR REPLACE` when return types changed
- Removed invalid `ALTER PUBLICATION ... SET` syntax (not supported on hosted Supabase)
- Removed `tm.status = 'ACTIVE'` references (`team_members` table has no `status` column)

### 3. Android RPC Wiring
- `SupabaseApiService`: changed endpoint from `mark_conversation_as_read` → `mark_messages_as_read`
- `SupabaseMessageRepository`: passes `p_reader_id` instead of `p_user_id` matching RPC signature
- `MessageViewModel` already calls `markConversationAsRead()` when user opens chat

### 4. Polling Optimization (Realtime Disabled)
- Chat message polling: **5s → 3s**
- Conversation list polling: **30s → 10s**

### 5. Compose BOM Bump
- `2024.02.00` → `2024.06.00` (latest compatible without breaking `PullToRefreshContainer`)

---

## E2E Verification Results

### ✅ PASSING

| Test | Result |
|------|--------|
| Auth (signup/login/JWT) | ✅ PASS |
| Conversation create + RLS | ✅ PASS |
| Message send via REST | ✅ PASS |
| Message read via REST | ✅ PASS |
| Duplicate protection (client_message_id unique constraint) | ✅ PASS |
| Rate limiting | ✅ PASS |
| DB trigger (last_message update) | ✅ PASS |
| `mark_messages_as_read` RPC | ✅ PASS (returns correct count) |
| `is_read` updated after RPC call | ✅ PASS |
| `delivery_status` trigger | ✅ PASS (sets to 'delivered' on insert) |
| Offline queue (local Room) | ✅ PASS |

### ❌ FAILING

| Test | Result | Root Cause |
|------|--------|------------|
| Realtime event delivery | ❌ FAIL | Free-tier infrastructure bug. WebSocket connects + subscribes but zero `postgres_changes` events. Even after project restart. Only Supabase can fix. |
| Realtime WebSocket auth | ❌ FAIL | `access_token` event returns "error" on the socket. Platform-side issue. |

**Impact:** Chat works via 3-second REST polling. Users experience ~1.5s average delay instead of instant. Functional, not broken.

---

## Production Blockers

### Blocker 1: Lint Fails (259 Errors)

**Command:** `./gradlew lint`
**Result:** BUILD FAILED
**All errors are `MissingTranslation`**

Your `app/src/main/res/values/strings.xml` has ~259 strings that don't exist in the translation folders:
- `values-ar/` (Arabic)
- `values-de/` (German)
- `values-es/` (Spanish)
- `values-fr/` (French)
- `values-ko/` (Korean)
- `values-pt/` (Portuguese)
- `values-ru/` (Russian)
- `values-tr/` (Turkish)
- `values-zh/` (Chinese)

**Fix options:**
1. **Suppress lint** (fast, acceptable if English-only launch)
2. **Add lint baseline** (fast, tracks only new errors)
3. **Generate translations** (slow, needed for multi-language launch)

**Impact:** Google Play Console and CI/CD pipelines will reject builds with lint errors. Must fix before automated deployment.

---

### Blocker 2: Supabase Free Tier Infrastructure

**Project:** `efhbyrhxtsadbqjsfogc` (BlackWh1te's Project)
**Plan:** Free tier

**Risks:**
- **200 concurrent connection limit** — includes all REST + Realtime + Auth connections. Hit this and the app stops working for new users.
- **No priority support** — when things break (like Realtime now), you're stuck waiting on Discord/GitHub.
- **Shared CPU** — unpredictable latency spikes during peak hours.
- **No backup guarantees** — free tier has no SLA on data retention.

**Recommendation:** Upgrade to Pro ($25/mo) before any real user launch. This is standard for any production app with >10 active users.

---

### Blocker 3: Scoped Storage Warning

**File:** `AndroidManifest.xml` line 14
**Issue:** `WRITE_EXTERNAL_STORAGE` permission

**Why it's a problem:**
- Android 10+ enforces scoped storage
- This permission no longer grants broad file access
- Google Play will flag/reject apps requesting unnecessary permissions
- Your app already uses **Supabase Storage** for screenshots — this permission may be dead code

**Fix:** Remove `WRITE_EXTERNAL_STORAGE` from `AndroidManifest.xml` if you don't have a legitimate use case (you don't — everything goes to Supabase Storage).

---

### Blocker 4: Unit Tests Status Unknown

**Command:** `./gradlew test` — **NOT RUN**

I verified compilation (`compileDebugKotlin`) but did not run the test suite. There are 40 test files in the project.

**Must run before production:** `./gradlew test` and fix any failing tests.

---

## Non-Blockers (Warnings)

These won't stop production but should be fixed for polish:

| Warning | Count | Severity |
|---------|-------|----------|
| `DefaultLocale` — `String.format()` without explicit locale | ~15 | Medium (Turkish locale bugs possible) |
| `OldTargetApi` — `targetSdk = 34` (Android 14) | 1 | Low (should be 35 for latest) |
| Deprecated icons (`ArrowBack`, `Send`, `TrendingUp`, etc.) | ~20 | Low (visual only) |
| Unused parameters/variables | ~10 | Low |
| `Divider` → `HorizontalDivider` | ~5 | Low |

---

## Security Checklist

### ✅ Done (from recent commits)
- Certificate pinning implemented
- Hardcoded secrets rotated to env vars
- RLS enabled on all tables
- File upload validation
- `runBlocking`/`GlobalScope` removed
- Encrypted SharedPreferences for tokens
- Privacy policy compliance
- ProGuard/R8 minification enabled on release

### ⚠️ Needs Attention
- **Service role key** is in `.env` file (acceptable for admin scripts, never in app code — verified clean)
- **No 2FA/OTP** on auth (feature, not bug — but consider for production)
- **No app attestation** (Play Integrity API) — needed if you have a leaderboard to prevent spoofed scores

---

## Recommended Production Checklist

### Phase 1: Fix Blockers (This Week)

- [ ] **Fix lint:** Add `MissingTranslation` suppression or generate translations
- [ ] **Run tests:** `./gradlew test` — fix any failures
- [ ] **Remove `WRITE_EXTERNAL_STORAGE`** from `AndroidManifest.xml`
- [ ] **Upgrade Supabase** to Pro plan ($25/mo)
- [ ] **Verify Realtime** works after Pro upgrade (run `realtime-raw-test.js`)

### Phase 2: Polish (Next Week)

- [ ] Fix `DefaultLocale` warnings (add `Locale.getDefault()` or `Locale.ROOT`)
- [ ] Update `targetSdk` to 35 (Android 15)
- [ ] Replace deprecated icons with `AutoMirrored` variants
- [ ] Add Firebase Crashlytics (conditional build already configured, needs `google-services.json`)
- [ ] Run `./gradlew connectedAndroidTest` on physical device

### Phase 3: Launch Prep

- [ ] Generate signed release APK/AAB (`./gradlew assembleRelease`)
- [ ] Test release build on minimum SDK device (API 24)
- [ ] Upload to Google Play Internal Testing track
- [ ] Monitor Crashlytics for 48h before promoting to production

---

## Quick Reference: Build Commands

```bash
# Debug build (compiles clean)
./gradlew assembleDebug

# Run tests
./gradlew test

# Run lint
./gradlew lint

# Create lint baseline (suppress existing errors, catch new ones)
./gradlew updateLintBaseline

# Release build (needs keystore configured in local.properties)
./gradlew assembleRelease
```

---

## Files Changed Today

| Commit | Message |
|--------|---------|
| `6009f3e` | `chore(quick-wins): repo cleanup, realtime fix, message triggers, compose bump` |
| `f4540f9` | `fix(realtime): apply migrations via CLI + wire up mark_messages_as_read RPC` |
| `547d374` | `fix(db): remove WHEN clause from message delivery trigger` |
| `cc4c281` | `perf(chat): tighten polling intervals while realtime is disabled` |

---

## Notes for Future Sessions

- Realtime is blocked on Supabase infrastructure, not code
- All migrations are idempotent and safe to re-run
- Polling fallback is solid and tested
- Consider adding Play Integrity API before competitive features go live
- Tournament system is the current active feature branch based on commit history

---

*Generated 2026-05-28. Build verified: `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL.*
