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