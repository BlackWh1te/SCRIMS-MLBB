# Tournament Android Screens (Kotlin + Jetpack Compose)

Build the Android APK tournament screens, data layer, and navigation.

## Plan Reference
Read `tournamentwork.md` at the project root. Sections 6 and 7 contain Android changes and notification types.

## Rules

### Before Writing Any Code
1. Read `tournamentwork.md` sections 6 and 7 completely.
2. Read the existing patterns in this project:
   - `data/model/Scrim.kt` — reference for data model pattern
   - `data/repository/SupabaseScrimRepository.kt` — reference for repository pattern
   - `data/service/SupabaseClient.kt` — table constants and REST API setup
   - `viewmodel/ScrimViewModel.kt` — reference for ViewModel pattern
   - `ui/screens/ScrimListScreen.kt` — reference for list screen pattern
   - `ui/screens/ScrimDetailScreen.kt` — reference for detail screen pattern
   - `ui/screens/CreateScrimScreen.kt` — reference for create form pattern
   - `ui/navigation/AuthNavigation.kt` — navigation routes and composable setup
   - `ui/components/AppBottomNav.kt` — bottom navigation tabs
3. Read `DESIGN.md` at the project root for the complete design system (colors, typography, spacing, components, motion).
4. Read `tournament-ui/SKILL.md` for tournament-specific UI/UX patterns.

### Data Layer

#### Models to Create (in `data/model/`)
- `Tournament.kt` — tournament data class
- `TournamentRequirement.kt` — requirement data class
- `TournamentApplication.kt` — application data class
- `TournamentTeam.kt` — tournament team with Swiss scores
- `TournamentSwissMatch.kt` — Swiss match pairing
- `TournamentHostRequest.kt` — host request form data
- `TournamentMatchRoster.kt` — active player selection

Follow the pattern from `Scrim.kt` — data class with computed properties, no logic.

#### Repository (in `data/repository/`)
- `TournamentRepository.kt` — interface (follow `ScrimRepositoryInterface.kt` pattern)
- `SupabaseTournamentRepository.kt` — implementation using REST API

Repository must:
- Use `UnifiedCacheManager` for stale-while-revalidate (existing pattern)
- Call RPC functions via REST API (`/rest/v1/rpc/function_name`)
- Handle Supabase Realtime subscriptions for live updates
- Follow existing error handling pattern

#### ViewModel (in `viewmodel/`)
- `TournamentViewModel.kt` — StateFlow-based (follow `ScrimViewModel.kt` pattern)
- Inject repository via Hilt (existing DI pattern)

### SupabaseClient Update
Add to `SupabaseConfig` in `data/service/SupabaseClient.kt`:
```kotlin
const val TABLE_TOURNAMENTS = "tournaments"
const val TABLE_TOURNAMENT_REQUIREMENTS = "tournament_requirements"
const val TABLE_TOURNAMENT_APPLICATIONS = "tournament_applications"
const val TABLE_TOURNAMENT_TEAMS = "tournament_teams"
const val TABLE_TOURNAMENT_SWISS_MATCHES = "tournament_swiss_matches"
const val TABLE_TOURNAMENT_HOST_REQUESTS = "tournament_host_requests"
const val TABLE_TOURNAMENT_HOST_ACCOUNTS = "tournament_host_accounts"
const val TABLE_TOURNAMENT_PLAYER_STATS = "tournament_player_stats"
const val TABLE_TOURNAMENT_MATCH_ROSTERS = "tournament_match_rosters"
const val TABLE_CONVERSATION_PARTICIPANTS = "conversation_participants"
const val BUCKET_TOURNAMENT_LOGOS = "tournament-logos"
```

### Navigation Routes (add to `AuthNavigation.kt`)
```kotlin
object TournamentList : Screen("tournament_list")
object TournamentDetail : Screen("tournament_detail/{tournamentId}") {
    fun createRoute(tournamentId: String) = "tournament_detail/$tournamentId"
}
object TournamentHostRequest : Screen("tournament_host_request")
object TournamentCreate : Screen("tournament_create")
object TournamentHostPanel : Screen("tournament_host_panel/{tournamentId}") {
    fun createRoute(tournamentId: String) = "tournament_host_panel/$tournamentId"
}
```

### Screens to Create (in `ui/screens/`)

1. **TournamentListScreen** — Browse all tournaments
   - Filter chips: status, prize_type, region, skill_level
   - Card-based list with tournament logo, title, prize, deadline countdown
   - Pull-to-refresh, loading skeletons

2. **TournamentDetailScreen** — View tournament info + apply
   - Hero section with logo, title, prize
   - Requirements list (telegram channels, custom)
   - Teams list (accepted teams)
   - Swiss bracket table (when in_progress)
   - "Apply" button with telegram validation gate
   - Application status tracking (for team leader)

3. **TournamentHostRequestScreen** — Form to request host role
   - Motivation textarea
   - Experience textarea
   - Telegram channel input
   - Social links input
   - Submit button → calls RPC

4. **TournamentCreateScreen** — Create tournament (tournament_host only)
   - Title input (max 100 chars)
   - Description textarea (max 200 words)
   - Logo upload (optional)
   - Prize type selector + description
   - Max teams slider (4-64)
   - Min team size selector (3-7)
   - Best of selector (BO1/BO2)
   - Region selector
   - Skill level selector
   - Registration deadline picker
   - Check-in deadline picker
   - Requirements builder (add/remove, max 15)
   - Live stream toggle
   - Preview + Publish

5. **TournamentHostPanelScreen** — Host manages their tournament
   - Tab layout: Overview | Applications | Bracket | Settings
   - Overview: stats, generated Gmail/password (shown once)
   - Applications: accept/reject with reason
   - Bracket: Swiss table, generate button, match scheduling
   - Settings: edit tournament details

### Profile Update
Add to `data/model/UserProfile.kt`:
```kotlin
val telegramUsername: String? = null,
val isTournamentHost: Boolean = false,
val hostTrustScore: Float = 5.0f,
```

### NotificationType Update
Add to `data/model/Notification.kt`:
```kotlin
TOURNAMENT_HOST_APPROVED,
TOURNAMENT_HOST_REJECTED,
TOURNAMENT_APPLICATION_NEW,
TOURNAMENT_APPLICATION_ACCEPTED,
TOURNAMENT_APPLICATION_REJECTED,
TOURNAMENT_APPLICATION_BLOCKED,
TOURNAMENT_MATCH_SCHEDULED,
TOURNAMENT_MATCH_STARTING,
TOURNAMENT_ROOM_READY,
TOURNAMENT_MATCH_RESULT,
TOURNAMENT_ROUND_ADVANCED,
TOURNAMENT_CANCELLED,
TOURNAMENT_COMPLETED,
TOURNAMENT_DISPUTE,
TOURNAMENT_NO_SHOW
```

### Bottom Navigation
Add "Tournaments" tab to `AppBottomNav.kt`:
- Icon: `EmojiEvents` or `MilitaryTech` (Material Icons)
- Position: between Home and Teams
- Badge: count of active tournament applications

### Chat Update
Update `ChatScreen.kt` and `Message.kt` to support `conversation_participants`:
- Load participants from `conversation_participants` table
- Show all participant names in chat header
- Host appears as 3rd participant in tournament match chats

## Voice Triggers
"tournament android", "tournament screens", "tournament apk"
