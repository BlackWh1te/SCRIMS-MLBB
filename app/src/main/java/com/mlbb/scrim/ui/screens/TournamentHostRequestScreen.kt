package com.mlbb.scrim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.TournamentHostRequest
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.*
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentHostRequestScreen(
    existingRequest: TournamentHostRequest?,
    isLoading: Boolean,
    error: String? = null,
    onSubmit: (String, String?, String?, List<String>) -> Unit = { _, _, _, _ -> },
    onNavigateBack: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    var motivation by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var telegramChannel by remember { mutableStateOf("") }
    var socialLinksText by remember { mutableStateOf("") }

    // Pre-fill if existing request
    LaunchedEffect(existingRequest) {
        existingRequest?.let {
            motivation = it.motivation
            experience = it.experience ?: ""
            telegramChannel = it.telegramChannel ?: ""
            socialLinksText = it.socialLinks.joinToString("\n")
        }
    }

    val isPending = existingRequest?.status == "pending"
    val isApproved = existingRequest?.status == "approved"
    val isRejected = existingRequest?.status == "rejected"
    val hasExisting = existingRequest != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkNavy.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.tournament_host_request),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }

            // ── Content ──
            if (hasExisting && (isPending || isApproved || isRejected)) {
                // Show existing request status
                ExistingRequestStatus(
                    request = existingRequest!!,
                    isApproved = isApproved,
                    isPending = isPending,
                    isRejected = isRejected
                )
            } else {
                // Show form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Info card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "Become a Tournament Host",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary
                                    )
                                )
                                Text(
                                    text = "Host Swiss-style tournaments, manage teams, and run live matches.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    }

                    // Motivation
                    Column {
                        Text(
                            text = stringResource(R.string.tournament_host_motivation),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = motivation,
                            onValueChange = { motivation = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("I want to host because…", color = TextTertiary) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Separator,
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                cursorColor = GoldPrimary,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                    }

                    // Experience
                    Column {
                        Text(
                            text = stringResource(R.string.tournament_host_experience),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = experience,
                            onValueChange = { experience = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Previous experience hosting…", color = TextTertiary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Separator,
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                cursorColor = GoldPrimary,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                    }

                    // Telegram
                    Column {
                        Text(
                            text = stringResource(R.string.tournament_host_telegram),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = telegramChannel,
                            onValueChange = { telegramChannel = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("@channel", color = TextTertiary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Chat, null, tint = BluePrimary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Separator,
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                cursorColor = GoldPrimary,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                    }

                    // Social links
                    Column {
                        Text(
                            text = stringResource(R.string.tournament_host_social),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "One link per line",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = socialLinksText,
                            onValueChange = { socialLinksText = it },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            placeholder = { Text("https://…\nhttps://…", color = TextTertiary) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = Separator,
                                focusedContainerColor = SurfaceElevated,
                                unfocusedContainerColor = SurfaceElevated,
                                cursorColor = GoldPrimary,
                                focusedTextColor = White,
                                unfocusedTextColor = White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Submit button
                    iOSPrimaryButton(
                        text = stringResource(R.string.tournament_host_submit),
                        onClick = {
                            val links = socialLinksText.lines().filter { it.isNotBlank() }
                            onSubmit(motivation, experience.ifBlank { null }, telegramChannel.ifBlank { null }, links)
                        },
                        enabled = motivation.isNotBlank() && !isLoading,
                        isLoading = isLoading,
                        backgroundColor = GoldPrimary,
                        contentColor = DarkNavy
                    )
                }
            }
        }

        // Error
        error?.let {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = ErrorRed,
                action = { TextButton(onClick = onDismissError) { Text("OK", color = White) } }
            ) { Text(it, color = White) }
        }
    }
}

@Composable
private fun ExistingRequestStatus(
    request: TournamentHostRequest,
    isApproved: Boolean,
    isPending: Boolean,
    isRejected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val statusColor = when {
            isApproved -> SuccessGreen
            isPending -> WarningOrange
            isRejected -> ErrorRed
            else -> TextTertiary
        }

        val statusIcon = when {
            isApproved -> Icons.Default.CheckCircle
            isPending -> Icons.Default.Schedule
            isRejected -> Icons.Default.Cancel
            else -> Icons.Default.Info
        }

        val statusText = when {
            isApproved -> stringResource(R.string.tournament_host_approved)
            isPending -> stringResource(R.string.tournament_host_pending)
            isRejected -> stringResource(R.string.tournament_host_rejected)
            else -> "Unknown"
        }

        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = statusColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        )

        if (isPending) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your request is being reviewed by the admin team. You'll be notified when a decision is made.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        if (isRejected && request.adminNotes != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Admin Notes",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = request.adminNotes,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }
            }
        }
    }
}
