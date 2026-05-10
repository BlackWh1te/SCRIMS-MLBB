# MLBB Scrim Host

An Android app for hosting and managing scrims (practice matches) for Mobile Legends: Bang Bang (MLBB). Built with Kotlin, Jetpack Compose, and Supabase.

## Features

- **User Authentication**: Email/password registration and login
- **Team Management**: Create teams, invite players (3-7 members), manage team settings
- **Scrim Scheduling**: Post available scrim slots with date/time
- **Scrim Search**: Find and apply to scrims from other teams
- **Real-time Chat**: Communicate with opposing team leaders
- **Match Verification**: Upload game screenshots for admin verification
- **XP & Ranking System**: 7-tier ranking system (Bronze to Grandmaster)
- **Admin Dashboard**: Web interface for verifying match results

## Tech Stack

- **Frontend**: Kotlin + Jetpack Compose
- **Backend**: Supabase (PostgreSQL, Auth, Storage, Realtime)
- **Admin Dashboard**: React/Next.js (planned)

## Design System

The app features an MLBB-themed design with:
- Epic gaming fantasy aesthetic
- Official MLBB colors (Gold, Blue, Dark Blue)
- Gaming fonts (Rajdhani, Roboto, Teko)
- Dark mode with gold accents
- Card-based layouts with tier badges

See [DESIGN.md](DESIGN.md) for complete design system documentation.

## Project Structure

```
Android/
├── app/
│   ├── src/main/java/com/mlbb/scrim/
│   │   ├── data/
│   │   │   ├── model/          # Data models (Profile, Team, Scrim, Match, etc.)
│   │   │   ├── repository/     # Repository classes for data access
│   │   │   └── service/        # Supabase client and services
│   │   ├── ui/
│   │   │   ├── auth/           # Login, registration screens
│   │   │   ├── team/           # Team management screens
│   │   │   ├── scrim/          # Scrim search and posting
│   │   │   ├── match/          # Match details and chat
│   │   │   ├── profile/        # User profile
│   │   │   └── leaderboard/    # Rankings and tiers
│   │   └── viewmodel/          # ViewModels for UI state
│   └── build.gradle.kts
├── supabase/
│   ├── schema.sql              # Database table definitions
│   ├── triggers.sql            # Database triggers (profile sync, XP calculation)
│   ├── rls_policies.sql        # Row Level Security policies
│   ├── functions.sql           # Custom SQL functions
│   └── seed_data.sql           # Initial data setup
├── DESIGN.md                   # Design system documentation
└── CLAUDE.md                   # Project notes and configuration
```

## Setup Instructions

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Supabase account (free tier works)

### 1. Supabase Setup

1. Create a new project at [supabase.com](https://supabase.com)
2. Go to the SQL Editor in your Supabase dashboard
3. Run the SQL files in order:
   - `supabase/schema.sql` (creates all tables)
   - `supabase/triggers.sql` (creates triggers for profile sync and XP calculation)
   - `supabase/rls_policies.sql` (sets up security policies)
   - `supabase/functions.sql` (creates custom functions)

4. Create an admin user:
   - Go to Authentication → Users
   - Create a new user with email/password
   - Go to the SQL Editor and run:
     ```sql
     UPDATE profiles SET is_admin = true WHERE email = 'your-admin@email.com';
     ```

5. Create a Storage bucket for screenshots:
   - Go to Storage → Create a new bucket
   - Name it `screenshots`
   - Make it public (or configure RLS policies)

6. Get your Supabase credentials:
   - Go to Project Settings → API
   - Copy the Project URL and Anon Key

### 2. Android Project Setup

1. Open the project in Android Studio
2. Update Supabase credentials in `app/src/main/java/com/mlbb/scrim/data/service/SupabaseClient.kt`:
   ```kotlin
   private const val SUPABASE_URL = "YOUR_SUPABASE_URL"
   private const val SUPABASE_KEY = "YOUR_SUPABASE_ANON_KEY"
   ```

3. Sync Gradle dependencies
4. Build and run the app

## Database Schema

### Tables

- **profiles**: User profiles (extends Supabase Auth)
- **teams**: Team information and stats
- **team_members**: Team membership with roles
- **scrims**: Available scrim slots posted by teams
- **scrim_applications**: Applications to join scrims
- **matches**: Confirmed matches between teams
- **messages**: Chat messages between team leaders
- **match_results**: Match results with screenshots
- **team_invitations**: Team invitations

See `supabase/schema.sql` for complete schema.

## Current Implementation Status

### Completed ✅
- Project structure and Gradle configuration
- Design system documentation
- Database schema and SQL files
- Data models (Kotlin data classes)
- Supabase client setup
- AuthRepository (login, register, logout)
- TeamRepository (create team, get team, invite players)

### In Progress 🚧
- README documentation
- Authentication UI screens

### Pending ⏳
- Team management UI
- Scrim system (post, search, apply)
- Real-time chat implementation
- Screenshot upload functionality
- XP and ranking system
- Admin web dashboard

## Next Steps

1. **Complete Authentication UI**
   - Login screen with MLBB-themed design
   - Registration screen
   - Session management

2. **Implement Team Management**
   - Team creation screen
   - Team profile screen
   - Member management
   - Team settings

3. **Build Scrim System**
   - Post scrim screen
   - Search scrims screen
   - Scrim details and application flow

4. **Add Real-time Chat**
   - Chat UI
   - Supabase Realtime integration
   - System messages

5. **Implement Screenshot Upload**
   - Image picker
   - Compression and upload to Supabase Storage
   - Preview functionality

6. **Create Admin Dashboard**
   - Simple React/Next.js web app
   - Verification queue
   - Screenshot comparison
   - Winner selection

7. **Polish and Test**
   - Apply design system consistently
   - Add animations and transitions
   - Test all user flows
   - Performance optimization

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Run lint
./gradlew lint

# Clean build
./gradlew clean
```

## Contributing

This is a personal project. Feel free to fork and modify for your own use.

## License

MIT License - feel free to use this code for your own projects.

## Support

For issues or questions, please refer to the project documentation or create an issue in the repository.