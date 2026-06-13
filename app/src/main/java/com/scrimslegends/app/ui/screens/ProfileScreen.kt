package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import com.scrimslegends.app.ui.components.LottieLoadingIndicator
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.GradientButton
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.components.RankBadge
import com.scrimslegends.app.ui.components.RankBadgeSize
import com.scrimslegends.app.ui.components.ResponsiveMetrics
import com.scrimslegends.app.ui.components.rememberResponsiveMetrics
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource

@Composable
fun ProfileScreen(
    userProfile: com.scrimslegends.app.data.model.UserProfile?,
    onNavigateBack: () -> Unit = {},
    isTab: Boolean = false,
    onUpdateProfile: (String, String, String?, String?, List<String>?) -> Unit,
    onUpdateEmail: (String, String) -> Unit = { _, _ -> },
    onUpdatePassword: (String, String, String) -> Unit = { _, _, _ -> },
    authResult: com.scrimslegends.app.data.model.AuthResult? = null,
    onResetAuthState: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onLogout: () -> Unit = {},
    onUploadAvatar: (android.net.Uri) -> Unit = {},
    unlockedAchievements: List<com.scrimslegends.app.data.model.Achievement> = emptyList(),
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
            is com.scrimslegends.app.data.model.AuthResult.Success -> {
                snackbarHostState.showSnackbar(changesSavedMessage)
                onResetAuthState()
            }
            is com.scrimslegends.app.data.model.AuthResult.Error -> {
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
                            style = iOSTitle2.copy(color = MaterialTheme.colorScheme.onBackground),
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
                                .background(if (isEditing) MaterialTheme.colorScheme.primary else appElevatedSurfaceColor())
                                .border(1.dp, if (isEditing) Color.Transparent else appBorderColor(), RoundedCornerShape(12.dp))
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
                                tint = if (isEditing) White else appTextPrimaryColor(),
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
                    .imePadding()
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
                            onAvatarClick = { imagePicker.launch("image/*") },
                            isUploadingAvatar = authResult is com.scrimslegends.app.data.model.AuthResult.Loading
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Profile Info Cards
                AnimatedEntrance(delayMillis = 250) {
                    ProfileInfoPanel(
                        items = listOf(
                            ProfileInfoItem(
                                icon = Icons.Default.Email,
                                label = stringResource(R.string.email),
                                value = userProfile?.email ?: stringResource(R.string.not_set),
                                color = MaterialTheme.colorScheme.primary
                            ),
                            ProfileInfoItem(
                                icon = Icons.Default.CalendarToday,
                                label = stringResource(R.string.member_since),
                                value = userProfile?.createdAt?.let {
                                    java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(it)
                                } ?: stringResource(R.string.not_set),
                                color = MaterialTheme.colorScheme.secondary
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

                Spacer(modifier = Modifier.height(40.dp))

                // Stats Section
                AnimatedEntrance(delayMillis = 375) {
                    Text(
                        text = stringResource(R.string.player_stats),
                        style = iOSTitle3.copy(color = MaterialTheme.colorScheme.onBackground)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedEntrance(delayMillis = 380) {
                    ProfileStatGrid(
                        stats = listOf(
                            ProfileStatItem(
                                label = stringResource(R.string.matches),
                                value = (userProfile?.totalMatches ?: 0).toString(),
                                color = MaterialTheme.colorScheme.secondary
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
                                color = MaterialTheme.colorScheme.primary
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
                            style = iOSTitle3.copy(color = MaterialTheme.colorScheme.onBackground)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedEntrance(delayMillis = 387) {
                        ProfileStatGrid(
                            stats = listOf(
                                ProfileStatItem(
                                    label = stringResource(R.string.hosted),
                                    value = userProfile.tournamentsHosted.toString(),
                                    color = MaterialTheme.colorScheme.secondary
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

                Spacer(modifier = Modifier.height(40.dp))

                // Achievements Section
                AnimatedEntrance(delayMillis = 388) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.achievements),
                            style = iOSTitle3.copy(color = MaterialTheme.colorScheme.onBackground)
                        )
                        TextButton(onClick = onNavigateToAchievements) {
                            Text(
                                text = stringResource(R.string.view_all),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedEntrance(delayMillis = 389) {
                    if (unlockedAchievements.isNotEmpty()) {
                        com.scrimslegends.app.ui.components.AchievementBadgeRow(
                            achievements = unlockedAchievements,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.no_achievements_yet),
                            style = iOSCaption1.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f).copy(alpha = 0.6f)),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // App Settings Section
                AnimatedEntrance(delayMillis = 390) {
                    Text(
                        text = stringResource(R.string.app_settings),
                        style = iOSTitle3.copy(color = MaterialTheme.colorScheme.onBackground)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedEntrance(delayMillis = 395) {
                    AccountActionCard(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.settings),
                        subtitle = stringResource(R.string.settings_sub),
                        color = MaterialTheme.colorScheme.primary,
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
                                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = appSurfaceColor()),
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
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Dashboard, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.host_credentials),
                                            style = iOSHeadline.copy(color = appTextPrimaryColor())
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            stringResource(R.string.host_credentials_sub),
                                            style = iOSCaption1.copy(color = appTextSecondaryColor()),
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
                                            style = iOSCaption1.copy(color = appTextSecondaryColor())
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = userProfile.email,
                                            style = iOSBody.copy(color = appTextPrimaryColor(), fontWeight = FontWeight.SemiBold)
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
                                            tint = if (emailCopied) SuccessGreen else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            if (emailCopied) stringResource(R.string.email_copied) else stringResource(R.string.copy_email),
                                            color = if (emailCopied) SuccessGreen else MaterialTheme.colorScheme.secondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // Password hint
                                Text(
                                    stringResource(R.string.host_password_hint),
                                    style = iOSCaption1.copy(color = appTextSecondaryColor().copy(alpha = 0.8f))
                                )

                                Spacer(Modifier.height(12.dp))

                                // Open Dashboard button
                                GradientButton(
                                    text = stringResource(R.string.open_dashboard),
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://admin.scrimslegends.app/host/login"))
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
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = stringResource(R.string.support),
                        subtitle = stringResource(R.string.support_sub),
                        color = SuccessGreen,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://form.jotform.com/shukhratmamatkulov1999/support-request-form"))
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Account Security Section
                AnimatedEntrance(delayMillis = 400) {
                    Text(
                        text = stringResource(R.string.account_security),
                        style = iOSTitle3.copy(color = MaterialTheme.colorScheme.onBackground)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Change Email Card
                AnimatedEntrance(delayMillis = 450) {
                    AccountActionCard(
                        icon = Icons.Default.Email,
                        title = stringResource(R.string.change_email),
                        subtitle = userProfile?.email ?: stringResource(R.string.not_set),
                        color = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { showPasswordDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

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
            containerColor = appSurfaceColor(),
            title = {
                Text(
                    text = stringResource(R.string.sign_out),
                    style = iOSTitle3.copy(color = appTextPrimaryColor(), fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.sign_out_confirm_message),
                    style = iOSBody.copy(color = appTextSecondaryColor())
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
                    Text(stringResource(R.string.cancel_btn), color = appTextSecondaryColor())
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, appBorderColor(), RoundedCornerShape(20.dp))
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
    userProfile: com.scrimslegends.app.data.model.UserProfile?,
    username: String,
    inGameId: String,
    bio: String,
    role: String,
    responsive: ResponsiveMetrics,
    onAvatarClick: () -> Unit,
    isUploadingAvatar: Boolean = false
) {
    val cardColor = appSurfaceColor()
    val elevatedColor = appElevatedSurfaceColor()
    val borderColor = appBorderColor()
    val primaryText = appTextPrimaryColor()
    val secondaryText = appTextSecondaryColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(appSurfaceColor())
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(responsive.profileAvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f), CircleShape)
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
                        style = if (responsive.isCompact) iOSTitle1 else iOSTitle1.copy(fontSize = 46.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(elevatedColor)
                        .border(1.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.content_desc_change_avatar),
                        tint = primaryText,
                        modifier = Modifier.size(17.dp)
                    )
                }

                if (isUploadingAvatar) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieLoadingIndicator(size = 30.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = username.ifBlank { stringResource(R.string.username) },
                style = iOSTitle1.copy(color = primaryText),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.in_game_id_label, inGameId.ifBlank { stringResource(R.string.not_set) }),
                style = iOSCallout.copy(color = secondaryText),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            if (userProfile?.shortId?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                val clipboardManager = LocalClipboardManager.current
                var idCopied by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        clipboardManager.setText(AnnotatedString(userProfile.shortId))
                        idCopied = true 
                    }.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: ${userProfile.shortId}",
                        style = iOSCallout.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (idCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy ID",
                        tint = if (idCopied) SuccessGreen else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (userProfile != null) {
                Spacer(modifier = Modifier.height(14.dp))
                RankBadge(tier = userProfile.currentTier, size = RankBadgeSize.LARGE)
            }

            if (bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = bio,
                    style = iOSCallout.copy(color = secondaryText),
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
                    add(ProfileHeroChipData(role.uppercase(), MaterialTheme.colorScheme.secondary, Icons.Default.Person))
                }
                if (userProfile?.isTournamentHost == true) {
                    add(ProfileHeroChipData(stringResource(R.string.tournament_host_badge), MaterialTheme.colorScheme.secondary, Icons.Default.EmojiEvents))
                }
                userProfile?.mainHeroes?.take(3)?.forEach {
                    add(ProfileHeroChipData(it, MaterialTheme.colorScheme.primary, Icons.Default.SportsEsports))
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
                color = appTextPrimaryColor(),
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
    val cardColor = appSurfaceColor()
    val borderColor = appBorderColor()
    val primaryText = appTextPrimaryColor()
    val secondaryText = appTextSecondaryColor()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.edit),
            style = iOSTitle3.copy(color = primaryText)
        )
        com.scrimslegends.app.ui.components.iOSInput(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = stringResource(R.string.username),
            modifier = Modifier.fillMaxWidth()
        )
        com.scrimslegends.app.ui.components.iOSInput(
            value = inGameId,
            onValueChange = onInGameIdChange,
            placeholder = stringResource(R.string.in_game_id),
            modifier = Modifier.fillMaxWidth()
        )
        com.scrimslegends.app.ui.components.iOSInput(
            value = role,
            onValueChange = onRoleChange,
            placeholder = stringResource(R.string.role_placeholder),
            modifier = Modifier.fillMaxWidth()
        )
        com.scrimslegends.app.ui.components.iOSInput(
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
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = borderColor,
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                unfocusedLabelColor = secondaryText,
                cursorColor = MaterialTheme.colorScheme.secondary,
                focusedTextColor = primaryText,
                unfocusedTextColor = primaryText
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
private fun ProfileInfoPanel(items: List<ProfileInfoItem>) {
    val cardColor = appSurfaceColor()
    val borderColor = appBorderColor()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        items.forEachIndexed { index, item ->
            ProfileInfoRow(item)
            if (index != items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = borderColor
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(item: ProfileInfoItem) {
    val primaryText = appTextPrimaryColor()
    val secondaryText = appTextSecondaryColor()

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
                style = iOSCaption1.copy(color = secondaryText, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                item.value,
                style = iOSHeadline.copy(color = primaryText),
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
    val cardColor = appSurfaceColor()
    val borderColor = appBorderColor()
    val primaryText = appTextPrimaryColor()
    val secondaryText = appTextSecondaryColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = iOSCaption1.copy(color = secondaryText, fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    value,
                    style = iOSHeadline.copy(color = primaryText),
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
    val cardColor = appSurfaceColor()
    val primaryText = appTextPrimaryColor()
    val secondaryText = appTextSecondaryColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        cardColor,
                        cardColor.copy(alpha = 0.92f),
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
                    style = iOSHeadline.copy(color = primaryText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = iOSCaption1.copy(color = secondaryText),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint     = secondaryText,
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
    val surfaceColor = appSurfaceColor()
    val primaryText = appTextPrimaryColor()
    val secondaryText = appTextSecondaryColor()
    val borderColor = appBorderColor()
    val errorEnterNewEmail = stringResource(R.string.error_enter_new_email)
    val errorEnterValidEmail = stringResource(R.string.error_enter_valid_email)
    val errorEnterCurrentPassword = stringResource(R.string.error_enter_current_password)
    var newEmail by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        title = {
            Text(
                text = stringResource(R.string.change_email),
                style = iOSTitle3.copy(color = primaryText)
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.current_email, currentEmail),
                    style = iOSCallout.copy(color = secondaryText),
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
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = secondaryText,
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
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
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = secondaryText,
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText
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
                Text(stringResource(R.string.cancel_btn), color = secondaryText)
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, borderColor, RoundedCornerShape(20.dp))
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val surfaceColor = appSurfaceColor()
    val primaryText = appTextPrimaryColor()
    val secondaryText = appTextSecondaryColor()
    val borderColor = appBorderColor()
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
        containerColor = surfaceColor,
        title = {
            Text(
                text = stringResource(R.string.change_password),
                style = iOSTitle3.copy(color = primaryText)
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMessage = ""
                    },
                    label = { Text(stringResource(R.string.current_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = secondaryText,
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText
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
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = secondaryText,
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText
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
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = secondaryText,
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText
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
                gradient = PremiumBlueGradient,
                height = 48.dp
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn), color = secondaryText)
            }
        },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, borderColor, RoundedCornerShape(20.dp))
    )
}

@Composable
fun ProfileStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val cardColor = appSurfaceColor()
    val secondaryText = appTextSecondaryColor()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
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
                style = iOSCaption1.copy(color = secondaryText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
