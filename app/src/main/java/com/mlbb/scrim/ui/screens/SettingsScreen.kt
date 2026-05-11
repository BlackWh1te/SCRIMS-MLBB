package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    notificationsEnabled: Boolean = true,
    matchNotifications: Boolean = true,
    messageNotifications: Boolean = true,
    soundEnabled: Boolean = true,
    vibrationEnabled: Boolean = true,
    onToggleNotifications: (Boolean) -> Unit = {},
    onToggleMatchNotifications: (Boolean) -> Unit = {},
    onToggleMessageNotifications: (Boolean) -> Unit = {},
    onToggleSound: (Boolean) -> Unit = {},
    onToggleVibration: (Boolean) -> Unit = {},
    appVersion: String = "1.0.0"
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    )

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Notifications Section
                AnimatedEntrance(delayMillis = 100) {
                    SettingsSectionTitle("Notifications")
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedEntrance(delayMillis = 150) {
                    SettingsToggleCard(
                        icon = Icons.Default.Notifications,
                        title = "Enable Notifications",
                        subtitle = "Receive push notifications",
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedEntrance(delayMillis = 200) {
                    SettingsToggleCard(
                        icon = Icons.Default.SportsEsports,
                        title = "Match Alerts",
                        subtitle = "Scrim invites & match results",
                        checked = matchNotifications && notificationsEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = onToggleMatchNotifications
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedEntrance(delayMillis = 250) {
                    SettingsToggleCard(
                        icon = Icons.Default.Chat,
                        title = "Message Alerts",
                        subtitle = "New chat messages",
                        checked = messageNotifications && notificationsEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = onToggleMessageNotifications
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedEntrance(delayMillis = 300) {
                    SettingsToggleCard(
                        icon = Icons.Default.VolumeUp,
                        title = "Sound",
                        subtitle = "Play sounds for notifications",
                        checked = soundEnabled && notificationsEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = onToggleSound
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedEntrance(delayMillis = 350) {
                    SettingsToggleCard(
                        icon = Icons.Default.Vibration,
                        title = "Vibration",
                        subtitle = "Vibrate on notifications",
                        checked = vibrationEnabled && notificationsEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = onToggleVibration
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // About Section
                AnimatedEntrance(delayMillis = 400) {
                    SettingsSectionTitle("About")
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedEntrance(delayMillis = 450) {
                    SettingsInfoCard(
                        icon = Icons.Default.Info,
                        title = "App Version",
                        value = appVersion
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedEntrance(delayMillis = 500) {
                    SettingsActionCard(
                        icon = Icons.Default.Policy,
                        title = "Privacy Policy",
                        onClick = { /* TODO */ }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedEntrance(delayMillis = 550) {
                    SettingsActionCard(
                        icon = Icons.Default.Description,
                        title = "Terms of Service",
                        onClick = { /* TODO */ }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = White
        )
    )
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (enabled) BluePrimary.copy(alpha = 0.15f) else White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (enabled) BluePrimary else MidGray,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) White else MidGray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = if (enabled) LightGray else MidGray.copy(alpha = 0.6f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GoldPrimary,
                    checkedTrackColor = GoldPrimary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MidGray,
                    uncheckedTrackColor = White.copy(alpha = 0.1f),
                    disabledUncheckedThumbColor = MidGray.copy(alpha = 0.3f),
                    disabledUncheckedTrackColor = White.copy(alpha = 0.05f)
                )
            )
        }
    }
}

@Composable
private fun SettingsInfoCard(
    icon: ImageVector,
    title: String,
    value: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = BluePrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BluePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = LightGray
                )
            }
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                spotColor = Color.Black.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkNavy),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = BluePrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BluePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = White,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MidGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
