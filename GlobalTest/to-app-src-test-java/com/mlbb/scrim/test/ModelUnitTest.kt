package com.mlbb.scrim.test

import com.mlbb.scrim.data.model.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive unit tests for all data model classes.
 * Covers computed properties, edge cases, null safety, and boundary conditions.
 */
class ModelUnitTest {

    // ═══════════════════════════════════════════════════════════════
    // RankTier Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `RankTier fromXp returns correct tier for boundary values`() {
        assertEquals(RankTier.BRONZE, RankTier.fromXp(0))
        assertEquals(RankTier.BRONZE, RankTier.fromXp(999))
        assertEquals(RankTier.SOLVER, RankTier.fromXp(1000))
        assertEquals(RankTier.SOLVER, RankTier.fromXp(2499))
        assertEquals(RankTier.GOLD, RankTier.fromXp(2500))
        assertEquals(RankTier.GOLD, RankTier.fromXp(4999))
        assertEquals(RankTier.GRANDMASTER, RankTier.fromXp(5000))
        assertEquals(RankTier.EPIC, RankTier.fromXp(8000))
        assertEquals(RankTier.LEGEND, RankTier.fromXp(12000))
        assertEquals(RankTier.MYTHIC, RankTier.fromXp(17000))
        assertEquals(RankTier.MYTHIC, RankTier.fromXp(999999))
    }

    @Test
    fun `RankTier fromXp handles negative xp safely`() {
        assertEquals(RankTier.BRONZE, RankTier.fromXp(-100))
    }

    @Test
    fun `RankTier nextTier returns correct next tier`() {
        assertEquals(RankTier.SOLVER, RankTier.nextTier(RankTier.BRONZE))
        assertEquals(RankTier.GOLD, RankTier.nextTier(RankTier.SOLVER))
        assertEquals(RankTier.MYTHIC, RankTier.nextTier(RankTier.LEGEND))
        assertNull(RankTier.nextTier(RankTier.MYTHIC))
    }

    @Test
    fun `RankTier xpToNextTier calculates correctly`() {
        assertEquals(1000, RankTier.xpToNextTier(0))
        assertEquals(1, RankTier.xpToNextTier(999))
        assertEquals(0, RankTier.xpToNextTier(17000))
        assertEquals(0, RankTier.xpToNextTier(99999))
    }

    @Test
    fun `RankTier xpProgressInTier returns bounded float`() {
        assertEquals(0.0f, RankTier.xpProgressInTier(0), 0.01f)
        assertEquals(0.5f, RankTier.xpProgressInTier(500), 0.01f)
        assertTrue(RankTier.xpProgressInTier(999) < 1.0f)
        assertTrue(RankTier.xpProgressInTier(17000) in 0.0f..1.0f)
        assertTrue(RankTier.xpProgressInTier(-1) >= 0f)
        assertTrue(RankTier.xpProgressInTier(Int.MAX_VALUE) <= 1f)
    }

    @Test
    fun `RegionalRank fromWins handles non RU regions`() {
        assertNull(RegionalRank.fromWins(100, "NA"))
        assertNull(RegionalRank.fromWins(100, "EU"))
        assertNull(RegionalRank.fromWins(100, "ASIA"))
    }

    @Test
    fun `RegionalRank fromWins returns correct ranks for RU regions`() {
        assertEquals(RegionalRank.TOP1, RegionalRank.fromWins(50, "KRD"))
        assertEquals(RegionalRank.TOP1, RegionalRank.fromWins(50, "MSK"))
        assertEquals(RegionalRank.TOP1, RegionalRank.fromWins(50, "EKB"))
        assertEquals(RegionalRank.TOP2, RegionalRank.fromWins(30, "KRD"))
        assertEquals(RegionalRank.TOP3, RegionalRank.fromWins(15, "KRD"))
        assertNull(RegionalRank.fromWins(14, "KRD"))
    }

    // ═══════════════════════════════════════════════════════════════
    // UserProfile Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `UserProfile winRate handles zero totalMatches`() {
        val profile = UserProfile(totalMatches = 0, wins = 0, losses = 0)
        assertEquals("0%", profile.winRate)
        assertEquals(0f, profile.winRateFloat, 0.01f)
    }

    @Test
    fun `UserProfile winRate calculates correctly`() {
        val profile = UserProfile(totalMatches = 10, wins = 7, losses = 3)
        assertEquals("70%", profile.winRate)
        assertEquals(70f, profile.winRateFloat, 0.01f)
    }

    @Test
    fun `UserProfile winRate with wins exceeding totalMatches`() {
        val profile = UserProfile(totalMatches = 5, wins = 10, losses = 0)
        assertEquals("200%", profile.winRate)
        assertEquals(200f, profile.winRateFloat, 0.01f)
    }

    @Test
    fun `UserProfile ptsDisplay formats correctly`() {
        assertEquals("+50", UserProfile(pts = 50).ptsDisplay)
        assertEquals("-20", UserProfile(pts = -20).ptsDisplay)
        assertEquals("+0", UserProfile(pts = 0).ptsDisplay)
    }

    @Test
    fun `UserProfile nextTierName returns correct value`() {
        assertEquals("Solver", UserProfile(currentTier = RankTier.BRONZE).nextTierName)
        assertEquals("Mythic", UserProfile(currentTier = RankTier.LEGEND).nextTierName)
        assertEquals("Max", UserProfile(currentTier = RankTier.MYTHIC).nextTierName)
    }

    @Test
    fun `UserProfile default values are safe`() {
        val default = UserProfile()
        assertEquals("", default.id)
        assertEquals("", default.username)
        assertEquals("", default.email)
        assertFalse(default.emailVerified)
        assertFalse(default.isBanned)
        assertTrue(default.mainHeroes.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════
    // Team Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `Team player count logic works correctly`() {
        val emptyTeam = Team()
        assertEquals(0, emptyTeam.currentPlayerCount)
        assertTrue(emptyTeam.canAddPlayer)
        assertFalse(emptyTeam.isFull)
        assertFalse(emptyTeam.meetsMinPlayers)

        val fullTeam = Team(players = List(7) { Player() })
        assertEquals(7, fullTeam.currentPlayerCount)
        assertFalse(fullTeam.canAddPlayer)
        assertTrue(fullTeam.isFull)
        assertTrue(fullTeam.meetsMinPlayers)
    }

    @Test
    fun `Team isBannedFromPosting checks time correctly`() {
        val pastBan = Team(canPostScrimsUntil = System.currentTimeMillis() - 1000)
        assertFalse(pastBan.isBannedFromPosting)

        val futureBan = Team(canPostScrimsUntil = System.currentTimeMillis() + 3600000)
        assertTrue(futureBan.isBannedFromPosting)
    }

    @Test
    fun `Team displayReputation coerces to valid range`() {
        assertEquals("5.0", Team(reputation = 10f).displayReputation)
        assertEquals("1.0", Team(reputation = 0f).displayReputation)
        assertEquals("3.5", Team(reputation = 3.5f).displayReputation)
    }

    // ═══════════════════════════════════════════════════════════════
    // Player Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `Player winRate handles zero matches`() {
        val player = Player(matchesPlayed = 0)
        assertEquals(0f, player.winRate, 0.01f)
        assertEquals("0%", player.winRateDisplay)
    }

    @Test
    fun `Player winRate calculates correctly`() {
        val player = Player(wins = 8, losses = 2, matchesPlayed = 10)
        assertEquals(80f, player.winRate, 0.01f)
        assertEquals("80%", player.winRateDisplay)
    }

    // ═══════════════════════════════════════════════════════════════
    // Scrim Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `Scrim chat timing calculations are correct`() {
        val now = System.currentTimeMillis()
        val futureScrim = Scrim(scheduledTime = now + 3600000)
        assertEquals(now + 3600000 - 7200000, futureScrim.chatOpensAt)
        assertFalse(futureScrim.isChatOpen)

        val pastScrim = Scrim(scheduledTime = now - 3600000)
        assertTrue(pastScrim.isChatOpen)
    }

    @Test
    fun `Scrim result deadlines are correct`() {
        val now = System.currentTimeMillis()
        val scrim = Scrim(scheduledTime = now)
        assertEquals(now + 3600000, scrim.resultDeadline)
        assertEquals(now + 7200000, scrim.autoCancelDeadline)
    }

    @Test
    fun `Scrim bothReady requires both teams`() {
        val readyA = Scrim(teamAReady = true, teamBReady = false)
        assertFalse(readyA.bothReady)

        val bothReady = Scrim(teamAReady = true, teamBReady = true)
        assertTrue(bothReady.bothReady)
    }

    @Test
    fun `Scrim canCompleteScrim requires screenshot`() {
        val readyNoScreenshot = Scrim(teamAReady = true, teamBReady = true)
        assertFalse(readyNoScreenshot.canCompleteScrim)

        val readyWithScreenshot = Scrim(
            teamAReady = true,
            teamBReady = true,
            teamAScreenshotUrl = "https://example.com/ss.jpg"
        )
        assertTrue(readyWithScreenshot.canCompleteScrim)
    }

    @Test
    fun `Scrim roster filtering works correctly`() {
        val activePlayer = ScrimRosterEntry(playerId = "1", isActive = true)
        val subPlayer = ScrimRosterEntry(playerId = "2", isActive = false)
        val scrim = Scrim(
            teamARoster = listOf(activePlayer, subPlayer),
            teamBRoster = listOf(activePlayer.copy(playerId = "3"))
        )
        assertEquals(1, scrim.teamAActiveRoster.size)
        assertEquals(1, scrim.teamASubstitutes.size)
        assertEquals(1, scrim.teamBActiveRoster.size)
        assertEquals(0, scrim.teamBSubstitutes.size)
    }

    @Test
    fun `Scrim timeUntilChatOpens is never negative`() {
        val pastScrim = Scrim(scheduledTime = System.currentTimeMillis() - 10000)
        assertTrue(pastScrim.timeUntilChatOpens >= 0)
    }

    // ═══════════════════════════════════════════════════════════════
    // BestOf Enum Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `BestOf fromGames returns correct enum`() {
        assertEquals(BestOf.BO1, BestOf.fromGames(1))
        assertEquals(BestOf.BO3, BestOf.fromGames(3))
        assertEquals(BestOf.BO5, BestOf.fromGames(5))
        assertEquals(BestOf.BO1, BestOf.fromGames(99))
    }

    @Test
    fun `BestOf games property is correct`() {
        assertEquals(1, BestOf.BO1.games)
        assertEquals(5, BestOf.BO5.games)
    }

    // ═══════════════════════════════════════════════════════════════
    // Region Enum Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `Region fromDisplayName returns correct region or UTC fallback`() {
        assertEquals(Region.EU, Region.fromDisplayName("Europe"))
        assertEquals(Region.NA, Region.fromDisplayName("North America"))
        assertEquals(Region.UTC, Region.fromDisplayName("Unknown"))
    }

    @Test
    fun `Region display names are not empty`() {
        Region.values().forEach { region ->
            assertTrue("Region ${region.name} has empty displayName", region.displayName.isNotEmpty())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Message & Conversation Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `Conversation isOtherTyping returns correct participant`() {
        val conv = Conversation(
            participantAId = "userA",
            participantBId = "userB",
            isParticipantATyping = true,
            isParticipantBTyping = false
        )
        assertTrue(conv.isOtherTyping("userB"))
        assertFalse(conv.isOtherTyping("userA"))
    }

    @Test
    fun `Conversation isChatOpenNow based on time`() {
        val future = Conversation(chatOpensAt = System.currentTimeMillis() + 3600000)
        assertFalse(future.isChatOpenNow)

        val past = Conversation(chatOpensAt = System.currentTimeMillis() - 1000)
        assertTrue(past.isChatOpenNow)
    }

    @Test
    fun `Conversation timeUntilChatOpens is non-negative`() {
        val past = Conversation(chatOpensAt = System.currentTimeMillis() - 1000)
        assertEquals(0L, past.timeUntilChatOpens)
    }

    // ═══════════════════════════════════════════════════════════════
    // MatchResult Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `MatchResult isDisputed detects conflicting reports`() {
        val reportA = TeamReport(reportedWinnerId = "teamA")
        val reportB = TeamReport(reportedWinnerId = "teamB")
        val match = MatchResult(teamAReport = reportA, teamBReport = reportB)
        assertTrue(match.isDisputed)

        val agreed = MatchResult(
            teamAReport = TeamReport(reportedWinnerId = "teamA"),
            teamBReport = TeamReport(reportedWinnerId = "teamA")
        )
        assertFalse(agreed.isDisputed)
    }

    @Test
    fun `MatchResult pendingReporterTeamId returns correct team`() {
        val missingA = MatchResult(teamAId = "A", teamBId = "B", teamAReport = null, teamBReport = TeamReport())
        assertEquals("A", missingA.pendingReporterTeamId)

        val missingB = MatchResult(teamAId = "A", teamBId = "B", teamAReport = TeamReport(), teamBReport = null)
        assertEquals("B", missingB.pendingReporterTeamId)

        val bothReported = MatchResult(
            teamAId = "A", teamBId = "B",
            teamAReport = TeamReport(), teamBReport = TeamReport()
        )
        assertNull(bothReported.pendingReporterTeamId)
    }

    @Test
    fun `MatchResult isConfirmed checks status`() {
        assertFalse(MatchResult(verificationStatus = VerificationStatus.PENDING).isConfirmed)
        assertTrue(MatchResult(verificationStatus = VerificationStatus.CONFIRMED).isConfirmed)
    }

    // ═══════════════════════════════════════════════════════════════
    // Notification Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `Notification icon mapping is complete`() {
        NotificationType.values().forEach { type ->
            val notification = Notification(type = type)
            assertTrue("Type $type has empty icon", notification.icon.isNotEmpty())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // TeamInvite Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `TeamInvite default status is PENDING`() {
        assertEquals(InviteStatus.PENDING, TeamInvite().status)
    }

    // ═══════════════════════════════════════════════════════════════
    // LfgPost Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `LfgPost default availability is true`() {
        assertTrue(LfgPost().isAvailable)
    }

    @Test
    fun `LfgPost default role is FLEX`() {
        assertEquals(GameRole.FLEX, LfgPost().role)
    }

    // ═══════════════════════════════════════════════════════════════
    // NewsArticle Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `NewsArticle default language is en`() {
        assertEquals("en", NewsArticle().originalLanguage)
    }

    @Test
    fun `NewsArticle default not translated`() {
        assertFalse(NewsArticle().isTranslated)
    }

    // ═══════════════════════════════════════════════════════════════
    // LeaderboardEntry Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `LeaderboardEntry winRate handles edge cases`() {
        assertEquals("0%", LeaderboardEntry(totalMatches = 0).winRate)
        assertEquals("100%", LeaderboardEntry(totalMatches = 10, wins = 10).winRate)
        assertEquals("33%", LeaderboardEntry(totalMatches = 3, wins = 1).winRate)
    }

    // ═══════════════════════════════════════════════════════════════
    // Achievement Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `Achievement checkUnlocks filters correctly`() {
        val profile = UserProfile(currentTier = RankTier.LEGEND)
        val stats = PlayerAchievements(
            matchesPlayed = 100,
            bestWinStreak = 5,
            scrimsCreated = 10,
            teamsCreated = 1,
            hasFlawlessVictory = true,
            hasRegionalTop = false
        )
        val unlocked = Achievement.checkUnlocks(profile, stats)
        assertTrue(unlocked.any { it == Achievement.FIRST_SCRIM })
        assertTrue(unlocked.any { it == Achievement.WIN_STREAK_5 })
        assertTrue(unlocked.any { it == Achievement.SCRIM_HOST_10 })
        assertTrue(unlocked.any { it == Achievement.FLAWLESS_VICTORY })
        assertTrue(unlocked.any { it == Achievement.LEGEND_WIN })
        assertTrue(unlocked.any { it == Achievement.VETERAN_100 })
        assertFalse(unlocked.any { it == Achievement.GODLIKE })
        assertFalse(unlocked.any { it == Achievement.MYTHIC_REACHED })
    }

    @Test
    fun `PlayerAchievements getProgress returns correct values`() {
        val stats = PlayerAchievements(matchesPlayed = 50, bestWinStreak = 3)
        val achievement = Achievement.WIN_STREAK_5
        assertEquals(3, stats.getProgress(achievement))
        assertEquals(0.6f, stats.getProgressPercentage(achievement), 0.01f)
    }

    @Test
    fun `PlayerAchievements getProgress for unlocked returns max`() {
        val stats = PlayerAchievements(unlockedAchievements = listOf("first_scrim"))
        val achievement = Achievement.FIRST_SCRIM
        assertEquals(achievement.condition.maxProgress, stats.getProgress(achievement))
        assertEquals(1.0f, stats.getProgressPercentage(achievement), 0.01f)
    }

    @Test
    fun `PlayerAchievements getProgressPercentage is bounded`() {
        val stats = PlayerAchievements(matchesPlayed = 9999)
        val achievement = Achievement.FIRST_SCRIM
        assertTrue(stats.getProgressPercentage(achievement) <= 1.0f)
    }

    @Test
    fun `PlayerAchievements isUnlocked checks list membership`() {
        val stats = PlayerAchievements(unlockedAchievements = listOf("test_achievement"))
        assertTrue(stats.isUnlocked(Achievement(id = "test_achievement", displayName = "", description = "", iconLetter = "", badgeColor = com.mlbb.scrim.ui.theme.GoldRank, glowColor = com.mlbb.scrim.ui.theme.GoldRank, condition = AchievementCondition.MatchesPlayed(1))))
        assertFalse(stats.isUnlocked(Achievement.FIRST_SCRIM))
    }

    // ═══════════════════════════════════════════════════════════════
    // ScrimApplication Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `ScrimApplication default status is PENDING`() {
        assertEquals(ApplicationStatus.PENDING, ScrimApplication().status)
    }

    // ═══════════════════════════════════════════════════════════════
    // TeamApplication Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `TeamApplication default status is PENDING`() {
        assertEquals(TeamApplicationStatus.PENDING, TeamApplication().status)
    }

    // ═══════════════════════════════════════════════════════════════
    // AuthResult Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `AuthResult sealed class structure`() {
        val idle: AuthResult = AuthResult.Idle
        val success: AuthResult = AuthResult.Success
        val loading: AuthResult = AuthResult.Loading
        val error: AuthResult = AuthResult.Error("Test error")
        val emailNotVerified: AuthResult = AuthResult.EmailNotVerified("test@example.com")

        assertTrue(idle is AuthResult.Idle)
        assertTrue(success is AuthResult.Success)
        assertTrue(loading is AuthResult.Loading)
        assertTrue(error is AuthResult.Error)
        assertTrue(emailNotVerified is AuthResult.EmailNotVerified)

        assertEquals("Test error", (error as AuthResult.Error).message)
        assertEquals("test@example.com", (emailNotVerified as AuthResult.EmailNotVerified).email)
    }

    // ═══════════════════════════════════════════════════════════════
    // GameRole Enum Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `GameRole display names are non-empty`() {
        GameRole.values().forEach { role ->
            assertTrue("Role ${role.name} has empty displayName", role.displayName.isNotEmpty())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Data Class Copy/Equality Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `UserProfile copy preserves identity and allows mutation`() {
        val original = UserProfile(id = "1", username = "Old", xp = 100)
        val copy = original.copy(username = "New", xp = 200)
        assertEquals("1", copy.id)
        assertEquals("New", copy.username)
        assertEquals(200, copy.xp)
        assertEquals("Old", original.username)
    }

    @Test
    fun `Team copy preserves players list immutability`() {
        val original = Team(players = listOf(Player(id = "p1")))
        val copy = original.copy(name = "NewName")
        assertEquals(1, copy.players.size)
        assertEquals("p1", copy.players[0].id)
    }

    @Test
    fun `equals and hashCode for data classes`() {
        val p1 = Player(id = "1", name = "Test")
        val p2 = Player(id = "1", name = "Test")
        val p3 = Player(id = "2", name = "Test")
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
        assertNotEquals(p1, p3)
    }

    // ═══════════════════════════════════════════════════════════════
    // Null Safety & Injection Vector Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `Message with null optional fields is safe`() {
        val msg = Message(
            id = "1",
            conversationId = "conv1",
            senderId = "user1",
            content = "Hello",
            imageUrl = null,
            voiceUrl = null
        )
        assertNull(msg.imageUrl)
        assertNull(msg.voiceUrl)
        assertNull(msg.voiceDuration)
    }

    @Test
    fun `Scrim with null opponent fields is safe`() {
        val scrim = Scrim(opponentTeamId = null, opponentTeamName = null)
        assertNull(scrim.opponentTeamId)
        assertNull(scrim.opponentTeamName)
    }

    @Test
    fun `MatchResult with null reports handles dispute check`() {
        val match = MatchResult(teamAReport = null, teamBReport = null)
        assertFalse(match.isDisputed)
        assertNull(match.pendingReporterTeamId)
    }

    @Test
    fun `LfgPost with empty social links is safe`() {
        val post = LfgPost(discord = "", telegram = "", vk = "", facebook = "")
        assertEquals("", post.discord)
    }

    @Test
    fun `NewsArticle with empty strings handles gracefully`() {
        val article = NewsArticle(title = "", description = "", content = "")
        assertEquals("", article.title)
        assertEquals("", article.description)
    }

    @Test
    fun `Conversation with empty lastMessage is valid`() {
        val conv = Conversation(lastMessage = "")
        assertEquals("", conv.lastMessage)
    }

    @Test
    fun `ScrimApplication with null notes is valid`() {
        val app = ScrimApplication(notes = null)
        assertNull(app.notes)
    }

    @Test
    fun `TeamApplication with null message and avatar is valid`() {
        val app = TeamApplication(message = null, applicantAvatarUrl = null)
        assertNull(app.message)
        assertNull(app.applicantAvatarUrl)
    }
}
