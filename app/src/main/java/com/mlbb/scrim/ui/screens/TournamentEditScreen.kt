package com.mlbb.scrim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
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
import com.mlbb.scrim.data.model.*
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentEditScreen(
    tournament: Tournament,
    existingRequirements: List<TournamentRequirement> = emptyList(),
    isLoading: Boolean,
    error: String? = null,
    onSave: (String, Map<String, Any?>) -> Unit = { _, _ -> },
    onSaveRequirements: (String, List<TournamentRequirement>) -> Unit = { _, _ -> },
    onUploadLogo: (String, android.net.Uri) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit,
    onDismissError: () -> Unit = {}
) {
    // Editable fields
    var title by remember { mutableStateOf(tournament.title) }
    var description by remember { mutableStateOf(tournament.description) }
    var prizeDescription by remember { mutableStateOf(tournament.prizeDescription ?: "") }
    var registrationDeadline by remember { mutableStateOf(formatTimestamp(tournament.registrationDeadline)) }
    var checkInDeadline by remember { mutableStateOf(formatTimestamp(tournament.checkInDeadline)) }
    var isLiveStreamEnabled by remember { mutableStateOf(tournament.isLiveStreamEnabled) }
    var swissRounds by remember { mutableStateOf(tournament.swissRounds?.toString() ?: "") }

    // ── Logo state ──
    var selectedLogoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val logoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? -> selectedLogoUri = uri; if (uri != null) onUploadLogo(tournament.id, uri) }

    // ── Requirements state ──
    var pendingRequirements by remember(existingRequirements) { mutableStateOf(existingRequirements) }
    var showAddReqDialog by remember { mutableStateOf(false) }
    var newReqType by remember { mutableStateOf(RequirementType.CUSTOM) }
    var newReqLabel by remember { mutableStateOf("") }
    var newReqUrl by remember { mutableStateOf("") }
    var requirementsDirty by remember { mutableStateOf(false) }

    // Date/time picker states — registration
    val regDatePickerState = rememberDatePickerState(initialSelectedDateMillis = tournament.registrationDeadline)
    val regTimePickerState = rememberTimePickerState(
        initialHour = java.util.Calendar.getInstance().apply { timeInMillis = tournament.registrationDeadline }.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = java.util.Calendar.getInstance().apply { timeInMillis = tournament.registrationDeadline }.get(java.util.Calendar.MINUTE)
    )
    var pendingRegDateMillis by remember { mutableLongStateOf(0L) }
    var showRegDatePicker by remember { mutableStateOf(false) }
    var showRegTimePicker by remember { mutableStateOf(false) }

    // Date/time picker states — check-in
    val checkInDatePickerState = rememberDatePickerState(initialSelectedDateMillis = tournament.checkInDeadline)
    val checkInTimePickerState = rememberTimePickerState(
        initialHour = java.util.Calendar.getInstance().apply { timeInMillis = tournament.checkInDeadline }.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = java.util.Calendar.getInstance().apply { timeInMillis = tournament.checkInDeadline }.get(java.util.Calendar.MINUTE)
    )
    var pendingCheckInDateMillis by remember { mutableLongStateOf(0L) }
    var showCheckInDatePicker by remember { mutableStateOf(false) }
    var showCheckInTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(existingRequirements) {
        if (!requirementsDirty) pendingRequirements = existingRequirements
    }

    LaunchedEffect(tournament) {
        title = tournament.title
        description = tournament.description
        prizeDescription = tournament.prizeDescription ?: ""
        registrationDeadline = formatTimestamp(tournament.registrationDeadline)
        checkInDeadline = formatTimestamp(tournament.checkInDeadline)
        isLiveStreamEnabled = tournament.isLiveStreamEnabled
        swissRounds = tournament.swissRounds?.toString() ?: ""
    }

    // Validation
    var titleError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var deadlineError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        titleError = if (title.isBlank()) "Title is required" else null
        descriptionError = if (description.isBlank()) "Description is required" else null
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val regTs = try { sdf.parse(registrationDeadline)?.time ?: 0L } catch (_: Exception) { 0L }
        val checkInTs = try { sdf.parse(checkInDeadline)?.time ?: 0L } catch (_: Exception) { 0L }
        deadlineError = when {
            regTs <= System.currentTimeMillis() -> "Registration deadline must be in the future"
            checkInTs >= regTs -> "Check-in must be before registration closes"
            else -> null
        }
        return titleError == null && descriptionError == null && deadlineError == null
    }

    fun buildUpdates(): Map<String, Any?> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val regTs = try { sdf.parse(registrationDeadline)?.time ?: tournament.registrationDeadline } catch (_: Exception) { tournament.registrationDeadline }
        val checkInTs = try { sdf.parse(checkInDeadline)?.time ?: tournament.checkInDeadline } catch (_: Exception) { tournament.checkInDeadline }
        val updates = mutableMapOf<String, Any?>(
            "title" to title,
            "description" to description,
            "prize_description" to prizeDescription.ifBlank { null },
            "registration_deadline" to regTs.toIsoString(),
            "check_in_deadline" to checkInTs.toIsoString(),
            "is_live_stream_enabled" to isLiveStreamEnabled
        )
        val swiss = swissRounds.toIntOrNull()
        if (swiss != null && swiss > 0) updates["swiss_rounds"] = swiss
        else updates["swiss_rounds"] = null
        return updates
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkNavy.copy(alpha = 0.5f)
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
                        text = "Edit Tournament",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = White
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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Immutable fields (read-only display)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Hosted by ${tournament.hostUsername}",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ImmutableInfo(label = "Region", value = tournament.region)
                            ImmutableInfo(label = "Max Teams", value = "${tournament.maxTeams}")
                            ImmutableInfo(label = "Team Size", value = "${tournament.minTeamSize}")
                            ImmutableInfo(label = "Best Of", value = "BO${tournament.bestOf}")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ImmutableInfo(label = "Skill Level", value = tournament.skillLevel)
                            ImmutableInfo(label = "Prize Type", value = tournament.prizeType.value.replace("_", " "))
                        }
                        Text(
                            text = "These fields cannot be changed after creation.",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                        )
                    }
                }

                // ── Basic Information ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Separator.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Basic Information",
                            style = MaterialTheme.typography.titleSmall.copy(color = GoldPrimary, fontWeight = FontWeight.Bold)
                        )
                        
                        // ── Logo picker ──
                        Column {
                            Label(text = "Tournament Logo")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(SurfaceElevated, RoundedCornerShape(12.dp))
                                        .border(1.dp, if (selectedLogoUri != null || tournament.logoUrl != null) GoldPrimary else Separator, RoundedCornerShape(12.dp))
                                        .clickable { logoPicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val displayUri: Any? = selectedLogoUri ?: tournament.logoUrl
                                    if (displayUri != null) {
                                        coil.compose.AsyncImage(
                                            model = displayUri,
                                            contentDescription = "Tournament logo",
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add logo", tint = TextTertiary, modifier = Modifier.size(28.dp))
                                    }
                                }
                                Column {
                                    Text(
                                        when {
                                            selectedLogoUri != null -> "New logo selected — saved automatically"
                                            tournament.logoUrl != null -> "Current logo — tap to change"
                                            else -> "No logo — tap to add"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (selectedLogoUri != null) SuccessGreen else if (tournament.logoUrl != null) GoldPrimary else TextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        Column {
                            Label(text = "Title")
                            Spacer(modifier = Modifier.height(8.dp))
                            StyledInput(
                                value = title,
                                onValueChange = { title = it; titleError = null },
                                placeholder = "Tournament title",
                                isError = titleError != null
                            )
                            titleError?.let {
                                Text(it, color = ErrorRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                            }
                        }

                        Column {
                            Label(text = "Description")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it; descriptionError = null },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                placeholder = { Text("Describe your tournament…", color = TextTertiary) },
                                shape = RoundedCornerShape(12.dp),
                                colors = inputColors(),
                                isError = descriptionError != null,
                                supportingText = {
                                    descriptionError?.let {
                                        Text(it, color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            )
                        }
                    }
                }

                // ── Prize & Formats ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Separator.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Prize & Formats",
                            style = MaterialTheme.typography.titleSmall.copy(color = GoldPrimary, fontWeight = FontWeight.Bold)
                        )
                        
                        Column {
                            Label(text = "Prize Description")
                            Spacer(modifier = Modifier.height(8.dp))
                            StyledInput(
                                value = prizeDescription,
                                onValueChange = { prizeDescription = it },
                                placeholder = "$50 USD / 500 Diamonds"
                            )
                        }

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
                    }
                }

                // ── Deadlines & Advanced ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Separator.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Deadlines & Advanced",
                            style = MaterialTheme.typography.titleSmall.copy(color = GoldPrimary, fontWeight = FontWeight.Bold)
                        )
                        
                        Column {
                            Label(text = "Registration Deadline")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = registrationDeadline,
                                    onValueChange = {},
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Pick date & time", color = TextTertiary, fontSize = 12.sp) },
                                    readOnly = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = inputColors(),
                                    textStyle = TextStyle(color = White, fontSize = 13.sp),
                                    isError = deadlineError != null,
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = LightGray, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp).clickable { showRegDatePicker = true })
                                    }
                                )
                            }
                        }

                        Column {
                            Label(text = "Check-In Deadline")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = checkInDeadline,
                                    onValueChange = {},
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Pick date & time", color = TextTertiary, fontSize = 12.sp) },
                                    readOnly = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = inputColors(),
                                    textStyle = TextStyle(color = White, fontSize = 13.sp),
                                    isError = deadlineError != null,
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LightGray, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp).clickable { showCheckInDatePicker = true })
                                    }
                                )
                            }
                            deadlineError?.let {
                                Text(it, color = ErrorRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = Separator.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

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
                                    tint = if (isLiveStreamEnabled) ErrorRed else TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Enable Live Stream",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isLiveStreamEnabled) White else TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                            Switch(
                                checked = isLiveStreamEnabled,
                                onCheckedChange = { isLiveStreamEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = GoldPrimary,
                                    checkedThumbColor = White,
                                    uncheckedTrackColor = SurfaceElevated,
                                    uncheckedThumbColor = TextTertiary
                                )
                            )
                        }
                    }
                }

                // ── Requirements editor ──────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Separator.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Entry Requirements",
                                style = MaterialTheme.typography.titleSmall.copy(color = GoldPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            )
                            if (pendingRequirements.size < 5) {
                                IconButton(onClick = {
                                    newReqType = RequirementType.CUSTOM
                                    newReqLabel = ""
                                    newReqUrl = ""
                                    showAddReqDialog = true
                                }) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add", tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        if (pendingRequirements.isEmpty()) {
                            Text(
                                "No requirements set. Tap + to add one.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)
                            )
                        } else {
                            pendingRequirements.forEachIndexed { index, req ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        req.label,
                                        style = MaterialTheme.typography.bodySmall.copy(color = White),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            pendingRequirements = pendingRequirements.toMutableList().also { it.removeAt(index) }
                                            requirementsDirty = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = ErrorRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        // Save Requirements button (shown when dirty)
                        if (requirementsDirty) {
                            iOSPrimaryButton(
                                text = "Save Requirements",
                                onClick = {
                                    onSaveRequirements(tournament.id, pendingRequirements)
                                    requirementsDirty = false
                                },
                                backgroundColor = BluePrimary,
                                contentColor = White
                            )
                        }
                    }
                }

                // ── Add Requirement dialog ──
                if (showAddReqDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showAddReqDialog = false },
                        containerColor = SurfaceCard,
                        title = { Text("Add Requirement", color = White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                                    fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)
                                                )
                                            },
                                            modifier = Modifier.height(30.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldPrimary.copy(alpha = 0.2f),
                                                selectedLabelColor = GoldPrimary,
                                                containerColor = SurfaceElevated,
                                                labelColor = LightGray
                                            )
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = newReqLabel,
                                    onValueChange = { newReqLabel = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Label", color = TextTertiary, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = inputColors()
                                )
                                OutlinedTextField(
                                    value = newReqUrl,
                                    onValueChange = { newReqUrl = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("URL (optional)", color = TextTertiary, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = inputColors()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (newReqLabel.isNotBlank()) {
                                    pendingRequirements = pendingRequirements + TournamentRequirement(
                                        type  = newReqType,
                                        label = newReqLabel.trim(),
                                        url   = newReqUrl.trim().ifBlank { null }
                                    )
                                    requirementsDirty = true
                                    showAddReqDialog = false
                                }
                            }) { Text("Add", color = GoldPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddReqDialog = false }) { Text("Cancel", color = TextSecondary) }
                        }
                    )
                }

                // Lock notice
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, null, tint = WarningOrange, modifier = Modifier.size(18.dp))
                        Text(
                            text = "After registration ends, all details become locked and cannot be changed.",
                            style = MaterialTheme.typography.bodySmall.copy(color = WarningOrange)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                iOSPrimaryButton(
                    text = "Save Changes",
                    onClick = {
                        if (validate()) {
                            onSave(tournament.id, buildUpdates())
                        }
                    },
                    backgroundColor = GoldPrimary,
                    contentColor = DarkNavy
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Registration Date Picker Dialog
        if (showRegDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showRegDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        regDatePickerState.selectedDateMillis?.let { dateMillis ->
                            showRegDatePicker = false
                            showRegTimePicker = true
                            pendingRegDateMillis = dateMillis
                        }
                    }) { Text("Next", color = GoldPrimary) }
                },
                dismissButton = {
                    TextButton(onClick = { showRegDatePicker = false }) { Text("Cancel", color = TextSecondary) }
                }
            ) {
                DatePicker(state = regDatePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = DarkNavy,
                        titleContentColor = White,
                        headlineContentColor = White,
                        navigationContentColor = GoldPrimary,
                        yearContentColor = LightGray,
                        currentYearContentColor = GoldPrimary,
                        selectedDayContentColor = DarkNavy,
                        selectedDayContainerColor = GoldPrimary,
                        dayContentColor = White,
                        weekdayContentColor = TextSecondary
                    )
                )
            }
        }

        // Registration Time Picker Dialog
        if (showRegTimePicker) {
            AlertDialog(
                onDismissRequest = { showRegTimePicker = false },
                containerColor = DarkNavy,
                confirmButton = {
                    TextButton(onClick = {
                        val cal = java.util.Calendar.getInstance().apply {
                            timeInMillis = pendingRegDateMillis ?: System.currentTimeMillis()
                            set(java.util.Calendar.HOUR_OF_DAY, regTimePickerState.hour)
                            set(java.util.Calendar.MINUTE, regTimePickerState.minute)
                            set(java.util.Calendar.SECOND, 0)
                        }
                        registrationDeadline = formatTimestamp(cal.timeInMillis)
                        deadlineError = null
                        showRegTimePicker = false
                    }) { Text("OK", color = GoldPrimary) }
                },
                dismissButton = {
                    TextButton(onClick = { showRegTimePicker = false }) { Text("Cancel", color = TextSecondary) }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Select Time", color = White, fontWeight = FontWeight.SemiBold)
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
                    }) { Text("Next", color = GoldPrimary) }
                },
                dismissButton = {
                    TextButton(onClick = { showCheckInDatePicker = false }) { Text("Cancel", color = TextSecondary) }
                }
            ) {
                DatePicker(state = checkInDatePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = DarkNavy,
                        titleContentColor = White,
                        headlineContentColor = White,
                        navigationContentColor = GoldPrimary,
                        yearContentColor = LightGray,
                        currentYearContentColor = GoldPrimary,
                        selectedDayContentColor = DarkNavy,
                        selectedDayContainerColor = GoldPrimary,
                        dayContentColor = White,
                        weekdayContentColor = TextSecondary
                    )
                )
            }
        }

        // Check-In Time Picker Dialog
        if (showCheckInTimePicker) {
            AlertDialog(
                onDismissRequest = { showCheckInTimePicker = false },
                containerColor = DarkNavy,
                confirmButton = {
                    TextButton(onClick = {
                        val cal = java.util.Calendar.getInstance().apply {
                            timeInMillis = pendingCheckInDateMillis ?: System.currentTimeMillis()
                            set(java.util.Calendar.HOUR_OF_DAY, checkInTimePickerState.hour)
                            set(java.util.Calendar.MINUTE, checkInTimePickerState.minute)
                            set(java.util.Calendar.SECOND, 0)
                        }
                        checkInDeadline = formatTimestamp(cal.timeInMillis)
                        deadlineError = null
                        showCheckInTimePicker = false
                    }) { Text("OK", color = GoldPrimary) }
                },
                dismissButton = {
                    TextButton(onClick = { showCheckInTimePicker = false }) { Text("Cancel", color = TextSecondary) }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Select Check-In Time", color = White, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        TimePicker(state = checkInTimePickerState)
                    }
                }
            )
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(DarkNavy.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(48.dp))
            }
        }

        // Error
        error?.let {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = ErrorRed,
                action = { TextButton(onClick = onDismissError) { Text("OK", color = White) } }
            ) { Text(it, color = White) }
        }
    }
}

@Composable
private fun ImmutableInfo(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                color = White,
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 9.sp)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun Long.toIsoString(): String {
    return java.time.Instant.ofEpochMilli(this).toString()
}
