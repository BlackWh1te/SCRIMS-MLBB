package com.scrimslegends.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.R
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.ui.components.PremiumFadeIn

private data class OnboardingPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val gradient: List<Color>
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.SportsEsports,
            title = stringResource(R.string.find_scrims),
            description = stringResource(R.string.find_scrims_desc),
            gradient = BlueGradient
        ),
        OnboardingPage(
            icon = Icons.Default.Group,
            title = stringResource(R.string.build_team),
            description = stringResource(R.string.build_team_desc),
            gradient = GoldGradient
        ),
        OnboardingPage(
            icon = Icons.Default.EmojiEvents,
            title = stringResource(R.string.climb_ranks),
            description = stringResource(R.string.climb_ranks_desc),
            gradient = PurpleGradient
        ),
        OnboardingPage(
            icon = Icons.Filled.Chat,
            title = stringResource(R.string.connect_compete),
            description = stringResource(R.string.connect_compete_desc),
            gradient = SuccessGradient
        )
    )

    var currentPage by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        // ── Background Glow Orbs ──────────────────────────────────
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(BluePrimary.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(GoldPrimary.copy(alpha = 0.10f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // ── Page Content ────────────────────────────────────
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith
                    fadeOut(animationSpec = tween(400))
                },
                label = "pageTransition",
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageData = pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Premium Glass Icon Container
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(SurfaceOverlay)
                            .border(1.dp, GlassBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Brush.verticalGradient(pageData.gradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = pageData.icon,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(48.dp))

                    Text(
                        text = pageData.title,
                        style = iOSTitle1.copy(color = TextPrimary),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = pageData.description,
                        style = iOSBody.copy(color = TextSecondary),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Page Indicators ─────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 32.dp)
            ) {
                pages.forEachIndexed { index, _ ->
                    val isSelected = index == currentPage
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "indicatorWidth"
                    )
                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) GoldPrimary else GlassBorder)
                    )
                }
            }

            // ── Navigation Buttons ──────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 60.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentPage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceOverlay)
                            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .clickable { currentPage-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.back),
                            style = iOSHeadline.copy(color = TextPrimary)
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                val isLast = currentPage == pages.size - 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(if (isLast) BlueGradient else GoldGradient))
                        .clickable {
                            if (isLast) onFinish() else currentPage++
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isLast) stringResource(R.string.get_started) else stringResource(R.string.next),
                        style = iOSHeadline.copy(color = if (isLast) White else DarkNavy)
                    )
                }
            }
        }
    }
}
