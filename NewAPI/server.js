require('dotenv').config();
const crypto = require('crypto');
const express = require('express');
const cron = require('node-cron');
const nodemailer = require('nodemailer');
const { scrapeAndArchive } = require('./scraper');
const {
    getArticlesForUser,
    getTotalCount,
    getUnseenCount,
    resetDb
} = require('./db');

const app = express();
const PORT = process.env.PORT || 3000;
const API_KEY = process.env.API_KEY || 'default-key';
const SCRAPE_INTERVAL_HOURS = parseInt(process.env.SCRAPE_INTERVAL_HOURS || '8', 10);
const OTP_TTL_MINUTES = parseInt(process.env.OTP_TTL_MINUTES || '10', 10);
const OTP_COOLDOWN_SECONDS = parseInt(process.env.OTP_COOLDOWN_SECONDS || '60', 10);
const OTP_MAX_SENDS_PER_DAY = parseInt(process.env.OTP_MAX_SENDS_PER_DAY || '20', 10);
const OTP_MAX_ATTEMPTS = parseInt(process.env.OTP_MAX_ATTEMPTS || '5', 10);
const SUPABASE_URL = process.env.SUPABASE_URL || '';
const SUPABASE_SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || '';
const GMAIL_USER = process.env.GMAIL_USER || '';
const GMAIL_APP_PASSWORD = (process.env.GMAIL_APP_PASSWORD || '').replace(/\s+/g, '');
const OTP_FROM_EMAIL = process.env.OTP_FROM_EMAIL || GMAIL_USER;
const OTP_FROM_NAME = process.env.OTP_FROM_NAME || 'MLBB Scrim Host';

const otpStore = new Map();
let lastScrapeTime = 0;

app.use(express.json());

app.use((req, res, next) => {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, X-API-Key');
    if (req.method === 'OPTIONS') {
        return res.sendStatus(200);
    }
    next();
});

const mailTransport = nodemailer.createTransport({
    host: 'smtp.gmail.com',
    port: 587,
    secure: false, // use STARTTLS (port 587 is open on Render, port 465 is blocked by default)
    auth: {
        user: GMAIL_USER,
        pass: GMAIL_APP_PASSWORD
    },
    connectionTimeout: 30000,
    greetingTimeout: 30000,
    socketTimeout: 30000
});

function checkApiKey(req, res, next) {
    const clientKey = req.headers['x-api-key'] || req.query.apiKey;
    // Uncomment for production:
    // if (clientKey !== API_KEY) {
    //     return res.status(401).json({ error: 'Invalid API key' });
    // }
    next();
}

function normalizeEmail(email) {
    return String(email || '').trim().toLowerCase();
}

function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function hashOtp(email, otp) {
    return crypto
        .createHash('sha256')
        .update(`${normalizeEmail(email)}:${otp}`)
        .digest('hex');
}

function generateOtp() {
    return String(Math.floor(100000 + Math.random() * 900000));
}

function getOtpEntry(email) {
    return otpStore.get(normalizeEmail(email));
}

function setOtpEntry(email, entry) {
    otpStore.set(normalizeEmail(email), entry);
}

function clearOtpEntry(email) {
    otpStore.delete(normalizeEmail(email));
}

function getNextScrapeMinutes() {
    if (!lastScrapeTime) return 0;
    const intervalMs = SCRAPE_INTERVAL_HOURS * 60 * 60 * 1000;
    const elapsed = Date.now() - lastScrapeTime;
    const remaining = Math.max(0, intervalMs - elapsed);
    return Math.floor(remaining / 60000);
}

function getMinutesSinceLastUnlock() {
    return 0;
}

function ensureOtpConfig() {
    if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
        return 'Server OTP configuration is incomplete. Missing Supabase admin credentials.';
    }
    if (!GMAIL_USER || !GMAIL_APP_PASSWORD || !OTP_FROM_EMAIL) {
        return 'Server OTP configuration is incomplete. Missing Gmail SMTP credentials.';
    }
    return null;
}

async function sendOtpEmail(email, otp) {
    await mailTransport.sendMail({
        from: `"${OTP_FROM_NAME}" <${OTP_FROM_EMAIL}>`,
        to: email,
        subject: 'Your MLBB Scrim Host verification code',
        text: `Your verification code is ${otp}. It expires in ${OTP_TTL_MINUTES} minutes.`,
        html: `
            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 500px; margin: 0 auto; background-color: #121212; color: #ffffff; padding: 40px; border-radius: 12px; border: 1px solid #333333; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);">
                <div style="text-align: center; margin-bottom: 30px;">
                    <h2 style="color: #c9a756; margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 1px; text-transform: uppercase;">MLBB Scrim Host</h2>
                    <p style="color: #888888; font-size: 14px; margin-top: 5px;">Account Verification</p>
                </div>
                
                <p style="font-size: 16px; line-height: 1.5; margin-bottom: 25px; color: #e0e0e0;">
                    Hello,<br><br>
                    You are receiving this email because a request was made to verify this address for an <strong>MLBB Scrim Host</strong> account. Please use the security code below to proceed:
                </p>
                
                <div style="background-color: #1a1a1a; border: 1px solid #c9a756; border-radius: 8px; padding: 20px; text-align: center; margin-bottom: 25px;">
                    <div style="font-size: 42px; font-weight: bold; letter-spacing: 8px; color: #c9a756; text-shadow: 0 0 10px rgba(201, 167, 86, 0.2);">
                        ${otp}
                    </div>
                </div>
                
                <div style="font-size: 14px; color: #a0a0a0; background-color: rgba(255, 77, 77, 0.1); padding: 15px; border-left: 4px solid #ff4d4d; border-radius: 4px; margin-bottom: 30px; line-height: 1.5;">
                    <strong style="color: #ff4d4d;">Security Warning:</strong><br>
                    This code will expire in <strong>${OTP_TTL_MINUTES} minutes</strong>. Do not share this code with anyone. MLBB Scrim Host staff will never ask for your verification code.
                </div>
                
                <hr style="border: none; border-top: 1px solid #333333; margin-bottom: 20px;" />
                
                <p style="font-size: 12px; color: #666666; text-align: center; line-height: 1.5;">
                    If you did not request this code, you can safely ignore this email. Another user might have entered your email address by mistake.<br><br>
                    &copy; ${new Date().getFullYear()} MLBB Scrim Host. All rights reserved.
                </p>
            </div>
        `
    });
}

async function createVerifiedSupabaseUser({ email, password, username, inGameId }) {
    const authResponse = await fetch(`${SUPABASE_URL}/auth/v1/admin/users`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            apikey: SUPABASE_SERVICE_ROLE_KEY,
            Authorization: `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`
        },
        body: JSON.stringify({
            email,
            password,
            email_confirm: true,
            user_metadata: {
                username,
                mlbb_id: inGameId
            }
        })
    });

    const authJson = await authResponse.json().catch(() => ({}));
    if (!authResponse.ok) {
        throw new Error(authJson.msg || authJson.error_description || authJson.error || 'Failed to create user');
    }

    const userId = authJson.user?.id || authJson.id;
    if (!userId) {
        throw new Error('Supabase user creation returned no user id');
    }

    const profileResponse = await fetch(`${SUPABASE_URL}/rest/v1/profiles`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            apikey: SUPABASE_SERVICE_ROLE_KEY,
            Authorization: `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
            Prefer: 'resolution=merge-duplicates,return=representation'
        },
        body: JSON.stringify({
            id: userId,
            username,
            email,
            mlbb_id: inGameId,
            email_verified: true
        })
    });

    if (!profileResponse.ok && profileResponse.status !== 409) {
        const profileJson = await profileResponse.json().catch(() => ({}));
        throw new Error(profileJson.message || profileJson.error || 'Failed to create profile');
    }

    const statsResponse = await fetch(`${SUPABASE_URL}/rest/v1/player_stats`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            apikey: SUPABASE_SERVICE_ROLE_KEY,
            Authorization: `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
            Prefer: 'resolution=merge-duplicates,return=representation'
        },
        body: JSON.stringify({
            user_id: userId,
            pts: 0,
            wins: 0,
            losses: 0,
            matches_play: 0
        })
    });

    if (!statsResponse.ok && statsResponse.status !== 409) {
        const statsJson = await statsResponse.json().catch(() => ({}));
        throw new Error(statsJson.message || statsJson.error || 'Failed to create player stats');
    }
}

app.post('/auth/send-otp', async (req, res) => {
    const configError = ensureOtpConfig();
    if (configError) {
        return res.status(500).json({ status: 'error', message: configError });
    }

    const email = normalizeEmail(req.body?.email);
    if (!isValidEmail(email)) {
        return res.status(400).json({ status: 'error', message: 'Invalid email address.' });
    }

    const now = Date.now();
    const existingEntry = getOtpEntry(email);
    if (existingEntry) {
        const secondsSinceLastSend = Math.floor((now - existingEntry.lastSentAt) / 1000);
        if (secondsSinceLastSend < OTP_COOLDOWN_SECONDS) {
            return res.status(429).json({
                status: 'error',
                message: `Please wait ${OTP_COOLDOWN_SECONDS - secondsSinceLastSend} seconds before requesting another code.`
            });
        }

        const dayWindowStart = now - 24 * 60 * 60 * 1000;
        existingEntry.sendHistory = existingEntry.sendHistory.filter((timestamp) => timestamp >= dayWindowStart);
        if (existingEntry.sendHistory.length >= OTP_MAX_SENDS_PER_DAY) {
            return res.status(429).json({
                status: 'error',
                message: 'Daily email limit reached for this address. Please try again tomorrow.'
            });
        }
    }

    const otp = generateOtp();
    const entry = existingEntry || { sendHistory: [] };
    entry.otpHash = hashOtp(email, otp);
    entry.expiresAt = now + OTP_TTL_MINUTES * 60 * 1000;
    entry.lastSentAt = now;
    entry.sendHistory = [...entry.sendHistory, now];
    entry.attemptsLeft = OTP_MAX_ATTEMPTS;
    setOtpEntry(email, entry);

    try {
        await sendOtpEmail(email, otp);
        res.json({ status: 'ok', message: 'Verification code sent successfully.' });
    } catch (error) {
        clearOtpEntry(email);
        res.status(500).json({
            status: 'error',
            message: `Failed to send verification email: ${error.message}`
        });
    }
});

app.post('/auth/verify-otp', async (req, res) => {
    const configError = ensureOtpConfig();
    if (configError) {
        return res.status(500).json({ status: 'error', message: configError });
    }

    const email = normalizeEmail(req.body?.email);
    const otp = String(req.body?.otp || '').trim();
    const password = String(req.body?.password || '');
    const username = String(req.body?.username || '').trim();
    const inGameId = String(req.body?.inGameId || '').trim();

    if (!isValidEmail(email) || !/^\d{6}$/.test(otp)) {
        return res.status(400).json({ status: 'error', message: 'Invalid or expired code.' });
    }
    if (password.length < 6 || !username) {
        return res.status(400).json({ status: 'error', message: 'Missing signup details.' });
    }

    const entry = getOtpEntry(email);
    if (!entry) {
        return res.status(400).json({ status: 'error', message: 'Invalid or expired code.' });
    }
    if (Date.now() > entry.expiresAt) {
        clearOtpEntry(email);
        return res.status(400).json({ status: 'error', message: 'Code expired. Please request a new one.' });
    }
    if (entry.attemptsLeft <= 0) {
        clearOtpEntry(email);
        return res.status(429).json({ status: 'error', message: 'Too many incorrect attempts. Please request a new code.' });
    }
    if (entry.otpHash !== hashOtp(email, otp)) {
        entry.attemptsLeft -= 1;
        setOtpEntry(email, entry);
        return res.status(400).json({ status: 'error', message: 'Invalid or expired code.' });
    }

    try {
        await createVerifiedSupabaseUser({ email, password, username, inGameId });
        clearOtpEntry(email);
        res.json({ status: 'ok', message: 'Email verified successfully.' });
    } catch (error) {
        const message = String(error.message || '');
        const alreadyExists = message.toLowerCase().includes('already');
        res.status(alreadyExists ? 409 : 500).json({
            status: 'error',
            message: alreadyExists
                ? 'This email is already registered. Please sign in instead.'
                : `Failed to create verified account: ${message}`
        });
    }
});

app.get('/news', checkApiKey, async (req, res) => {
    const userOffset = parseInt(req.query.offset, 10);
    if (Number.isNaN(userOffset) || userOffset < 0) {
        return res.status(400).json({
            error: 'Missing or invalid ?offset query param (must be >= 0)'
        });
    }

    const limit = parseInt(req.query.limit, 10) || null;
    const articles = await getArticlesForUser(userOffset, limit);
    const total = await getTotalCount();
    const unseen = await getUnseenCount(userOffset);

    res.json({
        status: 'ok',
        articles,
        unlocked: articles.length,
        totalInArchive: total,
        unseen,
        userOffset,
        nextUnlockInMinutes: Math.max(0, 120 - getMinutesSinceLastUnlock(userOffset)),
        source: 'archive'
    });
});

app.get('/news/count', async (req, res) => {
    res.json({
        status: 'ok',
        total: await getTotalCount()
    });
});

app.get('/health', async (req, res) => {
    res.json({
        status: 'ok',
        uptime: process.uptime(),
        totalArticles: await getTotalCount(),
        scraperIntervalHours: SCRAPE_INTERVAL_HOURS,
        nextScrapeInMinutes: getNextScrapeMinutes(),
        otpEnabled: Boolean(SUPABASE_URL && SUPABASE_SERVICE_ROLE_KEY && GMAIL_USER && GMAIL_APP_PASSWORD)
    });
});

app.post('/admin/scrape', async (req, res) => {
    const adminKey = req.headers['x-admin-key'];
    if (adminKey !== API_KEY) {
        return res.status(401).json({ error: 'Unauthorized' });
    }

    try {
        const result = await scrapeAndArchive();
        res.json({
            status: 'ok',
            fetched: result.fetched,
            archived: result.added,
            totalInArchive: await getTotalCount()
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.post('/admin/reset', async (req, res) => {
    const adminKey = req.headers['x-admin-key'];
    if (adminKey !== API_KEY) {
        return res.status(401).json({ error: 'Unauthorized' });
    }
    await resetDb();
    res.json({ status: 'ok', message: 'Archive reset' });
});

async function scheduledScrape() {
    console.log(`[${new Date().toISOString()}] Running scheduled scrape...`);
    lastScrapeTime = Date.now();
    try {
        const result = await scrapeAndArchive();
        console.log(`[${new Date().toISOString()}] Scrape done: ${result.fetched} fetched, ${result.added} archived. Total archive: ${await getTotalCount()}`);
    } catch (error) {
        console.error(`[${new Date().toISOString()}] Scrape failed:`, error.message);
    }
}

console.log(`Scraper interval: every ${SCRAPE_INTERVAL_HOURS} hours`);
scheduledScrape();
cron.schedule(`0 */${SCRAPE_INTERVAL_HOURS} * * *`, scheduledScrape);

app.listen(PORT, () => {
    console.log(`MLBB backend listening on http://localhost:${PORT}`);
});
