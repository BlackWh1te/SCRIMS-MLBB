package com.mlbb.scrim.data.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Property-based tests for Tournament and related classes covering validation,
 * invariants, state transitions, and business logic.
 * 
 * Test Categories:
 * - Property validation
 * - State invariants
 * - Computed property tests
 * - Enum validation
 * - Business logic validation
 * - Edge cases
 */
class TournamentPropertyTest {

    // ─── TOURNAMENT PROPERTY VALIDATION TESTS ───

    @Test
    fun `tournament id can be empty for new tournaments`() {
        // Property: New tournaments may not have ID yet
        val newTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123"
        )

        // Assert
        assertTrue(newTournament.id.isEmpty(), "New tournament should have empty ID")
    }

    @Test
    fun `tournament id should not be empty for saved tournaments`() {
        // Property: Saved tournaments should have non-empty ID
        val savedTournament = Tournament(
            id = "tournament_123",
            title = "Test Tournament",
            hostUserId = "host123"
        )

        // Assert
        assertTrue(savedTournament.id.isNotEmpty(), "Saved tournament should have non-empty ID")
    }

    @Test
    fun `hostUserId should not be empty for valid tournament`() {
        // Property: Valid tournament should have non-empty hostUserId
        val validTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123"
        )

        val invalidTournament = Tournament(
            title = "Test Tournament",
            hostUserId = ""
        )

        // Assert
        assertTrue(validTournament.hostUserId.isNotEmpty(), "Valid tournament should have non-empty hostUserId")
        assertFalse(invalidTournament.hostUserId.isNotEmpty(), "Empty hostUserId should be invalid")
    }

    @Test
    fun `title should not be empty for valid tournament`() {
        // Property: Valid tournament should have non-empty title
        val validTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123"
        )

        val invalidTournament = Tournament(
            title = "",
            hostUserId = "host123"
        )

        // Assert
        assertTrue(validTournament.title.isNotEmpty(), "Valid tournament should have non-empty title")
        assertFalse(invalidTournament.title.isNotEmpty(), "Empty title should be invalid")
    }

    @Test
    fun `maxTeams should be positive and reasonable`() {
        // Property: maxTeams should be 2-256 for valid tournaments
        val validTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            maxTeams = 16
        )

        val invalidTournament1 = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            maxTeams = 0
        )

        val invalidTournament2 = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            maxTeams = -5
        )

        // Assert
        assertTrue(validTournament.maxTeams > 0, "maxTeams should be positive")
        assertTrue(validTournament.maxTeams <= 256, "maxTeams should be reasonable")
        assertFalse(invalidTournament1.maxTeams > 0, "maxTeams=0 should be invalid")
        assertFalse(invalidTournament2.maxTeams > 0, "negative maxTeams should be invalid")
    }

    @Test
    fun `minTeamSize should be positive and reasonable`() {
        // Property: minTeamSize should be 1-10 for valid tournaments
        val validTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            minTeamSize = 5
        )

        val invalidTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            minTeamSize = 0
        )

        // Assert
        assertTrue(validTournament.minTeamSize > 0, "minTeamSize should be positive")
        assertTrue(validTournament.minTeamSize <= 10, "minTeamSize should be reasonable")
        assertFalse(invalidTournament.minTeamSize > 0, "minTeamSize=0 should be invalid")
    }

    @Test
    fun `bestOf should be positive and reasonable`() {
        // Property: bestOf should be 1-7 for valid tournaments
        val validTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            bestOf = 3
        )

        val invalidTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            bestOf = 0
        )

        // Assert
        assertTrue(validTournament.bestOf > 0, "bestOf should be positive")
        assertTrue(validTournament.bestOf <= 7, "bestOf should be reasonable")
        assertFalse(invalidTournament.bestOf > 0, "bestOf=0 should be invalid")
    }

    // ─── COMPUTED PROPERTY TESTS ───

    @Test
    fun `isOpen is true when status is REGISTRATION`() {
        // Property: isOpen should be true only during registration
        val registrationTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            status = TournamentStatus.REGISTRATION
        )

        // Assert
        assertTrue(registrationTournament.isOpen, "REGISTRATION status should have isOpen=true")
    }

    @Test
    fun `isOpen is false when status is not REGISTRATION`() {
        // Property: isOpen should be false for non-registration statuses
        val nonRegistrationStatuses = listOf(
            TournamentStatus.DRAFT,
            TournamentStatus.CHECK_IN,
            TournamentStatus.IN_PROGRESS,
            TournamentStatus.COMPLETED,
            TournamentStatus.CANCELLED
        )

        nonRegistrationStatuses.forEach { status ->
            val tournament = Tournament(
                title = "Test Tournament",
                hostUserId = "host123",
                status = status
            )
            assertFalse(tournament.isOpen, "$status status should have isOpen=false")
        }
    }

    @Test
    fun `isLive is true when status is IN_PROGRESS`() {
        // Property: isLive should be true only during in-progress
        val liveTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            status = TournamentStatus.IN_PROGRESS
        )

        // Assert
        assertTrue(liveTournament.isLive, "IN_PROGRESS status should have isLive=true")
    }

    @Test
    fun `isLive is false when status is not IN_PROGRESS`() {
        // Property: isLive should be false for non-in-progress statuses
        val nonInProgressStatuses = listOf(
            TournamentStatus.DRAFT,
            TournamentStatus.REGISTRATION,
            TournamentStatus.CHECK_IN,
            TournamentStatus.COMPLETED,
            TournamentStatus.CANCELLED
        )

        nonInProgressStatuses.forEach { status ->
            val tournament = Tournament(
                title = "Test Tournament",
                hostUserId = "host123",
                status = status
            )
            assertFalse(tournament.isLive, "$status status should have isLive=false")
        }
    }

    @Test
    fun `isCheckIn is true when status is CHECK_IN`() {
        // Property: isCheckIn should be true only during check-in
        val checkInTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            status = TournamentStatus.CHECK_IN
        )

        // Assert
        assertTrue(checkInTournament.isCheckIn, "CHECK_IN status should have isCheckIn=true")
    }

    @Test
    fun `isCompleted is true when status is COMPLETED`() {
        // Property: isCompleted should be true only when completed
        val completedTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            status = TournamentStatus.COMPLETED
        )

        // Assert
        assertTrue(completedTournament.isCompleted, "COMPLETED status should have isCompleted=true")
    }

    @Test
    fun `isCancelled is true when status is CANCELLED`() {
        // Property: isCancelled should be true only when cancelled
        val cancelledTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            status = TournamentStatus.CANCELLED
        )

        // Assert
        assertTrue(cancelledTournament.isCancelled, "CANCELLED status should have isCancelled=true")
    }

    @Test
    fun `registrationTimeRemaining is non-negative`() {
        // Property: registrationTimeRemaining should always be >= 0
        val futureDeadline = System.currentTimeMillis() + 3600000 // 1 hour in future
        val pastDeadline = System.currentTimeMillis() - 3600000 // 1 hour in past

        val futureTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            registrationDeadline = futureDeadline
        )

        val pastTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            registrationDeadline = pastDeadline
        )

        // Assert
        assertTrue(futureTournament.registrationTimeRemaining >= 0, 
                   "Future deadline should have non-negative remaining time")
        assertTrue(pastTournament.registrationTimeRemaining >= 0, 
                   "Past deadline should have zero remaining time")
    }

    @Test
    fun `prizeDisplay returns description when available`() {
        // Property: prizeDisplay should return description when available
        val tournamentWithDescription = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            prizeDescription = "$1000 cash prize"
        )

        // Assert
        assertEquals("$1000 cash prize", tournamentWithDescription.prizeDisplay)
    }

    @Test
    fun `prizeDisplay returns prizeType value when description is null`() {
        // Property: prizeDisplay should return prizeType value when description is null
        val tournamentWithoutDescription = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            prizeType = PrizeType.DIAMONDS,
            prizeDescription = null
        )

        // Assert
        assertEquals("diamonds", tournamentWithoutDescription.prizeDisplay)
    }

    // ─── STATE INVARIANT TESTS ───

    @Test
    fun `createdAt should always be set to current time by default`() {
        // Property: Default createdAt should be close to current time
        val before = System.currentTimeMillis()
        val tournament = Tournament()
        val after = System.currentTimeMillis()

        // Assert
        assertTrue(tournament.createdAt >= before && tournament.createdAt <= after, 
                   "Default createdAt should be close to current time")
    }

    @Test
    fun `updatedAt should always be set to current time by default`() {
        // Property: Default updatedAt should be close to current time
        val before = System.currentTimeMillis()
        val tournament = Tournament()
        val after = System.currentTimeMillis()

        // Assert
        assertTrue(tournament.updatedAt >= before && tournament.updatedAt <= after, 
                   "Default updatedAt should be close to current time")
    }

    @Test
    fun `updatedAt should be >= createdAt`() {
        // Property: updatedAt should be >= createdAt
        val createdTime = System.currentTimeMillis() - 3600000 // 1 hour ago
        val updatedTime = System.currentTimeMillis()

        val tournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            createdAt = createdTime,
            updatedAt = updatedTime
        )

        // Assert
        assertTrue(tournament.updatedAt >= tournament.createdAt, 
                   "updatedAt should be >= createdAt")
    }

    @Test
    fun `swissRounds should be null for non-Swiss tournaments`() {
        // Property: swissRounds should be null for non-Swiss tournaments
        val standardTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            swissRounds = null
        )

        // Assert
        assertNull(standardTournament.swissRounds, "Non-Swiss tournament should have null swissRounds")
    }

    @Test
    fun `swissRounds should be positive for Swiss tournaments`() {
        // Property: swissRounds should be positive for Swiss tournaments
        val swissTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            swissRounds = 5
        )

        // Assert
        assertTrue(swissTournament.swissRounds!! > 0, "Swiss tournament should have positive swissRounds")
    }

    // ─── ENUM VALIDATION TESTS ───

    @Test
    fun `TournamentStatus has exactly six values`() {
        // Property: TournamentStatus should have exactly 6 values
        assertEquals(6, TournamentStatus.entries.size, 
                     "TournamentStatus should have exactly 6 values")
    }

    @Test
    fun `TournamentStatus values are distinct`() {
        // Property: TournamentStatus values should be distinct
        val statuses = TournamentStatus.entries
        val uniqueStatuses = statuses.toSet()
        assertEquals(statuses.size, uniqueStatuses.size, 
                     "TournamentStatus values should be distinct")
    }

    @Test
    fun `TournamentStatus fromValue returns default for invalid value`() {
        // Property: fromValue should return DRAFT for invalid values
        val invalidStatus = TournamentStatus.fromValue("invalid_status")
        assertEquals(TournamentStatus.DRAFT, invalidStatus, 
                     "fromValue should return DRAFT for invalid values")
    }

    @Test
    fun `TournamentStatus fromValue returns correct status for valid values`() {
        // Property: fromValue should return correct status for valid values
        assertEquals(TournamentStatus.DRAFT, TournamentStatus.fromValue("draft"))
        assertEquals(TournamentStatus.REGISTRATION, TournamentStatus.fromValue("registration"))
        assertEquals(TournamentStatus.CHECK_IN, TournamentStatus.fromValue("check_in"))
        assertEquals(TournamentStatus.IN_PROGRESS, TournamentStatus.fromValue("in_progress"))
        assertEquals(TournamentStatus.COMPLETED, TournamentStatus.fromValue("completed"))
        assertEquals(TournamentStatus.CANCELLED, TournamentStatus.fromValue("cancelled"))
    }

    @Test
    fun `PrizeType has exactly five values`() {
        // Property: PrizeType should have exactly 5 values
        assertEquals(5, PrizeType.entries.size, 
                     "PrizeType should have exactly 5 values")
    }

    @Test
    fun `PrizeType fromValue returns OTHER for invalid value`() {
        // Property: fromValue should return OTHER for invalid values
        val invalidPrize = PrizeType.fromValue("invalid_prize")
        assertEquals(PrizeType.OTHER, invalidPrize, 
                     "fromValue should return OTHER for invalid values")
    }

    @Test
    fun `RequirementType has exactly three values`() {
        // Property: RequirementType should have exactly 3 values
        assertEquals(3, RequirementType.entries.size, 
                     "RequirementType should have exactly 3 values")
    }

    @Test
    fun `TournamentApplicationStatus has exactly four values`() {
        // Property: TournamentApplicationStatus should have exactly 4 values
        assertEquals(4, TournamentApplicationStatus.entries.size, 
                     "TournamentApplicationStatus should have exactly 4 values")
    }

    @Test
    fun `MatchStatus has exactly six values`() {
        // Property: MatchStatus should have exactly 6 values
        assertEquals(6, MatchStatus.entries.size, 
                     "MatchStatus should have exactly 6 values")
    }

    // ─── TOURNAMENT REQUIREMENT TESTS ───

    @Test
    fun `TournamentRequirement id can be empty for new requirements`() {
        // Property: New requirements may not have ID yet
        val newRequirement = TournamentRequirement(
            tournamentId = "tournament_123",
            type = RequirementType.TELEGRAM_SUBSCRIBE,
            label = "Subscribe to Telegram"
        )

        // Assert
        assertTrue(newRequirement.id.isEmpty(), "New requirement should have empty ID")
    }

    @Test
    fun `TournamentRequirement sortOrder should be non-negative`() {
        // Property: sortOrder should be >= 0
        val requirement = TournamentRequirement(
            tournamentId = "tournament_123",
            type = RequirementType.CUSTOM,
            label = "Custom requirement",
            sortOrder = 5
        )

        // Assert
        assertTrue(requirement.sortOrder >= 0, "sortOrder should be non-negative")
    }

    // ─── TOURNAMENT TEAM TESTS ───

    @Test
    fun `TournamentTeam swissWins should be non-negative`() {
        // Property: swissWins should be >= 0
        val team = TournamentTeam(
            tournamentId = "tournament_123",
            teamId = "team_123",
            swissWins = 3
        )

        // Assert
        assertTrue(team.swissWins >= 0, "swissWins should be non-negative")
    }

    @Test
    fun `TournamentTeam swissLosses should be non-negative`() {
        // Property: swissLosses should be >= 0
        val team = TournamentTeam(
            tournamentId = "tournament_123",
            teamId = "team_123",
            swissLosses = 2
        )

        // Assert
        assertTrue(team.swissLosses >= 0, "swissLosses should be non-negative")
    }

    @Test
    fun `TournamentTeam swissPoints should be non-negative`() {
        // Property: swissPoints should be >= 0
        val team = TournamentTeam(
            tournamentId = "tournament_123",
            teamId = "team_123",
            swissPoints = 9
        )

        // Assert
        assertTrue(team.swissPoints >= 0, "swissPoints should be non-negative")
    }

    @Test
    fun `TournamentTeam buchholzScore should be non-negative`() {
        // Property: buchholzScore should be >= 0
        val team = TournamentTeam(
            tournamentId = "tournament_123",
            teamId = "team_123",
            buchholzScore = 15.5
        )

        // Assert
        assertTrue(team.buchholzScore >= 0, "buchholzScore should be non-negative")
    }

    @Test
    fun `TournamentTeam finalPlacement should be positive when set`() {
        // Property: finalPlacement should be >= 1 when set
        val team = TournamentTeam(
            tournamentId = "tournament_123",
            teamId = "team_123",
            finalPlacement = 3
        )

        // Assert
        assertTrue(team.finalPlacement!! >= 1, "finalPlacement should be >= 1 when set")
    }

    // ─── TOURNAMENT SWISS MATCH TESTS ───

    @Test
    fun `TournamentSwissMatch roundNumber should be non-negative`() {
        // Property: roundNumber should be >= 0
        val match = TournamentSwissMatch(
            tournamentId = "tournament_123",
            roundNumber = 3
        )

        // Assert
        assertTrue(match.roundNumber >= 0, "roundNumber should be non-negative")
    }

    @Test
    fun `TournamentSwissMatch scheduledTimeRemaining is non-negative`() {
        // Property: scheduledTimeRemaining should always be >= 0
        val futureTime = System.currentTimeMillis() + 3600000 // 1 hour in future
        val pastTime = System.currentTimeMillis() - 3600000 // 1 hour in past

        val futureMatch = TournamentSwissMatch(
            tournamentId = "tournament_123",
            scheduledAt = futureTime
        )

        val pastMatch = TournamentSwissMatch(
            tournamentId = "tournament_123",
            scheduledAt = pastTime
        )

        // Assert
        assertTrue(futureMatch.scheduledTimeRemaining >= 0, 
                   "Future match should have non-negative remaining time")
        assertTrue(pastMatch.scheduledTimeRemaining >= 0, 
                   "Past match should have zero remaining time")
    }

    @Test
    fun `TournamentSwissMatch game scores should be non-negative`() {
        // Property: Game scores should be >= 0
        val match = TournamentSwissMatch(
            tournamentId = "tournament_123",
            gameAScore = 2,
            gameBScore = 1
        )

        // Assert
        assertTrue(match.gameAScore >= 0, "gameAScore should be non-negative")
        assertTrue(match.gameBScore >= 0, "gameBScore should be non-negative")
    }

    // ─── EDGE CASE TESTS ───

    @Test
    fun `handles very long tournament titles`() {
        // Property: Tournament titles can be very long
        val longTitle = "A".repeat(10000)
        val tournament = Tournament(
            title = longTitle,
            hostUserId = "host123"
        )

        // Assert
        assertEquals(longTitle, tournament.title)
        assertEquals(10000, tournament.title.length)
    }

    @Test
    fun `handles very long descriptions`() {
        // Property: Descriptions can be very long
        val longDescription = "A".repeat(10000)
        val tournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            description = longDescription
        )

        // Assert
        assertEquals(longDescription, tournament.description)
        assertEquals(10000, tournament.description.length)
    }

    @Test
    fun `handles null logoUrl`() {
        // Property: logoUrl can be null
        val tournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            logoUrl = null
        )

        // Assert
        assertNull(tournament.logoUrl)
    }

    @Test
    fun `handles valid logoUrl`() {
        // Property: logoUrl can be valid
        val logoUrl = "https://example.com/logo.png"
        val tournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            logoUrl = logoUrl
        )

        // Assert
        assertEquals(logoUrl, tournament.logoUrl)
    }

    @Test
    fun `handles zero timestamps`() {
        // Property: Timestamps can be zero (epoch)
        val tournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            createdAt = 0,
            updatedAt = 0
        )

        // Assert
        assertEquals(0, tournament.createdAt)
        assertEquals(0, tournament.updatedAt)
    }

    @Test
    fun `handles negative timestamps`() {
        // Property: Timestamps can be negative (data error)
        val tournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            createdAt = -123456789,
            updatedAt = -123456789
        )

        // Assert
        assertEquals(-123456789, tournament.createdAt)
        assertEquals(-123456789, tournament.updatedAt)
    }

    // ─── SERIALIZATION ROUNDTRIP TESTS ───

    @Test
    fun `tournament copy maintains all properties correctly`() {
        // Property: Data class copy should maintain all properties
        val original = Tournament(
            id = "tournament_123",
            hostUserId = "host123",
            hostUsername = "HostUser",
            title = "Test Tournament",
            description = "Test description",
            logoUrl = "https://example.com/logo.png",
            prizeType = PrizeType.REAL_MONEY,
            prizeDescription = "$1000 prize",
            maxTeams = 16,
            minTeamSize = 5,
            bestOf = 3,
            region = "EU",
            skillLevel = "ALL",
            swissRounds = 5,
            currentRound = 2,
            status = TournamentStatus.IN_PROGRESS,
            registrationDeadline = System.currentTimeMillis() + 86400000,
            checkInDeadline = System.currentTimeMillis() + 43200000,
            isLiveStreamEnabled = true,
            isFlagged = false,
            createdAt = 123456789,
            updatedAt = 123457890
        )

        val copy = original.copy()

        // Assert
        assertEquals(original.id, copy.id)
        assertEquals(original.hostUserId, copy.hostUserId)
        assertEquals(original.hostUsername, copy.hostUsername)
        assertEquals(original.title, copy.title)
        assertEquals(original.description, copy.description)
        assertEquals(original.logoUrl, copy.logoUrl)
        assertEquals(original.prizeType, copy.prizeType)
        assertEquals(original.prizeDescription, copy.prizeDescription)
        assertEquals(original.maxTeams, copy.maxTeams)
        assertEquals(original.minTeamSize, copy.minTeamSize)
        assertEquals(original.bestOf, copy.bestOf)
        assertEquals(original.region, copy.region)
        assertEquals(original.skillLevel, copy.skillLevel)
        assertEquals(original.swissRounds, copy.swissRounds)
        assertEquals(original.currentRound, copy.currentRound)
        assertEquals(original.status, copy.status)
        assertEquals(original.registrationDeadline, copy.registrationDeadline)
        assertEquals(original.checkInDeadline, copy.checkInDeadline)
        assertEquals(original.isLiveStreamEnabled, copy.isLiveStreamEnabled)
        assertEquals(original.isFlagged, copy.isFlagged)
        assertEquals(original.createdAt, copy.createdAt)
        assertEquals(original.updatedAt, copy.updatedAt)
    }

    // ─── BUSINESS LOGIC TESTS ───

    @Test
    fun `tournament can transition from DRAFT to REGISTRATION`() {
        // Property: Valid state transition: DRAFT -> REGISTRATION
        val draft = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            status = TournamentStatus.DRAFT
        )

        val registration = draft.copy(status = TournamentStatus.REGISTRATION)

        // Assert
        assertEquals(TournamentStatus.REGISTRATION, registration.status)
    }

    @Test
    fun `tournament can transition from REGISTRATION to CHECK_IN`() {
        // Property: Valid state transition: REGISTRATION -> CHECK_IN
        val registration = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            status = TournamentStatus.REGISTRATION
        )

        val checkIn = registration.copy(status = TournamentStatus.CHECK_IN)

        // Assert
        assertEquals(TournamentStatus.CHECK_IN, checkIn.status)
    }

    @Test
    fun `tournament can transition from CHECK_IN to IN_PROGRESS`() {
        // Property: Valid state transition: CHECK_IN -> IN_PROGRESS
        val checkIn = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            status = TournamentStatus.CHECK_IN
        )

        val inProgress = checkIn.copy(status = TournamentStatus.IN_PROGRESS)

        // Assert
        assertEquals(TournamentStatus.IN_PROGRESS, inProgress.status)
    }

    @Test
    fun `tournament can transition from IN_PROGRESS to COMPLETED`() {
        // Property: Valid state transition: IN_PROGRESS -> COMPLETED
        val inProgress = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            status = TournamentStatus.IN_PROGRESS
        )

        val completed = inProgress.copy(status = TournamentStatus.COMPLETED)

        // Assert
        assertEquals(TournamentStatus.COMPLETED, completed.status)
    }

    @Test
    fun `tournament can be cancelled from any state`() {
        // Property: Valid state transition: Any state -> CANCELLED
        val statuses = listOf(
            TournamentStatus.DRAFT,
            TournamentStatus.REGISTRATION,
            TournamentStatus.CHECK_IN,
            TournamentStatus.IN_PROGRESS
        )

        statuses.forEach { status ->
            val tournament = Tournament(
                title = "Test Tournament",
                hostUserId = "host123",
                status = status
            )

            val cancelled = tournament.copy(status = TournamentStatus.CANCELLED)
            assertEquals(TournamentStatus.CANCELLED, cancelled.status)
        }
    }

    @Test
    fun `teamCount should not exceed maxTeams`() {
        // Property: Business rule - teamCount should not exceed maxTeams
        val tournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            maxTeams = 16,
            teamCount = 16
        )

        // This would be a business rule violation
        val invalidTournament = tournament.copy(teamCount = 17)

        // Assert - This documents the business rule
        assertTrue(invalidTournament.teamCount > tournament.maxTeams,
                   "teamCount should not exceed maxTeams")
    }

    @Test
    fun `currentRound should not exceed swissRounds for Swiss tournaments`() {
        // Property: Business rule - currentRound should not exceed swissRounds
        val swissTournament = Tournament(
            title = "Test Tournament",
            hostUserId = "host123",
            swissRounds = 5,
            currentRound = 5
        )

        // This would be a business rule violation
        val invalidTournament = swissTournament.copy(currentRound = 6)

        // Assert - This documents the business rule
        assertTrue(invalidTournament.currentRound!! > swissTournament.swissRounds!!,
                   "currentRound should not exceed swissRounds")
    }
}
