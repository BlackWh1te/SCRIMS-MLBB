package com.scrimslegends.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.GradientButton

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateTeamScreen(
    onNavigateBack: () -> Unit,
    onCreateTeam: (String, Uri?, Boolean) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    existingTeamNames: List<String> = emptyList()
) {
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }
    var isOpenForApplications by remember { mutableStateOf(false) }
    var selectedLogoUri by remember { mutableStateOf<Uri?>(null) }
    var localError by remember { mutableStateOf("") }
    val displayedError = localError.takeIf { it.isNotEmpty() } ?: errorMessage ?: ""

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedLogoUri = uri
    }

    val pulseAnimation = rememberInfiniteTransition()
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = HeroGradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            AnimatedEntrance(delayMillis = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBackButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.background(
                            color = ErrorRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    )
                    Text(
                        text = stringResource(R.string.create_team),
                        style = iOSTitle2.copy(color = TextPrimary)
                    )
                    Spacer(modifier = Modifier.width(44.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                AnimatedEntrance(delayMillis = 80) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(pulseScale)
                            .shadow(12.dp, CircleShape, spotColor = BluePrimary.copy(alpha = 0.4f))
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        BluePrimary.copy(alpha = 0.25f),
                                        PurplePrimary.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(2.dp, BluePrimary.copy(alpha = 0.4f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { logoPicker.launch("image/*") }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedLogoUri != null) {
                            AsyncImage(
                                model = selectedLogoUri,
                                contentDescription = stringResource(R.string.content_desc_team_logo),
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = stringResource(R.string.content_desc_upload_logo),
                                    tint = BluePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "LOGO",
                                    style = iOSCaption2.copy(
                                        color = BluePrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                AnimatedEntrance(delayMillis = 140) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                "TEAM DETAILS",
                                style = iOSCaption1.copy(
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = teamName,
                                onValueChange = { teamName = it; localError = "" },
                                label = { Text("Team Name *", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = White.copy(alpha = 0.2f),
                                    focusedLabelColor = GoldPrimary,
                                    unfocusedLabelColor = TextTertiary,
                                    cursorColor = GoldPrimary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkNavy,
                                    unfocusedContainerColor = DarkNavy
                                ),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Shield, null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                }
                            )

                            Spacer(Modifier.height(14.dp))

                            OutlinedTextField(
                                value = teamDescription,
                                onValueChange = { teamDescription = it },
                                label = { Text("Description (optional)", color = TextSecondary) },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BluePrimary,
                                    unfocusedBorderColor = White.copy(alpha = 0.2f),
                                    focusedLabelColor = BluePrimary,
                                    unfocusedLabelColor = TextTertiary,
                                    cursorColor = BluePrimary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkNavy,
                                    unfocusedContainerColor = DarkNavy
                                ),
                                shape = RoundedCornerShape(14.dp),
                                maxLines = 3,
                                leadingIcon = {
                                    Icon(Icons.Default.Description, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                                }
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GroupAdd, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Open for applications",
                                            style = iOSBody.copy(color = TextPrimary, fontWeight = FontWeight.Medium)
                                        )
                                        Text(
                                            "Let players find and apply to join",
                                            style = iOSCaption2.copy(color = TextSecondary)
                                        )
                                    }
                                }
                                Switch(
                                    checked = isOpenForApplications,
                                    onCheckedChange = { isOpenForApplications = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = White,
                                        checkedTrackColor = SuccessGreen,
                                        uncheckedThumbColor = White,
                                        uncheckedTrackColor = White.copy(alpha = 0.3f)
                                    )
                                )
                            }

                            AnimatedVisibility(
                                visible = displayedError.isNotEmpty(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(displayedError, color = ErrorRed, style = iOSCaption1)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                AnimatedEntrance(delayMillis = 200) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(InfoBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Info, null, tint = InfoBlue, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.team_requirements),
                                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Spacer(Modifier.height(12.dp))

                            val tips = listOf(
                                "Minimum 5 players to enter scrims" to Icons.Default.Group,
                                "Leader manages roster and scrims" to Icons.Default.Shield,
                                "Team reputation affects matchmaking" to Icons.Default.Star
                            )
                            tips.forEach { (text, icon) ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(text, style = iOSCallout.copy(color = TextSecondary))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                AnimatedEntrance(delayMillis = 280) {
                    GradientButton(
                        text = stringResource(R.string.create_team),
                        onClick = {
                            when {
                                teamName.isBlank() -> localError = "Please enter a team name"
                                teamName.length < 3 -> localError = "Team name must be at least 3 characters"
                                existingTeamNames.any { it.equals(teamName, ignoreCase = true) } -> localError = "A team with this name already exists"
                                else -> { localError = ""; onCreateTeam(teamName, selectedLogoUri, isOpenForApplications) }
                            }
                        },
                        gradient = GoldGradient,
                        enabled = !isLoading,
                        isLoading = isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )
                }

                Spacer(Modifier.height(96.dp))
            }
        }
    }
}
