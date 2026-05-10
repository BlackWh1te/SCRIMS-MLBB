package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.data.model.TeamInvitation
import com.mlbb.scrim.data.model.TeamMember
import com.mlbb.scrim.data.service.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class TeamRepository {

    suspend fun createTeam(
        name: String,
        leaderId: UUID,
        description: String?,
        minPlayers: Int,
        maxPlayers: Int
    ): Result<Team> = withContext(Dispatchers.IO) {
        try {
            val team = SupabaseClient.postgrest.from("teams")
                .insert {
                    set("name", name)
                    set("leader_id", leaderId)
                    set("description", description)
                    set("min_players", minPlayers)
                    set("max_players", maxPlayers)
                }
                .decodeSingle<Team>()

            // Add leader as a member
            SupabaseClient.postgrest.from("team_members")
                .insert {
                    set("team_id", team.id)
                    set("user_id", leaderId)
                    set("role", "Leader")
                }

            Result.success(team)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeam(teamId: UUID): Result<Team> = withContext(Dispatchers.IO) {
        try {
            val team = SupabaseClient.postgrest.from("teams")
                .select {
                    filter {
                        eq("id", teamId)
                    }
                }
                .decodeSingle<Team>()

            Result.success(team)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserTeam(userId: UUID): Result<Team?> = withContext(Dispatchers.IO) {
        try {
            val teamMember = SupabaseClient.postgrest.from("team_members")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingle<TeamMember>()

            val team = SupabaseClient.postgrest.from("teams")
                .select {
                    filter {
                        eq("id", teamMember.teamId)
                    }
                }
                .decodeSingle<Team>()

            Result.success(team)
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    suspend fun getTeamMembers(teamId: UUID): Result<List<TeamMember>> = withContext(Dispatchers.IO) {
        try {
            val members = SupabaseClient.postgrest.from("team_members")
                .select {
                    filter {
                        eq("team_id", teamId)
                    }
                }
                .decodeList<TeamMember>()

            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun invitePlayer(teamId: UUID, invitedUserId: UUID, invitedBy: UUID): Result<TeamInvitation> = withContext(Dispatchers.IO) {
        try {
            val invitation = SupabaseClient.postgrest.from("team_invitations")
                .insert {
                    set("team_id", teamId)
                    set("invited_user_id", invitedUserId)
                    set("invited_by", invitedBy)
                }
                .decodeSingle<TeamInvitation>()

            Result.success(invitation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvitation(invitationId: UUID, userId: UUID): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val invitation = SupabaseClient.postgrest.from("team_invitations")
                .select {
                    filter {
                        eq("id", invitationId)
                    }
                }
                .decodeSingle<TeamInvitation>()

            // Add user to team
            SupabaseClient.postgrest.from("team_members")
                .insert {
                    set("team_id", invitation.teamId)
                    set("user_id", userId)
                    set("role", "Member")
                }

            // Update invitation status
            SupabaseClient.postgrest.from("team_invitations")
                .update {
                    set("status", "Accepted")
                } {
                    filter {
                        eq("id", invitationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingInvitations(userId: UUID): Result<List<TeamInvitation>> = withContext(Dispatchers.IO) {
        try {
            val invitations = SupabaseClient.postgrest.from("team_invitations")
                .select {
                    filter {
                        eq("invited_user_id", userId)
                        eq("status", "Pending")
                    }
                }
                .decodeList<TeamInvitation>()

            Result.success(invitations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTeamSettings(
        teamId: UUID,
        description: String?,
        availableDays: List<Int>,
        availableTimeStart: String?,
        availableTimeEnd: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseClient.postgrest.from("teams")
                .update {
                    set("description", description)
                    set("available_days", availableDays)
                    set("available_time_start", availableTimeStart)
                    set("available_time_end", availableTimeEnd)
                } {
                    filter {
                        eq("id", teamId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}