# Changelog

## 1.1.1 (2026-05-28)

APK version:
- `versionName`: `1.1.1`
- `versionCode`: `3`

Included in this release:
- Disabled the News entry points in app navigation because this feature is not being used right now.
- Updated Android configuration for current SDK targets and Android 13 notification permission handling.
- Removed placeholder external news API keys from `BuildConfig`.
- Tightened network security config by removing placeholder certificate pinning until real production pins are available.
- Fixed mock auth password verification for email and password changes.
- Fixed rank XP progress overflow handling at very high XP values.
- Fixed cancellation handling in mock team and match result repositories.
- Fixed pending match reporter resolution when team IDs are blank.
- Updated and aligned unit tests with current app behavior and data models.

Verification:
- `.\gradlew.bat --no-daemon :app:testDebugUnitTest`
- `.\gradlew.bat --no-daemon :app:testReleaseUnitTest`
- `.\gradlew.bat --no-daemon test`
