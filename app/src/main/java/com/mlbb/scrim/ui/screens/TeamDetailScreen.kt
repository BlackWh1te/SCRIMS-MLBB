package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    team: com.mlbb.scrim.data.model.Team,
    isLeader: Boolean = false,
    currentUserId: String = "",
    onNavigateBack: () -> Unit,
    onUpdatePlayerRole: ((playerId: String, newRole: com.mlbb.scrim.data.model.PlayerRole) -> Unit)? = null,
    onRemovePlayer: ((playerId: String) -> Unit)? = null,
    onLeaveTeam: (() -> Unit)? = null,
    onDisbandTeam: (() -> Unit)? = null,
    onInvitePlayer: (() -> Unit)? = null
) {
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDisbandDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var playerToRemove by remember { mutableStateOf<com.mlbb.scrim.data.model.Player?>(null) }

    val inviteCode = remember(team.id) {
        "MLBB-${team.name.take(3).uppercase()}${team.id.takeLast(4).uppercase()}"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = heroGradientBrush()
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        text = "Team Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                // Team Header Card
                item {
                    AnimatedEntrance(delayMillis = 100) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 8.dp,
                                    spotColor = BluePrimary.copy(alpha = 0.2f),
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
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Team Avatar with gradient
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .shadow(
                                            elevation = 12.dp,
                                            spotColor = BluePrimary.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(BluePrimary, Color(0xFF0A5A9F))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = team.name.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Team Name
                                Text(
                                    text = team.name,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Player Count
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Players",
                                        tint = LightGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${team.players.size} / 7 players",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 15.sp,
                                            color = LightGray
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Team Stats Section
                item {
                    AnimatedEntrance(delayMillis = 175) {
                        Text(
                            text = "Team Stats",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedEntrance(delayMillis = 185) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TeamStatBox(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.SportsEsports,
                                label = "Scrims",
                                value = "24",
                                tint = BluePrimary
                            )
                            TeamStatBox(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.EmojiEvents,
                                label = "Wins",
                                value = "16",
                                tint = SuccessGreen
                            )
                            TeamStatBox(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.TrendingUp,
                                label = "Win Rate",
                                value = "67%",
                                tint = GoldPrimary
                            )
                            TeamStatBox(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Star,
                                label = "Avg Rating",
                                value = "4.2",
                                tint = Purple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Players Section
                item {
                    AnimatedEntrance(delayMillis = 200) {
                        Text(
                            text = "Players",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Player List
                itemsIndexed(team.players) { index, player ->
                    AnimatedEntrance(delayMillis = 250 + index * 60) {
                        PlayerCard(
                            player = player,
                            isLeader = isLeader,
                            onChangeRole = if (isLeader && player.role != com.mlbb.scrim.data.model.PlayerRole.LEADER) {
                                { onUpdatePlayerRole?.invoke(player.id, it) }
                            } else null
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Add Player Button (if team not full)
                if (team.players.size < 7) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))

                        AnimatedEntrance(delayMillis = 250 + team.players.size * 60) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 4.dp,
                                        spotColor = Color.Black.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = DarkNavy
                                ),
                                shape = RoundedCornerShape(16.dp),
                                onClick = { showInviteDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Player",
                                        tint = BluePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Add Player",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BluePrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Team Actions
                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedEntrance(delayMillis = 400) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (isLeader) {
                                // Disband Team
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = 4.dp,
                                            spotColor = ErrorRed.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = ErrorRed.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    onClick = { showDisbandDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Disband",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Disband Team",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = ErrorRed
                                            )
                                        )
                                    }
                                }
                            } else {
                                // Leave Team
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = 4.dp,
                                            spotColor = WarningOrange.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(16.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = WarningOrange.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    onClick = { showLeaveDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExitToApp,
                                            contentDescription = "Leave",
                                            tint = WarningOrange,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Leave Team",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = WarningOrange
                                            )
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

    // Leave Team Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = "Leave Team?",
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to leave ${team.name}? You will need an invite to rejoin.",
                    color = LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLeaveTeam?.invoke()
                        showLeaveDialog = false
                    }
                ) {
                    Text("Leave", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel", color = MidGray)
                }
            }
        )
    }

    // Disband Team Dialog
    if (showDisbandDialog) {
        AlertDialog(
            onDismissRequest = { showDisbandDialog = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = "Disband Team?",
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently delete ${team.name} and remove all players. This action cannot be undone.",
                    color = LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDisbandTeam?.invoke()
                        showDisbandDialog = false
                    }
                ) {
                    Text("Disband", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisbandDialog = false }) {
                    Text("Cancel", color = MidGray)
                }
            }
        )
    }

    // Remove Player Dialog
    if (showRemoveDialog && playerToRemove != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = "Remove Player?",
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Remove ${playerToRemove!!.name} from the team?",
                    color = LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemovePlayer?.invoke(playerToRemove!!.id)
                        showRemoveDialog = false
                        playerToRemove = null
                    }
                ) {
                    Text("Remove", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel", color = MidGray)
                }
            }
        )
    }

    // Invite Player Dialog
    if (showInviteDialog) {
        InvitePlayerDialog(
            teamName = team.name,
            inviteCode = inviteCode,
            onDismiss = { showInviteDialog = false }
        )
    }
}

@Composable
fun PlayerCard(
    player: com.mlbb.scrim.data.model.Player,
    isLeader: Boolean = false,
    onChangeRole: ((com.mlbb.scrim.data.model.PlayerRole) -> Unit)? = null
) {
    var showRoleDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                if (isLeader && onChangeRole != null) {
                    showRoleDialog = true
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Avatar with gradient
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 6.dp,
                        spotColor = BluePrimary.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(BluePrimary.copy(alpha = 0.3f), BluePrimary.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.name.firstOrNull()?.uppercaseChar()?.toString() ?: "P",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Player Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Email: ${player.email}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = LightGray
                    )
                )
            }

            // Role Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (player.role) {
                        com.mlbb.scrim.data.model.PlayerRole.LEADER -> WarningOrange.copy(alpha = 0.15f)
                        com.mlbb.scrim.data.model.PlayerRole.CO_LEADER -> SuccessGreen.copy(alpha = 0.15f)
                        else -> White.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = player.role.name.replace("_", " "),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (player.role) {
                            com.mlbb.scrim.data.model.PlayerRole.LEADER -> WarningOrange
                            com.mlbb.scrim.data.model.PlayerRole.CO_LEADER -> SuccessGreen
                            else -> LightGray
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (isLeader && onChangeRole != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Change role",
                        tint = MidGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Role Change Dialog
    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            containerColor = DarkNavy,
            title = {
                Text(
                    text = "Change Role",
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Select a new role for ${player.name}:",
                        color = LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    com.mlbb.scrim.data.model.PlayerRole.values().filter { it != com.mlbb.scrim.data.model.PlayerRole.LEADER }.forEach { role ->
                        val isSelected = player.role == role
                        Button(
                            onClick = {
                                onChangeRole?.invoke(role)
                                showRoleDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) {
                                    when (role) {
                                        com.mlbb.scrim.data.model.PlayerRole.CO_LEADER -> SuccessGreen.copy(alpha = 0.3f)
                                        else -> White.copy(alpha = 0.2f)
                                    }
                                } else DarkSurface.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = role.name.replace("_", " "),
                                    color = when (role) {
                                        com.mlbb.scrim.data.model.PlayerRole.CO_LEADER -> SuccessGreen
                                        else -> White
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text("Cancel", color = MidGray)
                }
            }
        )
    }
}

@Composable
private fun TeamStatBox(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                spotColor = tint.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = DarkNavy
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = LightGray
                )
            )
        }
    }
}
