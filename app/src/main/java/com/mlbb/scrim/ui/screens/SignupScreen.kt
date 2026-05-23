package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.PremiumFadeIn
import com.mlbb.scrim.ui.components.PremiumCaptcha

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: com.mlbb.scrim.viewmodel.AuthViewModel
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var inGameId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isCaptchaVerified by remember { mutableStateOf(false) }
    val captchaError = stringResource(R.string.captcha_verify_human)

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is com.mlbb.scrim.data.model.AuthResult.Idle -> {}
            is com.mlbb.scrim.data.model.AuthResult.Success -> {
                isLoading = false
                onSignupSuccess()
            }
            is com.mlbb.scrim.data.model.AuthResult.Error -> {
                isLoading = false
                errorMessage = (authState as com.mlbb.scrim.data.model.AuthResult.Error).message
            }
            is com.mlbb.scrim.data.model.AuthResult.Loading -> {
                isLoading = true
            }
            is com.mlbb.scrim.data.model.AuthResult.EmailNotVerified -> {
                isLoading = false
            }
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
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-40).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(BluePrimary.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldPrimary.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Logo ────────────────────────────────────────────
            PremiumFadeIn(delayMillis = 0) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo),
                    contentDescription = stringResource(R.string.content_desc_app_logo),
                    modifier = Modifier.size(90.dp)
                )
            }

            Spacer(Modifier.height(22.dp))

            // ── Title & Subtitle ─────────────────────────────────
            PremiumFadeIn(delayMillis = 80) {
                Text(
                    stringResource(R.string.create_account),
                    style     = iOSTitle1.copy(color = TextPrimary),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(6.dp))
            PremiumFadeIn(delayMillis = 130) {
                Text(
                    stringResource(R.string.join_community),
                    style     = iOSCallout.copy(color = TextSecondary),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Form Card ────────────────────────────────────────
            PremiumFadeIn(delayMillis = 180) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceCard)
                        .border(
                            width  = 1.dp,
                            brush  = Brush.linearGradient(
                                colors = listOf(
                                    GlassBorder.copy(alpha = 0.8f),
                                    GlassBorder.copy(alpha = 0.2f)
                                )
                            ),
                            shape  = RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // Username field
                        SignupField(
                            value          = username,
                            onValueChange  = { username = it; errorMessage = "" },
                            placeholder    = stringResource(R.string.username),
                            leadingIcon    = Icons.Default.Person
                        )

                        Spacer(Modifier.height(12.dp))

                        // Email field
                        SignupField(
                            value          = email,
                            onValueChange  = { email = it; errorMessage = "" },
                            placeholder    = stringResource(R.string.email),
                            leadingIcon    = Icons.Default.Email,
                            keyboardType   = KeyboardType.Email
                        )

                        Spacer(Modifier.height(12.dp))

                        // In-Game ID field
                        SignupField(
                            value          = inGameId,
                            onValueChange  = { inGameId = it; errorMessage = "" },
                            placeholder    = stringResource(R.string.in_game_id),
                            leadingIcon    = Icons.Default.Tag
                        )

                        Spacer(Modifier.height(12.dp))

                        // Password field
                        SignupField(
                            value               = password,
                            onValueChange       = { password = it; errorMessage = "" },
                            placeholder         = stringResource(R.string.password),
                            leadingIcon         = Icons.Default.Lock,
                            trailingIcon        = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            onTrailingClick     = { passwordVisible = !passwordVisible },
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            keyboardType        = KeyboardType.Password
                        )

                        Spacer(Modifier.height(12.dp))

                        // Confirm Password field
                        SignupField(
                            value               = confirmPassword,
                            onValueChange       = { confirmPassword = it; errorMessage = "" },
                            placeholder         = stringResource(R.string.confirm_password),
                            leadingIcon         = Icons.Default.Lock,
                            trailingIcon        = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            onTrailingClick     = { confirmPasswordVisible = !confirmPasswordVisible },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                                                   else PasswordVisualTransformation(),
                            keyboardType        = KeyboardType.Password
                        )

                        // Error message
                        AnimatedVisibility(
                            visible = errorMessage.isNotEmpty(),
                            enter   = fadeIn() + expandVertically(),
                            exit    = fadeOut() + shrinkVertically()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ErrorRed.copy(alpha = 0.10f))
                                    .border(1.dp, ErrorRed.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ErrorOutline, null,
                                        tint     = ErrorRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(errorMessage, color = ErrorRed, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── Security CAPTCHA ────────────────────────
                        PremiumCaptcha(
                            onVerified = { isCaptchaVerified = it }
                        )

                        Spacer(Modifier.height(24.dp))

                        val fillAllFields = stringResource(R.string.fill_all_fields)
                        val passwordsNotMatch = stringResource(R.string.passwords_not_match)
                        val passwordMinLength = stringResource(R.string.password_min_length)
                        val invalidEmail = stringResource(R.string.invalid_email)

                        // ── CTA Button ────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = BlueGradient
                                    )
                                )
                                .clickable(enabled = !isLoading) {
                                    when {
                                        username.isBlank() || email.isBlank() || inGameId.isBlank() ||
                                        password.isBlank() || confirmPassword.isBlank() -> {
                                            errorMessage = fillAllFields
                                        }
                                        password != confirmPassword -> errorMessage = passwordsNotMatch
                                        password.length < 6 -> errorMessage = passwordMinLength
                                        !email.contains("@") -> errorMessage = invalidEmail
                                        !isCaptchaVerified -> errorMessage = captchaError
                                        else -> viewModel.signUp(email, password, username, inGameId)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    stringResource(R.string.create_account),
                                    style = iOSHeadline.copy(color = White)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Footer Link ─────────────────────────────────────
            PremiumFadeIn(delayMillis = 230) {
                Row(
                    modifier = Modifier.padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.already_have_account) + " ",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                    Text(
                        stringResource(R.string.sign_in),
                        color = GoldPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SignupField(
    value               : String,
    onValueChange       : (String) -> Unit,
    placeholder         : String,
    leadingIcon         : androidx.compose.ui.graphics.vector.ImageVector,
    trailingIcon        : androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingClick     : () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType        : KeyboardType = KeyboardType.Text
) {
    val isFocused = remember { mutableStateOf(false) }

    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        placeholder          = {
            Text(placeholder, color = TextTertiary, fontSize = 15.sp)
        },
        leadingIcon          = {
            Icon(leadingIcon, null, tint = if (isFocused.value) BluePrimary else TextTertiary, modifier = Modifier.size(20.dp))
        },
        trailingIcon         = if (trailingIcon != null) {{
            IconButton(onClick = onTrailingClick) {
                Icon(trailingIcon, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }} else null,
        visualTransformation = visualTransformation,
        modifier             = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused.value = it.isFocused },
        singleLine           = true,
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
        shape                = RoundedCornerShape(14.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = BluePrimary.copy(alpha = 0.7f),
            unfocusedBorderColor    = GlassBorder,
            focusedContainerColor   = SurfaceOverlay,
            unfocusedContainerColor = SurfaceOverlay,
            focusedTextColor        = TextPrimary,
            unfocusedTextColor      = TextPrimary,
            cursorColor             = BluePrimary
        ),
        textStyle = iOSBody.copy(fontSize = 15.sp)
    )
}
