package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.PremiumFadeIn

@Composable
fun VerificationScreen(
    email: String,
    initialSecondsUntilDeletion: Long = 3600L,
    onResendEmail: (String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onBackToLogin: () -> Unit,
    isLoading: Boolean = false,
    resentSuccess: Boolean = false,
    accountDeleted: Boolean = false,
    otpError: String? = null
) {
    val safeInitialSeconds = when {
        accountDeleted -> 0L
        initialSecondsUntilDeletion > 0L -> initialSecondsUntilDeletion
        else -> 3600L
    }

    var otpCode by remember { mutableStateOf("") }
    var resendCountdown by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(otpError) { localError = otpError }

    var deletionSeconds by remember(email) { mutableLongStateOf(safeInitialSeconds) }

    LaunchedEffect(resentSuccess) {
        if (resentSuccess) {
            canResend = false
            resendCountdown = 60
        }
    }

    LaunchedEffect(canResend, resendCountdown) {
        if (!canResend && resendCountdown > 0) {
            while (resendCountdown > 0) {
                kotlinx.coroutines.delay(1000)
                if (resendCountdown > 0) resendCountdown--
            }
            if (!canResend) canResend = true
        }
    }

    LaunchedEffect(email, safeInitialSeconds, accountDeleted) {
        deletionSeconds = safeInitialSeconds
        if (accountDeleted) return@LaunchedEffect
        while (deletionSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            if (deletionSeconds > 0) deletionSeconds--
        }
        if (localError.isNullOrBlank()) {
            localError = "Verification time expired."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        // ── Background Glow Orbs ──────────────────────────────────
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-100).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(BluePrimary.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (accountDeleted) {
                ExpiredAccountView(onBackToLogin = onBackToLogin)
            } else {
                Spacer(Modifier.height(60.dp))

                // ── Icon ──────────────────────────────────────────
                PremiumFadeIn(delayMillis = 0) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(SurfaceOverlay)
                            .border(1.dp, GlassBorder, RoundedCornerShape(26.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── Title & Subtitle ───────────────────────────────
                PremiumFadeIn(delayMillis = 100) {
                    Text(
                        stringResource(R.string.verify_email_title),
                        style = iOSTitle1.copy(color = TextPrimary),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(10.dp))
                PremiumFadeIn(delayMillis = 150) {
                    Text(
                        stringResource(R.string.verify_email_subtitle),
                        style = iOSCallout.copy(color = TextSecondary),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ── Masked Email ────────────────────────────────────
                PremiumFadeIn(delayMillis = 200) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceOverlay)
                            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = maskEmail(email),
                            style = iOSCallout.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── Warning Banner ──────────────────────────────────
                PremiumFadeIn(delayMillis = 220) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(ErrorRed.copy(alpha = 0.08f))
                            .border(1.dp, ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.account_auto_delete_warning),
                                    style = iOSFootnote.copy(color = ErrorRed, fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    formatDeletionTimer(deletionSeconds),
                                    style = iOSTitle3.copy(color = WarningOrange, fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── OTP Field ───────────────────────────────────────
                PremiumFadeIn(delayMillis = 240) {
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = {
                            if (it.length <= 8 && it.all { c -> c.isDigit() }) {
                                otpCode = it
                                localError = null
                            }
                        },
                        placeholder = { Text(stringResource(R.string.enter_code), color = TextTertiary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        textStyle = iOSTitle1.copy(
                            color = TextPrimary,
                            letterSpacing = 8.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = SurfaceOverlay,
                            unfocusedContainerColor = SurfaceOverlay,
                            cursorColor = BluePrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = !localError.isNullOrBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = localError ?: "",
                        color = ErrorRed,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                // ── Verify Button ───────────────────────────────────
                PremiumFadeIn(delayMillis = 280) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(BlueGradient))
                            .clickable(enabled = !isLoading) {
                                if (otpCode.length == 8) onVerifyOtp(otpCode)
                                else localError = "Enter the 8-digit code"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.i_have_verified), style = iOSHeadline.copy(color = White))
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Resend & Back ───────────────────────────────────
                PremiumFadeIn(delayMillis = 300) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(
                            onClick = { onResendEmail(email) },
                            enabled = canResend && !isLoading
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = if (canResend) BluePrimary else TextTertiary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (canResend) stringResource(R.string.resend_email)
                                else stringResource(R.string.resend_in_seconds, resendCountdown),
                                style = iOSCallout.copy(color = if (canResend) BluePrimary else TextTertiary, fontWeight = FontWeight.Medium)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(onClick = onBackToLogin) {
                            Text(stringResource(R.string.back_to_login), style = iOSCallout.copy(color = TextSecondary))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiredAccountView(onBackToLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(ErrorRed.copy(alpha = 0.1f))
                .border(1.dp, ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Warning, null, tint = ErrorRed, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(32.dp))
        Text(stringResource(R.string.account_deleted_title), style = iOSTitle1.copy(color = TextPrimary), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.account_deleted_subtitle),
            style = iOSCallout.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(BlueGradient))
                .clickable { onBackToLogin() },
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.back_to_login), style = iOSHeadline.copy(color = White))
        }
    }
}

private fun formatDeletionTimer(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

private fun maskEmail(email: String): String {
    if (!email.contains("@")) return email
    val parts = email.split("@")
    if (parts.size != 2) return email
    val local = parts[0]
    val domain = parts[1]
    val maskedLocal = if (local.length <= 2) local else local.first() + "***"
    return "$maskedLocal@$domain"
}
