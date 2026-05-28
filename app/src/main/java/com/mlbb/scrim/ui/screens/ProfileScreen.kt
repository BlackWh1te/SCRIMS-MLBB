package com.mlbb.scrim.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.GradientButton
import com.mlbb.scrim.ui.components.PullToRefreshContainer
import com.mlbb.scrim.ui.components.RankBadge
import com.mlbb.scrim.ui.components.RankBadgeSize
import com.mlbb.scrim.ui.components.ResponsiveMetrics
import com.mlbb.scrim.ui.components.rememberResponsiveMetrics
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource

@Composable
fun ProfileScreen(
    userProfile: com.mlbb.scrim.data.model.UserProfile?,
    onNavigateBack: () -> Unit = {},
    isTab: Boolean = false,
    onUpdateProfile: (String, String, String?, String?, List<String>?) -> Unit,
    onUpdateEmail: (String, String) -> Unit = { _, _ -> },
    onUpdatePassword: (String, String, String) -> Unit = { _, _, _ -> },
    authResult: com.mlbb.scrim.data.model.AuthResult? = null,
    onResetAuthState: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onLogout: () -> Unit = {},
    onUploadAvatar: (android.net.Uri) -> Unit = {},
    unlockedAchievements: List<com.mlbb.scrim.data.model.Achievement> = emptyList(),
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(userProfile?.username ?: "") }
    var inGameId by remember { mutableStateOf(userProfile?.inGameId ?: "") }
    var role by remember { mutableStateOf(userProfile?.role ?: "") }
    var bio by remember { mutableStateOf(userProfile?.bio ?: "") }
    var mainHeroesInput by remember { mutableStateOf(userProfile?.mainHeroes?.joinToString(", ") ?: "") }
    val responsive = rememberResponsiveMetrics()
    val changesSavedMessage = stringResource(R.string.changes_saved)

    // Sync local state with userProfile when not editing (e.g., after background refresh)
    LaunchedEffect(userProfile, isEditing) {
        if (!isEditing && userProfile != null) {
            username = userProfile.username
            inGameId = userProfile.inGameId
            role = userProfile.role
            bio = userProfile.bio
            mainHeroesInput = userProfile.mainHeroes.joinToString(", ")
        }
    }

    // Avatar picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onUploadAvatar(it) }
    }

    // Account security dialog states
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    // Success snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authResult) {
        when (authResult) {
            is com.mlbb.scrim.data.model.AuthResult.Success -> {
                snackbarHostState.showSnackbar(changesSavedMessage)
                onResetAuthState()
            }
            is com.mlbb.scrim.data.model.AuthResult.Error -> {
                snackbarHostState.showSnackbar(authResult.message)
                onResetAuthState()
            }
            else -> {}
        }
    }

    PullToRefreshContainer(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(
                brush = heroGradientBrush()
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header
            AnimatedEntrance(delayMillis = 0) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = responsive.contentMaxWidth)
                            .align(Alignment.Center)
                            .padding(horizontal = responsive.horizontalPadding, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isTab) {
                            GlassBackButton(onClick = onNavigateBack)
                        } else {
                            Spacer(modifier = Modifier.size(44.dp))
                        }

                        Text(
                            text = stringResource(R.string.my_profile),
                            style = iOSTitle2.copy(color = TextPrimary),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isEditing) BluePrimary else SurfaceOverlay)
                                .border(1.dp, if (isEditing) Color.Transparent else GlassBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isEditing) {
                                        val heroesList = mainHeroesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3)
                                        onUpdateProfile(username, inGameId, role.takeIf { it.isNotBlank() }, bio.takeIf { it.isNotBlank() }, heroesList)
                                    }
                                    isEditing = !isEditing
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditing) stringResource(R.string.save) else stringResource(R.string.edit),
                                tint = White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = responsive.contentMaxWidth)
                    .align(Alignment.CenterHorizontally)
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = responsive.horizontalPadding, vertical = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                AnimatedEntrance(delayMillis = 100) {
                    if (isEditing) {
                        ProfileEditCard(
                            username = username,
                            onUsernameChange = { username = it },
                            inGameId = inGameId,
                            onInGameIdChange = { inGameId = it },
                            role = role,
                            onRoleChange = { role = it },
                            bio = bio,
                            onBioChange = { bio = it },
                            mainHeroesInput = mainHeroesInput,
                            onMainHeroesChange = { mainHeroesInput = it }
                        )
                    } else {
                        ProfileHeroCard(
                            userProfile = userProfile,
                            username = username,
                            inGameId = inGameId,
                            bio = bio,
                            role = role,
                            responsive = responsive,
                            onAvatarClick = { imagePicker.launch("image/*") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Profile Info Cards
                AnimatedEntrance(delayMillis = 250) {
                    ProfileInfoPanel(
                        items = listOf(
                            ProfileInfoItem(
                                icon = Icons.Default.Email,
                                label = stringResource(R.string.email),
                                value = userProfile?.email ?: stringResource(R.string.not_set),
                                color = BluePrimary
                            ),
                            ProfileInfoItem(
                                icon = Icons.Default.CalendarToday,
                                label = stringResource(R.string.member_since),
                                value = userProfile?.createdAt?.let {
                                    java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(it)
                                } ?: stringResource(R.string.not_set),
                                color = GoldPrimary
                            ),
                            ProfileInfoItem(
                                icon = Icons.Default.SportsEsports,
                                label = stringResource(R.string.in_game_id),
                                value = userProfile?.inGameId ?: stringResource(R.string.not_set),
                                color = SuccessGreen
                            )
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Section
                AnimatedEntrance(delayMillis = 375) {
                    Text(
                        text = stringResource(R.string.player_stats),
                        style = iOSTitle3.copy(color = White)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedEntrance(delayMillis = 380) {
                    ProfileStatGrid(
                        stats = listOf(
                            ProfileStatItem(
                                label = stringResource(R.string.matches),
                                value = (userProfile?.totalMatches ?: 0).toString(),
                                color = GoldPrimary
                            ),
                            ProfileStatItem(
                                label = stringResource(R.string.wins),
                                value = (userProfile?.wins ?: 0).toString(),
                                color = SuccessGreen
                            ),
                            ProfileStatItem(
                                label = stringResource(R.string.losses),
                                value = (userProfile?.losses ?: 0).toString(),
                                color = ErrorRed
                            ),
                            ProfileStatItem(
                                label = stringResource(R.string.win_rate),
                                value = userProfile?.winRate ?: "0%",
                                color = BluePrimary
                            ),
                            ProfileStatItem(
                                label = stringResource(R.string.xp_label),
                                value = (userProfile?.xp ?: 0).toString(),
                                color = Purple
                            ),
                            ProfileStatItem(
                                label = stringResource(R.string.pts_label),
                                value = userProfile?.ptsDisplay ?: "0",
                                color = if ((userProfile?.pts ?: 0) >= 0) SuccessGreen else ErrorRed
                            )
                        ),
                        responsive = responsive
                    )
                }

                // Host Tournament Stats
                if (userProfile?.isTournamentHost == true) {
                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedEntrance(delayMillis = 386) {
                        Text(
                            text = stringResource(R.string.host_stats),
                            style = iOSTitle3.copy(color = White)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedEntrance(delayMillis = 387) {
                        ProfileStatGrid(
                            stats = listOf(
                                ProfileStatItem(
                                    label = stringResource(R.string.hosted),
                                    value = userProfile.tournamentsHosted.toString(),
                                    color = GoldPrimary
                                ),
                                ProfileStatItem(
                                    label = stringResource(R.string.host_stat_completed),
                                    value = userProfile.tournamentsCompleted.toString(),
                                    color = SuccessGreen
                                ),
                                ProfileStatItem(
                                    label = stringResource(R.string.host_stat_cancelled),
                                    value = userProfile.tournamentsCancelled.toString(),
                                    color = ErrorRed
                                )
                            ),
                            responsive = responsive
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Achievements Section
                AnimatedEntrance(delayMillis = 388) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.achievements),
                            style = iOSTitle3.copy(color = White)
                        )
                        TextButton(onClick = onNavigateToAchievements) {
                            Text(
                                text = stringResource(R.string.view_all),
                                color = BluePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedEntrance(delayMillis = 389) {
                    if (unlockedAchievements.isNotEmpty()) {
                        com.mlbb.scrim.ui.components.AchievementBadgeRow(
                            achievements = unlockedAchievements,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.no_achievements_yet),
                            fontSize = 13.sp,
                            color = LightGray.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // App Settings Section
                AnimatedEntrance(delayMillis = 390) {
                    Text(
                        text = stringResource(R.string.app_settings),
                        style = iOSTitle3.copy(color = White)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedEntrance(delayMillis = 395) {
                    AccountActionCard(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.settings),
                        subtitle = stringResource(R.string.settings_sub),
                        color = BluePrimary,
                        onClick = onNavigateToSettings
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Host Dashboard — web admin panel with credentials
                if (userProfile?.isTournamentHost == true) {
                    AnimatedEntrance(delayMillis = 396) {
                        val context = LocalContext.current
                        val clipboardManager = LocalClipboardManager.current
                        var emailCopied by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(GoldPrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                            .border(1.dp, GoldPrimary.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Dashboard, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.host_credentials),
                                            style = iOSHeadline.copy(color = TextPrimary)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            stringResource(R.string.host_credentials_sub),
                                            style = iOSCaption1.copy(color = TextSecondary),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Email row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.host_email_label),
                                            style = iOSCaption1.copy(color = TextSecondary)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = userProfile.email,
                                            style = iOSBody.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(userProfile.email))
                                            emailCopied = true
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = stringResource(R.string.copy_email),
                                            tint = if (emailCopied) SuccessGreen else GoldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            if (emailCopied) stringResource(R.string.email_copied) else stringResource(R.string.copy_email),
                                            color = if (emailCopied) SuccessGreen else GoldPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // Password hint
                                Text(
                                    stringResource(R.string.host_password_hint),
                                    style = iOSCaption1.copy(color = TextSecondary.copy(alpha = 0.8f))
                                )

                                Spacer(Modifier.height(12.dp))

                                // Open Dashboard button
                                GradientButton(
                                    text = stringResource(R.string.open_dashboard),
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://admin-panel-mlbb.vercel.app/host/login"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Support Button — opens JotForm support request
                AnimatedEntrance(delayMillis = 398) {
                    val context = LocalContext.current
                    AccountActionCard(
                        icon = Icons.Default.HelpOutline,
                        title = stringResource(R.string.support),
                        subtitle = stringResource(R.string.support_sub),
                        color = SuccessGreen,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://form.jotform.com/shukhratmamatkulov1999/support-request-form"))
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Account Security Section
                AnimatedEntrance(delayMillis = 400) {
                    Text(
                        text = stringResource(R.string.account_security),
                        style = iOSTitle3.copy(color = White)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Change Email Card
                AnimatedEntrance(delayMillis = 450) {
                    AccountActionCard(
                        icon = Icons.Default.Email,
                        title = stringResource(R.string.change_email),
                        subtitle = userProfile?.email ?: stringResource(R.string.not_set),
                        color = BluePrimary,
                        onClick = { showEmailDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Change Password Card
                AnimatedEntrance(delayMillis = 500) {
                    AccountActionCard(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.change_password),
                        subtitle = stringResource(R.string.update_password_sub),
                        color = GoldPrimary,
                        onClick = { showPasswordDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Sign Out Section
                AnimatedEntrance(delayMillis = 550) {
                    AccountActionCard(
                        icon = Icons.Default.Logout,
                        title = stringResource(R.string.sign_out),
                        subtitle = stringResource(R.string.sign_out_sub),
                        color = WarningOrange,
                        onClick = { showLogoutConfirm = true }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Email Change Dialog
    if (showEmailDialog) {
        ChangeEmailDialog(
            currentEmail = userProfile?.email ?: "",
            onDismiss = { showEmailDialog = false },
            onConfirm = { newEmail, currentPassword ->
                onUpdateEmail(newEmail, currentPassword)
                showEmailDialog = false
            }
        )
    }

    // Password Change Dialog
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { currentPassword, newPassword, confirmPassword ->
                onUpdatePassword(currentPassword, newPassword, confirmPassword)
                showPasswordDialog = false
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = stringResource(R.string.sign_out),
                    style = iOSTitle3.copy(color = White, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.sign_out_confirm_message),
                    style = iOSBody.copy(color = LightGray)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    }
                ) {
                    Text(stringResource(R.string.sign_out), color = WarningOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.cancel_btn), color = LightGray)
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
        )
    }
}
}

private data class ProfileInfoItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val value: String,
    val color: Color
)

private data class ProfileStatItem(
    val label: String,
    val value: String,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeroCard(
    userProfile: com.mlbb.scrim.data.model.UserProfile?,
    username: String,
    inGameId: String,
    bio: String,
    role: String,
    responsive: ResponsiveMetrics,
    onAvatarClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                spotColor = BluePrimary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        SurfaceElevated.copy(alpha = 0.96f),
                        SurfaceCard.copy(alpha = 0.98f),
                        DarkSurface.copy(alpha = 0.96f)
                    )
                )
            )
            .border(1.dp, GlassBorder.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(responsive.profileAvatarSize)
                    .shadow(
                        elevation = 18.dp,
                        spotColor = GoldPrimary.copy(alpha = 0.28f),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(colors = BlueGradient))
                    .border(2.dp, White.copy(alpha = 0.12f), CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                if (userProfile?.avatarUrl != null) {
                    AsyncImage(
                        model = userProfile.avatarUrl,
                        contentDescription = stringResource(R.string.content_desc_profile_avatar),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = username.firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                        fontSize = if (responsive.isCompact) 38.sp else 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceOverlay)
                        .border(1.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.content_desc_change_avatar),
                        tint = White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = username.ifBlank { stringResource(R.string.username) },
                style = iOSTitle1.copy(color = TextPrimary),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.in_game_id_label, inGameId.ifBlank { stringResource(R.string.not_set) }),
                style = iOSCallout.copy(color = TextSecondary),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            if (userProfile != null) {
                Spacer(modifier = Modifier.height(14.dp))
                RankBadge(tier = userProfile.currentTier, size = RankBadgeSize.LARGE)
            }

            if (bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = bio,
                    style = iOSCallout.copy(color = LightGray),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (responsive.isCompact) 0.dp else 16.dp)
                )
            }

            val chips = buildList {
                if (role.isNotBlank()) {
                    add(ProfileHeroChipData(role.uppercase(), GoldPrimary, Icons.Default.Person))
                }
                if (userProfile?.isTournamentHost == true) {
                    add(ProfileHeroChipData(stringResource(R.string.tournament_host_badge), GoldPrimary, Icons.Default.EmojiEvents))
                }
                userProfile?.mainHeroes?.take(3)?.forEach {
                    add(ProfileHeroChipData(it, BluePrimary, Icons.Default.SportsEsports))
                }
            }

            if (chips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { chip ->
                        ProfileHeroChip(chip)
                    }
                }
            }
        }
    }
}

private data class ProfileHeroChipData(
    val text: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun ProfileHeroChip(chip: ProfileHeroChipData) {
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(chip.color.copy(alpha = 0.13f))
            .border(1.dp, chip.color.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = chip.icon,
            contentDescription = null,
            tint = chip.color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = chip.text,
            style = iOSCaption1.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 132.dp)
        )
    }
}

@Composable
private fun ProfileEditCard(
    username: String,
    onUsernameChange: (String) -> Unit,
    inGameId: String,
    onInGameIdChange: (String) -> Unit,
    role: String,
    onRoleChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    mainHeroesInput: String,
    onMainHeroesChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.edit),
            style = iOSTitle3.copy(color = TextPrimary)
        )
        com.mlbb.scrim.ui.components.iOSInput(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = stringResource(R.string.username),
            modifier = Modifier.fillMaxWidth()
        )
        com.mlbb.scrim.ui.components.iOSInput(
            value = inGameId,
            onValueChange = onInGameIdChange,
            placeholder = stringResource(R.string.in_game_id),
            modifier = Modifier.fillMaxWidth()
        )
        com.mlbb.scrim.ui.components.iOSInput(
            value = role,
            onValueChange = onRoleChange,
            placeholder = stringResource(R.string.role_placeholder),
            modifier = Modifier.fillMaxWidth()
        )
        com.mlbb.scrim.ui.components.iOSInput(
            value = bio,
            onValueChange = onBioChange,
            placeholder = stringResource(R.string.bio_placeholder),
            singleLine = false,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp)
        )
        OutlinedTextField(
            value = mainHeroesInput,
            onValueChange = onMainHeroesChange,
            label = { Text(stringResource(R.string.top_3_main_heroes_hint)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = GlassBorder,
                focusedLabelColor = GoldPrimary,
                unfocusedLabelColor = TextSecondary,
                cursorColor = GoldPrimary,
                focusedTextColor = White,
                unfocusedTextColor = White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
private fun ProfileInfoPanel(items: List<ProfileInfoItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
    ) {
        items.forEachIndexed { index, item ->
            ProfileInfoRow(item)
            if (index != items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = GlassBorder
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(item: ProfileInfoItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(item.color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .border(1.dp, item.color.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, item.label, tint = item.color, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.label,
                style = iOSCaption1.copy(color = TextSecondary, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                item.value,
                style = iOSHeadline.copy(color = TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileStatGrid(
    stats: List<ProfileStatItem>,
    responsive: ResponsiveMetrics
) {
    val columns = responsive.profileStatColumns.coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
        stats.chunked(columns).forEach { rowStats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(responsive.cardSpacing)
            ) {
                rowStats.forEach { stat ->
                    ProfileStatBox(
                        label = stat.label,
                        value = stat.value,
                        color = stat.color,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowStats.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ProfileInfoCard(
    icon : androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(BluePrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, BluePrimary.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, label, tint = BluePrimary, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = iOSCaption1.copy(color = TextSecondary, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    value,
                    style = iOSHeadline.copy(color = TextPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountActionCard(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    title   : String,
    subtitle: String,
    color   : Color,
    onClick : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        SurfaceCard,
                        SurfaceCard.copy(alpha = 0.92f),
                        color.copy(alpha = 0.06f)
                    )
                )
            )
            .border(1.dp, color.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, title, tint = color, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = iOSHeadline.copy(color = TextPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = iOSCaption1.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint     = DimGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ChangeEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val errorEnterNewEmail = stringResource(R.string.error_enter_new_email)
    val errorEnterValidEmail = stringResource(R.string.error_enter_valid_email)
    val errorEnterCurrentPassword = stringResource(R.string.error_enter_current_password)
    var newEmail by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavy,
        title = {
            Text(
                text = stringResource(R.string.change_email),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.current_email, currentEmail),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = LightGray
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = {
                        newEmail = it
                        errorMessage = ""
                    },
                    label = { Text(stringResource(R.string.new_email)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = White.copy(alpha = 0.3f),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = LightGray,
                        cursorColor = GoldPrimary,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMessage = ""
                    },
                    label = { Text(stringResource(R.string.current_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = White.copy(alpha = 0.3f),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = LightGray,
                        cursorColor = GoldPrimary,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            GradientButton(
                text = stringResource(R.string.update_email),
                onClick = {
                    when {
                        newEmail.isBlank() -> errorMessage = errorEnterNewEmail
                        !newEmail.contains("@") -> errorMessage = errorEnterValidEmail
                        currentPassword.isBlank() -> errorMessage = errorEnterCurrentPassword
                        else -> onConfirm(newEmail, currentPassword)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                gradient = BlueGradient,
                height = 48.dp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn), color = LightGray)
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val errorPasswordMinLength = stringResource(R.string.error_password_min_length)
    val errorPasswordsNotMatch = stringResource(R.string.error_passwords_not_match)
    val errorPasswordMustDiffer = stringResource(R.string.error_password_must_differ)
    val errorEnterCurrentPassword = stringResource(R.string.error_enter_current_password)
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavy,
        title = {
            Text(
                text = stringResource(R.string.change_password),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMessage = ""
                    },
                    label = { Text(stringResource(R.string.current_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = White.copy(alpha = 0.3f),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = LightGray,
                        cursorColor = GoldPrimary,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = ""
                    },
                    label = { Text(stringResource(R.string.new_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = White.copy(alpha = 0.3f),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = LightGray,
                        cursorColor = GoldPrimary,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = ""
                    },
                    label = { Text(stringResource(R.string.confirm_new_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = White.copy(alpha = 0.3f),
                        focusedLabelColor = GoldPrimary,
                        unfocusedLabelColor = LightGray,
                        cursorColor = GoldPrimary,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            GradientButton(
                text = stringResource(R.string.update_password),
                onClick = {
                    when {
                        currentPassword.isBlank() -> errorMessage = errorEnterCurrentPassword
                        newPassword.length < 6 -> errorMessage = errorPasswordMinLength
                        newPassword != confirmPassword -> errorMessage = errorPasswordsNotMatch
                        currentPassword == newPassword -> errorMessage = errorPasswordMustDiffer
                        else -> onConfirm(currentPassword, newPassword, confirmPassword)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                gradient = GoldGradient,
                height = 48.dp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn), color = LightGray)
            }
        }
    )
}

@Composable
fun ProfileStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                spotColor = color.copy(alpha = 0.16f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, color.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 86.dp)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color.copy(alpha = 0.75f))
            )
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = value,
                style = StatsTextStyle.copy(color = color),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = iOSCaption1.copy(color = MidGray),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
