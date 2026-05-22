package com.mlbb.scrim.ui.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.mlbb.scrim.ui.theme.GoldPrimary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.mlbb.scrim.ui.screens.ChatScreen
import com.mlbb.scrim.ui.screens.CreateScrimScreen
import com.mlbb.scrim.ui.screens.CreateTeamScreen
import com.mlbb.scrim.ui.screens.ForgotPasswordScreen
import com.mlbb.scrim.ui.screens.HomeScreen
import com.mlbb.scrim.ui.screens.LeaderboardScreen
import com.mlbb.scrim.ui.screens.LoginScreen
import com.mlbb.scrim.ui.screens.MatchHistoryScreen
import com.mlbb.scrim.ui.screens.MatchResultDetailScreen
import com.mlbb.scrim.ui.screens.MatchResultListScreen
import com.mlbb.scrim.ui.screens.MessageListScreen
import com.mlbb.scrim.ui.screens.NewsScreen
import com.mlbb.scrim.ui.screens.NotificationScreen
import com.mlbb.scrim.ui.screens.OnboardingScreen
import com.mlbb.scrim.ui.screens.ScheduleScreen
import com.mlbb.scrim.ui.screens.ProfileScreen
import com.mlbb.scrim.ui.screens.ReportMatchResultScreen
import com.mlbb.scrim.ui.screens.ScrimDetailScreen
import com.mlbb.scrim.ui.screens.ScrimListScreen
import com.mlbb.scrim.ui.screens.ScrimRosterScreen
import com.mlbb.scrim.ui.screens.JoinTeamScreen
import com.mlbb.scrim.ui.screens.SettingsScreen
import com.mlbb.scrim.ui.screens.SignupScreen
import com.mlbb.scrim.ui.screens.SplashScreen
import com.mlbb.scrim.ui.screens.TeamDetailScreen
import com.mlbb.scrim.ui.screens.TeamListScreen
import com.mlbb.scrim.ui.screens.FindTeamsScreen
import com.mlbb.scrim.ui.screens.LfgBoardScreen
import com.mlbb.scrim.ui.screens.PlayerFinderScreen
import com.mlbb.scrim.ui.components.AppBottomNav
import com.mlbb.scrim.viewmodel.AuthViewModel
import com.mlbb.scrim.viewmodel.LeaderboardViewModel
import com.mlbb.scrim.viewmodel.MatchResultViewModel
import com.mlbb.scrim.viewmodel.MessageViewModel
import com.mlbb.scrim.viewmodel.NewsViewModel
import com.mlbb.scrim.viewmodel.NotificationViewModel
import com.mlbb.scrim.viewmodel.ScrimViewModel
import com.mlbb.scrim.viewmodel.SettingsViewModel
import com.mlbb.scrim.viewmodel.TeamViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object TeamList : Screen("team_list")
    object CreateTeam : Screen("create_team")
    object TeamDetail : Screen("team_detail/{teamId}") {
        fun createRoute(teamId: String) = "team_detail/$teamId"
    }
    object ScrimList : Screen("scrim_list")
    object CreateScrim : Screen("create_scrim")
    object ScrimDetail : Screen("scrim_detail/{scrimId}") {
        fun createRoute(scrimId: String) = "scrim_detail/$scrimId"
    }
    object MatchResultList : Screen("match_result_list")
    object MatchResultDetail : Screen("match_result_detail/{matchResultId}") {
        fun createRoute(matchResultId: String) = "match_result_detail/$matchResultId"
    }
    object ReportMatchResult : Screen("report_match_result/{matchResultId}") {
        fun createRoute(matchResultId: String) = "report_match_result/$matchResultId"
    }
    object MessageList : Screen("message_list")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object Leaderboard : Screen("leaderboard")
    object Settings : Screen("settings")
    object MatchHistory : Screen("match_history")
    object ForgotPassword : Screen("forgot_password")
    object JoinTeam : Screen("join_team")
    object FindTeams : Screen("find_teams")
    object Notifications : Screen("notifications")
    object Onboarding : Screen("onboarding")
    object Schedule : Screen("schedule")
    object News : Screen("news")
    object Verification : Screen("verification/{email}") {
        fun createRoute(email: String) = "verification/${Uri.encode(email)}"
    }
    object Achievements : Screen("achievements")
    object LfgBoard : Screen("lfg_board")
    object PlayerFinder : Screen("player_finder")
    object ScrimRoster : Screen("scrim_roster/{scrimId}/{teamId}") {
        fun createRoute(scrimId: String, teamId: String) = "scrim_roster/$scrimId/$teamId"
    }
}

@Composable
fun AuthNavigation(
    viewModel: AuthViewModel,
    teamViewModel: TeamViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    scrimViewModel: ScrimViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    matchResultViewModel: MatchResultViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    messageViewModel: MessageViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    leaderboardViewModel: LeaderboardViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    notificationViewModel: NotificationViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    settingsViewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    newsViewModel: NewsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    lfgViewModel: com.mlbb.scrim.viewmodel.LfgViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    context: Context
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isProfileRefreshing by viewModel.isProfileRefreshing.collectAsState()
    val teams by teamViewModel.teams.collectAsState()
    val scrims by scrimViewModel.scrims.collectAsState()
    val scrimIsLoading by scrimViewModel.isLoading.collectAsState()
    val matchResults by matchResultViewModel.matchResults.collectAsState()
    val matchResultIsLoading by matchResultViewModel.isLoading.collectAsState()
    val selectedMatchResult by matchResultViewModel.selectedMatchResult.collectAsState()
    val reportSuccess by matchResultViewModel.reportSuccess.collectAsState()
    val conversations by messageViewModel.conversations.collectAsState()
    val selectedConversation by messageViewModel.selectedConversation.collectAsState()
    val messagesIsLoading by messageViewModel.isLoading.collectAsState()
    val leaderboard by leaderboardViewModel.leaderboard.collectAsState()
    val leaderboardIsLoading by leaderboardViewModel.isLoading.collectAsState()
    val leaderboardTier by leaderboardViewModel.selectedTier.collectAsState()
    val leaderboardError by leaderboardViewModel.error.collectAsState()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    val matchNotifications by settingsViewModel.matchNotifications.collectAsState()
    val messageNotifications by settingsViewModel.messageNotifications.collectAsState()
    val soundEnabled by settingsViewModel.soundEnabled.collectAsState()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsState()
    val languageCode by settingsViewModel.languageCode.collectAsState()
    val darkMode by settingsViewModel.darkMode.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val notificationUnreadCount by notificationViewModel.unreadCount.collectAsState()
    val lfgPosts by lfgViewModel.posts.collectAsState()
    val lfgIsLoading by lfgViewModel.isLoading.collectAsState()
    val notificationIsLoading by notificationViewModel.isLoading.collectAsState()
    val notificationError by notificationViewModel.error.collectAsState()
    val openTeams by teamViewModel.openTeams.collectAsState()
    val teamApplicationSuccess by teamViewModel.applicationSuccess.collectAsState()
    val teamApplications by teamViewModel.teamApplications.collectAsState()
    val teamIsLoading by teamViewModel.isLoading.collectAsState()

    val unreadCount = conversations.sumOf { it.unreadCount }
    val pendingInviteCount = notifications.count { !it.isRead && it.type == com.mlbb.scrim.data.model.NotificationType.TEAM_INVITE }
    val playerTeamIds = teams.filter { it.leaderId == userProfile?.id }.map { it.id }
    // Derive achievement stats from available user data.
    // TODO: Add the following columns to player_stats table for full tracking:
    //   best_win_streak, ratings_given, has_regional_top, jungler_wins,
    //   roamer_wins, night_wins, five_star_matches, has_flawless_victory
    val derivedStats = com.mlbb.scrim.data.model.PlayerAchievements(
        playerId = userProfile?.id ?: "",
        matchesPlayed = userProfile?.totalMatches ?: 0,
        scrimsCreated = scrims.count { it.teamId in playerTeamIds },
        teamsCreated = playerTeamIds.size,
        // Fields below require DB schema changes to track properly:
        bestWinStreak = 0,
        ratingsGiven = 0,
        hasRegionalTop = false,
        junglerWins = 0,
        roamerWins = 0,
        nightWins = 0,
        fiveStarMatches = 0,
        hasFlawlessVictory = false
    )
    val derivedAchievements = userProfile?.let { profile ->
        com.mlbb.scrim.data.model.Achievement.checkUnlocks(
            profile = profile,
            stats = derivedStats
        )
    } ?: emptyList()

    // Set userId for notifications and team filtering when profile is available
    LaunchedEffect(userProfile?.id) {
        userProfile?.id?.let { id ->
            notificationViewModel.setUserId(id)
            teamViewModel.setUserId(id)
        }
    }

    // Start at splash screen
    val startDestination = Screen.Splash.route

    // Dialog states
    var showInviteDialog by remember { mutableStateOf(false) }

    // Navigate based on auth state changes
    // Only handle explicit logout here. Login navigation is handled
    // per-screen to avoid race conditions between isLoggedIn and authState effects.
    var previousIsLoggedIn by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(isLoggedIn) {
        when {
            // Only handle logout navigation here
            !isLoggedIn && previousIsLoggedIn == true -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        }
        previousIsLoggedIn = isLoggedIn
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isLoggedIn) {
                AppBottomNav(
                    navController = navController,
                    unreadMessageCount = unreadCount,
                    notificationCount = notificationUnreadCount,
                    pendingInviteCount = pendingInviteCount
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                // Enter: subtle slide + smooth scale + gentle fade — modern feel
                enterTransition = {
                    fadeIn(
                        animationSpec = tween(300, easing = com.mlbb.scrim.ui.theme.AppEaseOutCubic)
                    ) + slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetX = { it / 12 }
                    ) + scaleIn(
                        animationSpec = tween(300, easing = com.mlbb.scrim.ui.theme.AppEaseOutCubic),
                        initialScale = 0.97f
                    )
                },
                // Exit: faster fade-out with subtle slide away
                exitTransition = {
                    fadeOut(
                        animationSpec = tween(200, easing = com.mlbb.scrim.ui.theme.AppEaseInCubic)
                    ) + slideOutHorizontally(
                        animationSpec = tween(250, easing = com.mlbb.scrim.ui.theme.AppEaseInCubic),
                        targetOffsetX = { -it / 16 }
                    )
                },
                // Pop enter (back navigation): reverse direction
                popEnterTransition = {
                    fadeIn(
                        animationSpec = tween(300, easing = com.mlbb.scrim.ui.theme.AppEaseOutCubic)
                    ) + slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        initialOffsetX = { -it / 12 }
                    ) + scaleIn(
                        animationSpec = tween(300, easing = com.mlbb.scrim.ui.theme.AppEaseOutCubic),
                        initialScale = 0.97f
                    )
                },
                // Pop exit: slide out to the right
                popExitTransition = {
                    fadeOut(
                        animationSpec = tween(200, easing = com.mlbb.scrim.ui.theme.AppEaseInCubic)
                    ) + slideOutHorizontally(
                        animationSpec = tween(250, easing = com.mlbb.scrim.ui.theme.AppEaseInCubic),
                        targetOffsetX = { it / 16 }
                    )
                }
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onFinish = {
                            if (!isInitializing) {
                                if (isLoggedIn) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                    
                    // Fallback: If splash animation ends but we are still checking DB/Network
                    if (!isInitializing) {
                        LaunchedEffect(Unit) {
                            if (isLoggedIn) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }
                    }
                }

                composable(Screen.Login.route) {
                    val authState by viewModel.authState.collectAsState()
                    LaunchedEffect(authState) {
                        when (authState) {
                            is com.mlbb.scrim.data.model.AuthResult.Success -> {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                                viewModel.resetAuthState()
                            }
                            is com.mlbb.scrim.data.model.AuthResult.EmailNotVerified -> {
                                val email = (authState as com.mlbb.scrim.data.model.AuthResult.EmailNotVerified).email
                                navController.navigate(Screen.Verification.createRoute(email)) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                                viewModel.resetAuthState()
                            }
                            else -> {}
                        }
                    }
                    LoginScreen(
                        onLoginSuccess = {},
                        onNavigateToSignup = {
                            navController.navigate(Screen.Signup.route)
                        },
                        onNavigateToForgotPassword = {
                            navController.navigate(Screen.ForgotPassword.route)
                        },
                        onNavigateToOnboarding = {
                            navController.navigate(Screen.Onboarding.route)
                        },
                        viewModel = viewModel
                    )
                }

                composable(Screen.Signup.route) {
                    val authState by viewModel.authState.collectAsState()
                    LaunchedEffect(authState) {
                        when (authState) {
                            is com.mlbb.scrim.data.model.AuthResult.Success -> {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Signup.route) { inclusive = true }
                                }
                                viewModel.resetAuthState()
                            }
                            is com.mlbb.scrim.data.model.AuthResult.EmailNotVerified -> {
                                val email = (authState as com.mlbb.scrim.data.model.AuthResult.EmailNotVerified).email
                                navController.navigate(Screen.Verification.createRoute(email)) {
                                    popUpTo(Screen.Signup.route) { inclusive = true }
                                }
                                viewModel.resetAuthState()
                            }
                            else -> {}
                        }
                    }
                    SignupScreen(
                        onSignupSuccess = {},
                        onNavigateToLogin = {
                            navController.popBackStack()
                        },
                        viewModel = viewModel
                    )
                }

                composable(
                    route = Screen.Verification.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("email") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val email = Uri.decode(backStackEntry.arguments?.getString("email") ?: "")
                    val authState by viewModel.authState.collectAsState()
                    val resentSuccess by viewModel.resentSuccess.collectAsState()

                    LaunchedEffect(authState) {
                        if (authState is com.mlbb.scrim.data.model.AuthResult.Success) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Verification.route) { inclusive = true }
                            }
                            viewModel.resetAuthState()
                        }
                    }

                    val deletionSeconds = viewModel.secondsUntilDeletion()
                    val accountDeleted = authState is com.mlbb.scrim.data.model.AuthResult.Error
                            && (authState as? com.mlbb.scrim.data.model.AuthResult.Error)?.message?.contains("deleted", ignoreCase = true) == true

                    com.mlbb.scrim.ui.screens.VerificationScreen(
                        email = email,
                        initialSecondsUntilDeletion = deletionSeconds,
                        onResendEmail = { viewModel.resendVerificationEmail(it) },
                        onVerifyOtp = { otp -> viewModel.verifyOtp(otp) },
                        onBackToLogin = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Verification.route) { inclusive = true }
                            }
                        },
                        isLoading = authState is com.mlbb.scrim.data.model.AuthResult.Loading,
                        resentSuccess = resentSuccess,
                        accountDeleted = accountDeleted,
                        otpError = if (authState is com.mlbb.scrim.data.model.AuthResult.Error)
                            (authState as com.mlbb.scrim.data.model.AuthResult.Error).message else null
                    )
                }

                composable(Screen.Home.route) {
                    LaunchedEffect(languageCode) {
                        newsViewModel.loadNews(languageCode, forceRefresh = false)
                    }
                    HomeScreen(
                        userProfile = userProfile,
                        onLogout = { viewModel.signOut() },
                        onNavigateToCreateTeam = {
                            navController.navigate(Screen.CreateTeam.route)
                        },
                        onNavigateToCreateScrim = {
                            if (teams.isNotEmpty()) {
                                navController.navigate(Screen.CreateScrim.route)
                            } else {
                                navController.navigate(Screen.CreateTeam.route)
                            }
                        },
                        onNavigateToLeaderboard = {
                            navController.navigate(Screen.Leaderboard.route)
                        },
                        onNavigateToMatchHistory = {
                            navController.navigate(Screen.MatchHistory.route)
                        },
                        onNavigateToSchedule = {
                            navController.navigate(Screen.Schedule.route)
                        },
                        onNavigateToNotifications = {
                            navController.navigate(Screen.Notifications.route)
                        },
                        onNavigateToScrimDetail = { scrimId ->
                            navController.navigate(Screen.ScrimDetail.createRoute(scrimId))
                        },
                        scrims = scrims,
                        notificationCount = notificationUnreadCount,
                        isRefreshing = scrimIsLoading,
                        onRefresh = {
                            scrimViewModel.loadScrims()
                            newsViewModel.refresh(languageCode)
                            teamViewModel.loadTeams()
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    val authState by viewModel.authState.collectAsState()
                    ProfileScreen(
                        userProfile = userProfile,
                        isTab = true,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onUpdateProfile = { username, inGameId, role, bio, mainHeroes ->
                            viewModel.updateProfile(username, inGameId, role, bio, mainHeroes)
                        },
                        onUpdateEmail = { newEmail, currentPassword ->
                            viewModel.updateEmail(newEmail, currentPassword)
                        },
                        onUpdatePassword = { currentPassword, newPassword, confirmPassword ->
                            viewModel.updatePassword(currentPassword, newPassword, confirmPassword)
                        },
                        authResult = authState,
                        onResetAuthState = { viewModel.resetAuthState() },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onNavigateToAchievements = {
                            navController.navigate(Screen.Achievements.route)
                        },
                        onLogout = { viewModel.signOut() },
                        onUploadAvatar = { uri -> viewModel.uploadAvatar(uri) },
                        unlockedAchievements = derivedAchievements,
                        isRefreshing = isProfileRefreshing,
                        onRefresh = { viewModel.refreshProfile() }
                    )
                }

                composable(Screen.Achievements.route) {
                    com.mlbb.scrim.ui.screens.AchievementsScreen(
                        achievements = derivedStats.copy(
                            unlockedAchievements = derivedAchievements.map { it.id }
                        ),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.LfgBoard.route) {
                    LfgBoardScreen(
                        posts = lfgPosts,
                        isLoading = lfgIsLoading,
                        onNavigateBack = { navController.popBackStack() },
                        isRefreshing = lfgIsLoading,
                        onRefresh = { lfgViewModel.loadPosts() }
                    )
                }

                composable(Screen.TeamList.route) {
                    TeamListScreen(
                        teams = teams,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToCreateTeam = {
                            navController.navigate(Screen.CreateTeam.route)
                        },
                        onNavigateToJoinTeam = {
                            navController.navigate(Screen.JoinTeam.route)
                        },
                        onNavigateToFindTeams = {
                            navController.navigate(Screen.FindTeams.route)
                        },
                        onNavigateToTeamDetail = { team ->
                            navController.navigate(Screen.TeamDetail.createRoute(team.id))
                        },
                        onRefresh = {
                            teamViewModel.loadTeams()
                        }
                    )
                }

                composable(Screen.CreateTeam.route) {
                    val createSuccess by teamViewModel.createSuccess.collectAsState()
                    val isLoading by teamViewModel.isLoading.collectAsState()
                    val vmError by teamViewModel.errorMessage.collectAsState()

                    LaunchedEffect(Unit) {
                        teamViewModel.clearCreateSuccess()
                        teamViewModel.clearErrorMessage()
                    }

                    LaunchedEffect(createSuccess) {
                        createSuccess?.let {
                            teamViewModel.clearCreateSuccess()
                            navController.popBackStack()
                            navController.navigate(Screen.TeamList.route)
                        }
                    }

                    CreateTeamScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onCreateTeam = { teamName, logoUri, isOpen ->
                            teamViewModel.createTeam(teamName, userProfile?.id ?: "", logoUri, isOpen)
                        },
                        isLoading = isLoading,
                        errorMessage = vmError,
                        existingTeamNames = teams.map { it.name }
                    )
                }

                composable(
                    route = Screen.TeamDetail.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("teamId") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                    val team = teams.find { it.id == teamId }
                    if (team != null) {
                        val isLeader = team.leaderId == userProfile?.id
                        LaunchedEffect(teamId) {
                            if (isLeader) {
                                teamViewModel.loadTeamApplications(teamId)
                            }
                        }
                        TeamDetailScreen(
                            team = team,
                            isLeader = isLeader,
                            currentUserId = userProfile?.id ?: "",
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onUpdatePlayerRole = { playerId, newRole ->
                                teamViewModel.updatePlayerRole(teamId, playerId, newRole)
                            },
                            onRemovePlayer = { playerId ->
                                teamViewModel.removePlayer(teamId, playerId)
                            },
                            onLeaveTeam = {
                                teamViewModel.leaveTeam(teamId, userProfile?.id ?: "")
                                navController.popBackStack()
                            },
                            onDisbandTeam = {
                                teamViewModel.deleteTeam(teamId)
                                navController.popBackStack()
                            },
                            onInvitePlayer = {
                                // Show invite dialog with copy link option
                                showInviteDialog = true
                            },
                            onAddPlayer = { name, email, _ ->
                                teamViewModel.addPlayer(teamId, name, email)
                            },
                            applications = if (isLeader) teamApplications else emptyList(),
                            onAcceptApplication = { appId ->
                                teamViewModel.acceptApplication(appId)
                            },
                            onDeclineApplication = { appId ->
                                teamViewModel.declineApplication(appId, teamId)
                            }
                        )
                    }
                }

                composable(Screen.ScrimList.route) {
                    ScrimListScreen(
                        scrims = scrims,
                        isLoading = scrimIsLoading,
                        onNavigateToCreateScrim = {
                            navController.navigate(Screen.CreateScrim.route)
                        },
                        onNavigateToScrimDetail = { scrim ->
                            navController.navigate(Screen.ScrimDetail.createRoute(scrim.id))
                        },
                        onSearch = { query, gameMode, region, skillLevel, status ->
                            scrimViewModel.searchScrims(query, gameMode, region, skillLevel, status)
                        },
                        onRefresh = {
                            scrimViewModel.loadScrims()
                        }
                    )
                }

                composable(Screen.CreateScrim.route) {
                    CreateScrimScreen(
                        teamName = teams.firstOrNull()?.name ?: "My Team",
                        teamId = teams.firstOrNull()?.id ?: "",
                        teamLeader = userProfile?.username ?: "",
                        currentPlayerCount = teams.firstOrNull()?.currentPlayerCount ?: 0,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onCreateScrim = { gameMode, region, skillLevel, bestOf, scheduledTime, description ->
                            scrimViewModel.createScrim(
                                teamId = teams.firstOrNull()?.id ?: "",
                                teamName = teams.firstOrNull()?.name ?: "My Team",
                                teamLeader = userProfile?.username ?: "",
                                gameMode = gameMode,
                                region = region,
                                skillLevel = skillLevel,
                                bestOf = bestOf,
                                scheduledTime = scheduledTime,
                                description = description
                            )
                            navController.popBackStack()
                            navController.navigate(Screen.ScrimList.route)
                        }
                    )
                }

                composable(
                    route = Screen.ScrimDetail.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("scrimId") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val scrimId = backStackEntry.arguments?.getString("scrimId") ?: ""
                    val scrim = scrims.find { it.id == scrimId }
                    if (scrim != null) {
                        val myTeam = teams.firstOrNull()
                        val isTeamLeader = myTeam?.leaderId == userProfile?.id
                        val teamHasMinPlayers = myTeam?.meetsMinPlayers ?: false

                        ScrimDetailScreen(
                            scrim = scrim,
                            currentUserId = userProfile?.id ?: "",
                            currentUserTeamId = myTeam?.id,
                            currentUserTeamName = myTeam?.name,
                            isTeamLeader = isTeamLeader,
                            teamHasMinPlayers = teamHasMinPlayers,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onApplyScrim = { appliedScrim ->
                                if (myTeam != null && isTeamLeader && teamHasMinPlayers) {
                                    scrimViewModel.applyToScrim(
                                        scrimId = appliedScrim.id,
                                        applicantTeamId = myTeam.id,
                                        applicantTeamName = myTeam.name,
                                        applicantTeamLeader = myTeam.leaderId,
                                        applicantTeamLeaderName = userProfile?.username ?: ""
                                    )
                                }
                            },
                            onApproveApplication = { sid, appId ->
                                val convId = java.util.UUID.randomUUID().toString()
                                scrimViewModel.approveApplication(sid, appId, convId)
                                // Create conversation for the two team leaders
                                messageViewModel.sendApplyMessage(
                                    scrimId = sid,
                                    scrimTitle = scrim.teamName,
                                    applicantId = userProfile?.id ?: "",
                                    applicantName = userProfile?.username ?: "",
                                    applicantTeamId = myTeam?.id ?: "",
                                    applicantTeamName = myTeam?.name ?: "",
                                    scrimCreatorId = scrim.teamLeader,
                                    scrimCreatorName = scrim.teamLeader,
                                    scrimCreatorTeamId = scrim.teamId,
                                    scrimCreatorTeamName = scrim.teamName,
                                    teamPlayerCount = myTeam?.players?.size ?: 0,
                                    teamMaxPlayers = myTeam?.maxPlayers ?: 7
                                )
                            },
                            onRejectApplication = { sid, appId ->
                                scrimViewModel.rejectApplication(sid, appId)
                            },
                            onCancelApplication = { sid, appId ->
                                scrimViewModel.cancelApplication(sid, appId)
                            },
                            onCancelScrim = { sid ->
                                scrimViewModel.cancelScrim(sid)
                            },
                            onNavigateToChat = { convId ->
                                navController.navigate(Screen.Chat.createRoute(convId))
                            },
                            onNavigateToRoster = { sid, tid ->
                                navController.navigate(Screen.ScrimRoster.createRoute(sid, tid))
                            },
                            onMarkReady = { sid, tid ->
                                scrimViewModel.markReady(sid, tid)
                            },
                            onUploadScreenshot = { sid, tid, url ->
                                scrimViewModel.uploadScreenshot(sid, tid, url)
                            },
                            onCompleteScrim = { sid, winnerId ->
                                scrimViewModel.completeScrim(sid, winnerId)
                            }
                        )
                    }
                }

                composable(
                    route = Screen.ScrimRoster.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("scrimId") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("teamId") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val scrimId = backStackEntry.arguments?.getString("scrimId") ?: ""
                    val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                    val scrim = scrims.find { it.id == scrimId }
                    val team = teams.find { it.id == teamId }
                    if (scrim != null && team != null) {
                        val isTeamA = scrim.teamId == teamId
                        val existingRoster = if (isTeamA) scrim.teamARoster else scrim.teamBRoster
                        ScrimRosterScreen(
                            teamName = team.name,
                            teamId = teamId,
                            players = team.players,
                            existingRoster = existingRoster,
                            onNavigateBack = { navController.popBackStack() },
                            onConfirmRoster = { roster ->
                                scrimViewModel.setScrimRoster(scrimId, teamId, roster)
                                navController.popBackStack()
                            }
                        )
                    }
                }

                composable(Screen.MatchResultList.route) {
                    MatchResultListScreen(
                        matchResults = matchResults,
                        isLoading = matchResultIsLoading,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToMatchResultDetail = { result ->
                            navController.navigate(Screen.MatchResultDetail.createRoute(result.id))
                        },
                        onNavigateToReportResult = { result ->
                            navController.navigate(Screen.ReportMatchResult.createRoute(result.id))
                        },
                        currentUserTeamId = teams.firstOrNull()?.id,
                        onRefresh = {
                            matchResultViewModel.loadMatchResults()
                        }
                    )
                }

                composable(
                    route = Screen.MatchResultDetail.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("matchResultId") { type = androidx.navigation.NavType.StringType }
                    ),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "mlbbscrim://app/match_result/{matchResultId}" },
                        navDeepLink { uriPattern = "https://mlbbscrim.app/match_result/{matchResultId}" }
                    )
                ) { backStackEntry ->
                    val matchResultId = backStackEntry.arguments?.getString("matchResultId") ?: ""
                    var matchResult = matchResults.find { it.id == matchResultId }
                        ?: selectedMatchResult?.takeIf { it.id == matchResultId }

                    // Deep-link fallback: load from backend if not in local list
                    if (matchResult == null) {
                        LaunchedEffect(matchResultId) {
                            matchResultViewModel.loadMatchResultById(matchResultId)
                        }
                    }

                    if (matchResult != null) {
                        MatchResultDetailScreen(
                            matchResult = matchResult,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToReport = { result ->
                                navController.navigate(Screen.ReportMatchResult.createRoute(result.id))
                            },
                            currentUserTeamId = teams.firstOrNull()?.id
                        )
                    } else if (matchResultIsLoading) {
                        // Loading placeholder while fetching deep-linked match
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GoldPrimary)
                        }
                    }
                }

                composable(
                    route = Screen.ReportMatchResult.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("matchResultId") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val matchResultId = backStackEntry.arguments?.getString("matchResultId") ?: ""
                    val matchResult = matchResults.find { it.id == matchResultId }
                    if (matchResult != null) {
                        ReportMatchResultScreen(
                            matchResult = matchResult,
                            currentUserId = userProfile?.id ?: "",
                            currentUserName = userProfile?.username ?: "",
                            currentTeamId = teams.firstOrNull()?.id ?: "",
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onReportResult = { id, teamId, reporterId, reporterName, winnerId, notes ->
                                matchResultViewModel.reportResult(
                                    matchResultId = id,
                                    teamId = teamId,
                                    reporterId = reporterId,
                                    reporterName = reporterName,
                                    reportedWinnerId = winnerId,
                                    notes = notes
                                )
                            },
                            isLoading = matchResultIsLoading,
                            reportSuccess = reportSuccess,
                            onClearSuccess = {
                                matchResultViewModel.clearReportSuccess()
                            }
                        )
                    }
                }

                composable(Screen.MessageList.route) {
                    val userId = userProfile?.id ?: ""
                    LaunchedEffect(Unit) {
                        messageViewModel.loadConversations(userId)
                    }
                    // Start/stop background polling for new conversations
                    androidx.compose.runtime.DisposableEffect(userId) {
                        messageViewModel.startConversationsPolling(userId)
                        onDispose { messageViewModel.stopConversationsPolling() }
                    }
                    MessageListScreen(
                        conversations = conversations,
                        isLoading = messagesIsLoading,
                        currentUserId = userId,
                        isTab = true,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToChat = { conversation ->
                            messageViewModel.loadConversation(conversation.id)
                            messageViewModel.markAsRead(conversation.id, userId)
                            navController.navigate(Screen.Chat.createRoute(conversation.id))
                        },
                        onRefresh = {
                            messageViewModel.loadConversations(userId)
                        }
                    )
                }

                composable(
                    route = Screen.Chat.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("conversationId") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                    val conversation = conversations.find { it.id == conversationId }
                        ?: selectedConversation
                    val userId = userProfile?.id ?: ""

                    // Media Pickers
                    val imageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let { 
                            context.contentResolver.openInputStream(it)?.use { stream ->
                                messageViewModel.sendImageMessage(
                                    conversationId, userId, userProfile?.username ?: "", stream.readBytes()
                                )
                            }
                        }
                    }

                    // Start/stop real-time chat polling while on this screen
                    androidx.compose.runtime.DisposableEffect(conversationId) {
                        messageViewModel.startChatPolling(conversationId, userId)
                        onDispose { messageViewModel.stopChatPolling() }
                    }

                    if (conversation != null) {
                        ChatScreen(
                            conversation = conversation,
                            currentUserId = userId,
                            currentUserName = userProfile?.username ?: "",
                            onNavigateBack = {
                                messageViewModel.stopChatPolling()
                                navController.popBackStack()
                            },
                            onSendMessage = { content ->
                                messageViewModel.sendMessage(
                                    conversationId = conversationId,
                                    senderId = userId,
                                    senderName = userProfile?.username ?: "",
                                    content = content
                                )
                            },
                            onSendImage = { imageLauncher.launch("image/*") },
                            onSendVoice = {
                                // In a real app, this would open a recording UI
                                // For now, we simulate a small voice note
                                messageViewModel.sendVoiceMessage(
                                    conversationId, userId, userProfile?.username ?: "",
                                    ByteArray(100), 5 // 5 second mock
                                )
                            },
                            onUpdateTyping = { isTyping ->
                                messageViewModel.updateTypingStatus(conversationId, userId, isTyping)
                            },
                            onViewTeamInfo = { teamId, _ ->
                                val team = teams.find { it.id == teamId }
                                if (team != null) {
                                    navController.navigate(Screen.TeamDetail.createRoute(teamId))
                                }
                            },
                            isLoading = messagesIsLoading,
                            isRefreshing = messagesIsLoading,
                            onRefresh = { messageViewModel.loadConversation(conversationId) }
                        )
                    }
                }

                composable(Screen.Leaderboard.route) {
                    LeaderboardScreen(
                        entries = leaderboard,
                        isLoading = leaderboardIsLoading,
                        error = leaderboardError,
                        selectedTier = leaderboardTier,
                        onTierFilter = { tier ->
                            leaderboardViewModel.filterByTier(tier)
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onRefresh = {
                            leaderboardViewModel.loadLeaderboard()
                        },
                        onDismissError = {
                            leaderboardViewModel.clearError()
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onDeleteAccount = {
                            viewModel.deleteAccount()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        context = context,
                        notificationsEnabled = notificationsEnabled,
                        matchNotifications = matchNotifications,
                        messageNotifications = messageNotifications,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled,
                        languageCode = languageCode,
                        darkMode = darkMode,
                        onToggleNotifications = { settingsViewModel.toggleNotifications(it) },
                        onToggleMatchNotifications = { settingsViewModel.toggleMatchNotifications(it) },
                        onToggleMessageNotifications = { settingsViewModel.toggleMessageNotifications(it) },
                        onToggleSound = { settingsViewModel.toggleSound(it) },
                        onToggleVibration = { settingsViewModel.toggleVibration(it) },
                        onSetLanguage = { code ->
                            // MainActivity observes the languageCode flow and recreates itself
                            // automatically once the new value is persisted.
                            settingsViewModel.setLanguage(code)
                        },

                        onToggleDarkMode = { settingsViewModel.toggleDarkMode(it) },
                        onLogout = { viewModel.signOut() }
                    )
                }

                composable(Screen.MatchHistory.route) {
                    MatchHistoryScreen(
                        matchResults = matchResults,
                        isLoading = matchResultIsLoading,
                        currentUserTeamId = teams.firstOrNull()?.id,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToDetail = { match ->
                            matchResultViewModel.loadMatchResultById(match.id)
                            navController.navigate(Screen.MatchResultDetail.createRoute(match.id))
                        },
                        onRefresh = {
                            matchResultViewModel.loadMatchResults()
                        }
                    )
                }

                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.JoinTeam.route) {
                    JoinTeamScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onJoinTeam = {
                            // Join team - mock implementation
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.FindTeams.route) {
                    LaunchedEffect(Unit) {
                        teamViewModel.loadOpenTeams()
                    }
                    FindTeamsScreen(
                        teams = openTeams,
                        isLoading = teamIsLoading,
                        isRefreshing = teamIsLoading,
                        applicationSuccess = teamApplicationSuccess,
                        onRefresh = { teamViewModel.loadOpenTeams() },
                        onNavigateBack = { navController.popBackStack() },
                        onApplyToTeam = { teamId ->
                            teamViewModel.applyToTeam(teamId)
                        },
                        onNavigateToTeamDetail = { teamId ->
                            navController.navigate(Screen.TeamDetail.createRoute(teamId))
                        }
                    )
                }

                composable(Screen.Notifications.route) {
                    NotificationScreen(
                        notifications = notifications,
                        isLoading = notificationIsLoading,
                        error = notificationError,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onMarkAsRead = { id ->
                            notificationViewModel.markAsRead(id)
                        },
                        onMarkAllAsRead = {
                            notificationViewModel.markAllAsRead()
                        },
                        onDelete = { id ->
                            notificationViewModel.deleteNotification(id)
                        },
                        onRefresh = {
                            notificationViewModel.loadNotifications()
                        },
                        onDismissError = {
                            notificationViewModel.clearError()
                        }
                    )
                }

                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onFinish = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Schedule.route) {
                    ScheduleScreen(
                        scrims = scrims,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onScrimClick = { scrimId ->
                            navController.navigate(Screen.ScrimDetail.createRoute(scrimId))
                        }
                    )
                }

                composable(Screen.News.route) {
                    NewsScreen(
                        viewModel = newsViewModel,
                        languageCode = languageCode
                    )
                }

                composable(Screen.PlayerFinder.route) {
                    PlayerFinderScreen(
                        posts = lfgPosts,
                        isLoading = lfgIsLoading,
                        currentUserId = userProfile?.id ?: "",
                        currentUserProfile = userProfile,
                        myTeams = teams.filter { it.leaderId == userProfile?.id },
                        onCreatePost = { post ->
                            lfgViewModel.addPost(
                                playerId = post.playerId,
                                playerName = post.playerName,
                                role = post.role,
                                region = post.region,
                                skillLevel = post.skillLevel,
                                message = post.message,
                                mainHeroes = post.mainHeroes,
                                bio = post.bio,
                                rank = post.rank,
                                totalMatches = post.totalMatches,
                                winRate = post.winRate,
                                rankedWinRate = post.rankedWinRate,
                                inGameId = post.inGameId,
                                city = post.city,
                                screenshotUrl = post.screenshotUrl,
                                useMic = post.useMic,
                                playstyleTags = post.playstyleTags,
                                discord = post.discord,
                                telegram = post.telegram,
                                vk = post.vk,
                                facebook = post.facebook
                            )
                        },
                        onDeletePost = { postId -> lfgViewModel.deletePost(postId) },
                        onMessagePlayer = { post ->
                            messageViewModel.startDirectConversation(
                                senderId = userProfile?.id ?: "",
                                senderName = userProfile?.username ?: "",
                                recipientId = post.playerId,
                                recipientName = post.playerName
                            )
                            navController.navigate(Screen.MessageList.route)
                        },
                        isRefreshing = lfgIsLoading,
                        onRefresh = { lfgViewModel.loadPosts() }
                    )
                }
            }
            
            // Invite Dialog
            if (showInviteDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showInviteDialog = false },
                    title = { Text("Invite Player") },
                    text = { Text("Share invite link with other players to join your team.") },
                    confirmButton = {
                        TextButton(
                            onClick = { 
                                showInviteDialog = false
                                // Copy link functionality would go here
                            }
                        ) {
                            Text("Copy Link")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showInviteDialog = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
