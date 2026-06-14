package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.scrimslegends.app.data.model.Player
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
        description: String,
        currentPlayers: Int,
        selectedPlayerIds: List<String>
    ) -> Unit,
    isLoading: Boolean = false
) {
    // P0-4: Guard against empty teams list — show error state instead of crash
    if (teams.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = heroGradientBrush()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_team_warning),
                    style = iOSTitle3.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.create_team_first_hint),
                    style = iOSCallout.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                GradientButton(
                    text = stringResource(R.string.go_back),
                    onClick = onNavigateBack,
                    gradient = PremiumBlueGradient,
                    height = 48.dp
                )
            }
        }
        return
    }

    // Team selection state
    var selectedTeamIndex by remember { mutableIntStateOf(0) }
    val selectedTeam = teams.getOrElse(selectedTeamIndex) { teams.firstOrNull() }
    val teamName = selectedTeam?.name ?: stringResource(R.string.my_team_default)
    val teamId = selectedTeam?.id ?: ""

    // Player selection state — pre-select up to 5 team members as active by default
    var selectedPlayerIds by remember(selectedTeam) {
        mutableStateOf(selectedTeam?.players?.take(5)?.map { it.id }?.toSet() ?: emptySet())
    }
    var showPlayerSelectionDialog by remember { mutableStateOf(false) }
    // Active players = first 5 selected (MLBB 5v5). Extras are substitutes.
    val activePlayerCount = selectedPlayerIds.size.coerceAtMost(5)

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

    // Helper: get current hour/minute in a given region's timezone
    fun getRegionTime(region: Region): Pair<Int, Int> {
        val tz = java.util.TimeZone.getTimeZone(region.timeZoneId)
        val cal = java.util.Calendar.getInstance(tz)
        return Pair(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
    }

    var selectedHour by remember { mutableIntStateOf(getRegionTime(Region.EU).first) }
    var selectedMinute by remember { mutableIntStateOf(getRegionTime(Region.EU).second) }

    // When region changes, update time to current time in that region
    LaunchedEffect(selectedRegion) {
        val (hour, minute) = getRegionTime(selectedRegion)
        selectedHour = hour
        selectedMinute = minute
    }

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
                        style = iOSTitle3.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            // ── Scrollable Content ──────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {
                // ── Team Card (compact) ──────────────────────────
                AnimatedEntrance(delayMillis = 80) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = teamName.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.posting_as),
                                style = iOSCaption1.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = teamName,
                                    style = iOSHeadline.copy(color = MaterialTheme.colorScheme.onSurface),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (teams.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.SwapHoriz,
                                        contentDescription = stringResource(R.string.content_desc_switch_team),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        // Player count badge
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (activePlayerCount >= 5) SuccessGreen.copy(alpha = 0.15f) else WarningOrange.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.People, null,
                                    tint = if (activePlayerCount >= 5) SuccessGreen else WarningOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$activePlayerCount/5",
                                    style = iOSCaption2.copy(
                                        color = if (activePlayerCount >= 5) SuccessGreen else WarningOrange,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Player Selection Card ────────────────────────
                AnimatedEntrance(delayMillis = 100) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { showPlayerSelectionDialog = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.HowToReg, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.select_roster),
                                        style = iOSHeadline.copy(color = MaterialTheme.colorScheme.onSurface)
                                    )
                                    Text(
                                        text = if (activePlayerCount >= 5) {
                                            val subs = selectedPlayerIds.size - 5
                                            if (subs > 0) "5 active + $subs sub" else "5 active players"
                                        } else
                                            "Need at least 5 — $activePlayerCount selected",
                                        style = iOSCaption1.copy(
                                            color = if (activePlayerCount >= 5) SuccessGreen else WarningOrange
                                        )
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Show mini avatars of selected players
                        if (selectedTeam != null && selectedPlayerIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                selectedTeam.players.filter { it.id in selectedPlayerIds }.take(7).forEach { player ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = player.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (activePlayerCount > 7) {
                                    Text(
                                        "+${activePlayerCount - 7}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                AnimatedEntrance(delayMillis = 120) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
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
                                    selectedColor = MaterialTheme.colorScheme.primary
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
                                    selectedColor = MaterialTheme.colorScheme.secondary
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
                                    selectedColor = MaterialTheme.colorScheme.secondary
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
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                                DropdownMenu(
                                    expanded = monthExpanded,
                                    onDismissRequest = { monthExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                                ) {
                                    months.forEachIndexed { index, month ->
                                        DropdownMenuItem(
                                            text = { Text(month, color = MaterialTheme.colorScheme.onSurface) },
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
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                                DropdownMenu(
                                    expanded = dayExpanded,
                                    onDismissRequest = { dayExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                                ) {
                                    (1..maxDay).forEach { day ->
                                        DropdownMenuItem(
                                            text = { Text(day.toString(), color = MaterialTheme.colorScheme.onSurface) },
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
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                                DropdownMenu(
                                    expanded = yearExpanded,
                                    onDismissRequest = { yearExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                                ) {
                                    availableYears.forEachIndexed { index, year ->
                                        DropdownMenuItem(
                                            text = { Text(year.toString(), color = MaterialTheme.colorScheme.onSurface) },
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
                                    selectedColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = hourExpanded,
                                    onDismissRequest = { hourExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                                ) {
                                    (0..23).forEach { hour ->
                                        DropdownMenuItem(
                                            text = { Text(String.format("%02d", hour), color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = { selectedHour = hour; hourExpanded = false }
                                        )
                                    }
                                }
                            }

                            Text(":", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            // Minute
                            var minuteExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                CompactChip(
                                    text = String.format("%02d", selectedMinute),
                                    selected = true,
                                    onClick = { minuteExpanded = true },
                                    selectedColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = minuteExpanded,
                                    onDismissRequest = { minuteExpanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                                ) {
                                    listOf(0, 15, 30, 45).forEach { minute ->
                                        DropdownMenuItem(
                                            text = { Text(String.format("%02d", minute), color = MaterialTheme.colorScheme.onSurface) },
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedRegion.utcOffset,
                                    style = iOSCallout.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Summary line
                        Text(
                            text = "${months[selectedMonth]} $selectedDay, $selectedYear at ${String.format("%02d", selectedHour)}:${String.format("%02d", selectedMinute)} ${selectedRegion.utcOffset}",
                            style = iOSCaption1.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        FormSectionLabel(stringResource(R.string.description_optional))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text(stringResource(R.string.add_scrim_details_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                cursorColor = MaterialTheme.colorScheme.secondary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
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
                if (activePlayerCount < 5) {
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
                            text = "$activePlayerCount/5 players selected — need at least 5 to post.",
                            style = iOSCaption1.copy(color = WarningOrange)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Post Button ──────────────────────────────────
                // Compute scheduled time in the selected region's local timezone
                val scheduledTime = remember(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, selectedRegion) {
                    val tz = java.util.TimeZone.getTimeZone(selectedRegion.timeZoneId)
                    val cal = java.util.Calendar.getInstance(tz)
                    cal.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                val now = System.currentTimeMillis()
                val timeErrorRes = when {
                    scheduledTime <= now -> R.string.scrim_time_past
                    scheduledTime < now + 30 * 60 * 1000L -> R.string.scrim_time_min_advance
                    scheduledTime > now + 30L * 24 * 60 * 60 * 1000L -> R.string.scrim_time_max_advance
                    else -> 0
                }
                val canPost = activePlayerCount >= 5 && timeErrorRes == 0 && !isLoading
                val timeErrorText = if (timeErrorRes != 0) stringResource(timeErrorRes) else ""

                AnimatedEntrance(delayMillis = 280) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    } else {
                        GradientButton(
                            text = stringResource(R.string.post_scrim),
                            onClick = {
                                if (activePlayerCount < 5) {
                                    showMinPlayerDialog = true
                                } else if (timeErrorRes != 0) {
                                    errorMessage = timeErrorText
                                } else {
                                    errorMessage = ""
                                    onCreateScrim(
                                        teamId, teamName, selectedGameMode, selectedRegion,
                                        selectedSkillLevel, selectedBestOf, scheduledTime, description,
                                        activePlayerCount,
                                        selectedPlayerIds.toList()
                                    )
                                }
                            },
                            gradient = PremiumBlueGradient,
                            height = 52.dp,
                            enabled = canPost
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ── Team Picker Dialog ──────────────────────────────────────
    if (showTeamPicker) {
        AlertDialog(
            onDismissRequest = { showTeamPicker = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(stringResource(R.string.select_team), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    teams.forEachIndexed { index, team ->
                        val isSelected = index == selectedTeamIndex
                        val hasMinPlayers = team.meetsMinPlayers
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
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
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        team.name.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        team.name,
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTeamPicker = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
            }
        )
    }

    // ── Min Player Warning Dialog ─────────────────────────────────
    if (showMinPlayerDialog) {
        AlertDialog(
            onDismissRequest = { showMinPlayerDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            icon = {
                Icon(Icons.Default.Warning, null, tint = WarningOrange, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(stringResource(R.string.not_enough_players), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "You've selected $activePlayerCount out of 5 required players.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.need_5_players_to_post),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    val eligibleTeams = teams.filter { it.meetsMinPlayers && it.id != teamId }
                    if (eligibleTeams.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.switch_to_team_with_5_plus),
                            color = MaterialTheme.colorScheme.primary,
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
                                    Text(team.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                    Text(stringResource(R.string.ok), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── Player Selection Dialog ─────────────────────────────────
    if (showPlayerSelectionDialog && selectedTeam != null) {
        val teamPlayers = selectedTeam.players
        AlertDialog(
            onDismissRequest = { showPlayerSelectionDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Column {
                    Text(stringResource(R.string.select_roster), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People, null,
                            tint = if (activePlayerCount >= 5) SuccessGreen else WarningOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activePlayerCount >= 5) {
                                val subs = selectedPlayerIds.size - 5
                                if (subs > 0) "5 active + $subs sub" else "5/5 active"
                            } else "$activePlayerCount/5 active",
                            fontSize = 13.sp,
                            color = if (activePlayerCount >= 5) SuccessGreen else WarningOrange
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (activePlayerCount > 5) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(WarningOrange.copy(alpha = 0.10f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, tint = WarningOrange, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Only 5 play per game. Extra players will be substitutes.",
                                fontSize = 12.sp, color = WarningOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    // Select All / Deselect All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { selectedPlayerIds = teamPlayers.map { it.id }.toSet() }
                        ) {
                            Text("Select All", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(
                            onClick = { selectedPlayerIds = emptySet() }
                        ) {
                            Text("Deselect All", color = ErrorRed.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Player list
                    teamPlayers.forEach { player ->
                        val isSelected = player.id in selectedPlayerIds
                        val roleLabel = when (player.role.name) {
                            "LEADER" -> "CPT"
                            "CO_LEADER" -> "CO"
                            else -> null
                        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(10.dp),
            onClick = {
                selectedPlayerIds = if (isSelected) {
                    selectedPlayerIds - player.id
                } else {
                    selectedPlayerIds + player.id
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox indicator
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .then(
                            if (isSelected) Modifier.background(Brush.horizontalGradient(PremiumBlueGradient))
                            else Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        )
                        .then(
                            if (!isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check, null,
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                // Player initial
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = player.name,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
                // Role badge
                if (roleLabel != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (roleLabel == "CPT") MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else Purple.copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = roleLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (roleLabel == "CPT") MaterialTheme.colorScheme.secondary else Purple
                        )
                    }
                }
            }
        }
                    }
                }
            },
            confirmButton = {
                GradientButton(
                    text = "Done",
                    onClick = { showPlayerSelectionDialog = false },
                    gradient = PremiumBlueGradient,
                    height = 40.dp,
                    enabled = true
                )
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
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = iOSCallout.copy(
                color = if (selected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
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
                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = iOSHeadline.copy(color = MaterialTheme.colorScheme.onSurface)
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}
