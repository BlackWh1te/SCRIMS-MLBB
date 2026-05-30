package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.BestOf
import com.scrimslegends.app.data.model.GameMode
import com.scrimslegends.app.data.model.Region
import com.scrimslegends.app.data.model.SkillLevel
import com.scrimslegends.app.data.model.Team
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.GradientButton
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

    // Date/time picker state
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
    val availableYears = listOf(currentYear, currentYear + 1)
    var selectedYearIndex by remember { mutableIntStateOf(0) }
    val selectedYear = availableYears.getOrElse(selectedYearIndex) { currentYear }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    var selectedDay by remember { mutableIntStateOf(currentDay) }
    var selectedHour by remember { mutableIntStateOf(18) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val isLeapYear = (selectedYear % 4 == 0 && selectedYear % 100 != 0) || (selectedYear % 400 == 0)
    val daysInMonth = listOf(31, if (isLeapYear) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val maxDay = daysInMonth.getOrElse(selectedMonth) { 31 }
    if (selectedDay > maxDay) selectedDay = maxDay

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Header ──────────────────────────────────────────
            AnimatedEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(top = 8.dp),
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
                        style = iOSTitle3.copy(color = White, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            // ── Scrollable Content ──────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {
                // ── Team Card (compact) ──────────────────────────
                AnimatedEntrance(delayMillis = 80) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .then(
                                if (teams.size > 1) Modifier.clickable { showTeamPicker = true }
                                else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    brush = Brush.verticalGradient(colors = listOf(BluePrimary, Color(0xFF0A5A9F))),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = teamName.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.posting_as),
                                style = iOSCaption1.copy(color = TextSecondary)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = teamName,
                                    style = iOSHeadline.copy(color = TextPrimary),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (teams.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.SwapHoriz,
                                        contentDescription = stringResource(R.string.content_desc_switch_team),
                                        tint = BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        // Player count badge
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (meetsMinPlayers) SuccessGreen.copy(alpha = 0.15f) else WarningOrange.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.People, null,
                                    tint = if (meetsMinPlayers) SuccessGreen else WarningOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$currentPlayerCount/5",
                                    style = iOSCaption2.copy(
                                        color = if (meetsMinPlayers) SuccessGreen else WarningOrange,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Single compact form card ─────────────────────
                AnimatedEntrance(delayMillis = 120) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceCard)
                            .padding(16.dp)
                    ) {

                        // ── Game Mode ────────────────────────────
                        FormSectionLabel(stringResource(R.string.game_mode))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GameMode.selectable.forEach { mode ->
                                CompactChip(
                                    text = mode.displayName,
                                    selected = selectedGameMode == mode,
                                    onClick = { selectedGameMode = mode },
                                    selectedColor = BluePrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Region ───────────────────────────────
                        FormSectionLabel(stringResource(R.string.region))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Region.values().forEach { region ->
                                CompactChip(
                                    text = region.displayName,
                                    selected = selectedRegion == region,
                                    onClick = { selectedRegion = region },
                                    selectedColor = GoldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Skill Level ──────────────────────────
                        FormSectionLabel(stringResource(R.string.skill_level))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SkillLevel.values().forEach { level ->
                                CompactChip(
                                    text = level.name,
                                    selected = selectedSkillLevel == level,
                                    onClick = { selectedSkillLevel = level },
                                    selectedColor = Purple
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Best Of ──────────────────────────────
                        FormSectionLabel(stringResource(R.string.number_of_games))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BestOf.values().forEach { bestOf ->
                                CompactChip(
                                    text = stringResource(R.string.bo_format, bestOf.games),
                                    selected = selectedBestOf == bestOf,
                                    onClick = { selectedBestOf = bestOf },
                                    selectedColor = GoldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Date & Time ───────────────────────────
                        FormSectionLabel(stringResource(R.string.date_and_time))

                        // Date: scrollable month/day/year chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Month
                            var monthExpanded by remember { mutableStateOf(false) }
                            Box {
                                CompactChip(
                                    text = months[selectedMonth],
                                    selected = true,
                                    onClick = { monthExpanded = true },
                                    selectedColor = BluePrimary
                                )
                                DropdownMenu(
                                    expanded = monthExpanded,
                                    onDismissRequest = { monthExpanded = false },
                                    modifier = Modifier.background(DarkNavy)
                                ) {
                                    months.forEachIndexed { index, month ->
                                        DropdownMenuItem(
                                            text = { Text(month, color = White) },
                                            onClick = { selectedMonth = index; monthExpanded = false }
                                        )
                                    }
                                }
                            }

                            // Day
                            var dayExpanded by remember { mutableStateOf(false) }
                            Box {
                                CompactChip(
                                    text = selectedDay.toString(),
                                    selected = true,
                                    onClick = { dayExpanded = true },
                                    selectedColor = BluePrimary
                                )
                                DropdownMenu(
                                    expanded = dayExpanded,
                                    onDismissRequest = { dayExpanded = false },
                                    modifier = Modifier.background(DarkNavy)
                                ) {
                                    (1..maxDay).forEach { day ->
                                        DropdownMenuItem(
                                            text = { Text(day.toString(), color = White) },
                                            onClick = { selectedDay = day; dayExpanded = false }
                                        )
                                    }
                                }
                            }

                            // Year
                            var yearExpanded by remember { mutableStateOf(false) }
                            Box {
                                CompactChip(
                                    text = selectedYear.toString(),
                                    selected = true,
                                    onClick = { yearExpanded = true },
                                    selectedColor = BluePrimary
                                )
                                DropdownMenu(
                                    expanded = yearExpanded,
                                    onDismissRequest = { yearExpanded = false },
                                    modifier = Modifier.background(DarkNavy)
                                ) {
                                    availableYears.forEachIndexed { index, year ->
                                        DropdownMenuItem(
                                            text = { Text(year.toString(), color = White) },
                                            onClick = { selectedYearIndex = index; yearExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Time row: Hour / Minute / Timezone
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour
                            var hourExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                CompactChip(
                                    text = String.format("%02d", selectedHour) + "h",
                                    selected = true,
                                    onClick = { hourExpanded = true },
                                    selectedColor = GoldPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = hourExpanded,
                                    onDismissRequest = { hourExpanded = false },
                                    modifier = Modifier.background(DarkNavy)
                                ) {
                                    (0..23).forEach { hour ->
                                        DropdownMenuItem(
                                            text = { Text(String.format("%02d", hour), color = White) },
                                            onClick = { selectedHour = hour; hourExpanded = false }
                                        )
                                    }
                                }
                            }

                            Text(":", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            // Minute
                            var minuteExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                CompactChip(
                                    text = String.format("%02d", selectedMinute),
                                    selected = true,
                                    onClick = { minuteExpanded = true },
                                    selectedColor = GoldPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = minuteExpanded,
                                    onDismissRequest = { minuteExpanded = false },
                                    modifier = Modifier.background(DarkNavy)
                                ) {
                                    listOf(0, 15, 30, 45).forEach { minute ->
                                        DropdownMenuItem(
                                            text = { Text(String.format("%02d", minute), color = White) },
                                            onClick = { selectedMinute = minute; minuteExpanded = false }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Timezone badge
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BluePrimary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedRegion.utcOffset,
                                    style = iOSCallout.copy(color = BluePrimary, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Summary line
                        Text(
                            text = "${months[selectedMonth]} $selectedDay, $selectedYear at ${String.format("%02d", selectedHour)}:${String.format("%02d", selectedMinute)} ${selectedRegion.utcOffset}",
                            style = iOSCaption1.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Description card ────────────────────────────
                AnimatedEntrance(delayMillis = 200) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceCard)
                            .padding(16.dp)
                    ) {
                        FormSectionLabel(stringResource(R.string.description_optional))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text(stringResource(R.string.add_scrim_details_hint), color = MidGray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = White.copy(alpha = 0.3f),
                                cursorColor = GoldPrimary,
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedContainerColor = White.copy(alpha = 0.08f),
                                unfocusedContainerColor = White.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Error Message ────────────────────────────────
                AnimatedVisibility(
                    visible = errorMessage.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // ── Team size warning ────────────────────────────
                if (currentPlayerCount < 5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarningOrange.copy(alpha = 0.12f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning, null,
                            tint = WarningOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "$currentPlayerCount/5 players — need at least 5 to post.",
                            style = iOSCaption1.copy(color = WarningOrange)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Post Button ──────────────────────────────────
                AnimatedEntrance(delayMillis = 280) {
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
                                    teamId, teamName, selectedGameMode, selectedRegion,
                                    selectedSkillLevel, selectedBestOf, scheduledTime, description
                                )
                            }
                        },
                        gradient = GoldGradient,
                        height = 52.dp,
                        enabled = meetsMinPlayers
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
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

// ── Compact chip for selections ──────────────────────────────────

@Composable
private fun CompactChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) selectedColor.copy(alpha = 0.18f)
                else White.copy(alpha = 0.08f)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = iOSCallout.copy(
                color = if (selected) selectedColor else TextSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        )
    }
}

// ── Form section label ────────────────────────────────────────────

@Composable
private fun FormSectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(GoldPrimary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = iOSHeadline.copy(color = TextPrimary)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}
