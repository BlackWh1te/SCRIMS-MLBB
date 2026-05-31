package com.scrimslegends.app.util

/**
 * Centralized configuration for free-tier Supabase usage.
 *
 * All polling intervals, backoff parameters, and feature toggles live here
 * so they can be tuned in one place without touching multiple ViewModels.
 *
 * The app is on Supabase **Free Tier** which limits:
 * - API calls: ~100 req/min (varies by plan)
 * - Realtime messages: 1,000,000/month
 * - DB size: 500MB
 *
 * These values are tuned for a low-budget, high-user-count deployment.
 * Increase intervals if you see 429 (Too Many Requests) errors.
 */
object FreeTierConfig {

    // ── Polling Intervals (REST fallback when Realtime is down) ──

    /** How often to poll the conversation list (MessageListScreen). */
    const val CONVERSATION_POLL_INTERVAL_MS = 30_000L  // 30s (was 10s)

    /** How often to poll messages inside a chat when Realtime is not connected. */
    const val CHAT_FALLBACK_POLL_INTERVAL_MS = 15_000L // 15s (was 5s)

    /** How often to poll for scrim list updates (ScrimListScreen fallback). */
    const val SCRIM_POLL_INTERVAL_MS = 30_000L         // 30s

    /** How often to poll for notification updates. */
    const val NOTIFICATION_POLL_INTERVAL_MS = 60_000L  // 60s

    // ── Exponential Backoff ──

    /** Initial delay after a 429/503 before retrying a poll. */
    const val BACKOFF_INITIAL_MS = 5_000L

    /** Maximum delay between retries. */
    const val BACKOFF_MAX_MS = 60_000L

    /** Multiplier for each consecutive failure. */
    const val BACKOFF_MULTIPLIER = 2.0

    /** How many consecutive failures before we cap at max delay. */
    const val BACKOFF_MAX_FAILURES = 5

    // ── Realtime Limits ──

    /**
     * Whether to subscribe to ALL scrim updates on the Home screen.
     * Disabled for free tier — subscribe only when on ScrimListScreen
     * to avoid burning through the 1M realtime message quota.
     */
    const val SUBSCRIBE_ALL_SCRIMS_ON_HOME = false

    // ── Typing Indicators ──

    /** How long a typing indicator stays active after the user stops typing. */
    const val TYPING_INDICATOR_DURATION_MS = 3_000L
}