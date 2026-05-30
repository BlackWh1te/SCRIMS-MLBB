package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.*
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.*
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentListScreen(
    tournaments: List<Tournament>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    error: String? = null,
    isTournamentHost: Boolean = false,
    hostedTournaments: List<Tournament> = emptyList(),
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToTournamentDetail: (String) -> Unit,
    onNavigateToCreateTournament: () -> Unit = {},
    onNavigateToHostRequest: () -> Unit = {},
    onNavigateToHostManagement: () -> Unit = {},
    onSetStatusFilter: (String?) -> Unit = {},
    onRefresh: () -> Unit = {},
    onDismissError: () -> Unit = {},
    myRegisteredTournamentIds: Set<String> = emptySet()
) {
    var showFilters by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    val focusManager = LocalFocusManager.current
    val tabs = if (isTournamentHost) listOf(R.string.all_tournaments, R.string.my_tournaments) else listOf(R.string.all_tournaments)

    val baseList = if (selectedTab == 1 && isTournamentHost) hostedTournaments else tournaments
    val displayTournaments = baseList
        .let { list -> if (selectedStatusFilter != null) list.filter { it.status.value == selectedStatusFilter } else list }
        .let { list -> if (searchQuery.isNotBlank()) list.filter { it.title.contains(searchQuery, ignoreCase = true) || it.hostUsername.contains(searchQuery, ignoreCase = true) } else list }

    // Actual open/live counts for dynamic chips
    val openCount = baseList.count { it.status == TournamentStatus.REGISTRATION }
    val liveCount = baseList.count { it.isLive }

    val statusFilters = listOf(
        null to stringResource(R.string.tournament_filter_all),
        "registration" to stringResource(R.string.tournament_filter_open),
        "in_progress" to stringResource(R.string.tournament_filter_live),
        "completed" to stringResource(R.string.tournament_filter_completed)
    )

    PullToRefreshContainer(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Premium Header ──
            AnimatedEntrance(delayMillis = 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkNavy.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 8.dp, bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onNavigateBack != null) {
                                GlassBackButton(onClick = onNavigateBack)
                            } else {
                                Spacer(modifier = Modifier.size(44.dp))
                            }

                            Text(
                                text = stringResource(R.string.tournaments_title),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Search toggle
                                IconButton(
                                    onClick = {
                                        showSearch = !showSearch
                                        if (!showSearch) { searchQuery = ""; focusManager.clearFocus() }
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = if (showSearch) BluePrimary.copy(alpha = 0.2f) else White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = if (showSearch) Icons.Default.SearchOff else Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = if (showSearch) BluePrimary else LightGray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                if (isTournamentHost) {
                                    IconButton(
                                        onClick = onNavigateToCreateTournament,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(color = GoldPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Default.Add, "Create", tint = GoldPrimary, modifier = Modifier.size(22.dp))
                                    }
                                } else {
                                    IconButton(
                                        onClick = onNavigateToHostRequest,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(color = PurplePrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Default.EmojiEvents, "Become Host", tint = PurplePrimary, modifier = Modifier.size(22.dp))
                                    }
                                }

                                if (selectedTab == 1 && isTournamentHost) {
                                    IconButton(
                                        onClick = onNavigateToHostManagement,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(color = GoldPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(Icons.Default.Settings, "Host Management", tint = GoldPrimary, modifier = Modifier.size(22.dp))
                                    }
                                }

                                IconButton(
                                    onClick = { showFilters = !showFilters },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            color = if (showFilters || selectedStatusFilter != null) GoldPrimary.copy(alpha = 0.2f) else White.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filters",
                                        tint = if (showFilters || selectedStatusFilter != null) GoldPrimary else LightGray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // ── Search Bar ──
                        AnimatedVisibility(
                            visible = showSearch,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                placeholder = { Text("Search tournaments...", color = TextTertiary) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextTertiary, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BluePrimary,
                                    unfocusedBorderColor = Separator,
                                    focusedContainerColor = SurfaceElevated,
                                    unfocusedContainerColor = SurfaceElevated,
                                    cursorColor = BluePrimary,
                                    focusedTextColor = White,
                                    unfocusedTextColor = White
                                )
                            )
                        }

                        // ── Dynamic Quick Stats Bar ──
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumChip(
                                text = "${baseList.size} TOTAL",
                                icon = Icons.Default.EmojiEvents,
                                color = GoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            PremiumChip(
                                text = "$openCount OPEN",
                                icon = Icons.Default.CheckCircle,
                                color = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                            PremiumChip(
                                text = "$liveCount LIVE",
                                icon = Icons.Default.PlayArrow,
                                color = ErrorRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Tabs (All / My Tournaments) ──
            if (tabs.size > 1) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = DarkNavy.copy(alpha = 0.5f),
                    contentColor = White,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, titleRes ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = stringResource(titleRes),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            selectedContentColor = GoldPrimary,
                            unselectedContentColor = LightGray.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ── Status Filter Chips ──
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusFilters.forEach { (value, label) ->
                            FilterChip(
                                selected = selectedStatusFilter == value,
                                onClick = {
                                    selectedStatusFilter = value
                                    onSetStatusFilter(value)
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                modifier = Modifier.height(36.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = GoldPrimary,
                                    containerColor = SurfaceElevated,
                                    labelColor = LightGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = Separator,
                                    selectedBorderColor = GoldPrimary,
                                    enabled = true,
                                    selected = selectedStatusFilter == value
                                )
                            )
                        }
                    }
                }
            }

            // ── Tournament List ──
            if (isLoading && displayTournaments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = GoldPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            } else if (displayTournaments.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 1 && isTournamentHost)
                                stringResource(R.string.my_tournaments_empty)
                            else
                                stringResource(R.string.tournament_no_tournaments),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedTab == 1 && isTournamentHost)
                                stringResource(R.string.my_tournaments_empty_desc)
                            else
                                stringResource(R.string.tournament_no_tournaments_desc),
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
                ) {
                    items(displayTournaments, key = { it.id }) { tournament ->
                        AnimatedEntrance(delayMillis = displayTournaments.indexOf(tournament) * 50) {
                            TournamentCard(
                                tournament = tournament,
                                isRegistered = tournament.id in myRegisteredTournamentIds,
                                onClick = { onNavigateToTournamentDetail(tournament.id) }
                            )
                        }
                    }
                }
            }
        }

        error?.let {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = ErrorRed,
                action = {
                    TextButton(onClick = onDismissError) {
                        Text("OK", color = White)
                    }
                }
            ) {
                Text(it, color = White)
            }
        }
    }
    }
}

// ═══════════════════════════════════════════════════════════════
// TOURNAMENT CARD (Premium Redesign)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TournamentCard(
    tournament: Tournament,
    isRegistered: Boolean = false,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Live countdown update
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tournament.registrationDeadline, tournament.checkInDeadline, tournament.status) {
        if (tournament.status == TournamentStatus.REGISTRATION || tournament.status == TournamentStatus.CHECK_IN) {
            while(true) {
                kotlinx.coroutines.delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }

    val statusColor = when (tournament.status) {
        TournamentStatus.REGISTRATION -> SuccessGreen
        TournamentStatus.CHECK_IN -> WarningOrange
        TournamentStatus.IN_PROGRESS -> ErrorRed
        TournamentStatus.COMPLETED -> BluePrimary
        TournamentStatus.CANCELLED -> TextTertiary
        else -> TextTertiary
    }

    val statusLabel = when (tournament.status) {
        TournamentStatus.REGISTRATION -> stringResource(R.string.tournament_status_registration)
        TournamentStatus.CHECK_IN -> stringResource(R.string.tournament_status_check_in)
        TournamentStatus.IN_PROGRESS -> stringResource(R.string.tournament_status_in_progress)
        TournamentStatus.COMPLETED -> stringResource(R.string.tournament_status_completed)
        TournamentStatus.CANCELLED -> stringResource(R.string.tournament_status_cancelled)
        else -> tournament.status.value.replace("_", " ").uppercase()
    }

    val prizeTypeIcon = when (tournament.prizeType) {
        PrizeType.DIAMONDS -> Icons.Default.Diamond
        PrizeType.SKIN -> Icons.Default.Style
        PrizeType.STAR_PASS -> Icons.Default.Star
        else -> Icons.Default.EmojiEvents
    }

    val infiniteTransition = rememberInfiniteTransition(label = "border_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val borderColor = when {
        tournament.status == TournamentStatus.REGISTRATION -> SuccessGreen.copy(alpha = glowAlpha)
        tournament.isLive -> ErrorRed.copy(alpha = 0.5f)
        else -> Separator
    }
    val borderWidth = if (tournament.status == TournamentStatus.REGISTRATION || tournament.isLive) 1.5.dp else 0.5.dp

    Card(
        modifier = Modifier.fillMaxWidth().shadow(
            elevation = if (tournament.status == TournamentStatus.REGISTRATION) 8.dp else 0.dp,
            shape = RoundedCornerShape(20.dp),
            ambientColor = if (tournament.status == TournamentStatus.REGISTRATION) SuccessGreen else Color.Black,
            spotColor = if (tournament.status == TournamentStatus.REGISTRATION) SuccessGreen else Color.Black
        ),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = borderWidth,
            color = borderColor
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Top Accent Bar ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(statusColor, statusColor.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // ── Top row: Status badge + Live indicator ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Status badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusColor.copy(alpha = if (tournament.status == TournamentStatus.REGISTRATION) 0.25f else 0.15f),
                            border = if (tournament.status == TournamentStatus.REGISTRATION) androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = glowAlpha)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                if (tournament.status == TournamentStatus.REGISTRATION) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(SuccessGreen.copy(alpha = glowAlpha), CircleShape)
                                            .shadow(4.dp, CircleShape, ambientColor = SuccessGreen, spotColor = SuccessGreen)
                                    )
                                } else if (tournament.isLive) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(ErrorRed, CircleShape)
                                    )
                                }
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        // Registered badge
                        if (isRegistered) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = PurplePrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "REGISTERED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PurplePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Registration countdown (if open)
                    if (tournament.isOpen && tournament.registrationDeadline > 0) {
                        val remaining = maxOf(0L, tournament.registrationDeadline - currentTime)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (remaining < 3600000) ErrorRed else WarningOrange
                            )
                            Text(
                                text = formatTimeRemaining(remaining),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (remaining < 3600000) ErrorRed else WarningOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Logo + Title row ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!tournament.logoUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = tournament.logoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevated),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tournament.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = White,
                                fontSize = 19.sp,
                                letterSpacing = 0.3.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tournament_hosted_by, tournament.hostUsername),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                            if (tournament.hostTrustScore > 0) {
                                val trustColor = when {
                                    tournament.hostTrustScore >= 8.0 -> SuccessGreen
                                    tournament.hostTrustScore >= 5.0 -> WarningOrange
                                    else -> ErrorRed
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = trustColor.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, trustColor.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = trustColor,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "%.1f".format(tournament.hostTrustScore),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = trustColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Prize Pool Section (Prominent) ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = GoldPrimary.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = prizeTypeIcon,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.tournament_prize_pool),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextTertiary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = tournament.prizeDisplay.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Info Pills + FULL badge ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoPill(icon = Icons.Default.SportsEsports, label = "BO${tournament.bestOf}")
                    InfoPill(icon = Icons.Default.Public, label = tournament.region)
                    if (tournament.skillLevel != "ALL") {
                        InfoPill(icon = Icons.Default.BarChart, label = tournament.skillLevel)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // FULL badge
                    if (tournament.teamCount >= tournament.maxTeams && tournament.maxTeams > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ErrorRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "FULL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Team Slots Fill Bar ──
                if (tournament.maxTeams > 0) {
                    val fillFraction = (tournament.teamCount.toFloat() / tournament.maxTeams).coerceIn(0f, 1f)
                    val isFull = tournament.teamCount >= tournament.maxTeams
                    val barColor = when {
                        isFull -> ErrorRed
                        fillFraction > 0.75f -> WarningOrange
                        else -> SuccessGreen
                    }
                    val animatedFill by animateFloatAsState(
                        targetValue = fillFraction,
                        animationSpec = tween(800, easing = FastOutSlowInEasing),
                        label = "fill"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${tournament.teamCount}/${tournament.maxTeams} teams",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary, fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "${(fillFraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = barColor, fontWeight = FontWeight.Bold, fontSize = 11.sp
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Separator)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedFill)
                                    .fillMaxHeight()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(barColor.copy(alpha = 0.9f), barColor)
                                        ),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Expandable Details ──
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(thickness = 0.5.dp, color = Separator.copy(alpha = 0.5f))

                        // Description
                        if (tournament.description.isNotBlank()) {
                            Column {
                                Text(
                                    text = stringResource(R.string.description),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextTertiary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tournament.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }

                        // Deadlines
                        if (tournament.registrationDeadline > 0) {
                            DetailRow(
                                icon = Icons.Default.CalendarToday,
                                label = stringResource(R.string.tournament_registration_deadline),
                                value = formatDeadlineLong(tournament.registrationDeadline)
                            )
                        }
                        if (tournament.checkInDeadline > 0) {
                            DetailRow(
                                icon = Icons.Default.CheckCircle,
                                label = stringResource(R.string.tournament_check_in_deadline),
                                value = formatDeadlineLong(tournament.checkInDeadline)
                            )
                        }

                        // Skill Level
                        DetailRow(
                            icon = Icons.Default.BarChart,
                            label = stringResource(R.string.tournament_skill_level_label),
                            value = tournament.skillLevel
                        )

                        // Swiss Rounds (if set)
                        tournament.swissRounds?.let {
                            DetailRow(
                                icon = Icons.Default.FormatListNumbered,
                                label = stringResource(R.string.tournament_swiss_rounds_label),
                                value = it.toString()
                            )
                        }

                        // Live stream indicator
                        if (tournament.isLiveStreamEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.tournament_livestream),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ErrorRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Action Row: Details toggle + View Full Page ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (expanded) stringResource(R.string.tournament_hide_details) else stringResource(R.string.tournament_details),
                            color = LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    iOSPrimaryButton(
                        text = stringResource(R.string.tournament_view_full),
                        onClick = onClick,
                        backgroundColor = GoldPrimary.copy(alpha = 0.15f),
                        contentColor = GoldPrimary,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}



private fun formatTimeRemaining(ms: Long): String {
    if (ms <= 0) return "Closed"
    val hours = ms / 3600000
    val minutes = (ms % 3600000) / 60000
    return when {
        hours > 24 -> "${hours / 24}d ${hours % 24}h left"
        hours > 0 -> "${hours}h ${minutes}m left"
        else -> "${minutes}m left"
    }
}

private fun formatDeadlineLong(timestamp: Long): String {
    if (timestamp <= 0) return "TBD"
    val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

// ── New Card Helpers ──

@Composable
private fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevated.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = TextTertiary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = TextTertiary
        )
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary,
                fontWeight = FontWeight.Medium
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                color = LightGray,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
