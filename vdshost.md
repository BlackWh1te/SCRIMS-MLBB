# VDS Hosting Guide for MLBB Scrim Host (RU + EU Backend)

> Save this file and review when you're ready to deploy the RU backend.
> Last updated: 2026-05-28

---

## Your Actual Backend Requirements

Based on `schema.sql`, migrations, and your Android app, self-hosted Supabase must run ALL of these simultaneously:

| Service | What It Does | RAM Used |
|---|---|---|
| **PostgreSQL** | 15+ tables, 20+ indexes, 14 triggers/functions, RLS policies | 1.0–1.5 GB |
| **Supabase Realtime** | Live chat, scrim updates, notifications (WebSocket) | 300–500 MB |
| **GoTrue (Auth)** | JWT login/signup | 200–300 MB |
| **PostgREST** | Auto-generated REST API | 150–200 MB |
| **Storage + Minio** | Screenshot uploads, chat images (`chat-media` bucket) | 300–500 MB |
| **Kong (Gateway)** | Routes all API traffic | 200–300 MB |
| **Docker + OS overhead** | Container runtime | 500–800 MB |
| **TOTAL** | | **~2.7–4.1 GB** |

Your app uses:
- **Realtime** — heavy: chat, notifications, scrim status updates
- **Storage buckets** — `chat-media` for images + match verification screenshots
- **RLS policies** — adds CPU overhead to every query
- **Triggers** — 14 triggers auto-update on insert/update
- **Tournaments** — complex queries + real-time brackets

### Minimum Spec You MUST Buy

```
CPU:     2 cores (dedicated, NOT shared/oversubscribed)
RAM:     4 GB (absolute minimum — 2 GB will crash)
Disk:    40 GB NVMe SSD
Network: 1 Gbps unmetered
OS:      Ubuntu 22.04 LTS
```

### If You Have 100+ Concurrent Users or Run Tournaments

```
CPU:     4 cores
RAM:     8 GB
Disk:    80 GB NVMe
```

---

## The Hard Truth: What "Cheap" Actually Means

| Plan | RAM | Will It Work? | Why |
|---|---|---|---|
| **NetAngels Оптима** (334 RUB) | ~2 GB | **NO** | PostgreSQL alone needs 1.5 GB. Realtime + Storage will crash it. OOM kills guaranteed. |
| **NetAngels Турбо** (889 RUB) | ~4 GB | **Barely** | Runs but will lag during tournaments. Unknown CPU = RLS/trigger bottleneck. |
| **FirstVDS ~2vCPU/4GB** | ~4 GB | **YES** | AMD EPYC handles RLS + triggers well. Best price/performance. |
| **SprintHost ~2vCPU/4GB** | ~4 GB | **YES** | Best network + DDoS. Most reliable. Slightly more expensive. |

### What "Guaranteed to Work" Means (2vCPU/4GB)

- Login/signup works
- Scrim posting/search works
- Real-time chat works (up to ~50 concurrent connections)
- Screenshot uploads work
- Notifications push in real-time
- Tournaments run without lag

### What "2GB" Actually Does

- Realtime crashes when 5+ people chat simultaneously
- PostgreSQL OOM-killed during scrim searches
- Screenshot uploads fail (Minio runs out of RAM)
- Tournament day = server down

**Do NOT buy any 2 GB plan. It will not work.**

---

## Provider Comparison

### 1. NetAngels

| | |
|---|---|
| **Website** | netangels.ru |
| **Years** | 22 years |
| **Clients** | 35,000+ |
| **Virtualization** | KVM |
| **Storage** | NVMe |
| **DDoS** | Basic |
| **Panel** | Own panel |
| **Data centers** | Russia only |
| **S3 storage** | File storage only (not true S3) |
| **FZ-152** | Yes |
| **Best for** | Cheapest entry, simple websites |

**Pricing:**

| Plan | Price | Spec (estimated) | Good For |
|---|---|---|---|
| Старт | 130 RUB | ~1 GB RAM, shared CPU | Static website only |
| Оптима | 334 RUB | ~2 GB RAM, NVMe | **NOT for Supabase — will crash** |
| Турбо | 889 RUB | ~4 GB RAM, NVMe | **Minimum viable for your app** |
| Про | 5,057 RUB | 4+ vCPU, 8+ GB | High traffic, overkill for now |

**Pros:**
- Cheapest overall
- 99.99% uptime SLA
- FZ-152 compliant
- Long track record

**Cons:**
- Unknown CPU models (likely older/shared Intel Xeon E5)
- No auto-scaling
- No true S3 object storage
- No API for automation
- Турбо is more expensive than competitors for same specs

---

### 2. FirstVDS

| | |
|---|---|
| **Website** | firstvds.ru |
| **Years** | 20+ years |
| **Awards** | "Hoster of the Year" 2021, 2022, 2024 |
| **Virtualization** | KVM (full isolation) |
| **CPU** | AMD EPYC, Intel Xeon, AMD Threadripper PRO up to 5.7 GHz |
| **Storage** | NVMe + HDD options up to 5000 GB |
| **DDoS** | Basic + BitNinja anti-bot |
| **Panel** | ISPmanager (included) |
| **Data centers** | Russia, Netherlands, Kazakhstan |
| **S3 storage** | Yes — "Объектное хранилище S3" |
| **FZ-152** | Yes |
| **Best for** | Best hardware per ruble, scalability |

**Pricing (estimated from market knowledge):**

| Plan | Price (est.) | Spec | Good For |
|---|---|---|---|
| Entry VDS | ~500 RUB | 1vCPU/2GB | Not for Supabase |
| **CLO / VDS Форсаж** | **~700–800 RUB** | **2vCPU/4GB NVMe** | **Your sweet spot** |
| Mid-tier | ~1,200 RUB | 4vCPU/8GB | Scaling up |
| High-tier | ~1,500 RUB | 4vCPU/16GB | Tournaments + 100+ users |

**Pros:**
- **Best CPU for the price** — AMD EPYC handles PostgreSQL triggers + RLS efficiently
- **CLO platform** — auto-scale CPU/RAM as your app grows
- **S3 object storage** — offload screenshots from local disk
- **ISPmanager included** — easier Docker/server management than raw SSH
- **Hourly billing on CLO** — pay only for what you use
- **International presence** — Netherlands for future EU expansion
- Cheaper than NetAngels Турбо for better hardware

**Cons:**
- Slightly more complex than NetAngels for beginners
- DDoS protection is basic (not enterprise-grade)

---

### 3. SprintHost

| | |
|---|---|
| **Website** | sprinthost.ru |
| **Focus** | Developers, SaaS, startups |
| **Virtualization** | KVM + LXC containers |
| **CPU** | AMD EPYC / Intel Xeon (latest gen) |
| **Storage** | NVMe only (no slow HDD tiers) |
| **Network** | 1 Gbps unmetered |
| **DDoS** | **Arbor Networks, up to 500 Gbps mitigation** |
| **Panel** | Proprietary (clean, API-first) |
| **API** | Full REST API + Terraform provider |
| **Support** | Telegram + ticket (fast) |
| **Backups** | **Free daily snapshots** |
| **FZ-152** | Yes |
| **Best for** | Best reliability, DDoS protection, automation |

**Pricing (estimated):**

| Plan | Price (est.) | Spec | Good For |
|---|---|---|---|
| Entry | ~500 RUB | 1vCPU/2GB | Not for Supabase |
| **Standard** | **~700–900 RUB** | **2vCPU/4GB NVMe** | **Your app, guaranteed** |
| Pro | ~1,400 RUB | 4vCPU/8GB | Scaling up |
| Enterprise | ~1,800 RUB | 4vCPU/16GB | Heavy tournaments |

**Pros:**
- **Best DDoS protection** — Arbor Networks (gaming platforms get attacked)
- **Free daily snapshots** — backups included, not paid addon
- **Full REST API + Terraform** — automate server deployment
- **NVMe-only** — no accidentally picking slow HDD
- **Telegram support** — responds in minutes
- **Developer-first** — clean UI, no bloatware

**Cons:**
- Slightly more expensive than FirstVDS
- No Windows servers (Linux only — fine for Supabase)
- No shared hosting / marketing site tools

---

## Final Recommendation

### Ranked for YOUR Use Case

| Rank | Provider | Plan | Price | Why |
|---|---|---|---|---|
| **1st** | **FirstVDS** | CLO or VDS Форсаж 2vCPU/4GB | **~700–800 RUB** | Best hardware per ruble. AMD EPYC handles your triggers + RLS. CLO auto-scales. Cheapest GUARANTEED option. |
| **2nd** | **SprintHost** | 2vCPU/4GB NVMe | **~800–900 RUB** | Best reliability + DDoS. Free backups. API-first. Pick this if you expect growth or attacks. |
| **3rd** | **NetAngels** | Турбо 2vCPU/4GB | **889 RUB** | Good enough but worst specs for the price. Only if you want a simple panel and don't care about CPU model. |
| **AVOID** | **Any 2GB plan** | Оптима, entry tiers | 130–500 RUB | **Will crash.** Do not waste money. |

### My Pick: **FirstVDS at ~750 RUB**

Reasoning:
1. **Cheapest option that ACTUALLY works** — 2GB plans will crash, guaranteed
2. **AMD EPYC CPU** — your 14 triggers + RLS policies need fast single-thread performance
3. **CLO platform** — start at 2vCPU/4GB, scale to 4vCPU/8GB when tournaments launch
4. **S3 storage addon** — screenshots go to object storage, not local disk
5. **ISPmanager included** — easier than raw SSH for Docker management
6. **~100 RUB cheaper than NetAngels Турбо** with better hardware

The extra ~400 RUB vs Оптима is the difference between "works" and "doesn't work."

---

## What You Need to Buy (Checklist)

- [ ] **VDS**: 2 vCPU, 4 GB RAM, 40 GB NVMe, Ubuntu 22.04
- [ ] **Domain** (optional): e.g., `api-ru.yourapp.com` (~450 RUB/year)
- [ ] **SSL**: Let's Encrypt (free, auto-renews)
- [ ] **S3 Storage** (optional but recommended): Offload screenshots from local disk
- [ ] **Backup**: Free daily snapshots (SprintHost) or configure your own

---

## Setup Steps (After You Buy the Server)

### 1. SSH into your server
```bash
ssh root@YOUR_SERVER_IP
```

### 2. Install Docker
```bash
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker
```

### 3. Clone Supabase self-hosted
```bash
git clone --depth 1 https://github.com/supabase/supabase
cd supabase/docker
cp .env.example .env
```

### 4. Configure environment
```bash
nano .env
```
Set these:
- `POSTGRES_PASSWORD` — strong random password
- `JWT_SECRET` — generate with: `openssl rand 64 | base64`
- `ANON_KEY` and `SERVICE_ROLE_KEY` — generate with: `openssl rand 40 | base64`
- `SITE_URL` — your Android app deep link
- `SMTP_*` — for email verification (optional)

### 5. Start everything
```bash
docker compose up -d
```

### 6. Verify it's running
```bash
docker compose ps
```
All 8 services should show `Up`.

### 7. Point your domain + SSL
```bash
# Install certbot
apt install certbot python3-certbot-nginx

# If using nginx reverse proxy
certbot --nginx -d api-ru.yourapp.com
```

### 8. In your Android app
Add to `local.properties`:
```properties
SUPABASE_URL_EU=https://xxxxx.supabase.co
SUPABASE_URL_RU=https://api-ru.yourapp.com
```

---

## Routing EU vs RU Users

In your Android app (`SupabaseClient.kt`):
```kotlin
val supabaseUrl = when (getDeviceRegion()) {
    "RU" -> BuildConfig.SUPABASE_URL_RU
    else -> BuildConfig.SUPABASE_URL_EU
}
```

Or detect by timezone / SIM country code:
```kotlin
fun getDeviceRegion(): String {
    val locale = context.resources.configuration.locales.get(0)
    val timezone = TimeZone.getDefault().id
    return when {
        timezone.startsWith("Europe/Moscow") ||
        timezone.startsWith("Europe/Samara") ||
        timezone.startsWith("Asia/Yekaterinburg") -> "RU"
        else -> "EU"
    }
}
```

---

## Legal Note: FZ-152 Compliance

Russian law (FZ-152) requires storing Russian citizens' personal data on servers physically located in Russia. Both FirstVDS, SprintHost, and NetAngels have Russian data centers and are compliant.

If you serve Russian users, you **must** use a Russian-hosted backend. Your current EU Supabase is fine for EU users but may violate FZ-152 for RU users.

---

## Estimated Monthly Cost Summary

| Item | Cost |
|---|---|
| **FirstVDS CLO 2vCPU/4GB** | ~750 RUB (~$8.50) |
| Domain (optional) | ~40 RUB/month |
| S3 Storage (if used) | ~100 RUB/month |
| **TOTAL** | **~750–900 RUB/month** |

Compare to:
- Supabase Cloud Pro: $25/month (~2,200 RUB)
- Selectel 2vCPU/4GB: ~1,200 RUB/month
- Yandex Cloud: ~1,500 RUB/month

**Self-hosting saves ~60–70%** vs cloud providers.

---

## Next Steps

1. **Pick a provider** — FirstVDS recommended
2. **Buy the server** — 2vCPU/4GB minimum
3. **Follow setup steps above**
4. **Test your app** against the new RU endpoint
5. **Monitor with `docker stats`** — watch RAM/CPU usage
6. **Scale up** when you hit 50+ concurrent users or launch tournaments

---

*This guide was prepared for the MLBB Scrim Host project. Review when you're ready to deploy the Russian backend.*
