package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.PremiumFadeIn
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.iOSInput

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onSendResetLink: (String) -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var isSent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        // ── Background Glow Orbs ──────────────────────────────────
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
            // ── Header ──────────────────────────────────────────
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

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // ── Icon ──────────────────────────────────────────
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
                            tint = Color.White,
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
                        text = stringResource(R.string.forgot_password_hint),
                        style = iOSCallout.copy(color = TextSecondary, textAlign = TextAlign.Center),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                if (!isSent) {
                    // ── Request Form ────────────────────────────────
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
                                    onValueChange = {
                                        email = it
                                        errorMessage = ""
                                    },
                                    placeholder = "Email Address",
                                    modifier = Modifier.fillMaxWidth()
                                )

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
                                        Spacer(Modifier.width(8.dp))
                                        Text(errorMessage, color = ErrorRed, fontSize = 13.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                // Submit Button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Brush.verticalGradient(GoldGradient))
                                        .clickable {
                                            if (email.contains("@")) {
                                                onSendResetLink(email)
                                                isSent = true
                                            } else {
                                                errorMessage = "Please enter a valid email"
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stringResource(R.string.send_reset_link),
                                        style = iOSHeadline.copy(color = DarkNavy)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ── Success State ───────────────────────────────
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
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = stringResource(R.string.check_email),
                                    style = iOSTitle2.copy(color = SuccessGreen)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.reset_link_sent, email),
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
                                        style = iOSHeadline.copy(color = Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
