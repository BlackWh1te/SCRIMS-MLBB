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
import com.mlbb.scrim.ui.components.EnhancedStatusBadge
import com.mlbb.scrim.ui.components.SectionHeader
import com.mlbb.scrim.ui.components.EmptyState
import com.mlbb.scrim.ui.components.ShimmerCard
import com.mlbb.scrim.ui.components.GlassBackButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrimListScreen(
    scrims: List<Scrim>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToCreateScrim: () -> Unit,
    onNavigateToScrimDetail: (Scrim) -> Unit,
    onSearch: (GameMode?, Region?, SkillLevel?, ScrimStatus?) -> Unit,
    onRefresh: () -> Unit = {}
) {
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
                        text = "Find Scrims",
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Game Mode Filter
                        Text(
                            text = "Game Mode",
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
                                        onSearch(selectedGameMode, selectedRegion, selectedSkillLevel, null)
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
                            text = "Region",
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
                                        onSearch(selectedGameMode, selectedRegion, selectedSkillLevel, null)
                                    },
                                    label = { Text(region.name, fontSize = 13.sp) },
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
            if (scrims.isEmpty() && !isLoading) {
                // Empty State
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "No scrims found",
                    subtitle = "Be the first to post a scrim",
                    modifier = Modifier.fillMaxSize(),
                    action = {}
                )
            } else {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrimCard(
    scrim: Scrim,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100, easing = AppEaseOutCubic),
        label = "scrimCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isPressed) 2.dp else 4.dp,
                spotColor = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scrim.teamName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                )

                EnhancedStatusBadge(
                    text = when (scrim.status) {
                        ScrimStatus.OPEN -> "Open"
                        ScrimStatus.FILLED -> "Filled"
                        ScrimStatus.IN_PROGRESS -> "In Progress"
                        ScrimStatus.COMPLETED -> "Completed"
                        ScrimStatus.CANCELLED -> "Cancelled"
                    },
                    color = when (scrim.status) {
                        ScrimStatus.OPEN -> SuccessGreen
                        ScrimStatus.FILLED -> WarningOrange
                        ScrimStatus.IN_PROGRESS -> BluePrimary
                        ScrimStatus.COMPLETED -> LightGray
                        ScrimStatus.CANCELLED -> ErrorRed
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Game Mode and Region
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoChip(icon = Icons.Default.SportsEsports, text = scrim.gameMode.name)
                InfoChip(icon = Icons.Default.Public, text = scrim.region.name)
                InfoChip(icon = Icons.Default.Star, text = scrim.skillLevel.name)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            if (scrim.description.isNotBlank()) {
                Text(
                    text = scrim.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        color = LightGray
                    ),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Players
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Players",
                    tint = LightGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${scrim.currentPlayers} / ${scrim.maxPlayers}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        color = LightGray
                    )
                )

                Spacer(modifier = Modifier.width(20.dp))

                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time",
                    tint = LightGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatTime(scrim.scheduledTime),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        color = LightGray
                    )
                )
            }
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = White.copy(alpha = 0.15f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LightGray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = LightGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = timestamp - now

    return when {
        diff < 0 -> "Started"
        diff < 3600000 -> "${diff / 60000} min"
        diff < 86400000 -> "${diff / 3600000} hr"
        else -> "${diff / 86400000} days"
    }
}
