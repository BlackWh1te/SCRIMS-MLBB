package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.*
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.*
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentCreateScreen(
    isLoading: Boolean,
    error: String? = null,
    onCreate: (Tournament, List<TournamentRequirement>, android.net.Uri?) -> Unit = { _, _, _ -> },
    onNavigateBack: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var prizeType by remember { mutableStateOf(PrizeType.OTHER) }
    var prizeDescription by remember { mutableStateOf("") }
    var maxTeams by remember { mutableStateOf("16") }
    var minTeamSize by remember { mutableStateOf("5") }
    var bestOf by remember { mutableStateOf("1") }
    var region by remember { mutableStateOf("EU") }
    var skillLevel by remember { mutableStateOf("ALL") }
    var swissRounds by remember { mutableStateOf("") }
    var registrationDeadline by remember { mutableStateOf("") }
    var checkInDeadline by remember { mutableStateOf("") }
    var pendingRegDateMillis by remember { mutableLongStateOf(0L) }
    var isLiveStreamEnabled by remember { mutableStateOf(false) }

    // ── Logo state ──
    var selectedLogoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val logoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? -> selectedLogoUri = uri }

    // ── Requirements state ──
    var requirements by remember { mutableStateOf(listOf<TournamentRequirement>()) }
    var showAddReqDialog by remember { mutableStateOf(false) }
    var newReqType by remember { mutableStateOf(RequirementType.CUSTOM) }
    var newReqLabel by remember { mutableStateOf("") }
    var newReqUrl by remember { mutableStateOf("") }
    
    var validationError by remember { mutableStateOf<String?>(null) }

    // Date/time picker states — registration
    val regDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000
    )
    val regTimePickerState = rememberTimePickerState(
        initialHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = 0
    )
    var showRegDatePicker by remember { mutableStateOf(false) }
    var showRegTimePicker by remember { mutableStateOf(false) }

    // Date/time picker states — check-in
    val checkInDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000
    )
    val checkInTimePickerState = rememberTimePickerState(
        initialHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = 0
    )
    var pendingCheckInDateMillis by remember { mutableLongStateOf(0L) }
    var showCheckInDatePicker by remember { mutableStateOf(false) }
    var showCheckInTimePicker by remember { mutableStateOf(false) }

    val prizeTypes = PrizeType.entries
    val regions = listOf("EU", "NA", "SA", "ASIA", "MSK", "EKB", "KRD", "ALL")
    val skillLevels = listOf("ALL", "BEGINNER", "INTERMEDIATE", "ADVANCED", "PRO")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.tournament_create),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }

            // ── Form ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Column {
                    Label(text = stringResource(R.string.tournament_create_title))
                    Spacer(modifier = Modifier.height(8.dp))
                    StyledInput(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "Swiss Championship"
                    )
                }

                // Description
                Column {
                    Label(text = stringResource(R.string.tournament_create_description))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        placeholder = { Text("Describe your tournament…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors()
                    )
                }

                // Logo (optional)
                Column {
                    Label(text = "Tournament Logo (optional)")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Preview or placeholder
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .border(1.dp, if (selectedLogoUri != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .clickable { logoPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedLogoUri != null) {
                                coil.compose.AsyncImage(
                                    model = selectedLogoUri,
                                    contentDescription = "Tournament logo",
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add logo", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.size(28.dp))
                            }
                        }
                        Column {
                            Text(
                                if (selectedLogoUri != null) "Logo selected" else "Tap to choose a logo",
                                style = MaterialTheme.typography.bodySmall.copy(color = if (selectedLogoUri != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            if (selectedLogoUri != null) {
                                Text(
                                    "Tap to change",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                                    modifier = Modifier.clickable { logoPicker.launch("image/*") }
                                )
                            }
                        }
                    }
                }

                // Prize Type
                Column {
                    Label(text = stringResource(R.string.tournament_create_prize_type))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        prizeTypes.forEach { pt ->
                            FilterChip(
                                selected = prizeType == pt,
                                onClick = { prizeType = pt },
                                label = {
                                    Text(
                                        pt.value.replace("_", " ").uppercase(),
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.height(32.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.secondary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }

                // Prize Description
                Column {
                    Label(text = stringResource(R.string.tournament_create_prize_desc))
                    Spacer(modifier = Modifier.height(8.dp))
                    StyledInput(
                        value = prizeDescription,
                        onValueChange = { prizeDescription = it },
                        placeholder = "$50 USD / 500 Diamonds"
                    )
                }

                // Numeric fields row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Label(text = stringResource(R.string.tournament_max_teams))
                        Spacer(modifier = Modifier.height(8.dp))
                        StyledInput(
                            value = maxTeams,
                            onValueChange = { if (it.all { c -> c.isDigit() }) maxTeams = it },
                            placeholder = "16",
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Label(text = stringResource(R.string.tournament_min_team_size))
                        Spacer(modifier = Modifier.height(8.dp))
                        StyledInput(
                            value = minTeamSize,
                            onValueChange = { if (it.all { c -> c.isDigit() }) minTeamSize = it },
                            placeholder = "5",
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Label(text = stringResource(R.string.tournament_best_of))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("1", "2").forEach { bo ->
                                FilterChip(
                                    selected = bestOf == bo,
                                    onClick = { bestOf = bo },
                                    label = { Text("BO$bo", fontSize = 12.sp) },
                                    modifier = Modifier.height(32.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.secondary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Region
                Column {
                    Label(text = stringResource(R.string.tournament_region))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        regions.forEach { r ->
                            FilterChip(
                                selected = region == r,
                                onClick = { region = r },
                                label = { Text(r, fontSize = 10.sp) },
                                modifier = Modifier.height(32.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }

                // Skill Level
                Column {
                    Label(text = stringResource(R.string.tournament_skill_level))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        skillLevels.forEach { sl ->
                            FilterChip(
                                selected = skillLevel == sl,
                                onClick = { skillLevel = sl },
                                label = { Text(sl, fontSize = 10.sp) },
                                modifier = Modifier.height(32.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurplePrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = PurplePrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }

                // Swiss Rounds (optional)
                Column {
                    Label(text = "Swiss Rounds (optional)")
                    Spacer(modifier = Modifier.height(8.dp))
                    StyledInput(
                        value = swissRounds,
                        onValueChange = { if (it.all { c -> c.isDigit() }) swissRounds = it },
                        placeholder = "Auto-calculated if empty",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                }

                // Registration Deadline
                Column {
                    Label(text = stringResource(R.string.tournament_registration_deadline))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = registrationDeadline,
                            onValueChange = {},
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("24h from now (default)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp) },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp).clickable { showRegDatePicker = true }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = inputColors(),
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        )
                    }
                    if (registrationDeadline.isNotEmpty()) {
                        Text(
                            text = registrationDeadline,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Check-In Deadline
                Column {
                    Label(text = stringResource(R.string.tournament_check_in_deadline))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = checkInDeadline,
                            onValueChange = {},
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Pick date & time", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp) },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp).clickable { showCheckInDatePicker = true }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = inputColors(),
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        )
                    }
                    if (checkInDeadline.isNotEmpty()) {
                        Text(
                            text = checkInDeadline,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Live Stream toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (isLiveStreamEnabled) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.tournament_livestream),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isLiveStreamEnabled) White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Switch(
                        checked = isLiveStreamEnabled,
                        onCheckedChange = { isLiveStreamEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.secondary,
                            checkedThumbColor = White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }

                // ── Requirements editor ──────────────────────────────────
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Label(text = "Entry Requirements (optional)")
                            Text(
                                text = "Up to 5 requirements players must complete before applying",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            )
                        }
                        if (requirements.size < 5) {
                            IconButton(onClick = {
                                newReqType = RequirementType.CUSTOM
                                newReqLabel = ""
                                newReqUrl = ""
                                showAddReqDialog = true
                            }) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Add requirement", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                    if (requirements.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        requirements.forEachIndexed { index, req ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when (req.type) {
                                        RequirementType.TELEGRAM_SUBSCRIBE -> Icons.Default.Chat
                                        RequirementType.YOUTUBE_SUBSCRIBE  -> Icons.Default.PlayCircle
                                        else                               -> Icons.Default.CheckCircle
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = req.label,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { requirements = requirements.toMutableList().also { it.removeAt(index) } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Create button
                iOSPrimaryButton(
                    text = stringResource(R.string.tournament_create_submit),
                    onClick = {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        val regDeadlineTs = try {
                            if (registrationDeadline.isNotEmpty()) sdf.parse(registrationDeadline)?.time ?: 0L else 0L
                        } catch (_: Exception) { 0L }
                        val checkInDeadlineTs = try {
                            if (checkInDeadline.isNotEmpty()) sdf.parse(checkInDeadline)?.time ?: 0L else 0L
                        } catch (_: Exception) { 0L }
                        val maxTeamsVal = maxTeams.toIntOrNull() ?: 16
                        val minTeamSizeVal = minTeamSize.toIntOrNull() ?: 5
                        val bestOfVal = bestOf.toIntOrNull() ?: 1
                        if (maxTeamsVal < 4 || maxTeamsVal > 64) {
                            validationError = "Max teams must be between 4 and 64"
                            return@iOSPrimaryButton
                        }
                        if (minTeamSizeVal < 3 || minTeamSizeVal > 7) {
                            validationError = "Min team size must be between 3 and 7"
                            return@iOSPrimaryButton
                        }
                        if (bestOfVal !in listOf(1, 2)) return@iOSPrimaryButton
                        
                        validationError = null
                        val tournament = Tournament(
                            title = title.trim(),
                            description = description.trim(),
                            prizeType = prizeType,
                            prizeDescription = prizeDescription.trim().ifBlank { null },
                            maxTeams = maxTeamsVal,
                            minTeamSize = minTeamSizeVal,
                            bestOf = bestOfVal,
                            region = region,
                            skillLevel = skillLevel,
                            swissRounds = swissRounds.toIntOrNull(),
                            registrationDeadline = regDeadlineTs,
                            checkInDeadline = checkInDeadlineTs,
                            isLiveStreamEnabled = isLiveStreamEnabled,
                            status = TournamentStatus.REGISTRATION
                        )
                        onCreate(tournament, requirements, selectedLogoUri)
                    },
                    enabled = title.isNotBlank() && description.isNotBlank() && !isLoading,
                    isLoading = isLoading,
                    backgroundColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.background
                )

                // ── Add Requirement dialog ──
                if (showAddReqDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddReqDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Add Requirement", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Type selector
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RequirementType.entries.forEach { t ->
                                        FilterChip(
                                            selected = newReqType == t,
                                            onClick  = { newReqType = t },
                                            label    = {
                                                Text(
                                                    when (t) {
                                                        RequirementType.TELEGRAM_SUBSCRIBE -> "Telegram"
                                                        RequirementType.YOUTUBE_SUBSCRIBE  -> "YouTube"
                                                        else -> "Custom"
                                                    },
                                                    fontSize = 11.sp
                                                )
                                            },
                                            modifier = Modifier.height(30.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                                selectedLabelColor = MaterialTheme.colorScheme.secondary,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = newReqLabel,
                                    onValueChange = { newReqLabel = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Label (e.g. Subscribe to our channel)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = inputColors()
                                )
                                OutlinedTextField(
                                    value = newReqUrl,
                                    onValueChange = { newReqUrl = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("URL (optional)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = inputColors()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (newReqLabel.isNotBlank()) {
                                        requirements = requirements + TournamentRequirement(
                                            type  = newReqType,
                                            label = newReqLabel.trim(),
                                            url   = newReqUrl.trim().ifBlank { null }
                                        )
                                        showAddReqDialog = false
                                    }
                                }
                            ) { Text("Add", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddReqDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    )
                }
            }
        }

        // Registration Date Picker Dialog
        if (showRegDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showRegDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        regDatePickerState.selectedDateMillis?.let { dateMillis ->
                            // Store the date part, then show time picker
                            showRegDatePicker = false
                            showRegTimePicker = true
                            // Temporarily store date millis for combining with time
                            pendingRegDateMillis = dateMillis
                        }
                    }) { Text("Next", color = MaterialTheme.colorScheme.secondary) }
                },
                dismissButton = {
                    TextButton(onClick = { showRegDatePicker = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            ) {
                DatePicker(state = regDatePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        headlineContentColor = White,
                        navigationContentColor = MaterialTheme.colorScheme.secondary,
                        yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        currentYearContentColor = MaterialTheme.colorScheme.secondary,
                        selectedDayContentColor = MaterialTheme.colorScheme.background,
                        selectedDayContainerColor = MaterialTheme.colorScheme.secondary,
                        dayContentColor = White,
                        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Registration Time Picker Dialog
        if (showRegTimePicker) {
            AlertDialog(
                onDismissRequest = { showRegTimePicker = false },
                containerColor = MaterialTheme.colorScheme.background,
                confirmButton = {
                    TextButton(onClick = {
                        val cal = java.util.Calendar.getInstance().apply {
                            timeInMillis = pendingRegDateMillis ?: System.currentTimeMillis()
                            set(java.util.Calendar.HOUR_OF_DAY, regTimePickerState.hour)
                            set(java.util.Calendar.MINUTE, regTimePickerState.minute)
                            set(java.util.Calendar.SECOND, 0)
                        }
                        registrationDeadline = formatTimestamp(cal.timeInMillis)
                        showRegTimePicker = false
                    }) { Text("OK", color = MaterialTheme.colorScheme.secondary) }
                },
                dismissButton = {
                    TextButton(onClick = { showRegTimePicker = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Select Time", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        TimePicker(state = regTimePickerState)
                    }
                }
            )
        }

        // Check-In Date Picker Dialog
        if (showCheckInDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showCheckInDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        checkInDatePickerState.selectedDateMillis?.let { dateMillis ->
                            showCheckInDatePicker = false
                            showCheckInTimePicker = true
                            pendingCheckInDateMillis = dateMillis
                        }
                    }) { Text("Next", color = MaterialTheme.colorScheme.secondary) }
                },
                dismissButton = {
                    TextButton(onClick = { showCheckInDatePicker = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            ) {
                DatePicker(state = checkInDatePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        headlineContentColor = White,
                        navigationContentColor = MaterialTheme.colorScheme.secondary,
                        yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        currentYearContentColor = MaterialTheme.colorScheme.secondary,
                        selectedDayContentColor = MaterialTheme.colorScheme.background,
                        selectedDayContainerColor = MaterialTheme.colorScheme.secondary,
                        dayContentColor = White,
                        weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // Check-In Time Picker Dialog
        if (showCheckInTimePicker) {
            AlertDialog(
                onDismissRequest = { showCheckInTimePicker = false },
                containerColor = MaterialTheme.colorScheme.background,
                confirmButton = {
                    TextButton(onClick = {
                        val cal = java.util.Calendar.getInstance().apply {
                            timeInMillis = pendingCheckInDateMillis ?: System.currentTimeMillis()
                            set(java.util.Calendar.HOUR_OF_DAY, checkInTimePickerState.hour)
                            set(java.util.Calendar.MINUTE, checkInTimePickerState.minute)
                            set(java.util.Calendar.SECOND, 0)
                        }
                        checkInDeadline = formatTimestamp(cal.timeInMillis)
                        showCheckInTimePicker = false
                    }) { Text("OK", color = MaterialTheme.colorScheme.secondary) }
                },
                dismissButton = {
                    TextButton(onClick = { showCheckInTimePicker = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Select Check-In Time", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        TimePicker(state = checkInTimePickerState)
                    }
                }
            )
        }

        // Error
        error?.let {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = ErrorRed,
                action = { TextButton(onClick = onDismissError) { Text("OK", color = MaterialTheme.colorScheme.onSurface) } }
            ) { Text(it, color = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

@Composable
fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold
        )
    )
}

@Composable
fun StyledInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = inputColors(),
        isError = isError
    )
}

@Composable
fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.secondary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    cursorColor = MaterialTheme.colorScheme.secondary,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

private fun formatTimestamp(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}
