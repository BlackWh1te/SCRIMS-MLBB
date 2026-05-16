# MLBB Scrim Host - Design System

## Aesthetic Direction
**Epic Gaming Fantasy meets iOS Polish** — Inspired by MLBB's heroic fantasy aesthetic with gold/blue color scheme, bold typography, and dynamic elements that convey competitive energy, now enhanced with iOS-style glassmorphism, refined shadows, and smooth animations for a premium, polished feel.

## Color Palette

### iOS System Colors
In addition to MLBB colors, iOS system colors are used for native-feeling UI elements:
- **iOS Blue**: `#007AFF` — Primary actions, active states
- **iOS Green**: `#34C759` — Success states
- **iOS Red**: `#FF3B30` — Destructive actions, errors
- **iOS Orange**: `#FF9500` — Warnings
- **iOS Purple**: `#AF52DE` — Special accents
- **iOS Teal**: `#5AC8FA` — Info states
- **iOS Indigo**: `#5856D6` — Secondary accents

### Primary Colors (MLBB Official)
- **Gold Primary**: `#FFD700` — MLBB gold accents
- **Blue Primary**: `#1E90FF` — MLBB blue
- **Dark Blue**: `#0A1628` — Deep background
- **Dark Navy**: `#0D1B2A` — Secondary background

### Secondary Colors
- **White**: `#FFFFFF` — Text primary
- **Light Gray**: `#E0E0E0` — Text secondary
- **Success Green**: `#00C853` — Win/XP gain
- **Error Red**: `#FF3D00` — Loss/errors
- **Warning Orange**: `#FF9100` — Pending/notifications

### Accent Colors
- **Purple**: `#7C4DFF` — Epic/tier highlights
- **Cyan**: `#00E5FF` — Special events
- **Pink**: `#FF4081` — Featured content

### Gradients
- **Hero Gradient**: `#0A1628` → `#1E90FF` (Top to bottom)
- **Gold Gradient**: `#FFD700` → `#FFA500` (Buttons/highlights)
- **Tier Gradient**: Varies by rank (Bronze to Grandmaster)

## Typography

### iOS-Style Typography
Typography now follows iOS conventions with refined letter spacing and line heights:
- **Title 1**: 28sp Bold — Large titles
- **Title 2**: 22sp Bold — Medium titles
- **Title 3**: 20sp SemiBold — Small titles
- **Headline**: 17sp SemiBold — Section headers
- **Body**: 17sp Regular — Body text
- **Callout**: 16sp Regular — Secondary text
- **Footnote**: 13sp Regular — Captions
- **Caption 1**: 12sp Regular — Small labels
- **Caption 2**: 11sp Regular — Tiny labels

Letter spacing is slightly negative (-0.1 to -0.4sp) for a modern, tight look similar to iOS.

### Display Font (Headings)
- **Font**: Rajdhani or Orbitron (Google Fonts)
- **Style**: Bold, futuristic gaming aesthetic
- **Usage**: App title, section headers, team names, tier badges

### Body Font
- **Font**: Roboto or Montserrat (Google Fonts)
- **Style**: Clean, readable for longer text
- **Usage**: Descriptions, chat messages, form labels

### Number/Stats Font
- **Font**: Teko or Chakra Petch (Google Fonts)
- **Style**: Condensed, bold for XP, rankings, scores
- **Usage**: XP values, ranks, team sizes, match counts

### Font Sizes
- **Display XL**: 32sp — App title, hero
- **Display L**: 24sp — Section headers
- **Display M**: 20sp — Card titles
- **Body L**: 16sp — Primary text
- **Body M**: 14sp — Secondary text
- **Body S**: 12sp — Captions, labels
- **Micro**: 10sp — Tiny labels

## Spacing System

**Base Unit**: 8dp
- **XS**: 4dp
- **S**: 8dp
- **M**: 16dp
- **L**: 24dp
- **XL**: 32dp
- **XXL**: 48dp

**Density**: Medium-Dense (gaming apps use tighter spacing than enterprise)

## Border Radius

- **XS**: 4dp — Small elements, tags
- **S**: 8dp — Buttons, inputs
- **M**: 12dp — Cards
- **L**: 16dp — Large cards, modals
- **XL**: 24dp — Hero sections, featured content
- **Full**: 9999dp — Pills, badges

## Components

### iOS-Style Components
The app now includes iOS-style components for a premium feel:
- **iOSGlassCard**: Glassmorphism cards with subtle blur and borders
- **iOSElevatedCard**: Elevated cards with refined shadows
- **iOSPrimaryButton**: Full-width primary buttons with iOS styling
- **iOSSecondaryButton**: Outlined secondary buttons
- **iOSTextButton**: Text-only buttons for tertiary actions
- **iOSNavigationBar**: Large title navigation bars
- **iOSLargeTitleHeader**: 34sp bold headers for screen titles
- **iOSBottomSheet**: Modal bottom sheets with drag handles
- **iOSActionSheet**: Action sheet with cancel button
- **iOSChip**: Selectable filter chips
- **iOSInput**: Styled text fields with iOS appearance

### Glassmorphism
- Subtle glass backgrounds (72% opacity)
- Soft border highlights (8-12% white alpha)
- Layered depth with multiple shadow colors
- Blur effects on elevated surfaces

### Shadows
iOS-style shadow system with three levels:
- **Light**: 8% alpha black
- **Medium**: 12% alpha black
- **Heavy**: 20% alpha black

### Buttons
- **Primary**: Gold gradient background, dark blue text, 8dp radius, medium shadow
- **Secondary**: Blue background, white text, 8dp radius
- **Ghost**: Transparent with gold border, gold text
- **Destructive**: Red background, white text

### Cards
- Dark blue background (`#0D1B2A`)
- 12dp border radius
- Subtle blue glow on hover/active
- Gold accent border for featured cards

### Inputs
- Dark navy background
- Gold border on focus
- 8dp border radius
- Placeholder in light gray

### Tier Badges
- Gradient backgrounds by tier (Bronze: brown, Silver: gray, Gold: gold, etc.)
- Bold white text
- 16dp border radius (pill shape)
- Glow effect for higher tiers

### Chat Bubbles
- **Sent**: Blue background, white text
- **Received**: Dark navy background, white text
- 12dp border radius
- **System messages**: Gold accent

## Layout Approach

**Grid-Disciplined with Editorial Flair**
- 12-column grid for complex screens
- Max content width: 400dp (mobile)
- Card-based layouts with clear hierarchy
- Hero sections with gradient backgrounds
- Tier/rank information prominently displayed

## Motion Design

**Approach**: iOS-Inspired Smoothness
Animations follow iOS conventions with cubic bezier easing:
- **Ease Out Cubic**: Smooth exits (0.33, 1, 0.68, 1)
- **Ease In Out Cubic**: Balanced transitions (0.65, 0, 0.35, 1)
- **Ease Out Quart**: Decelerated exits (0.25, 1, 0.5, 1)
- **Ease In Cubic**: Accelerated entries (0.32, 0, 0.67, 0)
- **Spring**: Bouncy interactions (medium damping, low stiffness)

**Animation Durations**:
- **Micro**: 50-100ms (button taps, toggles)
- **Short**: 150-200ms (list items, chip selection)
- **Medium**: 250-400ms (screen transitions, modals)
- **Long**: 400-700ms (hero reveals, celebrations)

**Approach**: Intentional-Expressive
- **Micro animations**: 50-100ms (button taps, toggles)
- **Short animations**: 150-250ms (card reveals, list items)
- **Medium animations**: 250-400ms (screen transitions, modals)
- **Long animations**: 400-700ms (hero reveals, tier celebrations)

**Easing**:
- Enter: `ease-out`
- Exit: `ease-in`
- Move: `ease-in-out`

**Special Effects**:
- Gold particle burst on XP gain
- Tier upgrade celebration animation
- Match found pulse effect
- Screenshot upload progress shimmer

## Icon Style

**Line Icons with Gold Accents**
- 24dp standard size
- 2dp stroke width
- Gold fill on active states
- Rounded caps and joins
- Gaming-themed icons (swords, shields, crowns for tiers)

## Dark Mode

**Default**: Dark mode only (gaming aesthetic)
- Backgrounds: Dark blue/navy
- Text: White/light gray
- Accents: Gold/blue
- No light mode needed

## Screen-Specific Guidelines

### Login/Register
- Hero with MLBB-style gradient background
- Gold accent borders on inputs
- Epic title typography

### Team Profile
- Large tier badge at top
- Team stats in card grid
- Gold accent for leader badge
- Member list with role indicators

### Scrim Search
- Card-based scrim listings
- Tier badges prominently displayed
- Gold "Apply" buttons
- Filter chips with gold borders

### Match Chat
- Real-time message bubbles
- Gold accent for system messages
- Typing indicator in blue
- Room details in gold card

### Leaderboard
- Tier-based section dividers
- Gold gradient for top 3
- Number font for rankings
- Trophy icons for top positions

## iOS-Style Bottom Navigation
The bottom navigation bar follows iOS design patterns:
- **Height**: 72dp (taller for better touch targets)
- **Corner Radius**: 28dp (generous, organic curve)
- **Background**: Glassmorphism (72% opacity)
- **Shadow**: Subtle layered shadows (8dp elevation)
- **Active State**: iOS Blue with 12% alpha background
- **Icon Size**: 26dp (larger than standard)
- **Label**: 10sp SemiBold, appears on selection
- **Animation**: Spring-based scale (1.15x on active)
- **Badge**: iOS Red with bold text

## Tier System Colors

- **Bronze**: `#CD7F32` — Bronze gradient
- **Silver**: `#C0C0C0` — Silver gradient
- **Gold**: `#FFD700` — Gold gradient
- **Platinum**: `#E5E4E2` — Platinum gradient
- **Diamond**: `#B9F2FF` — Diamond gradient
- **Master**: `#FF00FF` — Purple gradient
- **Grandmaster**: `#FFD700 + #FF0000` — Gold/Red gradient

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025-05-10 | Initial design system created | MLBB-themed aesthetic for scrim hosting app |
| 2026-05-12 | iOS-style design system integration | Enhanced with glassmorphism, refined shadows, iOS typography, and smooth animations for premium feel |

## Memorable Thing
"The epic gold and blue fantasy aesthetic meets iOS polish — every interaction feels premium, smooth, and championship-ready."