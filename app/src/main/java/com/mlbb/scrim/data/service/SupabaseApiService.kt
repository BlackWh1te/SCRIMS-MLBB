package com.mlbb.scrim.data.service

import com.google.gson.annotations.SerializedName
import com.mlbb.scrim.data.model.TeamRole
import retrofit2.Response
import retrofit2.http.*

object PostgrestFilter {
    fun eq(value: String): String = "eq.$value"
    fun inList(values: List<String>): String = "in.(${values.joinToString(",")})"
}

// ─── Auth DTOs ───

data class SignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

data class SignInRequest(
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class OtpRequest(
    val email: String,
    val type: String = "signup",
    val data: Map<String, String>? = null
)

data class VerifyOtpRequest(
    val email: String,
    val token: String,
    val type: String = "signup"
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("expires_in") val expiresIn: Long = 0,
    @SerializedName("token_type") val tokenType: String = "",
    @SerializedName("user") val user: SupabaseUser? = null
)

data class SupabaseUser(
    @SerializedName("id") val id: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("user_metadata") val userMetadata: Map<String, Any>? = null,
    @SerializedName("email_confirmed_at") val emailConfirmedAt: String? = null
)

// ─── Profile DTO (matches database schema) ───

data class ProfileDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("mlbb_id") val mlbbId: String? = null,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("is_banned") val isBanned: Boolean = false,
    @SerializedName("email_verified") val emailVerified: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("role") val role: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("main_heroes") val mainHeroes: List<String>? = null
)

data class PlayerStatsDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("pts") val pts: Int = 0,
    @SerializedName("wins") val wins: Int = 0,
    @SerializedName("losses") val losses: Int = 0,
    @SerializedName("matches_play") val matchesPlay: Int = 0,
    @SerializedName("updated_at") val updatedAt: String? = null
)

// ─── Team DTOs ───

data class TeamDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("leader_id") val leaderId: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("min_players") val minPlayers: Int = 5,
    @SerializedName("max_players") val maxPlayers: Int = 7,
    @SerializedName("total_xp") val totalXp: Int = 0,
    @SerializedName("current_tier") val currentTier: String = "Bronze",
    @SerializedName("current_division") val currentDivision: Int = 1,
    // P1-1: Added missing DB columns for scheduling
    @SerializedName("available_days") val availableDays: List<String>? = null,
    @SerializedName("total_scrims") val totalScrims: Int = 0,
    @SerializedName("completed_scrims") val completedScrims: Int = 0,
    @SerializedName("reputation") val reputation: Float = 5.0f,
    @SerializedName("can_post_scrims_until") val canPostScrimsUntil: String? = null,
    @SerializedName("no_shows") val noShows: Int = 0,
    @SerializedName("available_time_start") val availableTimeStart: String? = null,
    @SerializedName("available_time_end") val availableTimeEnd: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("is_open_for_applications") val isOpenForApplications: Boolean = false,
    @SerializedName("created_at") val createdAt: String = ""
)

/**
 * Request DTO for creating a team. Excludes auto-generated fields (id, created_at).
 */
data class CreateTeamRequest(
    @SerializedName("name") val name: String,
    @SerializedName("leader_id") val leaderId: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("min_players") val minPlayers: Int = 5,
    @SerializedName("max_players") val maxPlayers: Int = 7,
    @SerializedName("is_open_for_applications") val isOpenForApplications: Boolean = false
)

data class TeamMemberDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("team_id") val teamId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("role") val role: String = TeamRole.MEMBER,
    @SerializedName("joined_at") val joinedAt: String = ""
)

/**
 * Request DTO for adding a team member. Excludes auto-generated fields (id, joined_at).
 */
data class AddTeamMemberRequest(
    @SerializedName("team_id") val teamId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("role") val role: String = TeamRole.MEMBER
)

// ─── Team Invitation DTO ───

data class TeamInvitationDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("team_id") val teamId: String = "",
    @SerializedName("invited_user_id") val invitedUserId: String = "",
    @SerializedName("invited_by") val invitedBy: String = "",
    @SerializedName("status") val status: String = "Pending",
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("responded_at") val respondedAt: String? = null
)

// ─── Scrim DTOs ───

data class ScrimDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("team_id") val teamId: String = "",
    @SerializedName("scheduled_date") val scheduledDate: String = "",
    @SerializedName("scheduled_time") val scheduledTime: String = "",
    @SerializedName("best_of") val bestOf: Int = 1,
    @SerializedName("status") val status: String = "Open",
    @SerializedName("description") val description: String? = null,
    @SerializedName("opponent_team_id") val opponentTeamId: String? = null,
    @SerializedName("opponent_team_name") val opponentTeamName: String? = null,
    @SerializedName("winner_team_id") val winnerTeamId: String? = null,
    @SerializedName("team_a_ready") val teamAReady: Boolean = false,
    @SerializedName("team_b_ready") val teamBReady: Boolean = false,
    @SerializedName("team_a_ready_at") val teamAReadyAt: String? = null,
    @SerializedName("team_b_ready_at") val teamBReadyAt: String? = null,
    @SerializedName("team_a_screenshot_url") val teamAScreenshotUrl: String? = null,
    @SerializedName("team_b_screenshot_url") val teamBScreenshotUrl: String? = null,
    @SerializedName("team_a_screenshot_uploaded_at") val teamAScreenshotUploadedAt: String? = null,
    @SerializedName("team_b_screenshot_uploaded_at") val teamBScreenshotUploadedAt: String? = null,
    // P1-2: Added missing DB columns
    @SerializedName("conversation_id") val conversationId: String? = null,
    @SerializedName("result_submitted_at") val resultSubmittedAt: String? = null,
    @SerializedName("cancellation_reason") val cancellationReason: String? = null,
    @SerializedName("cancelled_by") val cancelledBy: String? = null,
    // P1-7: Scrim search/filter fields
    @SerializedName("game_mode") val gameMode: String = "RANKED",
    @SerializedName("region") val region: String = "EU",
    @SerializedName("skill_level") val skillLevel: String = "ALL",
    @SerializedName("max_players") val maxPlayers: Int = 10,
    @SerializedName("current_players") val currentPlayers: Int = 0,
    @SerializedName("created_at") val createdAt: String = ""
)

// ─── Scrim Application DTO ───

data class ScrimApplicationDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("scrim_id") val scrimId: String = "",
    @SerializedName("applicant_team_id") val applicantTeamId: String = "",
    @SerializedName("status") val status: String = "Pending",
    @SerializedName("applied_at") val appliedAt: String = ""
)

// ─── Team Application DTO ───

data class TeamApplicationDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("team_id") val teamId: String = "",
    @SerializedName("applicant_user_id") val applicantUserId: String = "",
    @SerializedName("status") val status: String = "Pending",
    @SerializedName("message") val message: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("responded_at") val respondedAt: String? = null
)

// ─── Scrim Roster DTO ───

data class ScrimRosterDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("scrim_id") val scrimId: String = "",
    @SerializedName("team_id") val teamId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("is_active") val isActive: Boolean = false,
    @SerializedName("assigned_by") val assignedBy: String? = null,
    @SerializedName("assigned_at") val assignedAt: String = ""
)

// ─── Match DTO ───

data class MatchDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("scrim_id") val scrimId: String = "",
    @SerializedName("team_a_id") val teamAId: String = "",
    @SerializedName("team_b_id") val teamBId: String = "",
    @SerializedName("scheduled_date") val scheduledDate: String = "",
    @SerializedName("scheduled_time") val scheduledTime: String = "",
    @SerializedName("room_id") val roomId: String? = null,
    @SerializedName("room_password") val roomPassword: String? = null,
    @SerializedName("status") val status: String = "Scheduled",
    @SerializedName("created_at") val createdAt: String = ""
)

// ─── LFG DTO ───

data class LfgPostDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("player_id") val playerId: String = "",
    @SerializedName("player_name") val playerName: String = "",
    @SerializedName("role") val role: String = "",
    @SerializedName("region") val region: String = "",
    @SerializedName("skill_level") val skillLevel: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("main_heroes") val mainHeroes: List<String>? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("rank") val rank: String? = null,
    @SerializedName("total_matches") val totalMatches: Int? = null,
    @SerializedName("win_rate") val winRate: String? = null,
    @SerializedName("ranked_win_rate") val rankedWinRate: String? = null,
    @SerializedName("in_game_id") val inGameId: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("screenshot_url") val screenshotUrl: String? = null,
    @SerializedName("is_available") val isAvailable: Boolean? = null,
    @SerializedName("use_mic") val useMic: Boolean? = null,
    @SerializedName("playstyle_tags") val playstyleTags: List<String>? = null,
    @SerializedName("discord") val discord: String? = null,
    @SerializedName("telegram") val telegram: String? = null,
    @SerializedName("vk") val vk: String? = null,
    @SerializedName("facebook") val facebook: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// ─── Match Result DTO ───

data class MatchResultDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("match_id") val matchId: String = "",
    @SerializedName("team_a_screenshot_url") val teamAScreenshotUrl: String? = null,
    @SerializedName("team_b_screenshot_url") val teamBScreenshotUrl: String? = null,
    @SerializedName("winner_team_id") val winnerTeamId: String? = null,
    @SerializedName("admin_verified") val adminVerified: Boolean = false,
    @SerializedName("verified_by") val verifiedBy: String? = null,
    @SerializedName("verification_notes") val verificationNotes: String? = null,
    @SerializedName("xp_awarded") val xpAwarded: Boolean = false,
    @SerializedName("pts_awarded") val ptsAwarded: Boolean = false,
    @SerializedName("created_at") val createdAt: String = ""
)

// ─── Team Rating DTO ───

data class TeamRatingDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("team_id") val teamId: String = "",
    @SerializedName("rater_team_id") val raterTeamId: String = "",
    @SerializedName("rater_team_name") val raterTeamName: String = "",
    @SerializedName("rater_user_name") val raterUserName: String = "",
    @SerializedName("rating") val rating: Int = 0,
    @SerializedName("feedback") val feedback: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

// ─── Leaderboard DTO ───

data class LeaderboardEntryDto(
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("pts") val pts: Int = 0,
    @SerializedName("wins") val wins: Int = 0,
    @SerializedName("losses") val losses: Int = 0,
    @SerializedName("matches_play") val matchesPlay: Int = 0,
    @SerializedName("profiles") val profile: ProfileNameDto? = null
)

data class ProfileNameDto(
    @SerializedName("username") val username: String = ""
)

// ─── Message DTO ───

// ─── Notification DTO ───

data class NotificationDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("message") val message: String = "",
    @SerializedName("action_id") val actionId: String? = null,
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("created_at") val createdAt: String = ""
)

// ─── Message DTO ───

data class MessageDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("conversation_id") val conversationId: String = "",
    @SerializedName("match_id") val matchId: String? = null,
    @SerializedName("sender_id") val senderId: String = "",
    @SerializedName("sender_team_id") val senderTeamId: String? = null,
    @SerializedName("sender_name") val senderName: String? = null,
    @SerializedName("content") val content: String = "",
    @SerializedName("is_read") val isRead: Boolean = false,
    @SerializedName("read_at") val readAt: String? = null,
    @SerializedName("type") val type: String = "TEXT",
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("voice_url") val voice_url: String? = null,
    @SerializedName("voice_duration") val voiceDuration: Int? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

data class ConversationDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("scrim_id") val scrimId: String? = null,
    @SerializedName("participant_a_id") val participantAId: String = "",
    @SerializedName("participant_a_name") val participantAName: String = "",
    @SerializedName("participant_a_team_id") val participantATeamId: String = "",
    @SerializedName("participant_a_team_name") val participantATeamName: String = "",
    @SerializedName("participant_b_id") val participantBId: String = "",
    @SerializedName("participant_b_name") val participantBName: String = "",
    @SerializedName("participant_b_team_id") val participantBTeamId: String = "",
    @SerializedName("participant_b_team_name") val participantBTeamName: String = "",
    @SerializedName("last_message") val lastMessage: String = "",
    @SerializedName("last_message_time") val lastMessageTime: String = "",
    @SerializedName("chat_opens_at") val chatOpensAt: String = "",
    @SerializedName("participant_a_typing") val participantATyping: Boolean = false,
    @SerializedName("participant_b_typing") val participantBTyping: Boolean = false,
    @SerializedName("unread_count") val unreadCount: Int = 0
)

/**
 * Supabase Auth API service.
 *
 * Uses Supabase's Auth API for authentication operations.
 */
interface SupabaseAuthService {

    // ─── Auth Endpoints ───

    @POST("signup")
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthResponse>

    @POST("token?grant_type=password")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthResponse>

    @POST("token?grant_type=refresh_token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("logout")
    suspend fun signOut(@Header("Authorization") authHeader: String): Response<Unit>

    @POST("recover")
    suspend fun resetPassword(@Body request: Map<String, String>): Response<Unit>

    @POST("otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<Unit>

    @POST("verify")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthResponse>

    @PUT("user")
    suspend fun updateUser(
        @Header("Authorization") authHeader: String,
        @Body request: Map<String, String>
    ): Response<SupabaseUser>

    @GET("user")
    suspend fun getCurrentUser(@Header("Authorization") authHeader: String): Response<SupabaseUser>
}

object SupabaseAuthServiceClient {
    val api: SupabaseAuthService by lazy {
        SupabaseAuthRetrofitClient.retrofit.create(SupabaseAuthService::class.java)
    }
}

/**
 * Supabase REST API service.
 *
 * Uses Supabase's auto-generated PostgREST API for database operations.
 */
interface SupabaseApiService {

    // ─── Profile Endpoints ───

    @GET("profiles")
    suspend fun getProfiles(
        @Query("id") idFilter: String? = null,
        @Query("select") select: String = "*"
    ): Response<List<ProfileDto>>

    @GET("profiles")
    suspend fun getProfileById(
        @Query("id") id: String,
        @Query("select") select: String = "*"
    ): Response<List<ProfileDto>>

    @GET("profiles")
    suspend fun getProfileByMlbbId(
        @Query("mlbb_id") mlbbId: String,
        @Query("select") select: String = "id,is_banned"
    ): Response<List<ProfileDto>>

    @GET("profiles")
    suspend fun getProfileByEmail(
        @Query("email") email: String,
        @Query("select") select: String = "*"
    ): Response<List<ProfileDto>>

    @POST("profiles")
    suspend fun createProfile(@Body profile: ProfileDto): Response<List<ProfileDto>>

    @PATCH("profiles")
    suspend fun updateProfile(
        @Query("id") id: String,
        @Body profile: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<ProfileDto>>

    // ─── Player Stats Endpoints ───

    @GET("player_stats")
    suspend fun getPlayerStats(
        @Query("user_id") userId: String? = null
    ): Response<List<PlayerStatsDto>>

    @PATCH("player_stats")
    suspend fun updatePlayerStats(
        @Query("user_id") userId: String,
        @Body stats: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PlayerStatsDto>>

    @GET("player_stats")
    suspend fun getLeaderboard(
        @Query("select") select: String = "*,profiles(username)",
        @Query("order") order: String = "pts.desc",
        @Header("Range") range: String = "0-49"
    ): Response<List<LeaderboardEntryDto>>

    @POST("player_stats")
    suspend fun createPlayerStats(@Body stats: PlayerStatsDto): Response<List<PlayerStatsDto>>

    // ─── Team Endpoints ───

    @GET("teams")
    suspend fun getTeams(): Response<List<TeamDto>>

    @GET("teams")
    suspend fun getTeamsByIds(
        @Query("id") idFilter: String
    ): Response<List<TeamDto>>

    @GET("teams")
    suspend fun getTeamById(
        @Query("id") id: String
    ): Response<List<TeamDto>>

    @POST("teams")
    suspend fun createTeam(@Body team: CreateTeamRequest): Response<List<TeamDto>>

    @PATCH("teams")
    suspend fun updateTeam(
        @Query("id") id: String,
        @Body team: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<TeamDto>>

    @DELETE("teams")
    suspend fun deleteTeam(@Query("id") id: String): Response<Unit>

    // ─── Team Members Endpoints ───

    @GET("team_members")
    suspend fun getTeamMembers(
        @Query("id") id: String? = null,
        @Query("team_id") teamId: String? = null,
        @Query("user_id") userId: String? = null,
        @Query("select") select: String = "*"
    ): Response<List<TeamMemberDto>>

    @POST("team_members")
    suspend fun addTeamMember(@Body member: AddTeamMemberRequest): Response<List<TeamMemberDto>>

    @DELETE("team_members")
    suspend fun removeTeamMember(
        @Query("team_id") teamId: String,
        @Query("user_id") userId: String
    ): Response<Unit>

    @PATCH("team_members")
    suspend fun updateTeamMemberRole(
        @Query("team_id") teamId: String,
        @Query("user_id") userId: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    // ─── Team Invitation Endpoints ───

    @GET("team_invitations")
    suspend fun getTeamInvitations(
        @Query("team_id") teamId: String? = null,
        @Query("invited_user_id") invitedUserId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<TeamInvitationDto>>

    @POST("team_invitations")
    suspend fun createTeamInvitation(@Body invitation: TeamInvitationDto): Response<List<TeamInvitationDto>>

    @PATCH("team_invitations")
    suspend fun updateTeamInvitation(
        @Query("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<TeamInvitationDto>>

    @DELETE("team_invitations")
    suspend fun deleteTeamInvitation(@Query("id") id: String): Response<Unit>

    // ─── Team Application Endpoints ───

    @GET("team_applications")
    suspend fun getTeamApplications(
        @Query("team_id") teamId: String? = null,
        @Query("applicant_user_id") applicantUserId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<TeamApplicationDto>>

    @POST("team_applications")
    suspend fun createTeamApplication(@Body application: TeamApplicationDto): Response<List<TeamApplicationDto>>

    @PATCH("team_applications")
    suspend fun updateTeamApplication(
        @Query("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<TeamApplicationDto>>

    @DELETE("team_applications")
    suspend fun deleteTeamApplication(@Query("id") id: String): Response<Unit>

    // ─── Scrim Endpoints ───

    @GET("scrims")
    suspend fun getScrims(
        @Header("Range") range: String? = null,
        @Query("status") status: String? = null,
        @Query("game_mode") gameMode: String? = null,
        @Query("region") region: String? = null,
        @Query("skill_level") skillLevel: String? = null,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<ScrimDto>>

    @GET("scrims")
    suspend fun getScrimById(
        @Query("id") id: String
    ): Response<List<ScrimDto>>

    @POST("scrims")
    suspend fun createScrim(@Body scrim: ScrimDto): Response<List<ScrimDto>>

    @PATCH("scrims")
    suspend fun updateScrim(
        @Query("id") id: String,
        @Body scrim: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<ScrimDto>>

    @DELETE("scrims")
    suspend fun deleteScrim(@Query("id") id: String): Response<Unit>

    // ─── Match Endpoints ───

    @GET("matches")
    suspend fun getMatches(
        @Query("scrim_id") scrimId: String? = null,
        @Query("status") status: String? = null,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<MatchDto>>

    @GET("matches")
    suspend fun getMatchById(
        @Query("id") id: String
    ): Response<List<MatchDto>>

    @POST("matches")
    suspend fun createMatch(@Body match: MatchDto): Response<List<MatchDto>>

    @PATCH("matches")
    suspend fun updateMatch(
        @Query("id") id: String,
        @Body match: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<MatchDto>>

    @DELETE("matches")
    suspend fun deleteMatch(@Query("id") id: String): Response<Unit>

    // ─── Scrim Application Endpoints ───

    @GET("scrim_applications")
    suspend fun getScrimApplications(
        @Query("scrim_id") scrimId: String? = null
    ): Response<List<ScrimApplicationDto>>

    @POST("scrim_applications")
    suspend fun createScrimApplication(@Body application: ScrimApplicationDto): Response<List<ScrimApplicationDto>>

    @PATCH("scrim_applications")
    suspend fun updateScrimApplication(
        @Query("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<ScrimApplicationDto>>

    @PATCH("scrim_applications")
    suspend fun updateScrimApplicationsBulk(
        @Query("scrim_id") scrimId: String? = null,
        @Query("status") status: String? = null,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<ScrimApplicationDto>>

    @DELETE("scrim_applications")
    suspend fun deleteScrimApplication(@Query("id") id: String): Response<Unit>

    // ─── Scrim Roster Endpoints ───

    @GET("scrim_rosters")
    suspend fun getScrimRosters(
        @Query("scrim_id") scrimId: String? = null,
        @Query("team_id") teamId: String? = null
    ): Response<List<ScrimRosterDto>>

    @POST("scrim_rosters")
    suspend fun createScrimRosterEntry(@Body entry: ScrimRosterDto): Response<List<ScrimRosterDto>>

    @DELETE("scrim_rosters")
    suspend fun deleteScrimRosterEntry(
        @Query("scrim_id") scrimId: String,
        @Query("team_id") teamId: String,
        @Query("user_id") userId: String
    ): Response<Unit>

    // ─── LFG Endpoints ───

    @GET("lfg_posts")
    suspend fun getLfgPosts(
        @Header("Range") range: String? = null,
        @Query("player_id") playerId: String? = null,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<LfgPostDto>>

    @POST("lfg_posts")
    suspend fun createLfgPost(@Body post: LfgPostDto): Response<List<LfgPostDto>>

    @DELETE("lfg_posts")
    suspend fun deleteLfgPost(@Query("id") id: String): Response<Unit>

    // ─── Match Result Endpoints ───

    @GET("match_results")
    suspend fun getMatchResults(
        @Query("match_id") matchId: String? = null
    ): Response<List<MatchResultDto>>

    @POST("match_results")
    suspend fun createMatchResult(@Body result: MatchResultDto): Response<List<MatchResultDto>>

    @PATCH("match_results")
    suspend fun updateMatchResult(
        @Query("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<MatchResultDto>>

    // ─── Message Endpoints ───

    @GET("messages")
    suspend fun getMessages(
        @Query("conversation_id") conversationId: String? = null,
        @Query("order") order: String = "created_at.asc",
        @Query("created_at") createdAfter: String? = null
    ): Response<List<MessageDto>>

    @POST("messages")
    suspend fun sendMessage(@Body message: MessageDto): Response<List<MessageDto>>

    @POST("conversations")
    suspend fun createConversation(@Body conversation: ConversationDto): Response<List<ConversationDto>>

    @GET("conversations")
    suspend fun getConversations(
        @Query("or") orFilter: String? = null,
        @Query("id") idFilter: String? = null
    ): Response<List<ConversationDto>>

    @PATCH("conversations")
    suspend fun updateConversation(
        @Query("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<ConversationDto>>

    // ─── Notification Endpoints ───

    @GET("app_notifications")
    suspend fun getNotifications(
        @Query("user_id") userId: String,
        @Query("order") order: String = "created_at.desc",
        @Header("Range") range: String? = null
    ): Response<List<NotificationDto>>

    @POST("app_notifications")
    suspend fun createNotification(@Body notification: NotificationDto): Response<List<NotificationDto>>

    @PATCH("app_notifications")
    suspend fun markNotificationAsRead(
        @Query("id") id: String
    ): Response<List<NotificationDto>>

    @PATCH("app_notifications")
    suspend fun markAllNotificationsAsRead(
        @Query("user_id") userId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any> = mapOf("is_read" to true)
    ): Response<List<NotificationDto>>

    @DELETE("app_notifications")
    suspend fun deleteNotification(@Query("id") id: String): Response<Unit>

    // ─── Matches Endpoints (P0-3: canonical table that match_results and messages FK reference) ───

    // ─── RPC / Custom Functions ───

    // P2-1: get_team_stats RPC — must exist in DB schema or callers must be removed
    @POST("rpc/get_team_stats")
    suspend fun getTeamStats(@Body params: Map<String, String>): Response<Map<String, @JvmSuppressWildcards Any>>

    // P2-2: get_available_scrims RPC — must exist in DB schema or callers must be removed
    @POST("rpc/get_available_scrims")
    suspend fun getAvailableScrims(@Body params: Map<String, String>): Response<List<ScrimDto>>

    // P0-4 FIX: award_scrim_points — app must pass POSITIVE loss points; RPC negates internally
    @POST("rpc/award_scrim_points")
    suspend fun awardScrimPoints(@Body params: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    // P2-3: mark_conversation_as_read — implement in DB or remove; keeping for now as no-op safe
    @POST("rpc/mark_conversation_as_read")
    suspend fun markConversationAsRead(@Body params: Map<String, String>): Response<Unit>

    // ─── Conversations RPC ───
    @POST("rpc/get_conversations_for_user")
    suspend fun getConversationsForUserRpc(@Body params: Map<String, String>): Response<List<ConversationDto>>

    @POST("rpc/get_conversation_unread_count")
    suspend fun getConversationUnreadCountRpc(@Body params: Map<String, String>): Response<Int>

    // ─── Team Ratings ───
    @GET("rpc/get_team_ratings")
    suspend fun getTeamRatings(@Query("p_team_id") teamId: String): Response<List<TeamRatingDto>>

    @POST("rpc/get_team_average_rating")
    suspend fun getTeamAverageRating(@Body params: Map<String, String>): Response<Double>

    @POST("team_ratings")
    suspend fun createTeamRating(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<TeamRatingDto>>

    // ─── Utility Endpoints ───

    // P2-4: delete_user — replaced with standard profile soft-delete via PATCH
    @PATCH("profiles")
    suspend fun deactivateUser(
        @Query("id") userId: String,
        @Body body: Map<String, @JvmSuppressWildcards Any> = mapOf("deleted" to true)
    ): Response<List<ProfileDto>>
}

/**
 * Singleton accessor for Supabase API service.
 */
object SupabaseService {
    val api: SupabaseApiService by lazy {
        SupabaseRetrofitClient.retrofit.create(SupabaseApiService::class.java)
    }
}
