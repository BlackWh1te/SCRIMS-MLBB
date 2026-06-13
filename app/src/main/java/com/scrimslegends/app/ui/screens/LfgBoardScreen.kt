package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.data.model.LfgPost
import com.scrimslegends.app.data.model.GameRole
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.GlassBackButton
import com.scrimslegends.app.ui.components.GradientButton
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.theme.*
import com.scrimslegends.app.R
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
    onRefresh: () -> Unit = {},
    error: String? = null,
    onDismissError: () -> Unit = {}
) {
    var invitingPostIds by remember { mutableStateOf(setOf<String>()) }
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
                        style = iOSTitle2.copy(color = MaterialTheme.colorScheme.onSurface)
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
                            contentDescription = stringResource(R.string.create_post),
                            tint = MaterialTheme.colorScheme.onSurface,
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
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    error != null && posts.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(16.dp))
                                Text(stringResource(R.string.error_loading_data), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontSize = 13.sp)
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.retry), color = MaterialTheme.colorScheme.onSurface) }
                            }
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
                            items(posts, key = { it.id }) { post ->
                                val isInviting = invitingPostIds.contains(post.id)
                                LfgPostCard(
                                    post = post, 
                                    isInviting = isInviting,
                                    onInvite = { 
                                        invitingPostIds = invitingPostIds + post.id
                                        onInvitePlayer(post) 
                                    }
                                )
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
    isInviting: Boolean,
    onInvite: () -> Unit
) {
    val roleColor = roleColor(post.role)
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appBorderColor(), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Region
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = post.region.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f).copy(alpha = 0.7f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (post.message.isNotBlank()) {
                Text(
                    text = post.message,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f).copy(alpha = 0.85f),
                    lineHeight = 20.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    modifier = Modifier.clickable { expanded = !expanded }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f).copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )

                // Invite button
                TextButton(
                    onClick = onInvite,
                    modifier = Modifier.height(32.dp),
                    enabled = !isInviting
                ) {
                    if (isInviting) {
                        CircularProgressIndicator(color = SuccessGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f).copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.no_lfg_posts),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.be_first_to_post),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f).copy(alpha = 0.6f),
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
