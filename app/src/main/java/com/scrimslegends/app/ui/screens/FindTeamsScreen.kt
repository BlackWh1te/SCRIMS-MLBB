package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.Team
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.EmptyState
import com.scrimslegends.app.ui.components.ErrorSnackbar
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.PremiumLoadingState
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.theme.*
import androidx.compose.ui.graphics.Color
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindTeamsScreen(
    teams: List<Team>,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    applicationSuccess: Boolean = false,
    error: String? = null,
    onRefresh: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onApplyToTeam: (String) -> Unit = {},
    onNavigateToTeamDetail: (String) -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var appliedTeamId by remember { mutableStateOf<String?>(null) }
    val appSurface = appSurfaceColor()
    val appElevatedSurface = appElevatedSurfaceColor()
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()
    val appBorder = appBorderColor()

    val filteredTeams = teams.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(applicationSuccess) {
        if (applicationSuccess) {
            appliedTeamId = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            AnimatedEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)
                    Text(
                        text = stringResource(R.string.find_teams_title),
                        style = iOSTitle2.copy(color = appTextPrimary)
                    )
                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            // Search bar
            AnimatedEntrance(delayMillis = 50) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_teams_hint), color = appTextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = appTextSecondary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = appBorder,
                        focusedTextColor = appTextPrimary,
                        unfocusedTextColor = appTextPrimary,
                        focusedContainerColor = appSurface,
                        unfocusedContainerColor = appSurface
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Team List
            PullToRefreshContainer(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading && teams.isEmpty() -> {
                        PremiumLoadingState(
                            message = "Loading teams...",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    filteredTeams.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Default.GroupAdd,
                            title = if (searchQuery.isEmpty()) stringResource(R.string.no_open_teams_yet) else stringResource(R.string.no_teams_found),
                            subtitle = if (searchQuery.isEmpty()) stringResource(R.string.no_open_teams_explainer) else stringResource(R.string.try_different_search_term),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredTeams, key = { it.id }) { team ->
                                AnimatedEntrance(delayMillis = 0) {
                                    OpenTeamCard(
                                        team = team,
                                        onApply = {
                                            appliedTeamId = team.id
                                            onApplyToTeam(team.id)
                                        },
                                        onClick = { onNavigateToTeamDetail(team.id) },
                                        isApplying = appliedTeamId == team.id && isLoading
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
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
private fun OpenTeamCard(
    team: Team,
    onApply: () -> Unit,
    onClick: () -> Unit,
    isApplying: Boolean
) {
    val appSurface = appSurfaceColor()
    val appTextPrimary = appTextPrimaryColor()
    val appTextSecondary = appTextSecondaryColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appBorderColor(), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = appSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        style = iOSBody.copy(
                            color = appTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People,
                            null,
                            tint = appTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.players_fraction, team.currentPlayerCount, team.maxPlayers),
                            style = iOSCaption2.copy(color = appTextSecondary)
                        )
                    }
                }

                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 2.dp
                    )
                } else {
                    TextButton(
                        onClick = onApply,
                        colors = ButtonDefaults.textButtonColors(contentColor = SuccessGreen)
                    ) {
                        Text(stringResource(R.string.apply), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(InfoBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        stringResource(R.string.reputation_label, team.displayReputation),
                        style = iOSCaption2.copy(color = InfoBlue, fontWeight = FontWeight.Medium)
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (team.canAddPlayer) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SuccessGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            stringResource(R.string.recruiting),
                            style = iOSCaption2.copy(color = SuccessGreen, fontWeight = FontWeight.Medium)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ErrorRed.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            stringResource(R.string.full),
                            style = iOSCaption2.copy(color = ErrorRed, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    }
}
