# Privacy Policy — Scrims Legends

Last updated: 2026-05-28

## Data We Collect

- **Account data**: email, username, in-game ID (required for matching)
- **Team data**: team name, member list, scrim history
- **Device data**: country/city (from IP, for region-based matchmaking), language preference
- **Crash data**: ANR and fatal crash stack traces (collected only if Firebase Crashlytics is enabled in a future release)

## How We Use Data

- Authentication and matchmaking
- Leaderboard and ranking calculations
- Push notifications for scrim invites and messages
- Fraud detection and security monitoring

## Data Storage

- Supabase (PostgreSQL) with Row Level Security
- Encrypted local storage for auth tokens (AES-256-GCM)

## Third-Party Services

- **Supabase**: database, auth, storage, realtime
- **ML Kit**: on-device translation (no data sent to Google)

## Your Rights

You may delete your account at any time from Profile > Delete Account. This marks your profile as deleted and queues auth deletion.

## Contact

For privacy questions, contact the development team at support@scrimslegends.app.
