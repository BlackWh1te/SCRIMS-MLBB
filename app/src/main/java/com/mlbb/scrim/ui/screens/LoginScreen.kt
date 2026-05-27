package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
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
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.PremiumFadeIn
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource

@Composable
fun LoginScreen(
    onLoginSuccess          : () -> Unit,
    onNavigateToSignup      : () -> Unit,
    onNavigateToForgotPassword: () -> Unit = {},
    onNavigateToOnboarding  : () -> Unit = {},
    viewModel               : com.mlbb.scrim.viewmodel.AuthViewModel
) {
    var email          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading      by remember { mutableStateOf(false) }
    var errorMessage   by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is com.mlbb.scrim.data.model.AuthResult.Idle           -> {}
            is com.mlbb.scrim.data.model.AuthResult.Success        -> { isLoading = false; onLoginSuccess() }
            is com.mlbb.scrim.data.model.AuthResult.Error          -> {
                isLoading    = false
                errorMessage = (authState as com.mlbb.scrim.data.model.AuthResult.Error).message
            }
            is com.mlbb.scrim.data.model.AuthResult.Loading        -> isLoading = true
            is com.mlbb.scrim.data.model.AuthResult.EmailNotVerified -> isLoading = false
        }
    }

    // Animated background gradient shift
    val infiniteTransition = rememberInfiniteTransition(label = "bgAnim")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label         = "gradOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = heroGradientBrush()
            )
    ) {
        // Decorative glow orbs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(BluePrimary.copy(alpha = 0.18f), Color.Transparent)
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
                    stringResource(R.string.app_title),
                    style     = iOSTitle1.copy(color = TextPrimary),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(6.dp))
            PremiumFadeIn(delayMillis = 130) {
                Text(
                    stringResource(R.string.welcome_back),
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
                        // Email field
                        LoginField(
                            value          = email,
                            onValueChange  = { email = it; errorMessage = "" },
                            placeholder    = stringResource(R.string.email),
                            leadingIcon    = Icons.Default.Email,
                            keyboardType   = KeyboardType.Email
                        )

                        Spacer(Modifier.height(12.dp))

                        // Password field
                        LoginField(
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

                        // Error
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

                        Spacer(Modifier.height(8.dp))

                        // Forgot password
                        TextButton(
                            onClick  = onNavigateToForgotPassword,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                stringResource(R.string.forgot_password),
                                color      = BluePrimary,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Sign in button
                        val fillAllFields = stringResource(R.string.fill_all_fields)
                        Button(
                            onClick = {
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    viewModel.signIn(email, password)
                                } else {
                                    errorMessage = fillAllFields
                                }
                            },
                            enabled  = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = if (!isLoading)
                                            Brush.horizontalGradient(GoldGradient)
                                        else
                                            Brush.linearGradient(listOf(SurfaceOverlay, SurfaceOverlay)),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color       = White,
                                            modifier    = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(stringResource(R.string.signing_in), color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Text(
                                        stringResource(R.string.sign_in),
                                        color         = White,
                                        fontSize      = 16.sp,
                                        fontWeight    = FontWeight.Bold,
                                        letterSpacing = 0.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Sign Up Link ─────────────────────────────────────
            PremiumFadeIn(delayMillis = 280) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.dont_have_account) + " ",
                        color    = TextSecondary,
                        fontSize = 15.sp
                    )
                    TextButton(
                        onClick          = onNavigateToSignup,
                        contentPadding   = PaddingValues(0.dp)
                    ) {
                        Text(
                            stringResource(R.string.signup),
                            color      = GoldPrimary,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Tour Link ────────────────────────────────────────
            PremiumFadeIn(delayMillis = 330) {
                TextButton(
                    onClick  = onNavigateToOnboarding,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.PlayCircleOutline, null,
                        tint     = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.take_tour),
                        color    = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Login Field Component ────────────────────────────────────

@Composable
private fun LoginField(
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
    val borderColor by animateColorAsState(
        targetValue   = if (isFocused.value) BluePrimary.copy(alpha = 0.7f) else GlassBorder,
        animationSpec = tween(200),
        label         = "fieldBorder"
    )

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
            .fillMaxWidth(),
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
