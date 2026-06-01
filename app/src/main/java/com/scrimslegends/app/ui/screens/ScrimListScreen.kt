package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.*
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.*
import com.scrimslegends.app.ui.utils.HapticFeedback
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import kotlin.math.absoluteValue

// ── Status filter chip data ──────────────────────────────────

private data class StatusChip(
    val label  : String,
    val status : ScrimStatus?,   // null = "All"
    val color  : Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrimListScreen(
    scrims: List<Scrim>,
    isLoading: Boolean,
    error: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToCreateScrim: () -> Unit,
    onNavigateToScrimDetail: (Scrim) -> Unit,
    onSearch: (String, GameMode?, Region?, SkillLevel?, ScrimStatus?) -> Unit,
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery        by remember { mutableStateOf("") }
    var selectedGameMode   by remember { mutableStateOf<GameMode?>(null) }
    var selectedRegion     by remember { mutableStateOf<Region?>(null) }
    var selectedSkillLevel by remember { mutableStateOf<SkillLevel?>(null) }
    var selectedStatus     by remember { mutableStateOf<ScrimStatus?>(null) }
    var showFilters        by remember { mutableStateOf(false) }

    // Status chips — drives top filter row
    val statusChips = remember {
        listOf(
            StatusChip("All",         null,                    GoldPrimary),
            StatusChip("Open",        ScrimStatus.OPEN,        SuccessGreen),
            StatusChip("Filled",      ScrimStatus.FILLED,      WarningOrange),
            StatusChip("Ready",       ScrimStatus.READY_CHECK, WarningOrange),
            StatusChip("In Progress", ScrimStatus.IN_PROGRESS, BluePrimary),
            StatusChip("Completed",   ScrimStatus.COMPLETED,   LightGray),
            StatusChip("Cancelled",   ScrimStatus.CANCELLED,   ErrorRed)
        )
    }

    // Dynamic counts per status
    val openCount = scrims.count { it.status == ScrimStatus.OPEN }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────
            AnimatedEntrance(delayMillis = 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(DarkNavy.copy(alpha = 0.65f))
                        .drawBehind {
                            drawLine(
                                color       = GlassBorder,
                                start       = Offset(0f, size.height),
                                end         = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                ) {
                    // Title row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onNavigateBack != null) {
                            GlassBackButton(onClick = onNavigateBack)
                        } else {
                            Spacer(modifier = Modifier.size(40.dp))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text       = stringResource(R.string.scrims),
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color      = TextPrimary
                            )
                            if (openCount > 0) {
                                Text(
                                    text     = "$openCount open now",
                                    fontSize = 11.sp,
                                    color    = SuccessGreen.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Filter toggle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (showFilters) GoldPrimary.copy(alpha = 0.15f)
                                    else SurfaceOverlay
                                )
                                .border(
                                    1.dp,
                                    if (showFilters) GoldPrimary.copy(alpha = 0.4f) else GlassBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { showFilters = !showFilters },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Tune, null,
                                tint     = if (showFilters) GoldPrimary else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // ── Status filter chips row ──────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusChips.forEach { chip ->
                            val isActive = selectedStatus == chip.status
                            val count    = when (chip.status) {
                                null                  -> scrims.size
                                ScrimStatus.OPEN      -> scrims.count { it.status == ScrimStatus.OPEN }
                                ScrimStatus.FILLED    -> scrims.count { it.status == ScrimStatus.FILLED }
                                ScrimStatus.IN_PROGRESS -> scrims.count { it.status == ScrimStatus.IN_PROGRESS }
                                ScrimStatus.COMPLETED -> scrims.count { it.status == ScrimStatus.COMPLETED }
                                else -> 0
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isActive) chip.color.copy(alpha = 0.20f)
                                        else SurfaceOverlay.copy(alpha = 0.55f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isActive) chip.color.copy(alpha = 0.55f) else GlassBorder,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        selectedStatus = if (isActive) null else chip.status
                                        onSearch(
                                            searchQuery, selectedGameMode, selectedRegion,
                                            selectedSkillLevel, selectedStatus
                                        )
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (chip.status != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    if (isActive) chip.color else chip.color.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
                                        )
                                    }
                                    Text(
                                        text       = chip.label,
                                        fontSize   = 12.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                        color      = if (isActive) chip.color else TextSecondary
                                    )
                                    if (count > 0) {
                                        Text(
                                            text     = "($count)",
                                            fontSize = 11.sp,
                                            color    = if (isActive) chip.color.copy(alpha = 0.7f)
                                            else TextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Collapsible Filter Sheet ─────────────────────────
            AnimatedVisibility(
                visible = showFilters,
                enter   = expandVertically(tween(220, easing = AppEaseOutCubic)) + fadeIn(tween(180)),
                exit    = shrinkVertically(tween(180, easing = AppEaseInCubic)) + fadeOut(tween(130))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    colors    = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape     = RoundedCornerShape(20.dp),
                    border    = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value         = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, selectedStatus)
                            },
                            placeholder   = {
                                Text(
                                    stringResource(R.string.search_teams),
                                    color    = TextTertiary,
                                    fontSize = 14.sp
                                )
                            },
                            leadingIcon   = {
                                Icon(
                                    Icons.Default.Search, null,
                                    tint     = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon  = if (searchQuery.isNotBlank()) {
                                {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        onSearch("", selectedGameMode, selectedRegion, selectedSkillLevel, selectedStatus)
                                    }) {
                                        Icon(Icons.Default.Clear, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else null,
                            modifier      = Modifier.fillMaxWidth().height(50.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = GoldPrimary.copy(alpha = 0.5f),
                                unfocusedBorderColor    = GlassBorder,
                                focusedContainerColor   = SurfaceOverlay,
                                unfocusedContainerColor = SurfaceOverlay,
                                focusedTextColor        = TextPrimary,
                                unfocusedTextColor      = TextPrimary,
                                cursorColor             = GoldPrimary
                            ),
                            shape         = RoundedCornerShape(14.dp),
                            textStyle     = iOSBody.copy(fontSize = 14.sp),
                            singleLine    = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                        )

                        Spacer(Modifier.height(14.dp))

                        // Game Mode
                        FilterSectionLabel(stringResource(R.string.game_mode))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            GameMode.selectable.forEach { mode ->
                                CompactFilterChip(
                                    label    = mode.displayName,
                                    selected = selectedGameMode == mode,
                                    color    = BluePrimary,
                                    onClick  = {
                                        selectedGameMode = if (selectedGameMode == mode) null else mode
                                        onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, selectedStatus)
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Region
                        FilterSectionLabel(stringResource(R.string.region))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Region.entries.forEach { region ->
                                CompactFilterChip(
                                    label    = region.displayName,
                                    selected = selectedRegion == region,
                                    color    = GoldPrimary,
                                    onClick  = {
                                        selectedRegion = if (selectedRegion == region) null else region
                                        onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, selectedStatus)
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Skill Level
                        FilterSectionLabel(stringResource(R.string.skill_level))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            SkillLevel.entries.forEach { level ->
                                CompactFilterChip(
                                    label    = level.name,
                                    selected = selectedSkillLevel == level,
                                    color    = Purple,
                                    onClick  = {
                                        selectedSkillLevel = if (selectedSkillLevel == level) null else level
                                        onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, selectedStatus)
                                    }
                                )
                            }
                        }

                        // Clear filters
                        val hasActiveFilters = selectedGameMode != null || selectedRegion != null ||
                                selectedSkillLevel != null || searchQuery.isNotBlank()
                        if (hasActiveFilters) {
                            Spacer(Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    selectedGameMode   = null
                                    selectedRegion     = null
                                    selectedSkillLevel = null
                                    searchQuery        = ""
                                    onSearch("", null, null, null, selectedStatus)
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(
                                    Icons.Default.FilterAltOff, null,
                                    tint     = iOSRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.clear_filters),
                                    color    = iOSRed,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── Scrim List ───────────────────────────────────────
            PullToRefreshContainer(
                isRefreshing = isLoading,
                onRefresh    = onRefresh,
                modifier     = Modifier.weight(1f)
            ) {
                val displayScrims = if (selectedStatus != null)
                    scrims.filter { it.status == selectedStatus }
                else
                    scrims

                when {
                    isLoading && scrims.isEmpty() -> {
                        ScrimListSkeleton(
                            modifier  = Modifier.fillMaxSize(),
                            itemCount = 5
                        )
                    }

                    displayScrims.isEmpty() -> {
                        EmptyState(
                            icon     = Icons.Default.SportsEsports,
                            title    = if (selectedStatus != null)
                                "No ${selectedStatus!!.name.lowercase()} scrims"
                            else
                                stringResource(R.string.no_scrims_found),
                            subtitle = stringResource(R.string.be_first_to_post_scrim),
                            modifier = Modifier.fillMaxSize(),
                            action   = {}
                        )
                    }

                    else -> {
                        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                            LaunchedEffect(listState, displayScrims) {
                                androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                                    .collect { lastIndex ->
                                        if (lastIndex != null && lastIndex >= displayScrims.size - 3) {
                                            onLoadMore()
                                        }
                                    }
                            }
                            LazyColumn(
                                state = listState,
                            modifier            = Modifier.fillMaxSize(),
                            contentPadding      = PaddingValues(
                                start  = 14.dp,
                                end    = 14.dp,
                                top    = 14.dp,
                                bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(displayScrims, key = { _, s -> s.id }) { index, scrim ->
                                AnimatedEntrance(delayMillis = (index * 30).coerceAtMost(300)) {
                                    PremiumScrimCard(
                                        scrim   = scrim,
                                        onClick = { onNavigateToScrimDetail(scrim) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB — only on main tab
        if (onNavigateBack == null) {
            FloatingActionButton(
                onClick        = onNavigateToCreateScrim,
                modifier       = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp),
                containerColor = GoldPrimary,
                contentColor   = DarkNavy,
                shape          = CircleShape,
                elevation      = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.post_scrim),
                    modifier           = Modifier.size(28.dp)
                )
            }
        }

        // Error snackbar
        ErrorSnackbar(
            error     = error,
            onDismiss = onDismissError,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── Small filter section label ────────────────────────────────

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextTertiary,
        letterSpacing = 0.6.sp
    )
}

// ── Compact filter chip ───────────────────────────────────────

@Composable
private fun CompactFilterChip(
    label   : String,
    selected: Boolean,
    color   : Color,
    onClick : () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) color.copy(alpha = 0.22f)
                else SurfaceOverlay.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                if (selected) color.copy(alpha = 0.5f) else GlassBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (selected) color else TextSecondary
        )
    }
}

// ── Legacy PremiumChip (used elsewhere) ──────────────────────

@Composable
fun PremiumChip(
    text    : String,
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    color   : Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(20.dp),
        color    = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text     = text,
                style    = iOSCaption1.copy(color = color, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Premium Scrim Card ─────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScrimCard(
    scrim  : Scrim,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current

    LaunchedEffect(isPressed) {
        if (isPressed) HapticFeedback.performClick(context)
    }

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "scrimCardScale"
    )

    val statusColor = when (scrim.status) {
        ScrimStatus.OPEN        -> SuccessGreen
        ScrimStatus.FILLED      -> WarningOrange
        ScrimStatus.READY_CHECK -> WarningOrange
        ScrimStatus.IN_PROGRESS -> BluePrimary
        ScrimStatus.COMPLETED   -> LightGray
        ScrimStatus.CANCELLED   -> ErrorRed
    }

    val statusText = when (scrim.status) {
        ScrimStatus.OPEN        -> stringResource(R.string.open)
        ScrimStatus.FILLED      -> stringResource(R.string.filled)
        ScrimStatus.READY_CHECK -> "Ready"
        ScrimStatus.IN_PROGRESS -> stringResource(R.string.in_progress)
        ScrimStatus.COMPLETED   -> stringResource(R.string.completed)
        ScrimStatus.CANCELLED   -> stringResource(R.string.cancelled)
    }

    val playerRatio = (scrim.currentPlayers.toFloat() / scrim.maxPlayers.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation  = if (scrim.status == ScrimStatus.OPEN) 10.dp else 6.dp,
                spotColor  = statusColor.copy(alpha = if (scrim.status == ScrimStatus.OPEN) 0.22f else 0.12f),
                shape      = RoundedCornerShape(22.dp)
            ),
        colors        = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape         = RoundedCornerShape(22.dp),
        onClick       = onClick,
        interactionSource = interactionSource
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Top status accent bar ────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.9f),
                                statusColor.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // ── Team avatar + info + status badge ────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TeamAvatar(name = scrim.teamName)

                        Column {
                            Text(
                                text       = scrim.teamName,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color      = TextPrimary,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniTag(text = scrim.gameMode.displayName, color = BluePrimary)
                                MiniTag(text = scrim.region.displayName, color = GoldPrimary)
                            }
                        }
                    }

                    // Status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Text(
                                text       = statusText,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = statusColor
                            )
                        }
                    }
                }

                // ── Description (if present) ─────────────────────
                if (scrim.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text     = scrim.description,
                        fontSize = 13.sp,
                        color    = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Bottom row: players + skill + time ────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Player progress bar
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            Icons.Default.People, null,
                            tint     = statusColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(White.copy(alpha = 0.09f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(playerRatio)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(statusColor)
                            )
                        }
                        Text(
                            text     = "${scrim.currentPlayers}/${scrim.maxPlayers}",
                            fontSize = 12.sp,
                            color    = LightGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // Skill level
                        MiniTag(text = scrim.skillLevel.name, color = Purple)

                        // Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule, null,
                                tint     = statusColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text     = formatTime(scrim.scheduledTime),
                                fontSize = 12.sp,
                                color    = statusColor.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Mini tag component ─────────────────────────────────────────

@Composable
private fun MiniTag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text       = text,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = color
        )
    }
}

// ── Team Avatar ────────────────────────────────────────────────

@Composable
fun TeamAvatar(name: String) {
    val initials    = name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .take(2)
        .ifEmpty { "?" }

    val avatarColors = listOf(
        Color(0xFF3B82F6) to Color(0xFF1D4ED8),
        Color(0xFF8B5CF6) to Color(0xFF6D28D9),
        Color(0xFF10B981) to Color(0xFF059669),
        Color(0xFFF59E0B) to Color(0xFFD97706),
        Color(0xFFEC4899) to Color(0xFFBE185D),
        Color(0xFF06B6D4) to Color(0xFF0891B2),
        Color(0xFFEF4444) to Color(0xFFB91C1C)
    )
    val colorPair = avatarColors[name.hashCode().absoluteValue % avatarColors.size]

    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(colorPair.first, colorPair.second)
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = initials,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            color      = White
        )
    }
}

// ── Status Pill (used in ScrimDetailScreen) ───────────────────

@Composable
fun ScrimStatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color = color, shape = CircleShape)
            )
            Text(
                text       = text,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = color
            )
        }
    }
}

// ── Time formatter ─────────────────────────────────────────────

fun formatTime(timestamp: Long): String {
    val diff = timestamp - System.currentTimeMillis()
    return when {
        diff < 0           -> "Now"
        diff < 3_600_000L  -> "${diff / 60_000}m"
        diff < 86_400_000L -> "${diff / 3_600_000}h"
        else               -> "${diff / 86_400_000}d"
    }
}
