package com.scrimslegends.app.data.model

/**
 * All notification types used by the app.
 *
 * Two origins:
 *  - DB-generated: written by Postgres trigger functions in migrations
 *  - App-generated: written by Android client via createNotification()
 *
 * Keep this enum in sync with every INSERT INTO app_notifications
 * found across supabase/migrations/ (all .sql files).
 *
 * Mapping rules (DB string → enum):
 *   NotificationType.valueOf(dto.type)  — exact name match required.
 *   Unrecognised strings fall back to SYSTEM.
 */
enum class NotificationType {
    // ── Scrim notifications ──────────────────────────────────
    SCRIM_INVITE,               // App-generated: direct scrim invite to a team
    SCRIM_APPLICATION_NEW,      // DB: host receives a new application to their scrim
    SCRIM_APPLICATION_APPROVED, // DB: applicant's application was approved
    SCRIM_APPLICATION_REJECTED, // DB: applicant's application was declined

    // ── Match / result notifications ─────────────────────────
    MATCH_RESULT,               // App-generated: match result recorded

    // ── Team notifications ────────────────────────────────────
    TEAM_INVITE,                // DB (trigger) + App: user invited to join a team

    // ── Messaging notifications ───────────────────────────────
    MESSAGE,                    // App-generated: new chat message

    // ── System / progress notifications ──────────────────────
    SYSTEM,                     // Fallback and admin messages
    XP_GAIN,                    // App-generated: XP awarded
    TIER_UP,                    // App-generated: tier rank-up

    // ── Tournament notifications (DB-generated) ───────────────
    TOURNAMENT_APPLICATION_NEW,      // DB: host receives a new tournament application
    TOURNAMENT_APPLICATION_ACCEPTED, // DB: team's application was accepted
    TOURNAMENT_APPLICATION_REJECTED, // DB: team's application was rejected
    TOURNAMENT_APPLICATION_BLOCKED,  // DB: team's application was blocked (3+ rejections)
    TOURNAMENT_CANCELLED,            // DB: tournament was cancelled
    TOURNAMENT_COMPLETED,            // DB: tournament has concluded
    TOURNAMENT_ROUND_ADVANCED,       // DB: a new round has been generated
    TOURNAMENT_TEAM_DISQUALIFIED,    // DB: the user's team was disqualified
    TOURNAMENT_ROSTER_LOCKED,        // DB: tournament roster is locked
    TOURNAMENT_MATCH_RESULT,         // DB + App: individual match result in a tournament

    // ── Tournament notifications (legacy app-generated names) ─
    // Kept so that any existing cached/local rows survive the migration.
    TOURNAMENT_APPLICATION_STATUS,   // Legacy: maps to APPLICATION_ACCEPTED/REJECTED
    TOURNAMENT_MATCH_SCHEDULED,      // Legacy: match time scheduled
    TOURNAMENT_ROUND_START,          // Legacy alias for TOURNAMENT_ROUND_ADVANCED
    TOURNAMENT_HOST_REQUEST_STATUS,  // Legacy: host-request status update
    TOURNAMENT_HOST_APPROVED,        // DB + App: tournament host request approved
    TOURNAMENT_HOST_REJECTED,        // DB + App: tournament host request rejected
}

// ─── Semantic helpers ─────────────────────────────────────────────────────────

/**
 * Returns true for notifications controlled by the "Match Alerts" setting:
 * scrims, matches, tournament events, XP/tier.
 */
fun NotificationType.isMatchType(): Boolean = when (this) {
    NotificationType.SCRIM_INVITE,
    NotificationType.SCRIM_APPLICATION_NEW,
    NotificationType.SCRIM_APPLICATION_APPROVED,
    NotificationType.SCRIM_APPLICATION_REJECTED,
    NotificationType.MATCH_RESULT,
    NotificationType.XP_GAIN,
    NotificationType.TIER_UP,
    NotificationType.TOURNAMENT_APPLICATION_NEW,
    NotificationType.TOURNAMENT_APPLICATION_ACCEPTED,
    NotificationType.TOURNAMENT_APPLICATION_REJECTED,
    NotificationType.TOURNAMENT_APPLICATION_BLOCKED,
    NotificationType.TOURNAMENT_CANCELLED,
    NotificationType.TOURNAMENT_COMPLETED,
    NotificationType.TOURNAMENT_ROUND_ADVANCED,
    NotificationType.TOURNAMENT_TEAM_DISQUALIFIED,
    NotificationType.TOURNAMENT_ROSTER_LOCKED,
    NotificationType.TOURNAMENT_MATCH_RESULT,
    NotificationType.TOURNAMENT_APPLICATION_STATUS,
    NotificationType.TOURNAMENT_MATCH_SCHEDULED,
    NotificationType.TOURNAMENT_ROUND_START,
    NotificationType.TOURNAMENT_HOST_REQUEST_STATUS,
    NotificationType.TOURNAMENT_HOST_APPROVED,
    NotificationType.TOURNAMENT_HOST_REJECTED -> true
    else -> false
}

/**
 * Returns true for notifications controlled by the "Message Alerts" setting.
 */
fun NotificationType.isMessageType(): Boolean = this == NotificationType.MESSAGE

// ─── Data model ───────────────────────────────────────────────────────────────

data class Notification(
    val id: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionId: String = "", // scrimId, teamId, tournamentId, etc.
    val imageUrl: String? = null
) {
    val icon: String
        get() = when (type) {
            NotificationType.SCRIM_INVITE               -> "sports_esports"
            NotificationType.SCRIM_APPLICATION_NEW      -> "sports_esports"
            NotificationType.SCRIM_APPLICATION_APPROVED -> "check_circle"
            NotificationType.SCRIM_APPLICATION_REJECTED -> "cancel"
            NotificationType.MATCH_RESULT               -> "emoji_events"
            NotificationType.TEAM_INVITE                -> "group"
            NotificationType.MESSAGE                    -> "chat"
            NotificationType.SYSTEM                     -> "info"
            NotificationType.XP_GAIN                    -> "trending_up"
            NotificationType.TIER_UP                    -> "star"
            NotificationType.TOURNAMENT_APPLICATION_NEW      -> "emoji_events"
            NotificationType.TOURNAMENT_APPLICATION_ACCEPTED -> "check_circle"
            NotificationType.TOURNAMENT_APPLICATION_REJECTED -> "cancel"
            NotificationType.TOURNAMENT_APPLICATION_BLOCKED  -> "block"
            NotificationType.TOURNAMENT_CANCELLED            -> "cancel"
            NotificationType.TOURNAMENT_COMPLETED            -> "emoji_events"
            NotificationType.TOURNAMENT_ROUND_ADVANCED       -> "play_arrow"
            NotificationType.TOURNAMENT_TEAM_DISQUALIFIED    -> "person_off"
            NotificationType.TOURNAMENT_ROSTER_LOCKED        -> "lock"
            NotificationType.TOURNAMENT_MATCH_RESULT         -> "emoji_events"
            NotificationType.TOURNAMENT_APPLICATION_STATUS   -> "emoji_events"
            NotificationType.TOURNAMENT_MATCH_SCHEDULED      -> "schedule"
            NotificationType.TOURNAMENT_ROUND_START          -> "play_arrow"
            NotificationType.TOURNAMENT_HOST_REQUEST_STATUS  -> "verified"
            NotificationType.TOURNAMENT_HOST_APPROVED        -> "verified"
            NotificationType.TOURNAMENT_HOST_REJECTED        -> "cancel"
        }
}
