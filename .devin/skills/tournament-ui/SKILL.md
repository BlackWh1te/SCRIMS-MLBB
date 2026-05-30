# Tournament UI/UX — Vertical Phone Design System

Tournament-specific UI components and screen layouts optimized for vertical phone screens (default Android portrait mode).

## Plan Reference
Read `tournamentwork.md` at the project root for screen specifications.
Read `DESIGN.md` at the project root for the base design system.

## Design Philosophy

**"Tournament feels different from scrims."** Tournaments are bigger, more prestigious, more exciting. The UI must convey:
- **Grandeur** — bigger hero sections, gold accents, animated reveals
- **Urgency** — countdown timers, live indicators, pulsing badges
- **Clarity** — Swiss bracket must be instantly readable on a phone screen
- **Prestige** — prize display should feel like opening a treasure chest

## Vertical Phone Constraints

### Screen Dimensions (design for these)
- **Width**: 360dp (smallest common phone)
- **Height**: 640dp (scrollable content area)
- **Safe areas**: status bar (24dp), navigation bar (48dp), bottom nav (72dp)
- **Content width**: 360dp - 32dp (16dp padding each side) = **328dp usable**
- **Card width**: 328dp - 16dp (8dp margin) = **312dp card**

### Touch Targets
- Minimum 48dp x 48dp (Material Design guideline)
- Preferred 56dp x 56dp for primary actions
- Spacing between clickable elements: minimum 8dp

### Typography on Phone
- **Tournament title**: 20sp Bold (not 24sp — too big for 360dp width)
- **Section header**: 16sp SemiBold
- **Body**: 14sp Regular
- **Caption**: 12sp Regular
- **Micro**: 10sp Regular

---

## Tournament-Specific Components

### 1. TournamentCard — List item on TournamentListScreen

```
┌─────────────────────────────────────┐
│ ┌───┐                               │
│ │   │  MLBB Pro Cup          🟢 LIVE │
│ │LOGO│  Host: @CastGaming           │
│ │   │  Prize: 💎 5000 Diamonds      │
│ └───┘  12/16 teams · Registration   │
│         ⏰ 2d 14h remaining          │
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │
│ [🏆 Bronze+] [🌍 EU]   [Apply →]   │
└─────────────────────────────────────┘
```

**Specs**:
- Height: ~120dp (compact but informative)
- Logo: 56dp x 56dp rounded-xl (12dp radius)
- Status badge: top-right, pill shape, color-coded:
  - `registration` → iOS Green `#34C759`
  - `check_in` → iOS Orange `#FF9500`
  - `in_progress` → iOS Blue `#007AFF` + pulse animation
  - `completed` → Gold `#FFD700`
  - `cancelled` → iOS Red `#FF3B30`
- Countdown timer: Warning Orange when < 24h, Error Red when < 1h
- Skill level + region: chip badges, bottom-left
- Apply button: Gold gradient, bottom-right

### 2. TournamentHero — Top section of TournamentDetailScreen

```
┌─────────────────────────────────────┐
│         ┌──────────────┐            │
│         │              │            │
│         │  TOURNAMENT  │            │
│         │    LOGO      │            │
│         │   (120dp)    │            │
│         │              │            │
│         └──────────────┘            │
│                                     │
│        MLBB Pro Cup 2024            │
│        ─────────────────            │
│     💎 5,000 Diamonds Prize         │
│                                     │
│   ┌──────┐ ┌──────┐ ┌──────┐      │
│   │ 16   │ │ BO1  │ │ EU   │      │
│   │teams │ │format│ │region│      │
│   └──────┘ └──────┘ └──────┘      │
│                                     │
│   ⏰ Registration closes in 2d 14h  │
└─────────────────────────────────────┘
```

**Specs**:
- Full-width card with glassmorphism background
- Logo: 120dp x 120dp, rounded-2xl (24dp radius), gold border
- Title: 20sp Bold, Gold Primary `#FFD700`
- Prize: 16sp SemiBold, prize icon + description
- Stat pills: 3 across, glassmorphic background, 12sp labels
- Countdown: animated number flip or progress bar

### 3. SwissBracketTable — Compact Swiss standings

```
┌─────────────────────────────────────┐
│  Swiss Standings — Round 2/4       │
│ ─────────────────────────────────── │
│  #  Team        W  D  L  Pts  BH   │
│  1  Alpha       2  0  0   6  4.0  │
│  2  Bravo       1  1  0   4  3.5  │
│  3  Charlie     1  0  1   3  3.0  │
│  4  Delta       0  1  1   1  2.5  │
│  5  Echo        0  0  2   0  1.5  │
└─────────────────────────────────────┘
```

**Specs**:
- Compact table: 12sp font, 32dp row height
- Header row: glassmorphic background, gold text
- Team's own row: highlighted with iOS Blue tint (12% alpha)
- Placement column: gold gradient for #1, silver for #2, bronze for #3
- Horizontally scrollable if needed (but design to fit 360dp)
- Column widths: # (24dp), Team (flex), W (28dp), D (28dp), L (28dp), Pts (36dp), BH (36dp)

### 4. SwissMatchCard — Individual match in bracket

```
┌─────────────────────────────────────┐
│  Round 2 · Match 3                  │
│  ─────────────────────────────────  │
│  ┌─────────────┐  vs  ┌───────────┐│
│  │  Alpha       │      │  Charlie  ││
│  │  W2 D0 L0    │      │  W1 D0 L1 ││
│  └─────────────┘      └───────────┘│
│                                     │
│  📅 27 May · 15:00 UTC              │
│  ⏳ 2h 30m until match              │
│                                     │
│  [💬 Chat]  [🎮 Room ID]  [📺 Live]│
└─────────────────────────────────────┘
```

**Specs**:
- Card height: ~140dp
- Team boxes: glassmorphic, 48dp height
- Countdown: animated, color shifts (green > orange > red)
- Action buttons: compact, icon-only on narrow screens, icon+text on wider
- Room ID button: hidden until host drops it, then appears with gold pulse

### 5. RequirementCard — Tournament requirement display

```
┌─────────────────────────────────────┐
│  📋 Requirements to Join            │
│  ─────────────────────────────────── │
│  1. Subscribe to @CastGamingTG      │
│     🔗 t.me/CastGamingTG            │
│                                     │
│  2. Subscribe to @MLBBUpdates       │
│     🔗 t.me/MLBBUpdates             │
│                                     │
│  3. Follow host on YouTube          │
│     🔗 youtube.com/@cast            │
└─────────────────────────────────────┘
```

**Specs**:
- Numbered list with icon per type (Telegram = paper plane icon, Custom = link icon)
- URL is clickable (opens in browser)
- Telegram requirements: special blue tint background
- Max 15 items — scrollable if needed

### 6. ApplicationStatusCard — Team's application status

```
┌─────────────────────────────────────┐
│  Your Application                   │
│  ─────────────────────────────────── │
│  Status: 🔴 REJECTED                │
│  Attempt: 2/3                       │
│  Reason: "Team members missing      │
│           Telegram usernames"        │
│                                     │
│  [✏️ Re-Apply]  [❌ Blocked after 3]│
└─────────────────────────────────────┘
```

**Specs**:
- Status badge: color-coded (pending=orange, accepted=green, rejected=red, blocked=dark red)
- Attempt counter: visual dots (●●○ for 2/3)
- Rejection reason: expandable text area
- Re-Apply button: only visible if attempts < 3

### 7. TelegramGateDialog — Missing usernames warning

```
┌─────────────────────────────────────┐
│  ⚠️ Cannot Apply                    │
│  ─────────────────────────────────── │
│  These teammates need to add their  │
│  Telegram username:                 │
│                                     │
│  • Player1 (MLBB: 123456789)       │
│  • Player2 (MLBB: 987654321)       │
│                                     │
│  Ask them to update their profile.  │
│  Telegram username is required for  │
│  tournament communication.          │
│                                     │
│           [Got it]                  │
└─────────────────────────────────────┘
```

**Specs**:
- Modal dialog (not fullscreen)
- Warning icon + orange accent
- Player list: username + MLBB ID for identification
- Single dismiss button

### 8. HostCredentialCard — Generated account (shown ONCE)

```
┌─────────────────────────────────────┐
│  🔑 Your Host Account               │
│  ─────────────────────────────────── │
│  ⚠️ SAVE THIS — you won't see it    │
│     again!                          │
│                                     │
│  Email:                              │
│  ┌─────────────────────────────────┐│
│  │ tournamenthost001@mlbbhost.com ││
│  └─────────────────────────────────┘│
│                                     │
│  Password:                           │
│  ┌─────────────────────────────────┐│
│  │ njhGsw8@3^!              [👁️] ││
│  └─────────────────────────────────┘│
│                                     │
│  Use these to login at:              │
│  admin.mlbbhost.com/host/login       │
│                                     │
│  [📋 Copy Email] [📋 Copy Password] │
│  [✅ I've Saved It]                 │
└─────────────────────────────────────┘
```

**Specs**:
- Fullscreen dialog (important — user must not miss this)
- Red warning banner at top
- Copy-to-clipboard buttons for email and password
- Password toggle visibility (eye icon)
- "I've Saved It" confirmation button — once pressed, credentials gone forever
- URL is clickable (opens host login in browser)

---

## Screen Layouts (Vertical Phone)

### TournamentListScreen Layout
```
┌─────────────────────────────────────┐
│  🏆 Tournaments              [🔍]  │  ← Header (56dp)
├─────────────────────────────────────┤
│  [All] [Open] [Live] [Completed]   │  ← Filter chips (48dp)
│  [💎 Prize] [🌍 EU] [🎮 All Skil] │  ← Secondary filters
├─────────────────────────────────────┤
│  ┌─────────────────────────────────┐│
│  │  TournamentCard 1              ││  ← Scrollable list
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │  TournamentCard 2              ││
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │  TournamentCard 3              ││
│  └─────────────────────────────────┘│
│  ...                                │
├─────────────────────────────────────┤
│  [🏠] [🏆] [⚔️] [💬] [👤]        │  ← Bottom nav (72dp)
└─────────────────────────────────────┘
```

### TournamentDetailScreen Layout
```
┌─────────────────────────────────────┐
│  ← Back                    [⋯]     │  ← Top bar (56dp)
├─────────────────────────────────────┤
│  ┌─────────────────────────────────┐│
│  │  TournamentHero                 ││  ← Hero (280dp)
│  │  Logo + Title + Prize + Stats   ││
│  └─────────────────────────────────┘│
│                                     │
│  📋 Requirements                    │  ← Section header
│  ┌─────────────────────────────────┐│
│  │  RequirementCard                ││  ← Requirements list
│  └─────────────────────────────────┘│
│                                     │
│  👥 Teams (12/16)                   │  ← Section header
│  ┌─────────────────────────────────┐│
│  │  Team chips: [Alpha] [Bravo]... ││  ← Horizontal scroll
│  └─────────────────────────────────┘│
│                                     │
│  🏅 Swiss Standings                 │  ← Section header (if in_progress)
│  ┌─────────────────────────────────┐│
│  │  SwissBracketTable              ││  ← Compact table
│  └─────────────────────────────────┘│
│                                     │
│  📊 Your Matches                     │  ← Section header (if team is in)
│  ┌─────────────────────────────────┐│
│  │  SwissMatchCard                 ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────────┐│
│  │  ApplicationStatusCard          ││  ← If team applied
│  └─────────────────────────────────┘│
│                                     │
│  ╔═══════════════════════════════╗│
│  ║     [Apply for Tournament]    ║│  ← Sticky bottom CTA (64dp)
│  ╚═══════════════════════════════╝│
└─────────────────────────────────────┘
```

### TournamentCreateScreen Layout
```
┌─────────────────────────────────────┐
│  ← Create Tournament                │  ← Top bar (56dp)
├─────────────────────────────────────┤
│  Title *                            │
│  ┌─────────────────────────────────┐│
│  │ Enter tournament name (100 max) ││  ← Input fields
│  └─────────────────────────────────┘│
│                                     │
│  Description *                      │
│  ┌─────────────────────────────────┐│
│  │ Describe your tournament...     ││
│  │ (200 words max)                 ││
│  │                                 ││
│  └─────────────────────────────────┘│
│                                     │
│  📷 Logo (optional)    [Upload]    │
│                                     │
│  Prize Type *                       │
│  [💎] [💰] [🎮] [⭐] [📦]        │  ← Prize type selector
│  Prize Description                  │
│  ┌─────────────────────────────────┐│
│  │ e.g. "5000 Diamonds"            ││
│  └─────────────────────────────────┘│
│                                     │
│  ── Tournament Settings ──          │
│  Max Teams: ◄ 16 ►                 │  ← Stepper
│  Min Team Size: [5▼]               │  ← Dropdown
│  Format: [BO1 ▼]                   │  ← Dropdown
│  Region: [EU ▼]                    │  ← Dropdown
│  Skill Level: [All ▼]             │  ← Dropdown
│                                     │
│  Registration Deadline              │
│  ┌─────────────────────────────────┐│
│  │ 📅 Select date & time          ││
│  └─────────────────────────────────┘│
│  Check-in Deadline                  │
│  ┌─────────────────────────────────┐│
│  │ 📅 Select date & time          ││
│  └─────────────────────────────────┘│
│                                     │
│  ── Requirements ──                 │
│  1. [Telegram ▼] [label______] [×] │
│     [url_________________________] │
│  2. [Custom ▼] [label_______] [×] │
│  [+ Add Requirement] (13/15 left)  │
│                                     │
│  [🔴 Enable Live Stream]           │  ← Toggle
│                                     │
│  ╔═══════════════════════════════╗│
│  ║       [Create Tournament]     ║│  ← CTA button
│  ╚═══════════════════════════════╝│
└─────────────────────────────────────┘
```

---

## Animation Specifications

### Tournament Card Reveal
- Stagger animation: each card fades in + slides up with 80ms delay
- Duration: 300ms, ease-out-cubic

### Countdown Timer
- Number change: scale 1.0 → 0.95 → 1.0 (50ms) on each tick
- Color transition: green → orange → red as deadline approaches

### Apply Button
- Pulse glow when tournament is in registration phase
- Scale down on press (0.95), spring back on release

### Swiss Bracket Generation
- "Generating bracket..." shimmer animation
- Cards appear with stagger + scale-in (0.8 → 1.0, 200ms each)

### Match Result
- Winner team name: gold shimmer sweep animation
- Score update: number count-up animation (0 → 3, 300ms)

### Host Credential Reveal
- Credentials appear with typewriter effect (character by character)
- Warning icon pulses red

---

## Color Overrides for Tournament Elements

| Element | Color | Usage |
|---------|-------|-------|
| Tournament accent | `#FFD700` (Gold Primary) | Titles, prize, CTA buttons |
| Live indicator | `#007AFF` (iOS Blue) | "LIVE" badge, active match |
| Registration open | `#34C759` (iOS Green) | "Open" status badge |
| Check-in phase | `#FF9500` (iOS Orange) | "Check-in" status badge |
| Completed | `#AF52DE` (iOS Purple) | "Completed" badge |
| Cancelled | `#FF3B30` (iOS Red) | "Cancelled" badge |
| Swiss table header | `#1E90FF` (Blue Primary) | Table header row |
| Winner highlight | `#FFD700` (Gold) | Winning team row |
| Bye match | `#5AC8FA` (iOS Teal) | Bye indicator |
| Disputed match | `#FF3B30` (iOS Red) | Dispute badge |

---

## Compose Component Patterns

### Reusable Tournament Composables
```kotlin
// Card components
TournamentCard(tournament, onClick)
TournamentHero(tournament)
SwissBracketTable(teams, currentTeamId)
SwissMatchCard(match, currentTeamId, onChatClick, onRoomClick)
RequirementCard(requirements)
ApplicationStatusCard(application, onReApplyClick)
HostCredentialCard(email, password, onSaved)
TelegramGateDialog(missingUsers, onDismiss)

// Shared components
TournamentStatusBadge(status: String)      // Color-coded pill
CountdownTimer(deadline: Instant)          // Animated countdown
PrizeIcon(prizeType: String)              // Icon per prize type
SkillLevelChip(level: String)             // Filter chip
RegionChip(region: String)                // Filter chip
TournamentStatPill(value: String, label: String) // Stat in hero
```

### Compose Styling Patterns
- Use `Modifier.fillMaxWidth()` for all cards (phone is narrow)
- Use `padding(horizontal = 16.dp)` for screen-level padding
- Use `padding(8.dp)` for card internal padding
- Use `verticalScroll(rememberScrollState())` for all screens
- Use `stickyHeader` for section headers in LazyColumn
- Use `AnimatedVisibility` for show/hide sections
- Use `Crossfade` for status changes
- Use `rememberInfiniteTransition` for pulse/glow animations

## Voice Triggers
"tournament ui", "tournament design", "tournament ux", "tournament phone layout"
