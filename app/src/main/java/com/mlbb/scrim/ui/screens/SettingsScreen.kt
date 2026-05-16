package com.mlbb.scrim.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.R
import com.mlbb.scrim.data.localization.Language
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.PremiumFadeIn
import com.mlbb.scrim.ui.components.GlassBackButton

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onDeleteAccount: () -> Unit = {},
    context: Context,
    notificationsEnabled: Boolean = true,
    matchNotifications: Boolean = true,
    messageNotifications: Boolean = true,
    soundEnabled: Boolean = true,
    vibrationEnabled: Boolean = true,
    languageCode: String = "en",
    darkMode: Boolean = true,
    onToggleNotifications: (Boolean) -> Unit = {},
    onToggleMatchNotifications: (Boolean) -> Unit = {},
    onToggleMessageNotifications: (Boolean) -> Unit = {},
    onToggleSound: (Boolean) -> Unit = {},
    onToggleVibration: (Boolean) -> Unit = {},
    onSetLanguage: (String) -> Unit = {},
    onToggleDarkMode: (Boolean) -> Unit = {},
    onLogout: () -> Unit = {},
    appVersion: String = "1.0.0"
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        // ── Background Glow Orbs ──────────────────────────────────
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 150.dp, y = 150.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(BluePrimary.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──────────────────────────────────────────
            PremiumFadeIn(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(onClick = onNavigateBack)

                    Text(
                        text = stringResource(R.string.settings),
                        style = iOSTitle2.copy(color = TextPrimary)
                    )

                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // ── Notifications ─────────────────────────────────
                PremiumFadeIn(delayMillis = 100) {
                    SettingsSectionTitle(stringResource(R.string.notifications_section))
                }
                Spacer(Modifier.height(12.dp))
                PremiumFadeIn(delayMillis = 150) {
                    SettingsToggleCard(
                        icon = Icons.Default.Notifications,
                        title = stringResource(R.string.enable_notifications),
                        subtitle = stringResource(R.string.receive_push),
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications
                    )
                }
                Spacer(Modifier.height(10.dp))
                PremiumFadeIn(delayMillis = 200) {
                    SettingsToggleCard(
                        icon = Icons.Default.SportsEsports,
                        title = stringResource(R.string.match_alerts),
                        subtitle = stringResource(R.string.match_alerts_sub),
                        checked = matchNotifications && notificationsEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = onToggleMatchNotifications
                    )
                }
                Spacer(Modifier.height(10.dp))
                PremiumFadeIn(delayMillis = 250) {
                    SettingsToggleCard(
                        icon = Icons.Filled.Chat,
                        title = stringResource(R.string.message_alerts),
                        subtitle = stringResource(R.string.message_alerts_sub),
                        checked = messageNotifications && notificationsEnabled,
                        enabled = notificationsEnabled,
                        onCheckedChange = onToggleMessageNotifications
                    )
                }

                Spacer(Modifier.height(32.dp))

                // ── Appearance ───────────────────────────────────
                PremiumFadeIn(delayMillis = 300) {
                    SettingsSectionTitle(stringResource(R.string.appearance))
                }
                Spacer(Modifier.height(12.dp))
                PremiumFadeIn(delayMillis = 350) {
                    SettingsToggleCard(
                        icon = Icons.Default.DarkMode,
                        title = stringResource(R.string.dark_mode),
                        subtitle = stringResource(R.string.dark_mode_sub),
                        checked = darkMode,
                        onCheckedChange = onToggleDarkMode
                    )
                }
                Spacer(Modifier.height(10.dp))
                PremiumFadeIn(delayMillis = 400) {
                    val currentLang = Language.fromCode(languageCode)
                    SettingsValueCard(
                        icon = Icons.Default.Translate,
                        title = stringResource(R.string.language),
                        subtitle = stringResource(R.string.language_sub),
                        value = "${currentLang.flag} ${currentLang.displayName}",
                        onClick = { showLanguageDialog = true }
                    )
                }

                Spacer(Modifier.height(32.dp))

                // ── Support ──────────────────────────────────────
                PremiumFadeIn(delayMillis = 450) {
                    SettingsSectionTitle(stringResource(R.string.support))
                }
                Spacer(Modifier.height(12.dp))
                PremiumFadeIn(delayMillis = 500) {
                    SettingsActionCard(
                        icon = Icons.Default.ContactSupport,
                        title = stringResource(R.string.contact_support),
                        subtitle = stringResource(R.string.contact_support_sub),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@mlbbscrim.app")
                                putExtra(Intent.EXTRA_SUBJECT, "Support Request")
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(Modifier.height(32.dp))

                // ── Account ──────────────────────────────────────
                PremiumFadeIn(delayMillis = 550) {
                    SettingsSectionTitle(stringResource(R.string.account))
                }
                Spacer(Modifier.height(12.dp))
                PremiumFadeIn(delayMillis = 600) {
                    SettingsActionCard(
                        icon = Icons.Default.Logout,
                        title = stringResource(R.string.log_out),
                        subtitle = stringResource(R.string.log_out_sub),
                        color = WarningOrange,
                        onClick = { showLogoutConfirm = true }
                    )
                }
                Spacer(Modifier.height(10.dp))
                PremiumFadeIn(delayMillis = 650) {
                    SettingsActionCard(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.delete_account),
                        subtitle = stringResource(R.string.delete_account_sub),
                        color = ErrorRed,
                        onClick = { showDeleteConfirm = true }
                    )
                }

                Spacer(Modifier.height(60.dp))
                
                Text(
                    text = "Version $appVersion",
                    style = iOSFootnote.copy(color = TextTertiary),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 40.dp)
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // ── Dialogs ───────────────────────────────────────────────────
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentCode = languageCode,
            onDismiss = { showLanguageDialog = false },
            onSelect = { code ->
                showLanguageDialog = false
                onSetLanguage(code)
            }
        )
    }

    if (showLogoutConfirm) {
        PremiumConfirmDialog(
            title = stringResource(R.string.log_out),
            message = stringResource(R.string.logout_confirm_message),
            confirmText = stringResource(R.string.log_out),
            confirmColor = WarningOrange,
            onDismiss = { showLogoutConfirm = false },
            onConfirm = { onLogout(); showLogoutConfirm = false }
        )
    }

    if (showDeleteConfirm) {
        PremiumConfirmDialog(
            title = stringResource(R.string.delete_account),
            message = stringResource(R.string.delete_account_confirm_message),
            confirmText = stringResource(R.string.delete_account),
            confirmColor = ErrorRed,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = { onDeleteAccount(); showDeleteConfirm = false }
        )
    }
}

@Composable
private fun PremiumConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(28.dp)),
        title = { Text(title, style = iOSTitle2.copy(color = TextPrimary)) },
        text = { Text(message, style = iOSBody.copy(color = TextSecondary)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, style = iOSHeadline.copy(color = confirmColor))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), style = iOSHeadline.copy(color = TextTertiary))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun LanguageSelectionDialog(
    currentCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        modifier = Modifier.border(1.dp, GlassBorder, RoundedCornerShape(28.dp)),
        title = { Text(stringResource(R.string.select_language), style = iOSTitle2.copy(color = TextPrimary)) },
        text = {
            Column {
                Language.values().forEach { lang ->
                    val isSelected = lang.code == currentCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(lang.code) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(lang.flag, fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
                            Text(
                                lang.displayName,
                                style = iOSBody.copy(
                                    color = if (isSelected) GoldPrimary else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), style = iOSHeadline.copy(color = TextTertiary))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = iOSFootnote.copy(color = TextTertiary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (enabled) BluePrimary.copy(alpha = 0.1f) else SurfaceOverlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (enabled) BluePrimary else TextTertiary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = iOSHeadline.copy(color = if (enabled) TextPrimary else TextTertiary))
                Text(subtitle, style = iOSFootnote.copy(color = TextSecondary))
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GoldPrimary,
                    checkedTrackColor = GoldPrimary.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = SurfaceOverlay
                )
            )
        }
    }
}

@Composable
private fun SettingsValueCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BluePrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = iOSHeadline.copy(color = TextPrimary))
                Text(subtitle, style = iOSFootnote.copy(color = TextSecondary))
            }
            Text(value, style = iOSCallout.copy(color = GoldPrimary, fontWeight = FontWeight.Bold))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    color: Color = BluePrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = iOSHeadline.copy(color = TextPrimary))
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = iOSFootnote.copy(color = TextSecondary))
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}
