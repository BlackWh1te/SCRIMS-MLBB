package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.ui.components.GradientButton

private data class OnboardingPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val gradient: List<Color>
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.SportsEsports,
            title = "Find Scrims",
            description = "Discover and join ranked scrims from teams around the world. Filter by region, skill level, and game mode.",
            gradient = BlueGradient
        ),
        OnboardingPage(
            icon = Icons.Default.Group,
            title = "Build Your Team",
            description = "Create your squad, invite teammates, and manage roles. Teams of 3-7 players can compete together.",
            gradient = GoldGradient
        ),
        OnboardingPage(
            icon = Icons.Default.EmojiEvents,
            title = "Climb the Ranks",
            description = "Earn XP, win matches, and rise through 7 tiers from Bronze to Grandmaster. Track your progress on the leaderboard.",
            gradient = PurpleGradient
        ),
        OnboardingPage(
            icon = Icons.Default.Chat,
            title = "Connect & Compete",
            description = "Chat with team leaders, apply to scrims, and verify match results with screenshots.",
            gradient = SuccessGradient
        )
    )

    var currentPage by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Page Content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) +
                            slideInHorizontally { it / 4 } with
                            fadeOut(animationSpec = tween(300)) +
                            slideOutHorizontally { -it / 4 })
                },
                label = "pageTransition"
            ) { page ->
                val pageData = pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .shadow(
                                elevation = 20.dp,
                                spotColor = pageData.gradient[0].copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        pageData.gradient[0].copy(alpha = 0.3f),
                                        pageData.gradient[1].copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .shadow(
                                    elevation = 12.dp,
                                    spotColor = pageData.gradient[0].copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.verticalGradient(colors = pageData.gradient)
                                ),
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

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = pageData.title,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = pageData.description,
                        fontSize = 16.sp,
                        color = LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Page Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                pages.forEachIndexed { index, _ ->
                    val isSelected = index == currentPage
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "indicatorWidth"
                    )
                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) GoldPrimary else White.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = LightGray
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(White.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                val isLast = currentPage == pages.size - 1
                GradientButton(
                    text = if (isLast) "Get Started" else "Next",
                    onClick = {
                        if (isLast) {
                            onFinish()
                        } else {
                            currentPage++
                        }
                    },
                    modifier = Modifier.weight(1f),
                    gradient = GoldGradient,
                    height = 52.dp
                )
            }
        }
    }
}
