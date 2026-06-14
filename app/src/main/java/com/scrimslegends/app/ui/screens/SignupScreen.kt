package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.PremiumFadeIn
import com.scrimslegends.app.ui.components.PremiumCaptcha
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: com.scrimslegends.app.viewmodel.AuthViewModel
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
    var termsAgreed by remember { mutableStateOf(false) }
    val captchaError = stringResource(R.string.captcha_verify_human)
    val termsError = stringResource(R.string.terms_required)
    val fillAllFields = stringResource(R.string.fill_all_fields)
    val passwordsNotMatch = stringResource(R.string.passwords_not_match)
    val passwordMinLength = stringResource(R.string.password_min_length)
    val invalidEmail = stringResource(R.string.invalid_email)
    val emailFocusRequester = remember { FocusRequester() }
    val inGameIdFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val authState by viewModel.authState.collectAsStateWithLifecycle()

    fun submitSignup() {
        when {
            username.isBlank() || email.isBlank() || inGameId.isBlank() ||
                password.isBlank() || confirmPassword.isBlank() -> {
                errorMessage = fillAllFields
            }
            password != confirmPassword -> errorMessage = passwordsNotMatch
            password.length < 6 -> errorMessage = passwordMinLength
            !email.contains("@") -> errorMessage = invalidEmail
            !termsAgreed -> errorMessage = termsError
            !isCaptchaVerified -> errorMessage = captchaError
            else -> {
                focusManager.clearFocus()
                viewModel.signUp(email, password, username, inGameId)
            }
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is com.scrimslegends.app.data.model.AuthResult.Idle -> {}
            is com.scrimslegends.app.data.model.AuthResult.Success -> {
                isLoading = false
                onSignupSuccess()
            }
            is com.scrimslegends.app.data.model.AuthResult.Error -> {
                isLoading = false
                errorMessage = (authState as com.scrimslegends.app.data.model.AuthResult.Error).message
            }
            is com.scrimslegends.app.data.model.AuthResult.Loading -> {
                isLoading = true
            }
            is com.scrimslegends.app.data.model.AuthResult.EmailNotVerified -> {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Background Glow Orbs ──────────────────────────────────
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-40).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent)
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
                        colors = listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), Color.Transparent)
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
                    style     = iOSTitle1.copy(color = MaterialTheme.colorScheme.onSurface),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(6.dp))
            PremiumFadeIn(delayMillis = 130) {
                Text(
                    stringResource(R.string.join_community),
                    style     = iOSCallout.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width  = 1.dp,
                            color  = MaterialTheme.colorScheme.primary,
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
                            leadingIcon    = Icons.Default.Person,
                            imeAction      = ImeAction.Next,
                            keyboardActions = KeyboardActions(
                                onNext = { emailFocusRequester.requestFocus() }
                            ),
                            autofillTypes  = listOf(AutofillType.Username)
                        )

                        Spacer(Modifier.height(12.dp))

                        // Email field
                        SignupField(
                            value          = email,
                            onValueChange  = { email = it; errorMessage = "" },
                            placeholder    = stringResource(R.string.email),
                            leadingIcon    = Icons.Default.Email,
                            keyboardType   = KeyboardType.Email,
                            imeAction      = ImeAction.Next,
                            keyboardActions = KeyboardActions(
                                onNext = { inGameIdFocusRequester.requestFocus() }
                            ),
                            autofillTypes  = listOf(AutofillType.EmailAddress),
                            modifier       = Modifier.focusRequester(emailFocusRequester)
                        )

                        Spacer(Modifier.height(12.dp))

                        // In-Game ID field
                        SignupField(
                            value          = inGameId,
                            onValueChange  = { inGameId = it; errorMessage = "" },
                            placeholder    = stringResource(R.string.in_game_id),
                            leadingIcon    = Icons.Default.Tag,
                            imeAction      = ImeAction.Next,
                            keyboardActions = KeyboardActions(
                                onNext = { passwordFocusRequester.requestFocus() }
                            ),
                            modifier       = Modifier.focusRequester(inGameIdFocusRequester)
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
                            keyboardType        = KeyboardType.Password,
                            imeAction           = ImeAction.Next,
                            keyboardActions     = KeyboardActions(
                                onNext = { confirmPasswordFocusRequester.requestFocus() }
                            ),
                            autofillTypes       = listOf(AutofillType.Password),
                            modifier            = Modifier.focusRequester(passwordFocusRequester)
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
                            keyboardType        = KeyboardType.Password,
                            imeAction           = ImeAction.Done,
                            keyboardActions     = KeyboardActions(
                                onDone = { submitSignup() }
                            ),
                            autofillTypes       = listOf(AutofillType.Password),
                            modifier            = Modifier.focusRequester(confirmPasswordFocusRequester)
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

                        Spacer(Modifier.height(16.dp))

                        // ── Terms Agreement ─────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = termsAgreed,
                                onCheckedChange = { termsAgreed = it; errorMessage = "" },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val termsLabel = stringResource(R.string.terms_checkbox_label)
                            val termsLink = stringResource(R.string.terms_of_service_link)
                            val andText = stringResource(R.string.and)
                            val privacyLink = stringResource(R.string.privacy_policy_link)
                            Text(
                                text = "$termsLabel ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Text(
                                text = termsLink,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    context.startActivity(
                                        android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("https://scrimslegends.app/terms")
                                        }
                                    )
                                }
                            )
                            Text(
                                text = " $andText ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Text(
                                text = privacyLink,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    context.startActivity(
                                        android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("https://scrimslegends.app/privacy")
                                        }
                                    )
                                }
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // ── CTA Button ────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(color = MaterialTheme.colorScheme.primary)
                                .clickable(enabled = !isLoading) { submitSignup() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    stringResource(R.string.create_account),
                                    style = iOSHeadline.copy(color = MaterialTheme.colorScheme.onSurface)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                    Text(
                        stringResource(R.string.sign_in),
                        color = MaterialTheme.colorScheme.primary,
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SignupField(
    value               : String,
    onValueChange       : (String) -> Unit,
    placeholder         : String,
    leadingIcon         : androidx.compose.ui.graphics.vector.ImageVector,
    trailingIcon        : androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingClick     : () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType        : KeyboardType = KeyboardType.Text,
    imeAction           : ImeAction = ImeAction.Done,
    keyboardActions     : KeyboardActions = KeyboardActions.Default,
    autofillTypes       : List<AutofillType> = emptyList(),
    modifier            : Modifier = Modifier
) {
    val isFocused = remember { mutableStateOf(false) }
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current
    val autofillNode = remember(autofillTypes) {
        AutofillNode(
            autofillTypes = autofillTypes,
            onFill = onValueChange
        )
    }
    if (autofillTypes.isNotEmpty()) {
        autofillTree += autofillNode
    }

    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        placeholder          = {
            Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 15.sp)
        },
        leadingIcon          = {
            Icon(leadingIcon, null, tint = if (isFocused.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        },
        trailingIcon         = if (trailingIcon != null) {{
            IconButton(onClick = onTrailingClick) {
                Icon(trailingIcon, placeholder, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }} else null,
        visualTransformation = visualTransformation,
        modifier             = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                if (autofillTypes.isNotEmpty()) {
                    autofillNode.boundingBox = coordinates.boundsInWindow()
                }
            }
            .onFocusChanged { focusState ->
                isFocused.value = focusState.isFocused
                if (autofillTypes.isNotEmpty()) {
                    if (focusState.isFocused) {
                        autofill?.requestAutofillForNode(autofillNode)
                    } else {
                        autofill?.cancelAutofillForNode(autofillNode)
                    }
                }
            },
        singleLine           = true,
        keyboardOptions      = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions      = keyboardActions,
        shape                = RoundedCornerShape(14.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            unfocusedBorderColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor        = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
            cursorColor             = MaterialTheme.colorScheme.primary
        ),
        textStyle = iOSBody.copy(fontSize = 15.sp)
    )
}
