package com.mlbb.scrim.ui.navigation

import android.content.Context
import android.net.Uri
import timber.log.Timber
import androidx.compose.foundation.background
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.mlbb.scrim.ui.screens.BannedScreen
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
import com.mlbb.scrim.ui.screens.TournamentListScreen
import com.mlbb.scrim.ui.screens.TournamentDetailScreen
import com.mlbb.scrim.ui.screens.TournamentCreateScreen
import com.mlbb.scrim.ui.screens.TournamentEditScreen
import com.mlbb.scrim.ui.screens.TournamentHostRequestScreen
import com.mlbb.scrim.ui.screens.TournamentHostManagementScreen
import com.mlbb.scrim.data.model.Tournament
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
    object Banned : Screen("banned")
    // ── Tournament Routes ──
    object TournamentList : Screen("tournament_list")
    object TournamentDetail : Screen("tournament_detail/{tournamentId}") {
        fun createRoute(tournamentId: String) = "tournament_detail/$tournamentId"
    }
    object TournamentCreate : Screen("tournament_create")
    object TournamentHostRequest : Screen("tournament_host_request")
    object TournamentEdit : Screen("tournament_edit/{tournamentId}") {
        fun createRoute(tournamentId: String) = "tournament_edit/$tournamentId"
    }
    object TournamentHostManagement : Screen("tournament_host_management")
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
    tournamentViewModel: com.mlbb.scrim.viewmodel.TournamentViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
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
    val scrimIsRefreshing by scrimViewModel.isRefreshing.collectAsState()
    val scrimError by scrimViewModel.error.collectAsState()
    val matchResults by matchResultViewModel.matchResults.collectAsState()
    val matchResultIsLoading by matchResultViewModel.isLoading.collectAsState()
    val matchResultIsRefreshing by matchResultViewModel.isRefreshing.collectAsState()
    val selectedMatchResult by matchResultViewModel.selectedMatchResult.collectAsState()
    val reportSuccess by matchResultViewModel.reportSuccess.collectAsState()
    val conversations by messageViewModel.conversations.collectAsState()
    val selectedConversation by messageViewModel.selectedConversation.collectAsState()
    val messagesIsLoading by messageViewModel.isLoading.collectAsState()
    val messagesIsRefreshing by messageViewModel.isRefreshing.collectAsState()
    val messageError by messageViewModel.error.collectAsState()
    val leaderboard by leaderboardViewModel.leaderboard.collectAsState()
    val leaderboardIsLoading by leaderboardViewModel.isLoading.collectAsState()
    val leaderboardIsRefreshing by leaderboardViewModel.isRefreshing.collectAsState()
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
    val notificationIsLoading by notificationViewModel.isLoading.collectAsState()
    val notificationIsRefreshing by notificationViewModel.isRefreshing.collectAsState()
    val notificationError by notificationViewModel.error.collectAsState()
    val lfgPosts by lfgViewModel.posts.collectAsState()
    val lfgIsLoading by lfgViewModel.isLoading.collectAsState()
    val lfgIsRefreshing by lfgViewModel.isRefreshing.collectAsState()
    val lfgError by lfgViewModel.error.collectAsState()
    val openTeams by teamViewModel.openTeams.collectAsState()
    val teamApplicationSuccess by teamViewModel.applicationSuccess.collectAsState()
    val teamApplications by teamViewModel.teamApplications.collectAsState()
    val teamIsLoading by teamViewModel.isLoading.collectAsState()
    val teamIsRefreshing by teamViewModel.isRefreshing.collectAsState()
    val teamError by teamViewModel.errorMessage.collectAsState()
    val teamStats by teamViewModel.teamStats.collectAsState()
    val teamRatings by teamViewModel.teamRatings.collectAsState()
    val newsIsRefreshing by newsViewModel.isRefreshing.collectAsState()

    // ── Tournament state ──
    val tournaments by tournamentViewModel.tournaments.collectAsState()
    val selectedTournament by tournamentViewModel.selectedTournament.collectAsState()
    val tournamentRequirements by tournamentViewModel.requirements.collectAsState()
    val tournamentTeams by tournamentViewModel.tournamentTeams.collectAsState()
    val tournamentMatches by tournamentViewModel.matches.collectAsState()
    val tournamentIsLoading by tournamentViewModel.isLoading.collectAsState()
    val tournamentIsRefreshing by tournamentViewModel.isRefreshing.collectAsState()
    val tournamentError by tournamentViewModel.error.collectAsState()
    val hostedTournaments by tournamentViewModel.hostedTournaments.collectAsState()
    val myApplications by tournamentViewModel.myApplications.collectAsState()
    val myHostRequest by tournamentViewModel.myHostRequest.collectAsState()
    val isTournamentHost = userProfile?.isTournamentHost == true

    val unreadCount = conversations.sumOf { it.unreadCount }
    val pendingInviteCount = notifications.count { !it.isRead && it.type == com.mlbb.scrim.data.model.NotificationType.TEAM_INVITE }
    val playerTeamIds = teams.filter { it.leaderId == userProfile?.id }.map { it.id }
    val playerLedTeams = teams.filter { it.leaderId == userProfile?.id }

    // Sync user's team IDs to TournamentViewModel for isMyMatch flag
    LaunchedEffect(playerTeamIds) {
        tournamentViewModel.setMyTeamIds(playerTeamIds)
    }
    // Derive achievement stats from available user data.
    // NOTE: player_stats columns (best_win_streak, ratings_given, has_regional_top,
    // jungler_wins, roamer_wins, night_wins, five_star_matches, has_flawless_victory)
    // have been added to the DB schema. Full integration requires fetching player_stats
    // from the backend and wiring into a ViewModel.
    val derivedStats = com.mlbb.scrim.data.model.PlayerAchievements(
        playerId = userProfile?.id ?: "",
        matchesPlayed = userProfile?.totalMatches ?: 0,
        scrimsCreated = scrims.count { it.teamId in playerTeamIds },
        teamsCreated = playerTeamIds.size,
        // Placeholder until player_stats backend integration is complete
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

    // Direct-chat navigation gate: set true when Message tapped in PlayerFinder,
    // so the LaunchedEffect below knows to push Chat (not MessageList) once
    // startDirectConversation() succeeds and selectedConversation is populated.
    var pendingDirectChat by remember { mutableStateOf(false) }
    LaunchedEffect(selectedConversation) {
        val conv = selectedConversation
        Timber.d("MessageFlow", "Nav: selectedConversation changed convId=${conv?.id} pendingDirectChat=$pendingDirectChat")
        if (pendingDirectChat && conv != null) {
            pendingDirectChat = false
            Timber.d("MessageFlow", "Nav: navigating to chat/${conv.id}")
            navController.navigate(Screen.Chat.createRoute(conv.id))
        }
    }

    // Reset pendingDirectChat if conversation start fails so it doesn't stick forever
    LaunchedEffect(messageError) {
        Timber.d("MessageFlow", "Nav: messageError=$messageError pendingDirectChat=$pendingDirectChat")
        if (messageError != null && pendingDirectChat) {
            pendingDirectChat = false
        }
    }

    // Navigate based on auth state changes
    // Only handle explicit logout here. Login navigation is handled
    // per-screen to avoid race conditions between isLoggedIn and authState effects.
    var previousIsLoggedIn by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(isLoggedIn) {
        when {
            // Only handle logout navigation here
            !isLoggedIn && previousIsLoggedIn == true -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        previousIsLoggedIn = isLoggedIn
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = com.mlbb.scrim.ui.theme.heroGradientBrush()),
        containerColor = Color.Transparent, // Ensure background shows through
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                // We keep the padding to avoid content being hidden,
                // but since the background is on the Scaffold, it will look seamless.
                .padding(paddingValues)
        ) {
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
                                    if (userProfile?.isBanned == true) {
                                        navController.navigate(Screen.Banned.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Splash.route) { inclusive = true }
                                        }
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
                                if (userProfile?.isBanned == true) {
                                    navController.navigate(Screen.Banned.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Splash.route) { inclusive = true }
                                    }
                                }
                            } else {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }
                    }
                }

                composable(Screen.Banned.route) {
                    val currentProfile by viewModel.userProfile.collectAsState()
                    currentProfile?.let { profile ->
                        BannedScreen(
                            profile = profile,
                            onLogout = {
                                viewModel.signOut()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Banned.route) { inclusive = true }
                                }
                            }
                        )
                    }
                }

                // ── Tournament Routes ──

                composable(Screen.TournamentList.route) {
                    LaunchedEffect(Unit) {
                        viewModel.refreshProfile()
                        tournamentViewModel.loadTournaments()
                    }
                    TournamentListScreen(
                        tournaments = tournaments,
                        isLoading = tournamentIsLoading,
                        isRefreshing = tournamentIsRefreshing,
                        error = tournamentError,
                        isTournamentHost = isTournamentHost,
                        hostedTournaments = hostedTournaments,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTournamentDetail = { id ->
                            navController.navigate(Screen.TournamentDetail.createRoute(id))
                        },
                        onNavigateToCreateTournament = {
                            navController.navigate(Screen.TournamentCreate.route)
                        },
                        onNavigateToHostRequest = {
                            navController.navigate(Screen.TournamentHostRequest.route)
                        },
                        onNavigateToHostManagement = {
                            navController.navigate(Screen.TournamentHostManagement.route)
                        },
                        onSetStatusFilter = { tournamentViewModel.setStatusFilter(it) },
                        onRefresh = {
                            viewModel.refreshProfile()
                            tournamentViewModel.loadTournaments(isRefresh = true)
                        },
                        onDismissError = { tournamentViewModel.clearError() }
                    )
                }

                composable(
                    route = Screen.TournamentDetail.route,
                    arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: ""
                    val roomSecret by tournamentViewModel.roomSecret.collectAsState()
                    LaunchedEffect(tournamentId) { tournamentViewModel.loadTournamentById(tournamentId) }
                    TournamentDetailScreen(
                        tournament = selectedTournament,
                        requirements = tournamentRequirements,
                        teams = tournamentTeams,
                        matches = tournamentMatches,
                        roomSecret = roomSecret,
                        isLoading = tournamentIsLoading,
                        error = tournamentError,
                        myTeams = playerLedTeams,
                        myApplications = myApplications,
                        isHost = isTournamentHost && selectedTournament?.hostUserId == userProfile?.id,
                        onNavigateBack = { navController.popBackStack() },
                        onApply = { tid, teamId -> tournamentViewModel.applyForTournament(tid, teamId) },
                        onCheckIn = { tid, teamId -> tournamentViewModel.checkInTeam(tid, teamId) },
                        onRefresh = { tournamentViewModel.loadTournamentById(tournamentId) },
                        onDismissError = { tournamentViewModel.clearError() },
                        onNavigateToChat = { convId ->
                            navController.navigate(Screen.Chat.createRoute(convId))
                        },
                        onNavigateToEdit = { tid ->
                            navController.navigate(Screen.TournamentEdit.createRoute(tid))
                        },
                        onGeneratePairings = { tid -> tournamentViewModel.generateSwissPairings(tid) },
                        onReviewApplication = { appId, approved, reason -> tournamentViewModel.reviewApplication(appId, approved, reason) },
                        onSubmitMatchResult = { mid, winnerId, isDraw -> tournamentViewModel.submitMatchResult(mid, winnerId, isDraw) },
                        onCancelTournament = { tid, reason -> tournamentViewModel.cancelTournament(tid, reason) },
                        onCompleteTournament = { tid -> tournamentViewModel.completeTournament(tid) },
                        onDisqualifyTeam = { tid, teamId, reason -> tournamentViewModel.disqualifyTeam(tid, teamId, reason) },
                        onLoadRoomSecret = { mid -> tournamentViewModel.loadRoomSecret(mid) }
                    )
                }

                composable(Screen.TournamentCreate.route) {
                    val createResult by tournamentViewModel.createResult.collectAsState()
                    LaunchedEffect(createResult) {
                        if (createResult?.isSuccess == true) {
                            navController.popBackStack()
                        }
                    }
                    TournamentCreateScreen(
                        isLoading = tournamentIsLoading,
                        error = tournamentError,
                        onCreate = { tournament ->
                            tournamentViewModel.createTournament(tournament)
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onDismissError = { tournamentViewModel.clearError() }
                    )
                }

                composable(Screen.TournamentHostRequest.route) {
                    LaunchedEffect(Unit) { tournamentViewModel.loadMyHostRequest() }
                    TournamentHostRequestScreen(
                        existingRequest = myHostRequest,
                        isLoading = tournamentIsLoading,
                        error = tournamentError,
                        onSubmit = { motivation, experience, telegram, links ->
                            tournamentViewModel.submitHostRequest(motivation, experience, telegram, links)
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onDismissError = { tournamentViewModel.clearError() }
                    )
                }

                composable(
                    route = Screen.TournamentEdit.route,
                    arguments = listOf(navArgument("tournamentId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: ""
                    val updateResult by tournamentViewModel.updateResult.collectAsState()
                    LaunchedEffect(tournamentId) {
                        if (selectedTournament?.id != tournamentId) {
                            tournamentViewModel.loadTournamentById(tournamentId)
                        }
                    }
                    LaunchedEffect(updateResult) {
                        if (updateResult?.isSuccess == true) {
                            tournamentViewModel.clearUpdateResult()
                            navController.popBackStack()
                        }
                    }
                    TournamentEditScreen(
                        tournament = selectedTournament ?: Tournament(),
                        isLoading = tournamentIsLoading,
                        error = tournamentError,
                        onSave = { tid, updates ->
                            tournamentViewModel.updateTournament(tid, updates)
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onDismissError = { tournamentViewModel.clearError() }
                    )
                }

                composable(Screen.TournamentHostManagement.route) {
                    TournamentHostManagementScreen(
                        hostedTournaments = hostedTournaments,
                        isLoading = tournamentIsLoading,
                        isRefreshing = tournamentIsRefreshing,
                        error = tournamentError,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTournamentDetail = { id ->
                            navController.navigate(Screen.TournamentDetail.createRoute(id))
                        },
                        onNavigateToEditTournament = { id ->
                            navController.navigate(Screen.TournamentEdit.createRoute(id))
                        },
                        onCancelTournament = { tid, reason ->
                            tournamentViewModel.cancelTournament(tid, reason)
                        },
                        onCompleteTournament = { tid ->
                            tournamentViewModel.completeTournament(tid)
                        },
                        onRefresh = {
                            tournamentViewModel.loadTournaments(isRefresh = true)
                        },
                        onDismissError = { tournamentViewModel.clearError() }
                    )
                }

                // Redirect to BannedScreen if user becomes banned while logged in
                composable("_ban_redirect") {
                    LaunchedEffect(userProfile?.isBanned) {
                        if (userProfile?.isBanned == true &&
                            navController.currentDestination?.route != Screen.Banned.route
                        ) {
                            navController.navigate(Screen.Banned.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }

                composable(Screen.Login.route) {
                    val authState by viewModel.authState.collectAsState()
                    LaunchedEffect(authState) {
                        when (authState) {
                            is com.mlbb.scrim.data.model.AuthResult.Success -> {
                                if (userProfile?.isBanned == true) {
                                    navController.navigate(Screen.Banned.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
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
                    // Realtime: subscribe to scrim updates while on home
                    androidx.compose.runtime.DisposableEffect(Unit) {
                        scrimViewModel.subscribeToAllScrimUpdates()
                        onDispose { scrimViewModel.stopRealtimeSubscriptions() }
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
                        onNavigateToTeamDetail = { teamId ->
                            navController.navigate(Screen.TeamDetail.createRoute(teamId))
                        },
                        onNavigateToTournamentList = {
                            navController.navigate(Screen.TournamentList.route)
                        },
                        scrims = scrims,
                        teams = teams,
                        notificationCount = notificationUnreadCount,
                        isRefreshing = scrimIsRefreshing,
                        isTournamentHost = isTournamentHost,
                        onRefresh = {
                            scrimViewModel.loadScrims(isRefresh = true)
                            newsViewModel.refresh(languageCode)
                            teamViewModel.loadTeams(isRefresh = true)
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
                        isRefreshing = lfgIsRefreshing,
                        onRefresh = { lfgViewModel.loadPosts(isRefresh = true) }
                    )
                }

                composable(Screen.TeamList.route) {
                    TeamListScreen(
                        teams = teams,
                        isRefreshing = teamIsRefreshing,
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
                            teamViewModel.loadTeams(isRefresh = true)
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
                        // Realtime: subscribe to team updates while viewing team detail
                        val currentUserId = userProfile?.id ?: ""
                        androidx.compose.runtime.DisposableEffect(teamId, currentUserId) {
                            teamViewModel.subscribeToTeamUpdates(teamId)
                            if (currentUserId.isNotBlank()) {
                                teamViewModel.subscribeToTeamInvites(currentUserId)
                            }
                            onDispose { teamViewModel.stopRealtimeSubscriptions() }
                        }
                        TeamDetailScreen(
                            team = team,
                            isLeader = isLeader,
                            currentUserId = userProfile?.id ?: "",
                            teamStats = teamStats,
                            teamRatings = teamRatings,
                            onNavigateBack = {
                                teamViewModel.clearTeamStats()
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
                            },
                            onLoadStats = {
                                teamViewModel.loadTeamStats(teamId)
                                teamViewModel.loadTeamRatings(teamId)
                            }
                        )
                    }
                }

                composable(Screen.ScrimList.route) {
                    ScrimListScreen(
                        scrims = scrims,
                        isLoading = scrimIsLoading,
                        error = scrimError,
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
                            scrimViewModel.loadScrims(isRefresh = true)
                        },
                        onDismissError = { scrimViewModel.clearError() }
                    )
                }

                composable(Screen.CreateScrim.route) {
                    CreateScrimScreen(
                        teams = teams,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onCreateScrim = { teamId, teamName, gameMode, region, skillLevel, bestOf, scheduledTime, description ->
                            scrimViewModel.createScrim(
                                teamId = teamId,
                                teamName = teamName,
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
                        isRefreshing = matchResultIsRefreshing,
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
                            matchResultViewModel.loadMatchResults(isRefresh = true)
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
                                    scrimId = id,
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
                            messageViewModel.loadConversations(userId, isRefresh = true)
                        },
                        isRefreshing = messagesIsRefreshing,
                        error = messageError,
                        onDismissError = { messageViewModel.clearError() }
                    )
                }

                composable(
                    route = Screen.Chat.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("conversationId") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                    // Prefer selectedConversation if it matches current ID (has freshest messages),
                    // otherwise fall back to the conversations list
                    val conversation = when {
                        selectedConversation?.id == conversationId -> selectedConversation
                        else -> conversations.find { it.id == conversationId }
                    }
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

                    // Start/stop real-time chat subscription while on this screen
                    androidx.compose.runtime.DisposableEffect(conversationId) {
                        messageViewModel.startChatSubscription(conversationId, userId)
                        onDispose { messageViewModel.stopChatSubscription(conversationId) }
                    }

                    if (conversation != null) {
                        ChatScreen(
                            conversation = conversation,
                            currentUserId = userId,
                            currentUserName = userProfile?.username ?: "",
                            onNavigateBack = {
                                messageViewModel.stopChatSubscription(conversationId)
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
                            error = messageError,
                            onDismissError = { messageViewModel.clearError() },
                            isRefreshing = messagesIsRefreshing,
                            onRefresh = { messageViewModel.loadConversation(conversationId) }
                        )
                    }
                }

                composable(Screen.Leaderboard.route) {
                    LeaderboardScreen(
                        entries = leaderboard,
                        isLoading = leaderboardIsLoading,
                        isRefreshing = leaderboardIsRefreshing,
                        error = leaderboardError,
                        selectedTier = leaderboardTier,
                        onTierFilter = { tier ->
                            leaderboardViewModel.filterByTier(tier)
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onRefresh = {
                            leaderboardViewModel.loadLeaderboard(isRefresh = true)
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
                        isRefreshing = matchResultIsRefreshing,
                        currentUserTeamId = teams.firstOrNull()?.id,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToDetail = { match ->
                            matchResultViewModel.loadMatchResultById(match.id)
                            navController.navigate(Screen.MatchResultDetail.createRoute(match.id))
                        },
                        onRefresh = {
                            matchResultViewModel.loadMatchResults(isRefresh = true)
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
                        isRefreshing = teamIsRefreshing,
                        applicationSuccess = teamApplicationSuccess,
                        error = teamError,
                        onRefresh = { teamViewModel.loadOpenTeams() },
                        onNavigateBack = { navController.popBackStack() },
                        onApplyToTeam = { teamId ->
                            teamViewModel.applyToTeam(teamId)
                        },
                        onNavigateToTeamDetail = { teamId ->
                            navController.navigate(Screen.TeamDetail.createRoute(teamId))
                        },
                        onDismissError = { teamViewModel.clearErrorMessage() }
                    )
                }

                composable(Screen.Notifications.route) {
                    // Realtime: subscribe to new notifications while on this screen
                    val notifUserId = userProfile?.id ?: ""
                    androidx.compose.runtime.DisposableEffect(notifUserId) {
                        notificationViewModel.startRealtimeSubscription()
                        onDispose { notificationViewModel.stopRealtimeSubscription() }
                    }
                    NotificationScreen(
                        notifications = notifications,
                        isLoading = notificationIsLoading,
                        isRefreshing = notificationIsRefreshing,
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
                            notificationViewModel.loadNotifications(isRefresh = true)
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
                        languageCode = languageCode,
                        isRefreshing = newsIsRefreshing,
                        onRefresh = { newsViewModel.refresh(languageCode) }
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
                                facebook = post.facebook,
                                avatarUrl = post.avatarUrl
                            )
                        },
                        onDeletePost = { postId -> lfgViewModel.deletePost(postId) },
                        onViewCountIncrement = { postId -> lfgViewModel.incrementViewCount(postId) },
                        onMessagePlayer = { post ->
                            val senderId = userProfile?.id
                            val senderName = userProfile?.username
                            if (!senderId.isNullOrBlank() && !senderName.isNullOrBlank()) {
                                pendingDirectChat = true
                                messageViewModel.startDirectConversation(
                                    senderId = senderId,
                                    senderName = senderName,
                                    recipientId = post.playerId,
                                    recipientName = post.playerName
                                )
                            } else {
                                messageViewModel.setError("Profile not loaded yet. Please try again.")
                            }
                        },
                        onInvitePlayer = { _ ->
                            showInviteDialog = true
                        },
                        isRefreshing = lfgIsRefreshing,
                        onRefresh = { lfgViewModel.loadPosts(isRefresh = true) },
                        error = lfgError,
                        onDismissError = { lfgViewModel.clearError() },
                        messageLoading = messagesIsLoading,
                        messageError = messageError,
                        onDismissMessageError = { messageViewModel.clearError() }
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
