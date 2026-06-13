package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
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
import com.scrimslegends.app.data.model.TournamentHostRequest
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.*
import com.scrimslegends.app.R
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }

            // ── Content ──
            val request = existingRequest
            if (request != null && (isPending || isApproved || isRejected)) {
                // Show existing request status
                ExistingRequestStatus(
                    request = request,
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
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
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "Become a Tournament Host",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                                Text(
                                    text = "Host Swiss-style tournaments, manage teams, and run live matches.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }

                    // Motivation
                    Column {
                        Text(
                            text = stringResource(R.string.tournament_host_motivation),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = motivation,
                            onValueChange = { motivation = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("I want to host because…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.secondary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Experience
                    Column {
                        Text(
                            text = stringResource(R.string.tournament_host_experience),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = experience,
                            onValueChange = { experience = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Previous experience hosting…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.secondary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Telegram
                    Column {
                        Text(
                            text = stringResource(R.string.tournament_host_telegram),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = telegramChannel,
                            onValueChange = { telegramChannel = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("@channel", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Chat, null, tint = MaterialTheme.colorScheme.primary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.secondary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Social links
                    Column {
                        Text(
                            text = stringResource(R.string.tournament_host_social),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "One link per line",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = socialLinksText,
                            onValueChange = { socialLinksText = it },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            placeholder = { Text("https://…\nhttps://…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.secondary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.background
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
                action = { TextButton(onClick = onDismissError) { Text("OK", color = MaterialTheme.colorScheme.onSurface) } }
            ) { Text(it, color = MaterialTheme.colorScheme.onSurface) }
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
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}
