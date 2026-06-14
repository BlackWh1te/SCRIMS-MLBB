package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.GradientButton

@Composable
fun JoinTeamScreen(
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    requestSent: Boolean = false,
    onJoinTeam: (String) -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    var inviteCode by remember { mutableStateOf("") }

    val enterValidCodeError = stringResource(R.string.enter_valid_invite_code)
    var localError by remember { mutableStateOf("") }
    val appSurface = appSurfaceColor()
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()
    val appBorder = appBorderColor()
    val appElevatedSurface = appElevatedSurfaceColor()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                        text = stringResource(R.string.join_team),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = appTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                AnimatedEntrance(delayMillis = 100) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedEntrance(delayMillis = 150) {
                    Text(
                        text = stringResource(R.string.join_a_team),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = appTextPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedEntrance(delayMillis = 200) {
                    Text(
                        text = stringResource(R.string.invite_code_hint),
                        fontSize = 14.sp,
                        color = appTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!requestSent) {
                    AnimatedEntrance(delayMillis = 250) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, appBorderColor(), RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = appSurface),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                OutlinedTextField(
                                    value = inviteCode,
                                    onValueChange = {
                                        inviteCode = it.uppercase().filter { ch -> ch.isLetterOrDigit() || ch == '-' }.take(24)
                                        localError = ""
                                        onDismissError()
                                    },
                                    label = { Text(stringResource(R.string.invite_code)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                        unfocusedBorderColor = appBorder,
                                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                                        unfocusedLabelColor = appTextSecondary,
                                        cursorColor = MaterialTheme.colorScheme.secondary,
                                        focusedTextColor = appTextPrimary,
                                        unfocusedTextColor = appTextPrimary,
                                        focusedContainerColor = appElevatedSurface,
                                        unfocusedContainerColor = appElevatedSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                    singleLine = true,
                                    placeholder = { Text(stringResource(R.string.invite_code_placeholder), color = appTextSecondary) }
                                )

                                AnimatedVisibility(
                                    visible = localError.isNotEmpty() || !errorMessage.isNullOrBlank(),
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Text(
                                        text = localError.ifBlank { errorMessage.orEmpty() },
                                        color = ErrorRed,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                GradientButton(
                                    text = if (isLoading) stringResource(R.string.joining) else stringResource(R.string.join_team_btn),
                                    onClick = {
                                        if (inviteCode.isBlank() || inviteCode.length < 6) {
                                            localError = enterValidCodeError
                                            return@GradientButton
                                        }
                                        onJoinTeam(inviteCode)
                                    },
                                    isLoading = isLoading,
                                    gradient = BlueGradient
                                )
                            }
                        }
                    }
                } else {
                    AnimatedEntrance(delayMillis = 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SuccessGreen.copy(alpha = 0.30f), RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = SuccessGreen.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GroupAdd,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Request sent",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your join request is pending. The team leader can approve or reject it from Team Details.",
                                    fontSize = 14.sp,
                                    color = appTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                GradientButton(
                                    text = stringResource(R.string.go_to_teams),
                                    onClick = onNavigateBack,
                                    gradient = BlueGradient,
                                    height = 48.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
