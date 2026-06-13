package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
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
            icon = Icons.Default.Search,
            title = stringResource(R.string.onboarding_find_scrims_title),
            description = stringResource(R.string.onboarding_find_scrims_desc),
            gradient = BlueGradient
        ),
        OnboardingPage(
            icon = Icons.Default.Groups,
            title = stringResource(R.string.onboarding_team_title),
            description = stringResource(R.string.onboarding_team_desc),
            gradient = PremiumBlueGradient
        ),
        OnboardingPage(
            icon = Icons.Default.EmojiEvents,
            title = stringResource(R.string.onboarding_results_title),
            description = stringResource(R.string.onboarding_results_desc),
            gradient = PurpleGradient
        )
    )

    var currentPage by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Background Glow Orbs ──────────────────────────────────
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), Color.Transparent)
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
                        colors = listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f), Color.Transparent)
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
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
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(48.dp))

                    Text(
                        text = pageData.title,
                        style = iOSTitle1.copy(color = MaterialTheme.colorScheme.onSurface),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = pageData.description,
                        style = iOSBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .clickable { currentPage-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.back),
                            style = iOSHeadline.copy(color = MaterialTheme.colorScheme.onSurface)
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
                        .background(Brush.verticalGradient(if (isLast) BlueGradient else PremiumBlueGradient))
                        .clickable {
                            if (isLast) onFinish() else currentPage++
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isLast) stringResource(R.string.get_started) else stringResource(R.string.next),
                        style = iOSHeadline.copy(color = if (isLast) White else MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
}
