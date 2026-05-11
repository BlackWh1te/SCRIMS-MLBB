package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.GameMode
import com.mlbb.scrim.data.model.Region
import com.mlbb.scrim.data.model.SkillLevel
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.GradientButton
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScrimScreen(
    teamName: String,
    teamId: String,
    teamLeader: String,
    onNavigateBack: () -> Unit,
    onCreateScrim: (
        gameMode: GameMode,
        region: Region,
        skillLevel: SkillLevel,
        scheduledTime: Long,
        description: String
    ) -> Unit
) {
    var selectedGameMode by remember { mutableStateOf(GameMode.RANKED) }
    var selectedRegion by remember { mutableStateOf(Region.EU) }
    var selectedSkillLevel by remember { mutableStateOf(SkillLevel.ALL) }
    var description by remember { mutableStateOf("") }
    var scheduledHours by remember { mutableStateOf(1) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = heroGradientBrush()
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                        text = "Post Scrim",
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
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Team Info Card
                AnimatedEntrance(delayMillis = 100) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                spotColor = BluePrimary.copy(alpha = 0.2f),
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
                                        brush = Brush.verticalGradient(
                                            colors = listOf(BluePrimary, Color(0xFF0A5A9F))
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = teamName.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Posting as",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        color = LightGray
                                    )
                                )
                                Text(
                                    text = teamName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Game Mode Selection
                AnimatedEntrance(delayMillis = 150) {
                    SelectionCard(title = "Game Mode") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GameMode.values().forEach { mode ->
                                FilterChip(
                                    selected = selectedGameMode == mode,
                                    onClick = { selectedGameMode = mode },
                                    label = { Text(mode.name, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BluePrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = BluePrimary,
                                        containerColor = White.copy(alpha = 0.1f),
                                        labelColor = LightGray
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Region Selection
                AnimatedEntrance(delayMillis = 200) {
                    SelectionCard(title = "Region") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Region.values().forEach { region ->
                                FilterChip(
                                    selected = selectedRegion == region,
                                    onClick = { selectedRegion = region },
                                    label = { Text(region.name, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = GoldPrimary,
                                        containerColor = White.copy(alpha = 0.1f),
                                        labelColor = LightGray
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Skill Level Selection
                AnimatedEntrance(delayMillis = 250) {
                    SelectionCard(title = "Skill Level") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SkillLevel.values().forEach { level ->
                                FilterChip(
                                    selected = selectedSkillLevel == level,
                                    onClick = { selectedSkillLevel = level },
                                    label = { Text(level.name, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Purple.copy(alpha = 0.2f),
                                        selectedLabelColor = Purple,
                                        containerColor = White.copy(alpha = 0.1f),
                                        labelColor = LightGray
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Scheduled Time
                AnimatedEntrance(delayMillis = 300) {
                    SelectionCard(title = "Start Time") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (scheduledHours > 1) scheduledHours-- },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Text(
                                text = "$scheduledHours hour${if (scheduledHours > 1) "s" else ""} from now",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )

                            IconButton(
                                onClick = { if (scheduledHours < 24) scheduledHours++ },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Description
                AnimatedEntrance(delayMillis = 350) {
                    SelectionCard(title = "Description (Optional)") {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Add details about your scrim...") },
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
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error Message
                AnimatedVisibility(
                    visible = errorMessage.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Post Button
                AnimatedEntrance(delayMillis = 400) {
                    GradientButton(
                        text = "Post Scrim",
                        onClick = {
                            val scheduledTime = System.currentTimeMillis() + (scheduledHours * 3600000L)
                            onCreateScrim(
                                selectedGameMode,
                                selectedRegion,
                                selectedSkillLevel,
                                scheduledTime,
                                description
                            )
                        },
                        gradient = GoldGradient,
                        height = 56.dp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SelectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = Color.Black.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}
