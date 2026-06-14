package com.mlbb.scrim.data.repository

import com.mlbb.scrim.data.model.*
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Method

/**
 * Contract tests verifying repository interfaces declare expected methods.
 * Ensures implementations cannot silently drop required methods.
 */
class RepositoryInterfaceTest {

    @Test
    fun `AuthRepositoryInterface has all required methods`() {
        val methods = AuthRepositoryInterface::class.java.methods.map { it.name }.toSet()
        assertTrue("signUp", methods.contains("signUp"))
        assertTrue("signIn", methods.contains("signIn"))
        assertTrue("signOut", methods.contains("signOut"))
        assertTrue("deleteAccount", methods.contains("deleteAccount"))
        assertTrue("confirmEmail", methods.contains("confirmEmail"))
        assertTrue("resendVerificationEmail", methods.contains("resendVerificationEmail"))
        assertTrue("updateProfile", methods.contains("updateProfile"))
        assertTrue("updateEmail", methods.contains("updateEmail"))
        assertTrue("updatePassword", methods.contains("updatePassword"))
        assertTrue("getCurrentUser", methods.contains("getCurrentUser"))
        assertTrue("getUserProfile", methods.contains("getUserProfile"))
        assertTrue("isLoggedIn", methods.contains("isLoggedIn"))
        assertTrue("isVerificationExpired", methods.contains("isVerificationExpired"))
        assertTrue("secondsUntilDeletion", methods.contains("secondsUntilDeletion"))
        assertTrue("purgeIfExpired", methods.contains("purgeIfExpired"))
        assertTrue("sendOtp", methods.contains("sendOtp"))
        assertTrue("verifyOtp", methods.contains("verifyOtp"))
        assertTrue("updateAvatar", methods.contains("updateAvatar"))
        assertTrue("uploadAndSetAvatar", methods.contains("uploadAndSetAvatar"))
    }

    @Test
    fun `TeamRepositoryInterface has all required methods`() {
        val methods = TeamRepositoryInterface::class.java.methods.map { it.name }.toSet()
        assertTrue("createTeam", methods.contains("createTeam"))
        assertTrue("getTeams", methods.contains("getTeams"))
        assertTrue("getTeamsForUser", methods.contains("getTeamsForUser"))
        assertTrue("getTeam", methods.contains("getTeam"))
        assertTrue("addPlayer", methods.contains("addPlayer"))
        assertTrue("removePlayer", methods.contains("removePlayer"))
        assertTrue("updatePlayerRole", methods.contains("updatePlayerRole"))
        assertTrue("deleteTeam", methods.contains("deleteTeam"))
        assertTrue("sendInvite", methods.contains("sendInvite"))
        assertTrue("acceptInvite", methods.contains("acceptInvite"))
        assertTrue("declineInvite", methods.contains("declineInvite"))
        assertTrue("getInvitesForPlayer", methods.contains("getInvitesForPlayer"))
        assertTrue("getInvitesForTeam", methods.contains("getInvitesForTeam"))
        assertTrue("getOpenTeams", methods.contains("getOpenTeams"))
        assertTrue("applyToTeam", methods.contains("applyToTeam"))
        assertTrue("getTeamApplications", methods.contains("getTeamApplications"))
        assertTrue("getMyApplications", methods.contains("getMyApplications"))
        assertTrue("acceptApplication", methods.contains("acceptApplication"))
        assertTrue("declineApplication", methods.contains("declineApplication"))
        assertTrue("subscribeToTeam", methods.contains("subscribeToTeam"))
        assertTrue("subscribeToTeamInvites", methods.contains("subscribeToTeamInvites"))
    }

    @Test
    fun `ScrimRepositoryInterface has all required methods`() {
        val methods = ScrimRepositoryInterface::class.java.methods.map { it.name }.toSet()
        assertTrue("getAllScrims", methods.contains("getAllScrims"))
        assertTrue("getScrimById", methods.contains("getScrimById"))
        assertTrue("getScrimsByTeam", methods.contains("getScrimsByTeam"))
        assertTrue("searchScrims", methods.contains("searchScrims"))
        assertTrue("createScrim", methods.contains("createScrim"))
        assertTrue("updateScrim", methods.contains("updateScrim"))
        assertTrue("deleteScrim", methods.contains("deleteScrim"))
        assertTrue("applyToScrim", methods.contains("applyToScrim"))
        assertTrue("approveApplication", methods.contains("approveApplication"))
        assertTrue("rejectApplication", methods.contains("rejectApplication"))
        assertTrue("cancelApplication", methods.contains("cancelApplication"))
        assertTrue("setScrimRoster", methods.contains("setScrimRoster"))
        assertTrue("transitionToReadyCheck", methods.contains("transitionToReadyCheck"))
        assertTrue("markReady", methods.contains("markReady"))
        assertTrue("uploadScreenshot", methods.contains("uploadScreenshot"))
        assertTrue("completeScrim", methods.contains("completeScrim"))
        assertTrue("calculatePointsChanges", methods.contains("calculatePointsChanges"))
        assertTrue("submitResult", methods.contains("submitResult"))
        assertTrue("createAutoCancelledRecord", methods.contains("createAutoCancelledRecord"))
        assertTrue("subscribeToScrim", methods.contains("subscribeToScrim"))
        assertTrue("subscribeToAllScrims", methods.contains("subscribeToAllScrims"))
    }

    @Test
    fun `MatchResultRepositoryInterface has all required methods`() {
        val methods = MatchResultRepositoryInterface::class.java.methods.map { it.name }.toSet()
        assertTrue("getAllMatchResults", methods.contains("getAllMatchResults"))
        assertTrue("getMatchResultById", methods.contains("getMatchResultById"))
        assertTrue("getMatchResultsForScrim", methods.contains("getMatchResultsForScrim"))
        assertTrue("getMatchResultsForTeam", methods.contains("getMatchResultsForTeam"))
        assertTrue("reportResult", methods.contains("reportResult"))
        assertTrue("createMatchResult", methods.contains("createMatchResult"))
        assertTrue("resolveDispute", methods.contains("resolveDispute"))
        assertTrue("uploadScreenshot", methods.contains("uploadScreenshot"))
    }

    @Test
    fun `AuthRepository implements AuthRepositoryInterface`() {
        assertTrue(AuthRepositoryInterface::class.java.isAssignableFrom(AuthRepository::class.java))
    }

    @Test
    fun `TeamRepository implements TeamRepositoryInterface`() {
        assertTrue(TeamRepositoryInterface::class.java.isAssignableFrom(TeamRepository::class.java))
    }

    @Test
    fun `ScrimRepository implements ScrimRepositoryInterface`() {
        assertTrue(ScrimRepositoryInterface::class.java.isAssignableFrom(ScrimRepository::class.java))
    }

    @Test
    fun `MatchResultRepository implements MatchResultRepositoryInterface`() {
        assertTrue(MatchResultRepositoryInterface::class.java.isAssignableFrom(MatchResultRepository::class.java))
    }

    @Test
    fun `all repository interfaces return Flow types for async methods`() {
        val interfaces = listOf(
            AuthRepositoryInterface::class.java,
            TeamRepositoryInterface::class.java,
            ScrimRepositoryInterface::class.java,
            MatchResultRepositoryInterface::class.java
        )

        interfaces.forEach { iface ->
            iface.methods.forEach { method ->
                if (method.returnType != Void.TYPE) {
                    assertTrue(
                        "Method ${iface.simpleName}.${method.name} should return Flow",
                        method.returnType == kotlinx.coroutines.flow.Flow::class.java ||
                        method.returnType == com.mlbb.scrim.data.repository.PointsResult::class.java
                    )
                }
            }
        }
    }
}
