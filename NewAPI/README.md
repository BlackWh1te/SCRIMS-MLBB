# MLBB News Proxy API — Drip-Feed Edition

Centralized backend that scrapes official MLBB X/Twitter news, quality-filters it, stores it in a persistent archive, and **drip-feeds** articles to Android app clients — solving both the 100 req/month X API limit AND preventing news spam.

## Why Drip-Feed?

| Problem | Old Way | Drip-Feed Way |
|---------|---------|---------------|
| 20 articles at once | Overwhelming user, low engagement | +1 every 2 hours, keeps app feeling fresh |
| User opens app once | Sees everything, no reason to return | Always has something new to discover |
| Content feels stale | Same articles for days | Constant illusion of fresh updates |

## Architecture

```
┌─────────────┐     ┌─────────────────────┐     ┌─────────────────┐
│ Android App │────▶│ MLBB Proxy API      │────▶│  X API (Twitter)│
│ drip index  │◄────│ (archive + drip)    │◄────│  100 req/mo     │
│ +1 / 2h     │     └─────────────────────┘     └─────────────────┘
└─────────────┘              │
                             ▼
                    ┌──────────────────┐
                    │ articles-db.json │
                    │ (persistent      │
                    │  append-only DB) │
                    └──────────────────┘
```

**Request burn**: 1 scrape every 8h = **90 requests/month** (out of 100 free tier).

## Quick Start

```bash
cd NewAPI
npm install
npm start
```

Server runs on `http://localhost:3000`

## API Endpoints

### `GET /news?offset=5`
Drip-feed endpoint. Returns only articles with `dripIndex <= offset`.

```bash
curl "http://localhost:3000/news?offset=5"
```

Response:
```json
{
  "status": "ok",
  "articles": [...],
  "unlocked": 6,
  "totalInArchive": 24,
  "unseen": 18,
  "userOffset": 5,
  "nextUnlockInMinutes": 0,
  "source": "archive"
}
```

### `GET /news/count`
Total articles in archive (for progress bars).

```bash
curl http://localhost:3000/news/count
```

### `GET /health`
Health check + archive stats.

```bash
curl http://localhost:3000/health
```

### `POST /admin/scrape`
Manually trigger a scrape + quality filter + archive.

```bash
curl -X POST http://localhost:3000/admin/scrape \
  -H "X-Admin-Key: mlbb-news-secret-2024"
```

### `POST /admin/reset`
**DANGER** — wipes the archive. Use only for testing.

## Quality Filter

Every article goes through these gates before entering the archive:

| Check | Rejects |
|-------|---------|
| Content length < 15 chars | Empty / meaningless tweets |
| Title length < 10 chars | Poor quality |
| Spam keywords | `follow me`, `link in bio`, `giveaway`, `free diamonds`, `buy diamonds`, `boosting service`, `i'm live`, etc. |
| Total engagement < 5 | Low-quality / bot tweets |
| Starts with `@` + < 3 words | Pure mentions / replies |
| Duplicate ID | Already archived |

This keeps the archive clean — only real MLBB news gets in.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `X_BEARER_TOKEN` | (required) | X API v2 Bearer Token |
| `SCRAPE_INTERVAL_HOURS` | 8 | Hours between scrapes |
| `MAX_TWEETS_PER_FETCH` | 50 | Tweets per scrape (max 100) |
| `PORT` | 3000 | Server port |
| `API_KEY` | mlbb-news-secret-2024 | Admin API key |

## Deployment Options

### Option A: Render (Free Tier)
1. Push this folder to a GitHub repo
2. Create Web Service on [Render](https://render.com)
3. Set environment variables in Render dashboard
4. Done — free forever

### Option B: Railway (Free Tier)
1. `railway login`
2. `railway init`
3. `railway up`

### Option C: Self-Hosted (VPS / Raspberry Pi)
```bash
git clone <repo>
cd NewAPI
npm install
npm start
# Use pm2 or systemd for persistence
```

## Android Integration

The Android app now uses **drip-feed mode** by default:

1. User opens News tab → app sends its current `dripIndex` to `/news?offset={index}`
2. Backend returns only articles the user has "unlocked"
3. Every 2 hours, the app auto-increments `dripIndex` by 1
4. Next time user opens News, they see one more article

**Local cache** (2h TTL) prevents hitting the backend on every tab switch.

## Cost Analysis

| Service | Cost | Notes |
|---------|------|-------|
| Render Web Service | $0 | Free tier: sleeps after 15min idle, wakes on request |
| Railway | $0 | Free tier: $5 credit/month |
| Self-hosted VPS | $5-10/mo | Always on |
| X API Free | $0 | 90 req/month at 8h intervals |

## Security Notes

- The `X-API-Key` header check is commented out by default. Uncomment in production.
- The `.env` file contains your X Bearer Token — never commit it to a public repo.
- Use a persistent disk (not ephemeral) for `articles-db.json` if deploying to serverless.
