package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.GameMode
import com.mlbb.scrim.data.model.Region
import com.mlbb.scrim.data.model.Scrim
import com.mlbb.scrim.data.model.ScrimStatus
import com.mlbb.scrim.data.model.SkillLevel
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.SectionHeader
import com.mlbb.scrim.ui.components.EmptyState
import com.mlbb.scrim.ui.components.ScrimListSkeleton
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.PullToRefreshContainer
import com.mlbb.scrim.ui.components.DebouncedSearchBar
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
    onNavigateBack: () -> Unit,
    onNavigateToCreateScrim: () -> Unit,
    onNavigateToScrimDetail: (Scrim) -> Unit,
    onSearch: (String, GameMode?, Region?, SkillLevel?, ScrimStatus?) -> Unit,
    onRefresh: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGameMode by remember { mutableStateOf<GameMode?>(null) }
    var selectedRegion by remember { mutableStateOf<Region?>(null) }
    var selectedSkillLevel by remember { mutableStateOf<SkillLevel?>(null) }

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
                    GlassBackButton(onClick = onNavigateBack)

                    Text(
                        text = stringResource(R.string.find_scrims_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = LightGray,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToCreateScrim,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = BluePrimary,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Scrim",
                            tint = White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Search Filters
            AnimatedEntrance(delayMillis = 100) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .shadow(
                            elevation = 4.dp,
                            spotColor = Color.Black.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkNavy
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        SectionHeader(title = "Filters")

                        Spacer(modifier = Modifier.height(12.dp))

                        DebouncedSearchBar(
                            placeholder = "Search teams or descriptions...",
                            onSearch = {
                                searchQuery = it
                                onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, null)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Game Mode Filter
                        Text(
                            text = stringResource(R.string.game_mode),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = LightGray
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GameMode.values().forEach { mode ->
                                FilterChip(
                                    selected = selectedGameMode == mode,
                                    onClick = {
                                        selectedGameMode = if (selectedGameMode == mode) null else mode
                                        onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, null)
                                    },
                                    label = { Text(mode.name, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f).height(44.dp),
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Region Filter
                        Text(
                            text = stringResource(R.string.region),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = LightGray
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Region.values().forEach { region ->
                                FilterChip(
                                    selected = selectedRegion == region,
                                    onClick = {
                                        selectedRegion = if (selectedRegion == region) null else region
                                        onSearch(searchQuery, selectedGameMode, selectedRegion, selectedSkillLevel, null)
                                    },
                                    label = { Text(region.displayName, fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f).height(44.dp),
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
            }

            // Scrim List
            PullToRefreshContainer(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading && scrims.isEmpty() -> {
                            ScrimListSkeleton(
                                modifier = Modifier.fillMaxSize(),
                                itemCount = 5
                            )
                        }
                        scrims.isEmpty() -> {
                            EmptyState(
                                icon = Icons.Default.Search,
                                title = "No scrims found",
                                subtitle = "Be the first to post a scrim",
                                modifier = Modifier.fillMaxSize(),
                                action = {}
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                contentPadding = PaddingValues(vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(scrims) { index, scrim ->
                                    AnimatedEntrance(delayMillis = index * 60) {
                                        ScrimCard(
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrimCard(
    scrim: Scrim,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            HapticFeedback.performClick(context)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
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
        ScrimStatus.OPEN -> stringResource(R.string.scrim_status_open)
        ScrimStatus.FILLED -> stringResource(R.string.scrim_status_filled)
        ScrimStatus.READY_CHECK -> "Ready Check"
        ScrimStatus.IN_PROGRESS -> stringResource(R.string.scrim_status_in_progress)
        ScrimStatus.COMPLETED -> stringResource(R.string.scrim_status_completed)
        ScrimStatus.CANCELLED -> stringResource(R.string.scrim_status_cancelled)
    }

    val playerRatio = (scrim.currentPlayers.toFloat() / scrim.maxPlayers.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                spotColor = statusColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Colored status accent bar on left
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.9f),
                                statusColor.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
                    .align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                // Top Row: Team Avatar + Name + Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Team Avatar
                        TeamAvatar(name = scrim.teamName)

                        Column {
                            Text(
                                text = scrim.teamName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${scrim.gameMode.name} · ${scrim.region.displayName}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 13.sp,
                                    color = LightGray.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    // Status Pill
                    ScrimStatusPill(text = statusText, color = statusColor)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Middle: Description (if present)
                if (scrim.description.isNotBlank()) {
                    Text(
                        text = scrim.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = LightGray.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        ),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Bottom Row: Player Progress + Time + Skill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Player progress
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(4.dp)
                                .background(
                                    color = White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(playerRatio)
                                    .background(
                                        color = statusColor,
                                        shape = RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                        Text(
                            text = "${scrim.currentPlayers}/${scrim.maxPlayers}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = LightGray.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Right side info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Skill chip
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = White.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = scrim.skillLevel.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = LightGray.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = statusColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatTime(scrim.scheduledTime),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = statusColor.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamAvatar(name: String) {
    val initials = name
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
        Color(0xFFEF4444) to Color(0xFFB91C1C),
    )
    val colorPair = avatarColors[name.hashCode().absoluteValue % avatarColors.size]

    Box(
        modifier = Modifier
            .size(44.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(colorPair.first, colorPair.second),
                    radius = 28f
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
        )
    }
}

@Composable
fun ScrimStatusPill(
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color = color, shape = RoundedCornerShape(50))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = timestamp - now

    return when {
        diff < 0 -> "Started"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        else -> "${diff / 86400000}d"
    }
}
