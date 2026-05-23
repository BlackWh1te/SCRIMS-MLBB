package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import com.mlbb.scrim.data.model.Team
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
    teams: List<Team>,
    onNavigateBack: () -> Unit,
    onCreateScrim: (
        teamId: String,
        teamName: String,
        gameMode: GameMode,
        region: Region,
        skillLevel: SkillLevel,
        bestOf: BestOf,
        scheduledTime: Long,
        description: String
    ) -> Unit
) {
    // Team selection state
    var selectedTeamIndex by remember { mutableIntStateOf(0) }
    val selectedTeam = teams.getOrElse(selectedTeamIndex) { teams.firstOrNull() }
    val teamName = selectedTeam?.name ?: stringResource(R.string.my_team_default)
    val teamId = selectedTeam?.id ?: ""
    val currentPlayerCount = selectedTeam?.currentPlayerCount ?: 0
    val meetsMinPlayers = selectedTeam?.meetsMinPlayers ?: false

    var showTeamPicker by remember { mutableStateOf(false) }
    var showMinPlayerDialog by remember { mutableStateOf(false) }

    var selectedGameMode by remember { mutableStateOf(GameMode.RANKED) }
    var selectedRegion by remember { mutableStateOf(Region.EU) }
    var selectedSkillLevel by remember { mutableStateOf(SkillLevel.ALL) }
    var selectedBestOf by remember { mutableStateOf(BestOf.BO1) }
    var description by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Date/time picker state — defaults to today's date, current year
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) // 0-based
    val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
    val availableYears = listOf(currentYear, currentYear + 1)
    var selectedYearIndex by remember { mutableIntStateOf(0) } // 0 = current year
    val selectedYear = availableYears.getOrElse(selectedYearIndex) { currentYear }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    var selectedDay by remember { mutableIntStateOf(currentDay) }
    var selectedHour by remember { mutableIntStateOf(18) } // 6 PM default
    var selectedMinute by remember { mutableIntStateOf(0) }

    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val isLeapYear = (selectedYear % 4 == 0 && selectedYear % 100 != 0) || (selectedYear % 400 == 0)
    val daysInMonth = listOf(31, if (isLeapYear) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
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

                // Team Info Card — Clickable to switch team if multiple
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
                        shape = RoundedCornerShape(16.dp),
                        onClick = { if (teams.size > 1) showTeamPicker = true }
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

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.posting_as),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        color = LightGray
                                    )
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = teamName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = White
                                        ),
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1
                                    )
                                    if (teams.size > 1) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.SwapHoriz,
                                            contentDescription = stringResource(R.string.content_desc_switch_team),
                                            tint = BluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                // Player count indicator
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        tint = if (meetsMinPlayers) SuccessGreen else WarningOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$currentPlayerCount/5 players",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (meetsMinPlayers) SuccessGreen else WarningOrange
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Game Mode Selection
                AnimatedEntrance(delayMillis = 150) {
                    SelectionCard(title = stringResource(R.string.game_mode)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GameMode.values().forEach { mode ->
                                FilterChip(
                                    selected = selectedGameMode == mode,
                                    onClick = { selectedGameMode = mode },
                                    label = { Text(mode.name, fontSize = 11.sp) },
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
                    SelectionCard(title = stringResource(R.string.region)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Region.values().forEach { region ->
                                FilterChip(
                                    selected = selectedRegion == region,
                                    onClick = { selectedRegion = region },
                                    label = { Text(region.displayName, fontSize = 11.sp) },
                                    modifier = Modifier.width(80.dp).height(48.dp),
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
                    SelectionCard(title = stringResource(R.string.skill_level)) {
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
                    SelectionCard(title = stringResource(R.string.number_of_games)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BestOf.values().forEach { bestOf ->
                                FilterChip(
                                    selected = selectedBestOf == bestOf,
                                    onClick = { selectedBestOf = bestOf },
                                    label = { Text(stringResource(R.string.bo_format, bestOf.games), fontSize = 13.sp) },
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

                // Scheduled Date & Time
                AnimatedEntrance(delayMillis = 300) {
                    SelectionCard(title = stringResource(R.string.date_and_time)) {
                        Column {
                            // Date Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Year selector
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.year),
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                    var yearExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        OutlinedButton(
                                            onClick = { yearExpanded = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(40.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = White.copy(alpha = 0.1f)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, White.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                selectedYear.toString(),
                                                color = GoldPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = yearExpanded,
                                            onDismissRequest = { yearExpanded = false },
                                            modifier = Modifier.background(DarkNavy)
                                        ) {
                                            availableYears.forEachIndexed { index, year ->
                                                DropdownMenuItem(
                                                    text = { Text(year.toString(), color = White) },
                                                    onClick = {
                                                        selectedYearIndex = index
                                                        yearExpanded = false
                                                    }
                                                )
                                            }
                                        }
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
                                text = "Scrim will start: ${months[selectedMonth]} $selectedDay, $selectedYear at ${String.format("%02d", selectedHour)}:${String.format("%02d", selectedMinute)} ${selectedRegion.utcOffset}",
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
                    SelectionCard(title = stringResource(R.string.description_optional)) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text(stringResource(R.string.add_scrim_details_hint), color = MidGray) },
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

                // Team size warning
                if (currentPlayerCount < 5) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = WarningOrange.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Your team has $currentPlayerCount/5 players. Post scrims requires at least 5 players.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = WarningOrange,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                // Post Button — gated on minimum 5 players
                AnimatedEntrance(delayMillis = 400) {
                    GradientButton(
                        text = stringResource(R.string.post_scrim),
                        onClick = {
                            if (!meetsMinPlayers) {
                                showMinPlayerDialog = true
                            } else {
                                val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                                calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                                calendar.set(java.util.Calendar.MILLISECOND, 0)
                                val scheduledTime = calendar.timeInMillis
                                onCreateScrim(
                                    teamId,
                                    teamName,
                                    selectedGameMode,
                                    selectedRegion,
                                    selectedSkillLevel,
                                    selectedBestOf,
                                    scheduledTime,
                                    description
                                )
                            }
                        },
                        gradient = GoldGradient,
                        height = 56.dp,
                        enabled = meetsMinPlayers
                    )
                    if (!meetsMinPlayers) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.team_needs_5_players_to_post_scrims),
                            color = WarningOrange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp)
                )
            }
        }
    }

    // ── Team Picker Dialog ──────────────────────────────────────
    if (showTeamPicker) {
        AlertDialog(
            onDismissRequest = { showTeamPicker = false },
            containerColor = DarkNavy,
            title = {
                Text(stringResource(R.string.select_team), color = White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    teams.forEachIndexed { index, team ->
                        val isSelected = index == selectedTeamIndex
                        val hasMinPlayers = team.meetsMinPlayers
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) BluePrimary.copy(alpha = 0.15f) else White.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                selectedTeamIndex = index
                                showTeamPicker = false
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(BluePrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        team.name.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                        color = BluePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        team.name,
                                        color = White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.People, null,
                                            tint = if (hasMinPlayers) SuccessGreen else WarningOrange,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${team.currentPlayerCount}/5 players",
                                            color = if (hasMinPlayers) SuccessGreen else WarningOrange,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTeamPicker = false }) {
                    Text(stringResource(R.string.cancel), color = LightGray)
                }
            }
        )
    }

    // ── Min Player Warning Dialog ─────────────────────────────────
    if (showMinPlayerDialog) {
        AlertDialog(
            onDismissRequest = { showMinPlayerDialog = false },
            containerColor = DarkNavy,
            icon = {
                Icon(Icons.Default.Warning, null, tint = WarningOrange, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(stringResource(R.string.not_enough_players), color = White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Your team \"$teamName\" has only $currentPlayerCount out of 5 required players.",
                        color = LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.need_5_players_to_post),
                        color = MidGray,
                        fontSize = 13.sp
                    )
                    // Show other teams that DO have 5 players
                    val eligibleTeams = teams.filter { it.meetsMinPlayers && it.id != teamId }
                    if (eligibleTeams.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.switch_to_team_with_5_plus),
                            color = BluePrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        eligibleTeams.forEach { team ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp),
                                onClick = {
                                    selectedTeamIndex = teams.indexOf(team)
                                    showMinPlayerDialog = false
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(team.name, color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(stringResource(R.string.players_count_short, team.currentPlayerCount), color = SuccessGreen, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMinPlayerDialog = false }) {
                    Text(stringResource(R.string.ok), color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
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
