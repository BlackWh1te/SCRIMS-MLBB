package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.GradientButton

@Composable
fun JoinTeamScreen(
    onNavigateBack: () -> Unit,
    onJoinTeam: (String) -> Unit = {}
) {
    var inviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }

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
                            color = White
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
                            .shadow(
                                elevation = 12.dp,
                                spotColor = BluePrimary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(BluePrimary.copy(alpha = 0.2f), BluePrimary.copy(alpha = 0.05f))
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = BluePrimary,
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
                        color = White,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedEntrance(delayMillis = 200) {
                    Text(
                        text = stringResource(R.string.invite_code_hint),
                        fontSize = 14.sp,
                        color = LightGray,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (!success) {
                    AnimatedEntrance(delayMillis = 250) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 6.dp,
                                    spotColor = Color.Black.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = DarkNavy),
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
                                        inviteCode = it.uppercase()
                                        errorMessage = ""
                                    },
                                    label = { Text("Invite Code") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPrimary,
                                        unfocusedBorderColor = White.copy(alpha = 0.3f),
                                        focusedLabelColor = GoldPrimary,
                                        unfocusedLabelColor = White.copy(alpha = 0.7f),
                                        cursorColor = GoldPrimary,
                                        focusedTextColor = White,
                                        unfocusedTextColor = White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                    singleLine = true,
                                    placeholder = { Text("e.g. MLBB-ELI7A3B2", color = MidGray) }
                                )

                                AnimatedVisibility(
                                    visible = errorMessage.isNotEmpty(),
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Text(
                                        text = errorMessage,
                                        color = ErrorRed,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                GradientButton(
                                    text = if (isLoading) "Joining..." else "Join Team",
                                    onClick = {
                                        if (inviteCode.isBlank() || inviteCode.length < 6) {
                                            errorMessage = "Please enter a valid invite code"
                                            return@GradientButton
                                        }
                                        isLoading = true
                                        onJoinTeam(inviteCode)
                                        // Mock success for demo
                                        success = true
                                        isLoading = false
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
                                .shadow(
                                    elevation = 6.dp,
                                    spotColor = SuccessGreen.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(20.dp)
                                ),
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
                                    text = stringResource(R.string.welcome_to_team),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.joined_team_success),
                                    fontSize = 14.sp,
                                    color = LightGray,
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
