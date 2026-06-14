package com.scrimslegends.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.UserProfile
import com.scrimslegends.app.data.service.BanAppealDto
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.data.service.SupabaseService
import com.scrimslegends.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BannedScreen(
    profile: UserProfile,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var appealMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var appealStatus by remember { mutableStateOf<BanAppealDto?>(null) }
    var isLoadingAppeal by remember { mutableStateOf(true) }
    var showAppealDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Fetch existing appeal status
    LaunchedEffect(profile.id) {
        try {
            val response = SupabaseService.api.getUserAppealStatus(
                mapOf("p_user_id" to profile.id)
            )
            if (response.isSuccessful) {
                appealStatus = response.body()?.firstOrNull()
            }
        } catch (_: Exception) {
            // Ignore — may not have an appeal yet
        } finally {
            isLoadingAppeal = false
        }
    }

    val appeal = appealStatus
    val hasPendingAppeal = appeal != null &&
        (appeal.status == "pending" || appeal.status == "under_review")
    val hasResolvedAppeal = appeal != null &&
        (appeal.status == "approved" || appeal.status == "rejected")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)
                )
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        ErrorRed.copy(alpha = 0.15f),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Banned",
                    tint = ErrorRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = stringResource(R.string.banned_title),
                color = ErrorRed,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.banned_subtitle),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Ban reason card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        ErrorRed.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.banned_reason_label),
                    color = ErrorRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.banReason ?: stringResource(R.string.banned_no_reason),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 16.sp
                )

                if (profile.bannedAt != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.banned_on_date, formatBanDate(profile.bannedAt)),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appeal section
            if (isLoadingAppeal) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (hasPendingAppeal) {
                // Show pending appeal status
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            WarningOrange.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.banned_appeal_pending, appealStatus?.status?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: ""),
                        color = WarningOrange,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.banned_appeal_pending_desc),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (hasResolvedAppeal && appealStatus?.status == "rejected") {
                // Show rejected appeal
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            ErrorRed.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.banned_appeal_rejected),
                        color = ErrorRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    appealStatus?.adminNotes?.let { notes ->
                        Text(
                            text = stringResource(R.string.banned_admin_notes, notes),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // Show appeal form
                OutlinedTextField(
                    value = appealMessage,
                    onValueChange = {
                        appealMessage = it
                        errorMessage = ""
                    },
                    label = { Text(stringResource(R.string.banned_appeal_label)) },
                    placeholder = { Text(stringResource(R.string.banned_appeal_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage.isNotEmpty()
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val minLengthError = stringResource(R.string.banned_appeal_min_length)
                Button(
                    onClick = {
                        if (appealMessage.trim().length < 20) {
                            errorMessage = minLengthError
                            return@Button
                        }
                        showAppealDialog = true
                    },
                    enabled = appealMessage.trim().length >= 20 && !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.banned_submit_appeal), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout button
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            ) {
                Text(stringResource(R.string.banned_sign_out))
            }
        }
    }

    // Confirmation dialog
    if (showAppealDialog) {
        AlertDialog(
            onDismissRequest = { showAppealDialog = false },
            title = { Text(stringResource(R.string.banned_appeal_confirm_title)) },
            text = {
                Text(stringResource(R.string.banned_appeal_confirm_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                isSubmitting = true
                                showAppealDialog = false
                                val response = SupabaseService.api.submitBanAppeal(
                                    mapOf(
                                        "p_user_id" to profile.id,
                                        "p_appeal_message" to appealMessage.trim()
                                    )
                                )
                                if (response.isSuccessful) {
                                    showSuccessDialog = true
                                    // Refresh appeal status
                                    val statusResponse = SupabaseService.api.getUserAppealStatus(
                                        mapOf("p_user_id" to profile.id)
                                    )
                                    if (statusResponse.isSuccessful) {
                                        appealStatus = statusResponse.body()?.firstOrNull()
                                    }
                                    appealMessage = ""
                                } else {
                                    errorMessage = response.errorBody()?.string()
                                        ?.substringAfter("\"message\":\"")
                                        ?.substringBefore("\"")
                                        ?: "Failed to submit appeal"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Network error: ${e.message}"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.background)
                ) {
                    Text(stringResource(R.string.accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppealDialog = false }) {
                    Text(stringResource(R.string.decline))
                }
            }
        )
    }

    // Success dialog
    AnimatedVisibility(
        visible = showSuccessDialog,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(R.string.banned_appeal_success_title)) },
            text = {
                Text(stringResource(R.string.banned_appeal_success_message))
            },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text(stringResource(R.string.ready), color = MaterialTheme.colorScheme.secondary)
                }
            }
        )
    }
}

private fun formatBanDate(isoDate: String): String {
    return try {
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .parse(isoDate.substring(0, 10))
        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
            .format(date ?: return isoDate)
    } catch (_: Exception) {
        isoDate
    }
}
