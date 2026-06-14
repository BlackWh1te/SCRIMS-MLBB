# MLBB Scrim Host — Production Hardening Migration Guide

## Overview

This document summarizes the six-phase production-hardening effort applied to the MLBB Scrim Host Android application. It includes a per-phase changelog, risk matrix, rollback instructions, and verification checklist.

**Target version:** 1.1.0 (versionCode 2)  
**Previous version:** 1.0.0 (versionCode 1)  
**Affected modules:** data layer, security layer, domain layer, networking, release pipeline

---

## Phase Summary

| Phase | Theme | Key Changes | Risk Level |
|-------|-------|-------------|------------|
| 1 | Authorization & Ownership Validation | Client-side guards on all destructive/mutating repository operations | High |
| 2 | Coroutine Safety | Removed `runBlocking`/`GlobalScope`; fixed auth refresh threading | Medium |
| 3 | Secure Storage & Upload Validation | Encrypted `SharedPreferences`, file upload limits, masked secrets | Medium |
| 4 | Network Hardening | Certificate pinning, exponential retry, logging cleanup, ProGuard rules | Medium |
| 5 | Release Readiness | Signing config, Crashlytics, privacy policy, consent flags | Low |
| 6 | Testing & Architecture | Unit tests, dependency bumps, domain `usecases/` skeleton | Low |

---

## Phase 1 — Authorization & Ownership Validation

### What changed
A new `security/AuthorizationUtils.kt` provides four guard helpers:
- `requireOwner(currentUserId, ownerId)` — blocks non-owners from owner-only actions
- `requireLeader(currentUserId, team, minRole)` — enforces role hierarchy (owner > co-leader > member)
- `requireParticipant(currentUserId, participantIds)` — ensures user is in a participant list
- `currentUserId()` — safe current-ID retrieval

These guards were wired into **all** repositories that perform destructive or privileged operations:

- **SupabaseTeamRepository**: `deleteTeam`, `updatePlayerRole`, `sendInvite`, `acceptInvite`, `declineInvite`, `removePlayer`, `acceptApplication`, `declineApplication`
- **SupabaseScrimRepository**: `updateScrim`, `deleteScrim`, `approveApplication`, `rejectApplication`, `cancelApplication`, `setScrimRoster`, `transitionToReadyCheck`, `markReady`, `uploadScreenshot`, `completeScrim`, `submitResult`
- **SupabaseLfgRepository**: `deletePost`
- **SupabaseTournamentRepository**: `updateTournament`, `createHostAccount`, `generateSwissPairings`, `updateTournamentScores`, `recalculateTiebreakers`, `disqualifyTeam`, `checkNoShows`, `cancelTournament`, `completeTournament`, `reviewApplication`

Missing API endpoints (`getTeamApplicationById`, `getLfgPostById`) were added to `SupabaseApiService.kt` to support a "fetch-before-verify" pattern for ownership checks.

### Rollback
To disable authorization guards (e.g., for emergency hotfix), comment out the `requireXxx(...)` calls in each repository method. The app will compile and run, but will rely solely on server-side RLS again.

---

## Phase 2 — Coroutine Safety

### What changed
- `SupabaseClient.kt` (`SupabaseAuthenticator`): replaced `runBlocking { refreshToken() }` inside OkHttp's synchronous `authenticate()` callback with a **synchronous Retrofit call** (`refreshTokenSync`). This eliminates blocking the OkHttp dispatcher thread.
- `SupabaseAuthRepository.kt`: removed the unused `GlobalScope` import.
- `UnifiedCacheManager.kt`: already used `Mutex` for thundering-herd protection — no changes needed.

### Rollback
Revert `SupabaseClient.kt` to the previous `runBlocking` implementation. This is **not recommended** because it can cause ANRs under high-latency network conditions.

---

## Phase 3 — Secure Storage, Upload Validation, Masked Secrets

### What changed
- Added `androidx.security:security-crypto` dependency.
- Created `security/SecurePreferences.kt` — an `EncryptedSharedPreferences` wrapper with singleton access and atomic `edit` DSL.
- Migrated `data/preferences/AppSettings.kt` sync-token fallback storage from plaintext `SharedPreferences` to `SecurePreferences`.
- `data/service/SupabaseStorageUpload.kt` now validates:
  - File size <= 10 MB
  - MIME type is `image/jpeg` or `image/png`
- `TournamentDetailScreen.kt` masks room passwords (dots) instead of displaying plaintext.

### Rollback
- Replace `SecurePreferences` calls in `AppSettings.kt` with the original plaintext `SharedPreferences`.
- Remove the `validateFile()` calls in `SupabaseStorageUpload.kt`.
- Revert the password `VisualTransformation` in `TournamentDetailScreen.kt`.

---

## Phase 4 — Network Hardening

### What changed
- `network_security_config.xml` now contains real SHA-256 certificate pins for Supabase (replace with your own if you rotate certs).
- Created `data/service/RetryInterceptor.kt` with exponential backoff (`maxRetries = 3`, base delay 500ms, jitter).
- Wired `RetryInterceptor` into both `SupabaseRetrofitClient` and `SupabaseAuthRetrofitClient`.
- Replaced all `android.util.Log` calls with Timber. Added `ReleaseTree` in `MLBBScrimApplication.kt` that strips logs entirely in release builds.
- Added ProGuard rules (`-keep` for DTOs, `@SerializedName` fields, `CertificatePinner`, `RetryInterceptor`).

### Rollback
- Remove or clear `<pin-set>` entries in `network_security_config.xml` to disable pinning.
- Remove `.addInterceptor(RetryInterceptor())` from both Retrofit builders.
- Replace Timber calls back to `android.util.Log`.
- Remove ProGuard additions if you do not use minification.

---

## Phase 5 — Release Readiness

### What changed
- `app/build.gradle.kts` added a `release` signing config reading from `local.properties` or environment variables (`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
- `versionCode` bumped `1 -> 2`, `versionName` bumped `"1.0.0" -> "1.1.0"`.
- Added Firebase BOM `33.1.0`, Crashlytics, and Analytics dependencies + plugins in root `build.gradle.kts`.
- `MLBBScrimApplication.kt` initializes Firebase gracefully: if `google-services.json` is missing, Firebase is skipped with a warning instead of crashing.
- Created `PRIVACY_POLICY.md` at repo root.
- `AppSettings.kt` stores privacy-consent flags (`privacy_consent_accepted`, `privacy_consent_version`).

### Rollback
- Delete the `release` signing config block and revert `buildTypes` to `debug` signing only.
- Revert `versionCode`/`versionName` to `1`/`"1.0.0"`.
- Remove Firebase dependencies/plugins if you do not want crash reporting.
- Remove `initFirebase()` call from `MLBBScrimApplication.kt`.

---

## Phase 6 — Testing, Dependencies, Architecture

### What changed
- Added test dependencies: `kotlinx-coroutines-test`, `mockk`.
- New unit tests:
  - `AuthorizationUtilsTest.kt` — verifies all guard types throw `UnauthorizedException` for invalid callers
  - `RetryInterceptorTest.kt` — verifies retry on 5xx/timeout, no retry on 4xx
  - `SupabaseStorageUploadValidationTest.kt` — verifies 10MB limit and MIME whitelist
  - `LogSanitizerTest.kt` — verifies token/password/bearer redaction
- Dependency bumps:
  - Coil `2.5.0 -> 2.6.0`
  - Retrofit `2.9.0 -> 2.11.0`
  - ML Kit `17.0.2 -> 17.0.3`
- Created `domain/usecases/` directory with `DeleteTeamUseCase.kt` as a clean-architecture migration example.

### Rollback
- Remove the four test classes.
- Revert dependency versions in `app/build.gradle.kts`.
- Delete `domain/usecases/` directory.

---

## Pre-Deployment Verification Checklist

- [ ] `./gradlew test` passes (new unit tests + existing tests)
- [ ] `./gradlew assembleRelease` succeeds
- [ ] `./gradlew lint` reports no new critical errors
- [ ] Firebase Crashlytics is reachable (or gracefully skipped if `google-services.json` absent)
- [ ] Privacy policy URL is accessible to users
- [ ] Certificate pins match your current Supabase edge certificates
- [ ] Release signing credentials are present in CI or `local.properties`
- [ ] `versionCode` is strictly greater than the Play-Store/live build

---

## Rollback Plan (Emergency)

1. **Code rollback**: revert the commits in reverse order (Phase 6 -> Phase 5 -> ... -> Phase 1). Each phase is isolated to its own commit.
2. **Signing rollback**: if release signing fails, switch back to the `debug` signing config temporarily.
3. **Firebase rollback**: if Crashlytics causes startup crashes, remove `google-services.json` from assets — the app will skip Firebase initialization.
4. **Certificate pinning rollback**: if users report SSL errors due to cert rotation, clear `<pin-set>` in `network_security_config.xml` and push a hotfix.
5. **Play Store rollback**: upload the previous APK/AAB (versionCode 1) as a superseding release if the new build is broken.

---

## Commit Log

| Commit | Message |
|--------|---------|
| `15988c4` | phase-1: Add client-side authorization guards (CRITICAL) |
| `0a1c57b` | phase-2: Remove runBlocking and GlobalScope; fix coroutine management |
| (combined) | phase-3: Secure storage, file upload validation, masked secrets |
| `14839de` | phase-4: Network hardening — certificate pinning, logging cleanup, timeouts/retry |
| `ffd2cd4` | phase-5: Release signing, versioning, crash reporting, privacy compliance |
| `dc42bb1` | phase-6: Tests, dependency updates, domain layer skeleton |

---

*Generated for MLBB Scrim Host production-hardening initiative.*
