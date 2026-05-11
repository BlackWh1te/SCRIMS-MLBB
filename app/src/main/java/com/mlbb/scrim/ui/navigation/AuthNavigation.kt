package com.mlbb.scrim.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.mlbb.scrim.ui.screens.NotificationScreen
import com.mlbb.scrim.ui.screens.OnboardingScreen
import com.mlbb.scrim.ui.screens.ScheduleScreen
import com.mlbb.scrim.ui.screens.ProfileScreen
import com.mlbb.scrim.ui.screens.ReportMatchResultScreen
import com.mlbb.scrim.ui.screens.ScrimDetailScreen
import com.mlbb.scrim.ui.screens.ScrimListScreen
import com.mlbb.scrim.ui.screens.JoinTeamScreen
import com.mlbb.scrim.ui.screens.SettingsScreen
import com.mlbb.scrim.ui.screens.SignupScreen
import com.mlbb.scrim.ui.screens.SplashScreen
import com.mlbb.scrim.ui.screens.TeamDetailScreen
import com.mlbb.scrim.ui.screens.TeamListScreen
import com.mlbb.scrim.ui.components.AppBottomNav
import com.mlbb.scrim.viewmodel.AuthViewModel
import com.mlbb.scrim.viewmodel.LeaderboardViewModel
import com.mlbb.scrim.viewmodel.MatchResultViewModel
import com.mlbb.scrim.viewmodel.MessageViewModel
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
    object Notifications : Screen("notifications")
    object Onboarding : Screen("onboarding")
    object Schedule : Screen("schedule")
}

@Composable
fun AuthNavigation(
    viewModel: AuthViewModel,
    teamViewModel: TeamViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    scrimViewModel: ScrimViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    matchResultViewModel: MatchResultViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    messageViewModel: MessageViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    leaderboardViewModel: LeaderboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    notificationViewModel: NotificationViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    navController: NavHostController = rememberNavController(),
    context: Context
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val teams by teamViewModel.teams.collectAsState()
    val scrims by scrimViewModel.scrims.collectAsState()
    val scrimIsLoading by scrimViewModel.isLoading.collectAsState()
    val matchResults by matchResultViewModel.matchResults.collectAsState()
    val matchResultIsLoading by matchResultViewModel.isLoading.collectAsState()
    val reportSuccess by matchResultViewModel.reportSuccess.collectAsState()
    val conversations by messageViewModel.conversations.collectAsState()
    val selectedConversation by messageViewModel.selectedConversation.collectAsState()
    val messagesIsLoading by messageViewModel.isLoading.collectAsState()
    val leaderboard by leaderboardViewModel.leaderboard.collectAsState()
    val leaderboardIsLoading by leaderboardViewModel.isLoading.collectAsState()
    val leaderboardTier by leaderboardViewModel.selectedTier.collectAsState()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    val matchNotifications by settingsViewModel.matchNotifications.collectAsState()
    val messageNotifications by settingsViewModel.messageNotifications.collectAsState()
    val soundEnabled by settingsViewModel.soundEnabled.collectAsState()
    val vibrationEnabled by settingsViewModel.vibrationEnabled.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val notificationUnreadCount by notificationViewModel.unreadCount.collectAsState()
    val notificationIsLoading by notificationViewModel.isLoading.collectAsState()

    val unreadCount = conversations.sumOf { it.unreadCount }

    // Start at splash screen
    val startDestination = Screen.Splash.route

    // Navigate based on auth state changes
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isLoggedIn) {
                AppBottomNav(
                    navController = navController,
                    unreadMessageCount = unreadCount
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { fadeIn() + slideInHorizontally { it / 8 } },
                exitTransition = { fadeOut() + slideOutHorizontally { -it / 8 } },
                popEnterTransition = { fadeIn() + slideInHorizontally { -it / 8 } },
                popExitTransition = { fadeOut() + slideOutHorizontally { it / 8 } }
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onSplashFinished = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Login.route) {
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
                    SignupScreen(
                        onSignupSuccess = {},
                        onNavigateToLogin = {
                            navController.popBackStack()
                        },
                        viewModel = viewModel
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        userProfile = userProfile,
                        onLogout = {
                            viewModel.signOut()
                        },
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
                        notificationCount = notificationUnreadCount
                    )
                }

                composable(Screen.Profile.route) {
                    val authState by viewModel.authState.collectAsState()
                    ProfileScreen(
                        userProfile = userProfile,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onUpdateProfile = { username, inGameId ->
                            viewModel.updateProfile(username, inGameId)
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
                        }
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
                        onNavigateToTeamDetail = { team ->
                            navController.navigate(Screen.TeamDetail.createRoute(team.id))
                        },
                        onRefresh = {
                            teamViewModel.loadTeams()
                        }
                    )
                }

                composable(Screen.CreateTeam.route) {
                    CreateTeamScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onCreateTeam = { teamName ->
                            teamViewModel.createTeam(teamName, userProfile?.email ?: "")
                            navController.popBackStack()
                            navController.navigate(Screen.TeamList.route)
                        }
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
                        val isLeader = team.leaderId == userProfile?.email
                        TeamDetailScreen(
                            team = team,
                            isLeader = isLeader,
                            currentUserId = userProfile?.email ?: "",
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
                                teamViewModel.leaveTeam(teamId, userProfile?.email ?: "")
                                navController.popBackStack()
                            },
                            onDisbandTeam = {
                                teamViewModel.deleteTeam(teamId)
                                navController.popBackStack()
                            },
                            onInvitePlayer = {
                                // TODO: Show invite dialog or copy link
                            }
                        )
                    }
                }

                composable(Screen.ScrimList.route) {
                    ScrimListScreen(
                        scrims = scrims,
                        isLoading = scrimIsLoading,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToCreateScrim = {
                            navController.navigate(Screen.CreateScrim.route)
                        },
                        onNavigateToScrimDetail = { scrim ->
                            navController.navigate(Screen.ScrimDetail.createRoute(scrim.id))
                        },
                        onSearch = { gameMode, region, skillLevel, status ->
                            scrimViewModel.searchScrims(gameMode, region, skillLevel, status)
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
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onCreateScrim = { gameMode, region, skillLevel, scheduledTime, description ->
                            scrimViewModel.createScrim(
                                teamId = teams.firstOrNull()?.id ?: "",
                                teamName = teams.firstOrNull()?.name ?: "My Team",
                                teamLeader = userProfile?.username ?: "",
                                gameMode = gameMode,
                                region = region,
                                skillLevel = skillLevel,
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
                        ScrimDetailScreen(
                            scrim = scrim,
                            currentUserId = userProfile?.email ?: "",
                            applicantTeamId = teams.firstOrNull()?.id,
                            applicantTeamName = teams.firstOrNull()?.name,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onJoinScrim = { sid ->
                                scrimViewModel.joinScrim(sid, userProfile?.email ?: "")
                            },
                            onLeaveScrim = { sid ->
                                scrimViewModel.leaveScrim(sid, userProfile?.email ?: "")
                            },
                            onApplyScrim = { appliedScrim ->
                                val myTeam = teams.firstOrNull()
                                if (myTeam != null) {
                                    messageViewModel.sendApplyMessage(
                                        scrimId = appliedScrim.id,
                                        scrimTitle = "${appliedScrim.teamName} - ${appliedScrim.gameMode.name}",
                                        applicantId = userProfile?.email ?: "",
                                        applicantName = userProfile?.username ?: "",
                                        applicantTeamId = myTeam.id,
                                        applicantTeamName = myTeam.name,
                                        scrimCreatorId = appliedScrim.teamLeader,
                                        scrimCreatorName = appliedScrim.teamLeader,
                                        scrimCreatorTeamId = appliedScrim.teamId,
                                        scrimCreatorTeamName = appliedScrim.teamName,
                                        teamPlayerCount = myTeam.players.size,
                                        teamMaxPlayers = myTeam.maxPlayers
                                    )
                                    navController.navigate(Screen.MessageList.route)
                                }
                            },
                            onCancelScrim = { sid ->
                                scrimViewModel.cancelScrim(sid)
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
                    )
                ) { backStackEntry ->
                    val matchResultId = backStackEntry.arguments?.getString("matchResultId") ?: ""
                    val matchResult = matchResults.find { it.id == matchResultId }
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
                            currentUserId = userProfile?.email ?: "",
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
                    LaunchedEffect(Unit) {
                        messageViewModel.loadConversations(userProfile?.email ?: "")
                    }
                    MessageListScreen(
                        conversations = conversations,
                        isLoading = messagesIsLoading,
                        currentUserId = userProfile?.email ?: "",
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToChat = { conversation ->
                            messageViewModel.loadConversation(conversation.id)
                            messageViewModel.markAsRead(conversation.id, userProfile?.email ?: "")
                            navController.navigate(Screen.Chat.createRoute(conversation.id))
                        },
                        onRefresh = {
                            messageViewModel.loadConversations(userProfile?.email ?: "")
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
                    if (conversation != null) {
                        ChatScreen(
                            conversation = conversation,
                            currentUserId = userProfile?.email ?: "",
                            currentUserName = userProfile?.username ?: "",
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onSendMessage = { content ->
                                messageViewModel.sendMessage(
                                    conversationId = conversationId,
                                    senderId = userProfile?.email ?: "",
                                    senderName = userProfile?.username ?: "",
                                    content = content
                                )
                            },
                            onViewTeamInfo = { teamId, _ ->
                                val team = teams.find { it.id == teamId }
                                if (team != null) {
                                    navController.navigate(Screen.TeamDetail.createRoute(teamId))
                                }
                            },
                            isLoading = messagesIsLoading
                        )
                    }
                }

                composable(Screen.Leaderboard.route) {
                    LeaderboardScreen(
                        entries = leaderboard,
                        isLoading = leaderboardIsLoading,
                        selectedTier = leaderboardTier,
                        onTierFilter = { tier ->
                            leaderboardViewModel.filterByTier(tier)
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onRefresh = {
                            leaderboardViewModel.loadLeaderboard()
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        notificationsEnabled = notificationsEnabled,
                        matchNotifications = matchNotifications,
                        messageNotifications = messageNotifications,
                        soundEnabled = soundEnabled,
                        vibrationEnabled = vibrationEnabled,
                        onToggleNotifications = { settingsViewModel.toggleNotifications(it) },
                        onToggleMatchNotifications = { settingsViewModel.toggleMatchNotifications(it) },
                        onToggleMessageNotifications = { settingsViewModel.toggleMessageNotifications(it) },
                        onToggleSound = { settingsViewModel.toggleSound(it) },
                        onToggleVibration = { settingsViewModel.toggleVibration(it) }
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
                            // TODO: Actually join team via repository
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Notifications.route) {
                    NotificationScreen(
                        notifications = notifications,
                        isLoading = notificationIsLoading,
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
            }
        }
    }
}
