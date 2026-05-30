package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ScrimRepositoryTest {

    private lateinit var repository: ScrimRepository

    @Before
    fun setup() {
        repository = ScrimRepository()
    }

    // ─── Get tests ───

    @Test
    fun `getAllScrims returns initial sample data`() = runBlocking {
        val result = repository.getAllScrims().first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isNotEmpty() == true)
    }

    @Test
    fun `getScrimById returns existing scrim`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val firstId = all.first().id
        val result = repository.getScrimById(firstId).first()
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `getScrimById returns null for nonexistent id`() = runBlocking {
        val result = repository.getScrimById("nonexistent").first()
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `getScrimsByTeam filters by team`() = runBlocking {
        val result = repository.getScrimsByTeam("team1").first()
        assertTrue(result.isSuccess)
        val scrims = result.getOrNull()!!
        assertTrue(scrims.all { it.teamId == "team1" || it.opponentTeamId == "team1" })
    }

    // ─── Search tests ───

    @Test
    fun `searchScrims with empty query returns all`() = runBlocking {
        val result = repository.searchScrims("", null, null, null, null).first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isNotEmpty() == true)
    }

    @Test
    fun `searchScrims filters by query text`() = runBlocking {
        val result = repository.searchScrims("elite", null, null, null, null).first()
        assertTrue(result.isSuccess)
        val scrims = result.getOrNull()!!
        assertTrue(scrims.all {
            it.teamName.contains("elite", ignoreCase = true) ||
            it.description.contains("elite", ignoreCase = true)
        })
    }

    @Test
    fun `searchScrims filters by region`() = runBlocking {
        val result = repository.searchScrims("", null, Region.EU, null, null).first()
        assertTrue(result.isSuccess)
        val scrims = result.getOrNull()!!
        assertTrue(scrims.all { it.region == Region.EU })
    }

    @Test
    fun `searchScrims filters by skill level`() = runBlocking {
        val result = repository.searchScrims("", null, null, SkillLevel.PRO, null).first()
        assertTrue(result.isSuccess)
        val scrims = result.getOrNull()!!
        assertTrue(scrims.all { it.skillLevel == SkillLevel.PRO })
    }

    @Test
    fun `searchScrims filters by status`() = runBlocking {
        val result = repository.searchScrims("", null, null, null, ScrimStatus.FILLED).first()
        assertTrue(result.isSuccess)
        val scrims = result.getOrNull()!!
        assertTrue(scrims.all { it.status == ScrimStatus.FILLED })
    }

    @Test
    fun `searchScrims combines filters`() = runBlocking {
        val result = repository.searchScrims("", null, Region.NA, SkillLevel.ADVANCED, null).first()
        assertTrue(result.isSuccess)
        val scrims = result.getOrNull()!!
        assertTrue(scrims.all { it.region == Region.NA && it.skillLevel == SkillLevel.ADVANCED })
    }

    // ─── Create / Update / Delete tests ───

    @Test
    fun `createScrim adds new scrim`() = runBlocking {
        val scrim = Scrim(teamId = "team99", teamName = "New Team", description = "Test")
        val result = repository.createScrim(scrim).first()
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull()?.id)
        assertTrue(result.getOrNull()?.id?.isNotBlank() == true)
    }

    @Test
    fun `updateScrim modifies existing`() = runBlocking {
        val created = repository.createScrim(Scrim(teamId = "team99", description = "Original")).first().getOrNull()!!
        val updated = created.copy(description = "Updated")
        val result = repository.updateScrim(updated).first()
        assertTrue(result.isSuccess)
        assertEquals("Updated", result.getOrNull()?.description)
    }

    @Test
    fun `updateScrim fails for nonexistent`() = runBlocking {
        val result = repository.updateScrim(Scrim(id = "fake", teamId = "team99")).first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteScrim removes scrim`() = runBlocking {
        val created = repository.createScrim(Scrim(teamId = "team99")).first().getOrNull()!!
        val result = repository.deleteScrim(created.id).first()
        assertTrue(result.isSuccess)
        val afterDelete = repository.getScrimById(created.id).first().getOrNull()
        assertNull(afterDelete)
    }

    @Test
    fun `deleteScrim fails for nonexistent`() = runBlocking {
        val result = repository.deleteScrim("nonexistent").first()
        assertTrue(result.isFailure)
    }

    // ─── Application flow tests ───

    @Test
    fun `applyToScrim adds application`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val openScrim = all.first { it.status == ScrimStatus.OPEN }
        val application = ScrimApplication(applicantTeamId = "appTeam", applicantTeamName = "Applicants")
        val result = repository.applyToScrim(openScrim.id, application).first()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.applications?.size)
    }

    @Test
    fun `applyToScrim fails when scrim not open`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val filledScrim = all.first { it.status == ScrimStatus.FILLED }
        val application = ScrimApplication(applicantTeamId = "appTeam")
        val result = repository.applyToScrim(filledScrim.id, application).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("no longer open") == true)
    }

    @Test
    fun `approveApplication transitions to FILLED`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val openScrim = all.first { it.status == ScrimStatus.OPEN }
        val application = ScrimApplication(applicantTeamId = "appTeam", applicantTeamName = "Applicants")
        val applied = repository.applyToScrim(openScrim.id, application).first().getOrNull()!!
        val appId = applied.applications.first().id
        val result = repository.approveApplication(openScrim.id, appId, "conv1").first()
        assertTrue(result.isSuccess)
        assertEquals(ScrimStatus.FILLED, result.getOrNull()?.status)
        assertNotNull(result.getOrNull()?.opponentTeamId)
    }

    @Test
    fun `approveApplication cancels other pending applications`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val openScrim = all.first { it.status == ScrimStatus.OPEN }
        val app1 = ScrimApplication(applicantTeamId = "teamA")
        val app2 = ScrimApplication(applicantTeamId = "teamB")
        repository.applyToScrim(openScrim.id, app1).first()
        val applied = repository.applyToScrim(openScrim.id, app2).first().getOrNull()!!
        val appId = applied.applications.first { it.applicantTeamId == "teamA" }.id
        val result = repository.approveApplication(openScrim.id, appId, "conv1").first()
        val finalScrim = result.getOrNull()!!
        val cancelledApps = finalScrim.applications.filter { it.status == ApplicationStatus.CANCELLED }
        assertTrue(cancelledApps.isNotEmpty())
    }

    @Test
    fun `rejectApplication updates status`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val openScrim = all.first { it.status == ScrimStatus.OPEN }
        val application = ScrimApplication(applicantTeamId = "appTeam")
        val applied = repository.applyToScrim(openScrim.id, application).first().getOrNull()!!
        val appId = applied.applications.first().id
        val result = repository.rejectApplication(openScrim.id, appId).first()
        assertTrue(result.isSuccess)
        assertEquals(ApplicationStatus.REJECTED, result.getOrNull()?.applications?.first()?.status)
    }

    // ─── Roster tests ───

    @Test
    fun `setScrimRoster fails with fewer than 5 active players`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val scrim = all.first()
        val roster = List(4) { ScrimRosterEntry(playerId = "p$it", isActive = true) }
        val result = repository.setScrimRoster(scrim.id, scrim.teamId, roster).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Minimum 5") == true)
    }

    @Test
    fun `setScrimRoster succeeds with 5 active players`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val scrim = all.first()
        val roster = List(5) { ScrimRosterEntry(playerId = "p$it", teamId = scrim.teamId, isActive = true) }
        val result = repository.setScrimRoster(scrim.id, scrim.teamId, roster).first()
        assertTrue(result.isSuccess)
    }

    // ─── Ready flow tests ───

    @Test
    fun `transitionToReadyCheck fails from OPEN status`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val openScrim = all.first { it.status == ScrimStatus.OPEN }
        val result = repository.transitionToReadyCheck(openScrim.id).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("must be in FILLED") == true)
    }

    @Test
    fun `markReady sets teamA ready`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val filledScrim = all.first { it.status == ScrimStatus.FILLED }
        val result = repository.markReady(filledScrim.id, filledScrim.teamId).first()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.teamAReady == true)
    }

    @Test
    fun `markReady transitions to IN_PROGRESS when both ready`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val filledScrim = all.first { it.status == ScrimStatus.FILLED }
        repository.markReady(filledScrim.id, filledScrim.teamId).first()
        val opponentId = filledScrim.opponentTeamId ?: "opponent"
        val result = repository.markReady(filledScrim.id, opponentId).first()
        assertTrue(result.isSuccess)
        assertEquals(ScrimStatus.IN_PROGRESS, result.getOrNull()?.status)
    }

    // ─── Screenshot flow tests ───

    @Test
    fun `uploadScreenshot fails when not IN_PROGRESS`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val openScrim = all.first { it.status == ScrimStatus.OPEN }
        val result = repository.uploadScreenshot(openScrim.id, openScrim.teamId, "url").first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("must be in progress") == true)
    }

    // ─── Complete scrim tests ───

    @Test
    fun `completeScrim fails without screenshot`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val scrim = all.first { it.status == ScrimStatus.FILLED }
        val result = repository.completeScrim(scrim.id, scrim.teamId).first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("screenshot") == true)
    }

    @Test
    fun `completeScrim fails with invalid winner`() = runBlocking {
        val all = repository.getAllScrims().first().getOrNull()!!
        val scrim = all.first { it.status == ScrimStatus.FILLED }
        val updated = scrim.copy(teamAScreenshotUrl = "url")
        repository.updateScrim(updated).first()
        val result = repository.completeScrim(scrim.id, "invalid_team").first()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Winner must be") == true)
    }

    // ─── Points calculation tests ───

    @Test
    fun `calculatePointsChanges returns empty when no winner`() {
        val scrim = Scrim(teamARoster = List(5) { ScrimRosterEntry(playerId = "p$it", teamId = "A", isActive = true) })
        val result = repository.calculatePointsChanges(scrim)
        assertTrue(result.teamAChanges.isEmpty())
        assertTrue(result.teamBChanges.isEmpty())
    }

    @Test
    fun `calculatePointsChanges awards points to winners`() {
        val scrim = Scrim(
            teamId = "A",
            opponentTeamId = "B",
            winnerTeamId = "A",
            teamARoster = List(5) { ScrimRosterEntry(playerId = "a$it", teamId = "A", isActive = true) },
            teamBRoster = List(5) { ScrimRosterEntry(playerId = "b$it", teamId = "B", isActive = true) }
        )
        val result = repository.calculatePointsChanges(scrim)
        assertEquals(5, result.teamAChanges.size)
        assertEquals(5, result.teamBChanges.size)
        assertTrue(result.teamAChanges.all { it.pointsChange == ScrimRepository.PTS_PER_WIN })
        assertTrue(result.teamBChanges.all { it.pointsChange == -ScrimRepository.PTS_PER_LOSS })
    }

    @Test
    fun `calculatePointsChanges handles substitutes with zero points`() {
        val scrim = Scrim(
            teamId = "A",
            opponentTeamId = "B",
            winnerTeamId = "A",
            teamARoster = listOf(
                ScrimRosterEntry(playerId = "a1", teamId = "A", isActive = true),
                ScrimRosterEntry(playerId = "a2", teamId = "A", isActive = false)
            ),
            teamBRoster = listOf(
                ScrimRosterEntry(playerId = "b1", teamId = "B", isActive = true),
                ScrimRosterEntry(playerId = "b2", teamId = "B", isActive = false)
            )
        )
        val result = repository.calculatePointsChanges(scrim)
        assertEquals(1, result.teamAChanges.size)
        assertEquals(1, result.teamASubstitutes.size)
        assertEquals(0, result.teamASubstitutes.first().pointsChange)
        assertTrue(result.teamASubstitutes.first().isSubstitute)
    }

    @Test
    fun `PTS_PER_WIN is 25`() {
        assertEquals(25, ScrimRepository.PTS_PER_WIN)
    }

    @Test
    fun `PTS_PER_LOSS is 15`() {
        assertEquals(15, ScrimRepository.PTS_PER_LOSS)
    }
}
