package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.mlbb.scrim.data.model.*
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.*
import com.mlbb.scrim.ui.utils.HapticFeedback
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlin.math.absoluteValue

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
    onDismissError: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGameMode by remember { mutableStateOf<GameMode?>(null) }
    var selectedRegion by remember { mutableStateOf<Region?>(null) }
    var selectedSkillLevel by remember { mutableStateOf<SkillLevel?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Premium Header with iOS-style large title
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
                                text = stringResource(R.string.scrims),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            )

                            IconButton(
                                onClick = { showFilters = !showFilters },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        color = if (showFilters) GoldPrimary.copy(alpha = 0.2f) else White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.filters),
                                    tint = if (showFilters) GoldPrimary else LightGray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Quick Stats Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumChip(
                                text = "${scrims.size} SCRIMS",
                                icon = Icons.Default.SportsEsports,
                                color = BluePrimary,
                                modifier = Modifier.weight(1f)
                            )
                            PremiumChip(
                                text = stringResource(R.string.open),
                                icon = Icons.Default.CheckCircle,
                                color = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                            PremiumChip(
                                text = stringResource(R.string.region_eu_na),
                                icon = Icons.Default.Public,
                                color = GoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Collapsible Filter Section
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Search Bar
                        DebouncedSearchBar(
                            placeholder = stringResource(R.string.search_teams),
                            onSearch = {
                                searchQuery = it
                                onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, null)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Game Mode - All options with horizontal scroll
                        Text(
                            text = stringResource(R.string.game_mode),
                            style = iOSFootnote.copy(color = LightGray)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GameMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = selectedGameMode == mode,
                                    onClick = {
                                        selectedGameMode = if (selectedGameMode == mode) null else mode
                                        onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, null)
                                    },
                                    label = { Text(mode.name, fontSize = 12.sp) },
                                    modifier = Modifier.height(36.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BluePrimary.copy(alpha = 0.25f),
                                        selectedLabelColor = BluePrimary,
                                        containerColor = White.copy(alpha = 0.08f),
                                        labelColor = LightGray
                                    ),
                                    border = null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Region - All options with horizontal scroll
                        Text(
                            text = stringResource(R.string.region),
                            style = iOSFootnote.copy(color = LightGray)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Region.entries.forEach { region ->
                                FilterChip(
                                    selected = selectedRegion == region,
                                    onClick = {
                                        selectedRegion = if (selectedRegion == region) null else region
                                        onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, null)
                                    },
                                    label = { Text(region.displayName, fontSize = 12.sp) },
                                    modifier = Modifier.height(36.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary.copy(alpha = 0.25f),
                                        selectedLabelColor = GoldPrimary,
                                        containerColor = White.copy(alpha = 0.08f),
                                        labelColor = LightGray
                                    ),
                                    border = null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Skill Level - All options with horizontal scroll
                        Text(
                            text = stringResource(R.string.skill_level),
                            style = iOSFootnote.copy(color = LightGray)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SkillLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = selectedSkillLevel == level,
                                    onClick = {
                                        selectedSkillLevel = if (selectedSkillLevel == level) null else level
                                        onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, null)
                                    },
                                    label = { Text(level.name, fontSize = 12.sp) },
                                    modifier = Modifier.height(36.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Purple.copy(alpha = 0.25f),
                                        selectedLabelColor = Purple,
                                        containerColor = White.copy(alpha = 0.08f),
                                        labelColor = LightGray
                                    ),
                                    border = null
                                )
                            }
                        }

                        // Clear Filters Button
                        if (selectedGameMode != null || selectedRegion != null || selectedSkillLevel != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    selectedGameMode = null
                                    selectedRegion = null
                                    selectedSkillLevel = null
                                    searchQuery = ""
                                    onSearch("", null, null, null, null)
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(R.string.clear_filters), color = iOSRed, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Scrim List
            PullToRefreshContainer(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading && scrims.isEmpty() -> {
                        ScrimListSkeleton(modifier = Modifier.fillMaxSize(), itemCount = 5)
                    }
                    scrims.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.SportsEsports,
                            title = stringResource(R.string.no_scrims_found),
                            subtitle = stringResource(R.string.be_first_to_post_scrim),
                            modifier = Modifier.fillMaxSize(),
                            action = {}
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(scrims) { index, scrim ->
                                AnimatedEntrance(delayMillis = index * 50) {
                                    PremiumScrimCard(
                                        scrim = scrim,
                                        onClick = { onNavigateToScrimDetail(scrim) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Premium FAB
        if (onNavigateBack == null) {
            FloatingActionButton(
                onClick = onNavigateToCreateScrim,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp),
                containerColor = GoldPrimary,
                contentColor = DarkNavy,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.post_scrim),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Error snackbar
        ErrorSnackbar(
            error = error,
            onDismiss = onDismissError,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun PremiumChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = iOSCaption1.copy(color = color, fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScrimCard(
    scrim: Scrim,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current

    LaunchedEffect(isPressed) {
        if (isPressed) HapticFeedback.performClick(context)
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scrimCardScale"
    )

    val statusColor = when (scrim.status) {
        ScrimStatus.OPEN -> SuccessGreen
        ScrimStatus.FILLED -> WarningOrange
        ScrimStatus.READY_CHECK -> WarningOrange
        ScrimStatus.IN_PROGRESS -> BluePrimary
        ScrimStatus.COMPLETED -> LightGray
        ScrimStatus.CANCELLED -> ErrorRed
    }

    val statusText = when (scrim.status) {
        ScrimStatus.OPEN -> stringResource(R.string.open)
        ScrimStatus.FILLED -> stringResource(R.string.filled)
        ScrimStatus.READY_CHECK -> "Ready"
        ScrimStatus.IN_PROGRESS -> stringResource(R.string.in_progress)
        ScrimStatus.COMPLETED -> stringResource(R.string.completed)
        ScrimStatus.CANCELLED -> stringResource(R.string.cancelled)
    }

    val playerRatio = (scrim.currentPlayers.toFloat() / scrim.maxPlayers.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation = 8.dp, spotColor = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(24.dp),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Top Row: Team Avatar + Info + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Team Avatar with gradient
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(4.dp, CircleShape, spotColor = BluePrimary.copy(alpha = 0.3f))
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(listOf(BluePrimary, Color(0xFF0A5A9F)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                            Text(
                                text = scrim.teamName.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        )
                    }

                    Column {
                        Text(
                            text = scrim.teamName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BluePrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = scrim.gameMode.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = iOSCaption1.copy(color = BluePrimary, fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = scrim.region.displayName,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = iOSCaption1.copy(color = GoldPrimary, fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = iOSCaption1.copy(color = statusColor, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description (if present)
            if (scrim.description.isNotBlank()) {
                Text(
                    text = scrim.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = LightGray,
                        lineHeight = 20.sp
                    ),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Row: Progress + Skill + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player Progress
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = statusColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(6.dp)
                            .background(White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(playerRatio)
                                .background(statusColor, RoundedCornerShape(3.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${scrim.currentPlayers}/${scrim.maxPlayers}",
                        style = iOSCaption1.copy(color = LightGray, fontWeight = FontWeight.Medium)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skill Level
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Purple.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = scrim.skillLevel.name,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = iOSCaption1.copy(color = Purple, fontWeight = FontWeight.SemiBold)
                        )
                    }

                    // Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = statusColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatTime(scrim.scheduledTime),
                            style = iOSCaption1.copy(color = statusColor.copy(alpha = 0.9f), fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TeamAvatar(name: String) {
    val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(2).ifEmpty { "?" }
    val avatarColors = listOf(
        Color(0xFF3B82F6) to Color(0xFF1D4ED8),
        Color(0xFF8B5CF6) to Color(0xFF6D28D9),
        Color(0xFF10B981) to Color(0xFF059669),
        Color(0xFFF59E0B) to Color(0xFFD97706),
        Color(0xFFEC4899) to Color(0xFFBE185D),
        Color(0xFF06B6D4) to Color(0xFF0891B2),
        Color(0xFFEF4444) to Color(0xFFB91C1C),
    )
    val colorPair = avatarColors[name.hashCode().absoluteValue % avatarColors.size]

    Box(
        modifier = Modifier
            .size(44.dp)
            .background(brush = Brush.radialGradient(colors = listOf(colorPair.first, colorPair.second), radius = 28f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initials, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White))
    }
}

@Composable
fun ScrimStatusPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).background(color = color, shape = androidx.compose.foundation.shape.CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold))
        }
    }
}

fun formatTime(timestamp: Long): String {
    val diff = timestamp - System.currentTimeMillis()
    return when {
        diff < 0 -> "Now"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        else -> "${diff / 86400000}d"
    }
}