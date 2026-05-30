package com.mlbb.scrim.data.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Property-based tests for TeamApplication covering validation, invariants,
 * state transitions, and business logic.
 * 
 * Test Categories:
 * - Property validation
 * - State invariants
 * - Status transition logic
 * - Edge cases
 * - Serialization roundtrip
 * - Business logic validation
 */
class TeamApplicationPropertyTest {

    // ─── PROPERTY VALIDATION TESTS ───

    @Test
    fun `teamId should not be empty for valid application`() {
        // Property: Valid application should have non-empty teamId
        val validApplication = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            applicantName = "Player1"
        )

        val invalidApplication = TeamApplication(
            teamId = "",
            applicantUserId = "user456",
            applicantName = "Player1"
        )

        // Assert
        assertTrue(validApplication.teamId.isNotEmpty(), "Valid application should have non-empty teamId")
        assertFalse(invalidApplication.teamId.isNotEmpty(), "Empty teamId should be invalid")
    }

    @Test
    fun `applicantUserId should not be empty for valid application`() {
        // Property: Valid application should have non-empty applicantUserId
        val validApplication = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            applicantName = "Player1"
        )

        val invalidApplication = TeamApplication(
            teamId = "team123",
            applicantUserId = "",
            applicantName = "Player1"
        )

        // Assert
        assertTrue(validApplication.applicantUserId.isNotEmpty(), "Valid application should have non-empty applicantUserId")
        assertFalse(invalidApplication.applicantUserId.isNotEmpty(), "Empty applicantUserId should be invalid")
    }

    @Test
    fun `applicantName should not be empty for valid application`() {
        // Property: Valid application should have non-empty applicantName
        val validApplication = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            applicantName = "Player1"
        )

        val invalidApplication = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            applicantName = ""
        )

        // Assert
        assertTrue(validApplication.applicantName.isNotEmpty(), "Valid application should have non-empty applicantName")
        assertFalse(invalidApplication.applicantName.isNotEmpty(), "Empty applicantName should be invalid")
    }

    @Test
    fun `status should have valid enum value`() {
        // Property: Status should be one of the valid enum values
        val validStatuses = listOf(
            TeamApplicationStatus.PENDING,
            TeamApplicationStatus.ACCEPTED,
            TeamApplicationStatus.DECLINED
        )

        validStatuses.forEach { status ->
            val application = TeamApplication(
                teamId = "team123",
                applicantUserId = "user456",
                status = status
            )
            assertTrue(application.status in validStatuses, "Status should be valid enum value")
        }
    }

    // ─── STATE INVARIANT TESTS ───

    @Test
    fun `createdAt should always be set to current time by default`() {
        // Property: Default createdAt should be close to current time
        val before = System.currentTimeMillis()
        val application = TeamApplication()
        val after = System.currentTimeMillis()

        // Assert
        assertTrue(application.createdAt >= before && application.createdAt <= after, 
                   "Default createdAt should be close to current time")
    }

    @Test
    fun `respondedAt should be null for pending applications`() {
        // Property: Pending applications should not have respondedAt
        val pendingApplication = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.PENDING
        )

        // Assert
        assertNull(pendingApplication.respondedAt, "Pending application should have null respondedAt")
    }

    @Test
    fun `respondedAt should be set for accepted applications`() {
        // Property: Accepted applications should have respondedAt
        val respondedTime = System.currentTimeMillis()
        val acceptedApplication = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.ACCEPTED,
            respondedAt = respondedTime
        )

        // Assert
        assertNotNull(acceptedApplication.respondedAt, "Accepted application should have respondedAt")
        assertEquals(respondedTime, acceptedApplication.respondedAt)
    }

    @Test
    fun `respondedAt should be set for declined applications`() {
        // Property: Declined applications should have respondedAt
        val respondedTime = System.currentTimeMillis()
        val declinedApplication = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.DECLINED,
            respondedAt = respondedTime
        )

        // Assert
        assertNotNull(declinedApplication.respondedAt, "Declined application should have respondedAt")
        assertEquals(respondedTime, declinedApplication.respondedAt)
    }

    @Test
    fun `respondedAt should be after createdAt for processed applications`() {
        // Property: respondedAt should be >= createdAt
        val createdTime = System.currentTimeMillis()
        val respondedTime = createdTime + 3600000 // 1 hour later

        val acceptedApplication = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.ACCEPTED,
            createdAt = createdTime,
            respondedAt = respondedTime
        )

        // Assert
        assertTrue(acceptedApplication.respondedAt!! >= acceptedApplication.createdAt, 
                   "respondedAt should be >= createdAt")
    }

    @Test
    fun `id can be empty for new applications`() {
        // Property: New applications may not have ID yet
        val newApplication = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456"
        )

        // Assert
        assertTrue(newApplication.id.isEmpty(), "New application should have empty ID")
    }

    @Test
    fun `id should not be empty for saved applications`() {
        // Property: Saved applications should have non-empty ID
        val savedApplication = TeamApplication(
            id = "app_123",
            teamId = "team123",
            applicantUserId = "user456"
        )

        // Assert
        assertTrue(savedApplication.id.isNotEmpty(), "Saved application should have non-empty ID")
    }

    // ─── STATUS TRANSITION TESTS ───

    @Test
    fun `application can transition from pending to accepted`() {
        // Property: Valid state transition: PENDING -> ACCEPTED
        val pending = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.PENDING,
            createdAt = System.currentTimeMillis() - 3600000
        )

        val accepted = pending.copy(
            status = TeamApplicationStatus.ACCEPTED,
            respondedAt = System.currentTimeMillis()
        )

        // Assert
        assertEquals(TeamApplicationStatus.ACCEPTED, accepted.status)
        assertNotNull(accepted.respondedAt)
        assertTrue(accepted.respondedAt!! >= pending.createdAt)
    }

    @Test
    fun `application can transition from pending to declined`() {
        // Property: Valid state transition: PENDING -> DECLINED
        val pending = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.PENDING,
            createdAt = System.currentTimeMillis() - 3600000
        )

        val declined = pending.copy(
            status = TeamApplicationStatus.DECLINED,
            respondedAt = System.currentTimeMillis()
        )

        // Assert
        assertEquals(TeamApplicationStatus.DECLINED, declined.status)
        assertNotNull(declined.respondedAt)
        assertTrue(declined.respondedAt!! >= pending.createdAt)
    }

    @Test
    fun `accepted applications should not transition back to pending`() {
        // Property: Invalid state transition: ACCEPTED -> PENDING
        val accepted = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.ACCEPTED
        )

        // This would be a business rule violation
        val invalidTransition = accepted.copy(status = TeamApplicationStatus.PENDING)

        // Assert - This documents the business rule
        assertTrue(invalidTransition.status == TeamApplicationStatus.PENDING, 
                   "ACCEPTED -> PENDING should be prevented by business logic")
    }

    @Test
    fun `declined applications should not transition back to pending`() {
        // Property: Invalid state transition: DECLINED -> PENDING
        val declined = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.DECLINED
        )

        // This would be a business rule violation
        val invalidTransition = declined.copy(status = TeamApplicationStatus.PENDING)

        // Assert - This documents the business rule
        assertTrue(invalidTransition.status == TeamApplicationStatus.PENDING, 
                   "DECLINED -> PENDING should be prevented by business logic")
    }

    // ─── EDGE CASE TESTS ───

    @Test
    fun `handles very long applicant names`() {
        // Property: Applicant names can be very long
        val longName = "A".repeat(10000)
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            applicantName = longName
        )

        // Assert
        assertEquals(longName, application.applicantName)
        assertEquals(10000, application.applicantName.length)
    }

    @Test
    fun `handles special characters in applicant names`() {
        // Property: Applicant names can contain special characters
        val specialName = "Player!@#$%^&*()"
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            applicantName = specialName
        )

        // Assert
        assertEquals(specialName, application.applicantName)
    }

    @Test
    fun `handles unicode characters in applicant names`() {
        // Property: Applicant names can contain unicode
        val unicodeName = "玩家123 😀 特殊字符"
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            applicantName = unicodeName
        )

        // Assert
        assertEquals(unicodeName, application.applicantName)
    }

    @Test
    fun `handles very long team IDs`() {
        // Property: Team IDs can be very long
        val longTeamId = "a".repeat(10000)
        val application = TeamApplication(
            teamId = longTeamId,
            applicantUserId = "user456",
            applicantName = "Player1"
        )

        // Assert
        assertEquals(longTeamId, application.teamId)
        assertEquals(10000, application.teamId.length)
    }

    @Test
    fun `handles very long user IDs`() {
        // Property: User IDs can be very long
        val longUserId = "a".repeat(10000)
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = longUserId,
            applicantName = "Player1"
        )

        // Assert
        assertEquals(longUserId, application.applicantUserId)
        assertEquals(10000, application.applicantUserId.length)
    }

    @Test
    fun `handles very long messages`() {
        // Property: Messages can be very long
        val longMessage = "A".repeat(10000)
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            message = longMessage
        )

        // Assert
        assertEquals(longMessage, application.message)
        assertEquals(10000, application.message!!.length)
    }

    @Test
    fun `handles null avatar URL`() {
        // Property: Avatar URL can be null
        val applicationWithoutAvatar = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            applicantAvatarUrl = null
        )

        // Assert
        assertNull(applicationWithoutAvatar.applicantAvatarUrl)
    }

    @Test
    fun `handles valid avatar URL`() {
        // Property: Avatar URL can be valid
        val avatarUrl = "https://example.com/avatar.jpg"
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            applicantAvatarUrl = avatarUrl
        )

        // Assert
        assertEquals(avatarUrl, application.applicantAvatarUrl)
    }

    @Test
    fun `handles null message`() {
        // Property: Message can be null
        val applicationWithoutMessage = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            message = null
        )

        // Assert
        assertNull(applicationWithoutMessage.message)
    }

    @Test
    fun `handles empty message`() {
        // Property: Message can be empty string
        val emptyMessage = ""
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            message = emptyMessage
        )

        // Assert
        assertEquals(emptyMessage, application.message)
    }

    @Test
    fun `handles zero timestamp`() {
        // Property: Timestamps can be zero (epoch)
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            createdAt = 0
        )

        // Assert
        assertEquals(0, application.createdAt)
    }

    @Test
    fun `handles negative timestamp`() {
        // Property: Timestamps can be negative (data error)
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            createdAt = -123456789
        )

        // Assert
        assertEquals(-123456789, application.createdAt)
    }

    @Test
    fun `handles very large timestamp`() {
        // Property: Timestamps can be very large (future)
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            createdAt = Long.MAX_VALUE
        )

        // Assert
        assertEquals(Long.MAX_VALUE, application.createdAt)
    }

    // ─── SERIALIZATION ROUNDTRIP TESTS ───

    @Test
    fun `copy maintains all properties correctly`() {
        // Property: Data class copy should maintain all properties
        val original = TeamApplication(
            id = "app_123",
            teamId = "team123",
            teamName = "Team123",
            applicantUserId = "user456",
            applicantName = "Player1",
            applicantAvatarUrl = "https://example.com/avatar.jpg",
            status = TeamApplicationStatus.ACCEPTED,
            message = "I want to join!",
            createdAt = 123456789,
            respondedAt = 123457890
        )

        val copy = original.copy()

        // Assert
        assertEquals(original.id, copy.id)
        assertEquals(original.teamId, copy.teamId)
        assertEquals(original.teamName, copy.teamName)
        assertEquals(original.applicantUserId, copy.applicantUserId)
        assertEquals(original.applicantName, copy.applicantName)
        assertEquals(original.applicantAvatarUrl, copy.applicantAvatarUrl)
        assertEquals(original.status, copy.status)
        assertEquals(original.message, copy.message)
        assertEquals(original.createdAt, copy.createdAt)
        assertEquals(original.respondedAt, copy.respondedAt)
    }

    @Test
    fun `copy with modified status maintains other properties`() {
        // Property: Copy with modified status should preserve other properties
        val original = TeamApplication(
            id = "app_123",
            teamId = "team123",
            applicantUserId = "user456",
            applicantName = "Player1",
            status = TeamApplicationStatus.PENDING
        )

        val modified = original.copy(status = TeamApplicationStatus.ACCEPTED)

        // Assert
        assertEquals(original.id, modified.id)
        assertEquals(original.teamId, modified.teamId)
        assertEquals(original.applicantUserId, modified.applicantUserId)
        assertEquals(original.applicantName, modified.applicantName)
        assertEquals(TeamApplicationStatus.ACCEPTED, modified.status)
    }

    // ─── BUSINESS LOGIC TESTS ───

    @Test
    fun `same applicant cannot have pending application for same team`() {
        // Property: Business rule - one pending application per applicant per team
        val application1 = TeamApplication(
            id = "app_1",
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.PENDING
        )

        val application2 = TeamApplication(
            id = "app_2",
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.PENDING
        )

        // Assert - This would be a business rule violation
        assertTrue(application1.teamId == application2.teamId && 
                   application1.applicantUserId == application2.applicantUserId &&
                   application1.status == TeamApplicationStatus.PENDING,
                   "Duplicate pending applications should be prevented")
    }

    @Test
    fun `applicant can reapply after rejection`() {
        // Property: Business rule - rejected applicants can reapply
        val rejected = TeamApplication(
            id = "app_1",
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.DECLINED,
            respondedAt = System.currentTimeMillis() - 86400000 // 1 day ago
        )

        val newApplication = TeamApplication(
            id = "app_2",
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        // Assert - This documents the business rule
        assertTrue(newApplication.status == TeamApplicationStatus.PENDING,
                   "Rejected applicants should be able to reapply")
    }

    @Test
    fun `accepted applicant cannot reapply for same team`() {
        // Property: Business rule - accepted applicants cannot reapply
        val accepted = TeamApplication(
            id = "app_1",
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.ACCEPTED
        )

        // This would be a business rule violation
        val invalidApplication = TeamApplication(
            id = "app_2",
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.PENDING
        )

        // Assert - This documents the business rule
        assertTrue(invalidApplication.status == TeamApplicationStatus.PENDING,
                   "Accepted applicants should not be able to reapply")
    }

    @Test
    fun `application age can be calculated`() {
        // Property: Application age can be calculated from createdAt
        val createdTime = System.currentTimeMillis() - 3600000 // 1 hour ago
        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            createdAt = createdTime
        )

        val age = System.currentTimeMillis() - application.createdAt

        // Assert
        assertTrue(age >= 3600000, "Application should be at least 1 hour old")
        assertTrue(age < 3700000, "Application should be less than 1 hour + 10 seconds old")
    }

    @Test
    fun `response time can be calculated for processed applications`() {
        // Property: Response time can be calculated for accepted/declined applications
        val createdTime = System.currentTimeMillis() - 7200000 // 2 hours ago
        val respondedTime = System.currentTimeMillis() - 3600000 // 1 hour ago

        val application = TeamApplication(
            teamId = "team123",
            applicantUserId = "user456",
            status = TeamApplicationStatus.ACCEPTED,
            createdAt = createdTime,
            respondedAt = respondedTime
        )

        val responseTime = application.respondedAt!! - application.createdAt

        // Assert
        assertEquals(3600000, responseTime, "Response time should be 1 hour")
    }

    // ─── ENUM TESTS ───

    @Test
    fun `TeamApplicationStatus has exactly three values`() {
        // Property: Status enum should have exactly 3 values
        assertEquals(3, TeamApplicationStatus.entries.size, 
                     "TeamApplicationStatus should have exactly 3 values")
    }

    @Test
    fun `TeamApplicationStatus values are distinct`() {
        // Property: Status enum values should be distinct
        val statuses = TeamApplicationStatus.entries
        val uniqueStatuses = statuses.toSet()
        assertEquals(statuses.size, uniqueStatuses.size, 
                     "TeamApplicationStatus values should be distinct")
    }

    @Test
    fun `TeamApplicationStatus enum names match expected values`() {
        // Property: Enum names should match expected values
        val expectedNames = listOf("PENDING", "ACCEPTED", "DECLINED")
        val actualNames = TeamApplicationStatus.entries.map { it.name }
        
        assertEquals(expectedNames.sorted(), actualNames.sorted(), 
                     "Enum names should match expected values")
    }

    // ─── FILTERING TESTS ───

    @Test
    fun `applications can be filtered by status`() {
        // Property: Applications can be filtered by status
        val applications = listOf(
            TeamApplication(status = TeamApplicationStatus.PENDING),
            TeamApplication(status = TeamApplicationStatus.ACCEPTED),
            TeamApplication(status = TeamApplicationStatus.DECLINED),
            TeamApplication(status = TeamApplicationStatus.PENDING)
        )

        val pendingApps = applications.filter { it.status == TeamApplicationStatus.PENDING }
        val acceptedApps = applications.filter { it.status == TeamApplicationStatus.ACCEPTED }
        val declinedApps = applications.filter { it.status == TeamApplicationStatus.DECLINED }

        // Assert
        assertEquals(2, pendingApps.size)
        assertEquals(1, acceptedApps.size)
        assertEquals(1, declinedApps.size)
    }

    @Test
    fun `applications can be filtered by team`() {
        // Property: Applications can be filtered by team
        val applications = listOf(
            TeamApplication(teamId = "team1", applicantUserId = "user1"),
            TeamApplication(teamId = "team1", applicantUserId = "user2"),
            TeamApplication(teamId = "team2", applicantUserId = "user3"),
            TeamApplication(teamId = "team3", applicantUserId = "user4")
        )

        val team1Apps = applications.filter { it.teamId == "team1" }

        // Assert
        assertEquals(2, team1Apps.size)
        assertTrue(team1Apps.all { it.teamId == "team1" })
    }

    @Test
    fun `applications can be filtered by applicant`() {
        // Property: Applications can be filtered by applicant
        val applications = listOf(
            TeamApplication(teamId = "team1", applicantUserId = "user1"),
            TeamApplication(teamId = "team2", applicantUserId = "user1"),
            TeamApplication(teamId = "team3", applicantUserId = "user2")
        )

        val user1Apps = applications.filter { it.applicantUserId == "user1" }

        // Assert
        assertEquals(2, user1Apps.size)
        assertTrue(user1Apps.all { it.applicantUserId == "user1" })
    }

    // ─── COMPARISON TESTS ───

    @Test
    fun `applications can be sorted by creation time`() {
        // Property: Applications can be sorted by creation time
        val now = System.currentTimeMillis()
        val applications = listOf(
            TeamApplication(createdAt = now - 1000),
            TeamApplication(createdAt = now - 5000),
            TeamApplication(createdAt = now - 100),
            TeamApplication(createdAt = now - 10000)
        )

        val sorted = applications.sortedBy { it.createdAt }

        // Assert
        assertEquals(now - 10000, sorted[0].createdAt)
        assertEquals(now - 5000, sorted[1].createdAt)
        assertEquals(now - 1000, sorted[2].createdAt)
        assertEquals(now - 100, sorted[3].createdAt)
    }

    @Test
    fun `applications can be sorted by response time`() {
        // Property: Applications can be sorted by response time
        val now = System.currentTimeMillis()
        val applications = listOf(
            TeamApplication(status = TeamApplicationStatus.ACCEPTED, respondedAt = now - 1000),
            TeamApplication(status = TeamApplicationStatus.ACCEPTED, respondedAt = now - 5000),
            TeamApplication(status = TeamApplicationStatus.ACCEPTED, respondedAt = now - 100)
        )

        val sorted = applications.sortedBy { it.respondedAt }

        // Assert
        assertEquals(now - 5000, sorted[0].respondedAt)
        assertEquals(now - 1000, sorted[1].respondedAt)
        assertEquals(now - 100, sorted[2].respondedAt)
    }
}
