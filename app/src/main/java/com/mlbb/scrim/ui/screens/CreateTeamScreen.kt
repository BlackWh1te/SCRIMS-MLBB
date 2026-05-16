package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.GradientButton

@Composable
fun CreateTeamScreen(
    onNavigateBack: () -> Unit,
    onCreateTeam: (String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    existingTeamNames: List<String> = emptyList()
) {
    var teamName by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf("") }

    val displayedError = localError.takeIf { it.isNotEmpty() } ?: errorMessage ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = heroGradientBrush()
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    GlassBackButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.background(
                            color = ErrorRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    )

                    Text(
                        text = stringResource(R.string.create_team),
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
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Main Card
                AnimatedEntrance(delayMillis = 100) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                spotColor = Color.Black.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkNavy
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp)
                        ) {
                            // Team Avatar Preview
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .shadow(
                                        elevation = 8.dp,
                                        spotColor = BluePrimary.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(BluePrimary, Color(0xFF0A5A9F))
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = teamName.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Team Name Input
                            OutlinedTextField(
                                value = teamName,
                                onValueChange = {
                                    teamName = it
                                    localError = ""
                                },
                                label = { Text("Team Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
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
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MidGray
                                    )
                                }
                            )

                            // Error Message
                            AnimatedVisibility(
                                visible = displayedError.isNotEmpty(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Text(
                                    text = displayedError,
                                    color = ErrorRed,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Gradient Button
                            GradientButton(
                                text = stringResource(R.string.create_team),
                                onClick = {
                                    when {
                                        teamName.isBlank() -> {
                                            localError = "Please enter a team name"
                                        }
                                        teamName.length < 3 -> {
                                            localError = "Team name must be at least 3 characters"
                                        }
                                        existingTeamNames.any { it.equals(teamName, ignoreCase = true) } -> {
                                            localError = "A team with this name already exists"
                                        }
                                        else -> {
                                            localError = ""
                                            onCreateTeam(teamName)
                                        }
                                    }
                                },
                                gradient = GoldGradient,
                                enabled = !isLoading,
                                isLoading = isLoading
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Info Card
                AnimatedEntrance(delayMillis = 200) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                spotColor = Color.Black.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkNavy
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = WarningOrange.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info",
                                    tint = WarningOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = stringResource(R.string.team_requirements),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = White
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.team_requirements_desc),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        color = LightGray
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
