package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.LfgPost
import com.mlbb.scrim.data.model.GameRole
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GlassBackButton
import com.mlbb.scrim.ui.components.GradientButton
import com.mlbb.scrim.ui.components.PullToRefreshContainer
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LfgBoardScreen(
    posts: List<LfgPost>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onCreatePost: () -> Unit = {},
    onInvitePlayer: (LfgPost) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
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
                        text = stringResource(R.string.lfg_board),
                        style = iOSTitle2.copy(color = White)
                    )

                    IconButton(
                        onClick = onCreatePost,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = SuccessGreen,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Post",
                            tint = White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Posts list
            PullToRefreshContainer(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading && posts.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GoldPrimary)
                        }
                    }
                    posts.isEmpty() -> {
                        EmptyLfgState(onCreatePost = onCreatePost)
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(posts) { post ->
                                LfgPostCard(post = post, onInvite = { onInvitePlayer(post) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LfgPostCard(
    post: LfgPost,
    onInvite: () -> Unit
) {
    val roleColor = when (post.role) {
        GameRole.TANK -> Color(0xFF4CAF50)
        GameRole.FIGHTER -> Color(0xFFFF9800)
        GameRole.ASSASSIN -> Color(0xFFF44336)
        GameRole.MAGE -> Color(0xFF9C27B0)
        GameRole.MARKSMAN -> Color(0xFF2196F3)
        GameRole.SUPPORT -> Color(0xFF00BCD4)
        GameRole.FLEX -> Color(0xFF607D8B)
    }

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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Role badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = roleColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = post.role.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = roleColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = post.playerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = White
                    )
                }

                // Region
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = post.region.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = LightGray.copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (post.message.isNotBlank()) {
                Text(
                    text = post.message,
                    fontSize = 14.sp,
                    color = LightGray.copy(alpha = 0.85f),
                    lineHeight = 20.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skill level
                Text(
                    text = post.skillLevel.name,
                    fontSize = 12.sp,
                    color = LightGray.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )

                // Invite button
                TextButton(
                    onClick = onInvite,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.invite),
                        color = SuccessGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLfgState(onCreatePost: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = LightGray.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.no_lfg_posts),
                color = LightGray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.be_first_to_post),
                color = LightGray.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            GradientButton(
                text = stringResource(R.string.create_post),
                onClick = onCreatePost,
                modifier = Modifier
                    .width(180.dp)
                    .height(48.dp)
            )
        }
    }
}
