package com.scrimslegends.app.security

import com.scrimslegends.app.data.service.SupabaseSession

/**
 * Client-side authorization guard for sensitive repository operations.
 *
 * IMPORTANT: These checks are a UX optimization and defense-in-depth layer.
 * The authoritative access control is Supabase RLS (Row Level Security).
 * Never rely solely on client-side validation.
 */
object AuthorizationUtils {

    fun currentUserId(): String? = SupabaseSession.getUserIdOrNull()

    fun requireAuth(): Result<String> {
        val userId = currentUserId()
        return if (userId != null) {
            Result.success(userId)
        } else {
            Result.failure(SecurityException("Unauthorized: no authenticated user"))
        }
    }

    fun requireOwner(
        resourceOwnerId: String,
        action: String = "perform this action"
    ): Result<Unit> {
        val userId = currentUserId()
        return when {
            userId == null -> Result.failure(SecurityException("Unauthorized: not authenticated"))
            userId != resourceOwnerId -> Result.failure(
                SecurityException("Forbidden: you do not have permission to $action")
            )
            else -> Result.success(Unit)
        }
    }

    fun requireLeader(
        teamLeaderId: String,
        action: String = "perform this action"
    ): Result<Unit> = requireOwner(teamLeaderId, action)

    fun requireMemberOrSelf(
        teamMemberIds: List<String>,
        targetUserId: String,
        action: String = "perform this action"
    ): Result<Unit> {
        val userId = currentUserId()
        return when {
            userId == null -> Result.failure(SecurityException("Unauthorized: not authenticated"))
            userId != targetUserId && userId !in teamMemberIds -> Result.failure(
                SecurityException("Forbidden: you do not have permission to $action")
            )
            else -> Result.success(Unit)
        }
    }

    fun requireParticipant(
        participantIds: List<String>,
        action: String = "perform this action"
    ): Result<Unit> {
        val userId = currentUserId()
        return when {
            userId == null -> Result.failure(SecurityException("Unauthorized: not authenticated"))
            userId !in participantIds -> Result.failure(
                SecurityException("Forbidden: you do not have permission to $action")
            )
            else -> Result.success(Unit)
        }
    }
}
