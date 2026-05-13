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
import com.mlbb.scrim.data.model.BestOf
import com.mlbb.scrim.data.model.GameMode
import com.mlbb.scrim.data.model.Region
import com.mlbb.scrim.data.model.SkillLevel
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource
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
        bestOf: BestOf,
        scheduledTime: Long,
        description: String
    ) -> Unit
) {
    var selectedGameMode by remember { mutableStateOf(GameMode.RANKED) }
    var selectedRegion by remember { mutableStateOf(Region.EU) }
    var selectedSkillLevel by remember { mutableStateOf(SkillLevel.ALL) }
    var selectedBestOf by remember { mutableStateOf(BestOf.BO1) }
    var description by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Hardcoded 2026 date/time picker state
    var selectedMonth by remember { mutableStateOf(0) } // 0 = January
    var selectedDay by remember { mutableStateOf(1) }
    var selectedHour by remember { mutableStateOf(18) } // 6 PM default
    var selectedMinute by remember { mutableStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val daysInMonth = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val maxDay = daysInMonth.getOrElse(selectedMonth) { 31 }
    if (selectedDay > maxDay) selectedDay = maxDay

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
                        text = stringResource(R.string.post_scrim),
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
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
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
                                    text = stringResource(R.string.posting_as),
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
                                    label = { Text(region.displayName + " " + region.utcOffset, fontSize = 12.sp) },
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

                // Best Of Selection
                AnimatedEntrance(delayMillis = 275) {
                    SelectionCard(title = "Number of Games") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BestOf.values().forEach { bestOf ->
                                FilterChip(
                                    selected = selectedBestOf == bestOf,
                                    onClick = { selectedBestOf = bestOf },
                                    label = { Text("BO${bestOf.games}", fontSize = 13.sp) },
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

                // Scheduled Date & Time (2026 hardcoded)
                AnimatedEntrance(delayMillis = 300) {
                    SelectionCard(title = "Date & Time (2026)") {
                        Column {
                            // Date Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Year (fixed 2026 display)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.year),
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .background(
                                                color = White.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "2026",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary
                                        )
                                    }
                                }

                                // Month dropdown
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.month),
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                    var monthExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(
                                            onClick = { monthExpanded = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(40.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = White.copy(alpha = 0.1f)
                                            ),
                                            border = androidx.compose.ui.graphics.SolidColor(White.copy(alpha = 0.3f))
                                                .let { androidx.compose.foundation.BorderStroke(1.dp, it) },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                months[selectedMonth],
                                                color = White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = monthExpanded,
                                            onDismissRequest = { monthExpanded = false },
                                            modifier = Modifier.background(DarkNavy)
                                        ) {
                                            months.forEachIndexed { index, month ->
                                                DropdownMenuItem(
                                                    text = { Text(month, color = White) },
                                                    onClick = {
                                                        selectedMonth = index
                                                        monthExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Day dropdown
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.day),
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                    var dayExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(
                                            onClick = { dayExpanded = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(40.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = White.copy(alpha = 0.1f)
                                            ),
                                            border = androidx.compose.ui.graphics.SolidColor(White.copy(alpha = 0.3f))
                                                .let { androidx.compose.foundation.BorderStroke(1.dp, it) },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                selectedDay.toString(),
                                                color = White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = dayExpanded,
                                            onDismissRequest = { dayExpanded = false },
                                            modifier = Modifier.background(DarkNavy)
                                        ) {
                                            (1..maxDay).forEach { day ->
                                                DropdownMenuItem(
                                                    text = { Text(day.toString(), color = White) },
                                                    onClick = {
                                                        selectedDay = day
                                                        dayExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Time Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hour dropdown
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.hour),
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                    var hourExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(
                                            onClick = { hourExpanded = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(40.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = White.copy(alpha = 0.1f)
                                            ),
                                            border = androidx.compose.ui.graphics.SolidColor(White.copy(alpha = 0.3f))
                                                .let { androidx.compose.foundation.BorderStroke(1.dp, it) },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                String.format("%02d", selectedHour),
                                                color = White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = hourExpanded,
                                            onDismissRequest = { hourExpanded = false },
                                            modifier = Modifier.background(DarkNavy)
                                        ) {
                                            (0..23).forEach { hour ->
                                                DropdownMenuItem(
                                                    text = { Text(String.format("%02d", hour), color = White) },
                                                    onClick = {
                                                        selectedHour = hour
                                                        hourExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Minute dropdown
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.minute),
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                    var minuteExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(
                                            onClick = { minuteExpanded = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(40.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = White.copy(alpha = 0.1f)
                                            ),
                                            border = androidx.compose.ui.graphics.SolidColor(White.copy(alpha = 0.3f))
                                                .let { androidx.compose.foundation.BorderStroke(1.dp, it) },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                String.format("%02d", selectedMinute),
                                                color = White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = minuteExpanded,
                                            onDismissRequest = { minuteExpanded = false },
                                            modifier = Modifier.background(DarkNavy)
                                        ) {
                                            listOf(0, 15, 30, 45).forEach { minute ->
                                                DropdownMenuItem(
                                                    text = { Text(String.format("%02d", minute), color = White) },
                                                    onClick = {
                                                        selectedMinute = minute
                                                        minuteExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // UTC offset display
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = stringResource(R.string.timezone),
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .background(
                                                color = BluePrimary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = selectedRegion.utcOffset,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Scrim will start: ${months[selectedMonth]} $selectedDay, 2026 at ${String.format("%02d", selectedHour)}:${String.format("%02d", selectedMinute)} ${selectedRegion.utcOffset}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    color = MidGray
                                )
                            )
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
                            placeholder = { Text("Add details about your scrim...", color = MidGray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = White.copy(alpha = 0.3f),
                                focusedLabelColor = GoldPrimary,
                                unfocusedLabelColor = White.copy(alpha = 0.7f),
                                cursorColor = GoldPrimary,
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedContainerColor = White.copy(alpha = 0.08f),
                                unfocusedContainerColor = White.copy(alpha = 0.05f)
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
                        text = stringResource(R.string.post_scrim),
                        onClick = {
                            val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                            calendar.set(2026, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                            calendar.set(java.util.Calendar.MILLISECOND, 0)
                            val scheduledTime = calendar.timeInMillis
                            onCreateScrim(
                                selectedGameMode,
                                selectedRegion,
                                selectedSkillLevel,
                                selectedBestOf,
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
