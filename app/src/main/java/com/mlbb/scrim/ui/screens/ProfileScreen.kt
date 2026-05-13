package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.GradientButton
import com.mlbb.scrim.ui.components.RankBadge
import com.mlbb.scrim.ui.components.RankBadgeSize
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource

@Composable
fun ProfileScreen(
    userProfile: com.mlbb.scrim.data.model.UserProfile?,
    onNavigateBack: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onUpdateEmail: (String, String) -> Unit = { _, _ -> },
    onUpdatePassword: (String, String, String) -> Unit = { _, _, _ -> },
    authResult: com.mlbb.scrim.data.model.AuthResult? = null,
    onResetAuthState: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    unlockedAchievements: List<com.mlbb.scrim.data.model.Achievement> = emptyList()
) {
    var isEditing by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf(userProfile?.username ?: "") }
    var inGameId by remember { mutableStateOf(userProfile?.inGameId ?: "") }

    // Account security dialog states
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // Success snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authResult) {
        when (authResult) {
            is com.mlbb.scrim.data.model.AuthResult.Success -> {
                snackbarHostState.showSnackbar("Changes saved successfully!")
                onResetAuthState()
            }
            is com.mlbb.scrim.data.model.AuthResult.Error -> {
                snackbarHostState.showSnackbar(authResult.message)
                onResetAuthState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)

                    Text(
                        text = stringResource(R.string.my_profile),
                        style = iOSTitle2.copy(color = White)
                    )

                    IconButton(
                        onClick = {
                            if (isEditing) {
                                onUpdateProfile(username, inGameId)
                            }
                            isEditing = !isEditing
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (isEditing) BluePrimary else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit",
                            tint = White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Avatar with gold glow for premium feel
                AnimatedEntrance(delayMillis = 100) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .align(Alignment.CenterHorizontally)
                            .shadow(
                                elevation = 16.dp,
                                spotColor = GoldPrimary.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(BluePrimary, Color(0xFF0A5A9F))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = username.firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tier Badge
                AnimatedEntrance(delayMillis = 125) {
                    if (userProfile != null && !isEditing) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            RankBadge(tier = userProfile.currentTier, size = RankBadgeSize.LARGE)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Username
                AnimatedEntrance(delayMillis = 150) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = White.copy(alpha = 0.3f),
                                focusedLabelColor = GoldPrimary,
                                unfocusedLabelColor = White.copy(alpha = 0.7f),
                                cursorColor = GoldPrimary,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // In-Game ID
                AnimatedEntrance(delayMillis = 200) {
                    if (isEditing) {
                        OutlinedTextField(
                            value = inGameId,
                            onValueChange = { inGameId = it },
                            label = { Text("In-Game ID") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = White.copy(alpha = 0.3f),
                                focusedLabelColor = GoldPrimary,
                                unfocusedLabelColor = White.copy(alpha = 0.7f),
                                cursorColor = GoldPrimary,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.in_game_id_label, inGameId),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                color = LightGray
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Profile Info Cards
                AnimatedEntrance(delayMillis = 250) {
                    ProfileInfoCard(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = userProfile?.email ?: "Not set"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedEntrance(delayMillis = 300) {
                    ProfileInfoCard(
                        icon = Icons.Default.CalendarToday,
                        label = "Member Since",
                        value = userProfile?.createdAt?.let {
                            java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(it)
                        } ?: "Not set"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedEntrance(delayMillis = 350) {
                    ProfileInfoCard(
                        icon = Icons.Default.SportsEsports,
                        label = "In-Game ID",
                        value = userProfile?.inGameId ?: "Not set"
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatBox(
                            label = "Matches",
                            value = (userProfile?.totalMatches ?: 0).toString(),
                            color = GoldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatBox(
                            label = "Wins",
                            value = (userProfile?.wins ?: 0).toString(),
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatBox(
                            label = "Losses",
                            value = (userProfile?.losses ?: 0).toString(),
                            color = ErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedEntrance(delayMillis = 385) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatBox(
                            label = "Win Rate",
                            value = userProfile?.winRate ?: "0%",
                            color = BluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatBox(
                            label = "XP",
                            value = (userProfile?.xp ?: 0).toString(),
                            color = Purple,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatBox(
                            label = "PTS",
                            value = userProfile?.ptsDisplay ?: "0",
                            color = if ((userProfile?.pts ?: 0) >= 0) SuccessGreen else ErrorRed,
                            modifier = Modifier.weight(1f)
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
                        title = "Change Email",
                        subtitle = userProfile?.email ?: "Not set",
                        color = BluePrimary,
                        onClick = { showEmailDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Change Password Card
                AnimatedEntrance(delayMillis = 500) {
                    AccountActionCard(
                        icon = Icons.Default.Lock,
                        title = "Change Password",
                        subtitle = "Update your account password",
                        color = GoldPrimary,
                        onClick = { showPasswordDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
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
}

@Composable
fun ProfileInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = BluePrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = BluePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = LightGray,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        color = White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = LightGray
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = LightGray.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
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
                    label = { Text("New Email") },
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
                    label = { Text("Current Password") },
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
                        newEmail.isBlank() -> errorMessage = "Please enter a new email"
                        !newEmail.contains("@") -> errorMessage = "Please enter a valid email"
                        currentPassword.isBlank() -> errorMessage = "Please enter your current password"
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
                Text("Cancel", color = LightGray)
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
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
                    label = { Text("Current Password") },
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
                    label = { Text("New Password") },
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
                    label = { Text("Confirm New Password") },
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
                        currentPassword.isBlank() -> errorMessage = "Please enter your current password"
                        newPassword.length < 6 -> errorMessage = "New password must be at least 6 characters"
                        newPassword != confirmPassword -> errorMessage = "New passwords do not match"
                        currentPassword == newPassword -> errorMessage = "New password must be different"
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
                Text("Cancel", color = LightGray)
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
    Card(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                spotColor = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = StatsTextStyle.copy(color = color)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = iOSCaption1.copy(color = MidGray)
            )
        }
    }
}
