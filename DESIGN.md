# MLBB Scrim Host - Design System

## Aesthetic Direction
**Epic Gaming Fantasy** — Inspired by MLBB's heroic fantasy aesthetic with gold/blue color scheme, bold typography, and dynamic elements that convey competitive energy.

## Color Palette

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

## Memorable Thing
"The epic gold and blue fantasy aesthetic that makes every match feel like a championship battle."