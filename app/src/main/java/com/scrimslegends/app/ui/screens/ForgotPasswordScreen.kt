package com.scrimslegends.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.R
import com.scrimslegends.app.data.model.AuthResult
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.PremiumFadeIn
import com.scrimslegends.app.ui.components.iOSInput
import com.scrimslegends.app.ui.theme.BlueGradient
import com.scrimslegends.app.ui.theme.BluePrimary
import com.scrimslegends.app.ui.theme.DarkNavy
import com.scrimslegends.app.ui.theme.DimGray
import com.scrimslegends.app.ui.theme.ErrorRed
import com.scrimslegends.app.ui.theme.GlassBorder
import com.scrimslegends.app.ui.theme.GoldGradient
import com.scrimslegends.app.ui.theme.GoldPrimary
import com.scrimslegends.app.ui.theme.Separator
import com.scrimslegends.app.ui.theme.SuccessGreen
import com.scrimslegends.app.ui.theme.SurfaceCard
import com.scrimslegends.app.ui.theme.TextPrimary
import com.scrimslegends.app.ui.theme.TextSecondary
import com.scrimslegends.app.ui.theme.White
import com.scrimslegends.app.ui.theme.iOSBlue
import com.scrimslegends.app.ui.theme.iOSBody
import com.scrimslegends.app.ui.theme.iOSCallout
import com.scrimslegends.app.ui.theme.iOSHeadline
import com.scrimslegends.app.ui.theme.iOSInputShape
import com.scrimslegends.app.ui.theme.iOSTitle1
import com.scrimslegends.app.ui.theme.iOSTitle2

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    authResult: AuthResult = AuthResult.Idle,
    onSendResetOtp: (String) -> Unit = {},
    onConfirmResetPassword: (String, String, String) -> Unit = { _, _, _ -> },
    onResetAuthState: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(ResetPasswordStep.Email) }
    var errorMessage by remember { mutableStateOf("") }

    val invalidEmail = stringResource(R.string.invalid_email)
    val passwordMinLength = stringResource(R.string.password_min_length)
    val passwordsNotMatch = stringResource(R.string.passwords_not_match)
    val isLoading = authResult is AuthResult.Loading

    LaunchedEffect(authResult) {
        when (authResult) {
            is AuthResult.EmailNotVerified -> {
                email = authResult.email
                step = ResetPasswordStep.Verify
                errorMessage = ""
                onResetAuthState()
            }
            is AuthResult.Success -> {
                step = ResetPasswordStep.Done
                errorMessage = ""
                onResetAuthState()
            }
            is AuthResult.Error -> {
                errorMessage = authResult.message
                onResetAuthState()
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldPrimary.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(BluePrimary.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            PremiumFadeIn(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)
                    Text(
                        text = stringResource(R.string.reset_password),
                        style = iOSTitle2.copy(color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                PremiumFadeIn(delayMillis = 100) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Brush.verticalGradient(BlueGradient))
                            .border(1.dp, GlassBorder, RoundedCornerShape(26.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                PremiumFadeIn(delayMillis = 150) {
                    Text(
                        text = stringResource(R.string.forgot_password),
                        style = iOSTitle1.copy(color = TextPrimary, textAlign = TextAlign.Center)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                PremiumFadeIn(delayMillis = 200) {
                    Text(
                        text = stringResource(R.string.forgot_password_otp_hint),
                        style = iOSCallout.copy(color = TextSecondary, textAlign = TextAlign.Center),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                if (step == ResetPasswordStep.Done) {
                    ResetPasswordDoneCard(
                        email = email,
                        onNavigateBack = onNavigateBack
                    )
                } else {
                    ResetPasswordFormCard(
                        email = email,
                        onEmailChange = {
                            email = it
                            errorMessage = ""
                        },
                        otpCode = otpCode,
                        onOtpChange = {
                            otpCode = it.filter { char -> char.isDigit() }.take(8)
                            errorMessage = ""
                        },
                        newPassword = newPassword,
                        onNewPasswordChange = {
                            newPassword = it
                            errorMessage = ""
                        },
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = {
                            confirmPassword = it
                            errorMessage = ""
                        },
                        step = step,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onSubmit = {
                            if (isLoading) return@ResetPasswordFormCard
                            when (step) {
                                ResetPasswordStep.Email -> {
                                    if (email.contains("@")) {
                                        onSendResetOtp(email.trim())
                                    } else {
                                        errorMessage = invalidEmail
                                    }
                                }
                                ResetPasswordStep.Verify -> {
                                    when {
                                        otpCode.length != 8 -> errorMessage = "Enter the 8-digit code"
                                        newPassword.length < 6 -> errorMessage = passwordMinLength
                                        newPassword != confirmPassword -> errorMessage = passwordsNotMatch
                                        else -> onConfirmResetPassword(email.trim(), otpCode, newPassword)
                                    }
                                }
                                ResetPasswordStep.Done -> Unit
                            }
                        },
                        onResend = {
                            if (!isLoading && email.contains("@")) {
                                onSendResetOtp(email.trim())
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResetPasswordFormCard(
    email: String,
    onEmailChange: (String) -> Unit,
    otpCode: String,
    onOtpChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    step: ResetPasswordStep,
    isLoading: Boolean,
    errorMessage: String,
    onSubmit: () -> Unit,
    onResend: () -> Unit
) {
    PremiumFadeIn(delayMillis = 250) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCard)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column {
                iOSInput(
                    value = email,
                    onValueChange = onEmailChange,
                    placeholder = stringResource(R.string.email),
                    enabled = step == ResetPasswordStep.Email && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(
                    visible = step == ResetPasswordStep.Verify,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        iOSInput(
                            value = otpCode,
                            onValueChange = onOtpChange,
                            placeholder = stringResource(R.string.enter_code),
                            enabled = !isLoading,
                            keyboardType = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        PasswordResetField(
                            value = newPassword,
                            onValueChange = onNewPasswordChange,
                            placeholder = stringResource(R.string.new_password),
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        PasswordResetField(
                            value = confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            placeholder = stringResource(R.string.confirm_new_password),
                            enabled = !isLoading
                        )
                    }
                }

                AnimatedVisibility(
                    visible = errorMessage.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(errorMessage, color = ErrorRed, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(GoldGradient))
                        .clickable { onSubmit() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = DarkNavy,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            if (step == ResetPasswordStep.Email) {
                                stringResource(R.string.send_reset_code)
                            } else {
                                stringResource(R.string.reset_password)
                            },
                            style = iOSHeadline.copy(color = DarkNavy)
                        )
                    }
                }

                if (step == ResetPasswordStep.Verify) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = onResend,
                        enabled = !isLoading,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            stringResource(R.string.resend_email),
                            color = BluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordResetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = DimGray) },
        leadingIcon = {
            Icon(Icons.Default.Lock, null, tint = DimGray, modifier = Modifier.size(20.dp))
        },
        enabled = enabled,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = iOSInputShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = iOSBlue,
            unfocusedBorderColor = Separator,
            errorBorderColor = ErrorRed,
            cursorColor = iOSBlue,
            focusedTextColor = White,
            unfocusedTextColor = White
        ),
        textStyle = iOSBody
    )
}

@Composable
private fun ResetPasswordDoneCard(
    email: String,
    onNavigateBack: () -> Unit
) {
    PremiumFadeIn(delayMillis = 100) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SuccessGreen.copy(alpha = 0.05f))
                .border(1.dp, SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.reset_password),
                    style = iOSTitle2.copy(color = SuccessGreen)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.password_reset_complete, email),
                    style = iOSBody.copy(color = TextSecondary, textAlign = TextAlign.Center)
                )
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(BlueGradient))
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.back_to_login),
                        style = iOSHeadline.copy(color = White)
                    )
                }
            }
        }
    }
}

private enum class ResetPasswordStep {
    Email,
    Verify,
    Done
}
