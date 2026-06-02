package com.scrimslegends.app.ui.screens

import android.net.Uri
import timber.log.Timber
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.scrimslegends.app.data.model.*
import com.scrimslegends.app.data.model.UserProfile
import com.scrimslegends.app.data.model.Team
import com.scrimslegends.app.data.service.SupabaseConfig
import com.scrimslegends.app.data.service.SupabaseStorageUpload
import com.scrimslegends.app.ui.components.AnimatedEntrance
import com.scrimslegends.app.ui.components.ErrorSnackbar
import com.scrimslegends.app.ui.components.iOSChip
import com.scrimslegends.app.ui.components.PullToRefreshContainer
import com.scrimslegends.app.ui.theme.*
import androidx.compose.ui.res.stringResource
import com.scrimslegends.app.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerFinderScreen(
    posts: List<LfgPost>,
    isLoading: Boolean,
    currentUserId: String,
    currentUserProfile: UserProfile?,
    myTeams: List<Team>,
    onCreatePost: (LfgPost) -> Unit,
    onDeletePost: (String) -> Unit,
    onMessagePlayer: (LfgPost) -> Unit,
    onInvitePlayer: (LfgPost) -> Unit = {},
    onViewCountIncrement: (String) -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    error: String? = null,
    onDismissError: () -> Unit = {},
    messageLoading: Boolean = false,
    messageError: String? = null,
    onDismissMessageError: () -> Unit = {}
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedRoleFilter by remember { mutableStateOf<GameRole?>(null) }
    var selectedCityFilter by remember { mutableStateOf<String?>(null) }
    var showScreenshotDialog by remember { mutableStateOf<String?>(null) }

    val myPost = posts.find { it.playerId == currentUserId }
    val isTeamLeader = myTeams.isNotEmpty()

    val cities = remember(posts) {
        posts.map { it.city }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredPosts = posts.filter { post ->
        (selectedRoleFilter == null || post.role == selectedRoleFilter) &&
        (selectedCityFilter == null || post.city == selectedCityFilter)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────
            AnimatedEntrance(delayMillis = 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.player_finder_title),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = White
                            )
                            Text(
                                text = if (isTeamLeader) stringResource(R.string.recruit_players)
                                    else stringResource(R.string.post_stats_discovered),
                                fontSize = 13.sp,
                                color = LightGray.copy(alpha = 0.7f)
                            )
                        }

                        if (myPost == null) {
                            IconButton(
                                onClick = { showCreateSheet = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        brush = Brush.linearGradient(BlueGradient),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_post), tint = White, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            IconButton(
                                onClick = { onDeletePost(myPost.id) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ErrorRed.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.content_desc_remove_post), tint = ErrorRed, modifier = Modifier.size(22.dp))
                            }
                        }
                    }

                    if (myPost != null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SuccessGreen.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(SuccessGreen, CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.your_post_is_live),
                                        color = SuccessGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val twentyFourHoursAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                                    if (myPost.createdAt > twentyFourHoursAgo) {
                                        Text(
                                            text = stringResource(R.string.lfg_post_limit_reached),
                                            color = LightGray.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Filters ─────────────────────────────────────────
            AnimatedEntrance(delayMillis = 80) {
                Column {
                    // Role filter
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            iOSChip(
                                text = stringResource(R.string.all_roles),
                                selected = selectedRoleFilter == null,
                                onClick = { selectedRoleFilter = null }
                            )
                        }
                        items(GameRole.values()) { role ->
                            iOSChip(
                                text = role.displayName,
                                selected = selectedRoleFilter == role,
                                onClick = { selectedRoleFilter = if (selectedRoleFilter == role) null else role }
                            )
                        }
                    }

                    // City filter
                    if (cities.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                iOSChip(
                                    text = stringResource(R.string.all_cities),
                                    selected = selectedCityFilter == null,
                                    onClick = { selectedCityFilter = null }
                                )
                            }
                            items(cities) { city ->
                                iOSChip(
                                    text = city,
                                    selected = selectedCityFilter == city,
                                    onClick = { selectedCityFilter = if (selectedCityFilter == city) null else city }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Post List ────────────────────────────────────────
            PullToRefreshContainer(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading && posts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                } else if (filteredPosts.isEmpty()) {
                    EmptyPlayerFinderState(
                        isTeamLeader = isTeamLeader,
                        hasFilters = selectedRoleFilter != null || selectedCityFilter != null,
                        onPost = { showCreateSheet = true },
                        onClearFilters = { selectedRoleFilter = null; selectedCityFilter = null }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredPosts, key = { it.id }) { post ->
                            AnimatedEntrance(delayMillis = 0) {
                                PlayerCard(
                                    post = post,
                                    isTeamLeader = isTeamLeader,
                                    isMyPost = post.playerId == currentUserId,
                                    onMessage = { onMessagePlayer(post) },
                                    onInvite = { onInvitePlayer(post) },
                                    onScreenshotClick = { url -> showScreenshotDialog = url },
                                    onExpanded = { onViewCountIncrement(post.id) }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }

        // ── Screenshot Full View Dialog ──────────────────────
        if (showScreenshotDialog != null) {
            AlertDialog(
                onDismissRequest = { showScreenshotDialog = null },
                containerColor = Color.Black,
                modifier = Modifier.fillMaxWidth(),
                title = {},
                text = {
                    AsyncImage(
                        model = showScreenshotDialog,
                        contentDescription = "Profile Screenshot",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showScreenshotDialog = null }) {
                        Text(stringResource(R.string.close), color = White)
                    }
                }
            )
        }

        // ── Bottom Sheet: Create Post ────────────────────────
        if (showCreateSheet) {
            CreatePostSheet(
                currentUserProfile = currentUserProfile,
                onDismiss = { showCreateSheet = false },
                onSubmit = { post ->
                    onCreatePost(post)
                    showCreateSheet = false
                }
            )
        }

        // Messaging loading overlay
        if (messageLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        }

        // Error snackbars
        ErrorSnackbar(
            error = error,
            onDismiss = onDismissError,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        ErrorSnackbar(
            error = messageError,
            onDismiss = onDismissMessageError,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp)
        )
    }
}

// ────────────────────────────────────────────────────────────
// Player Card — Premium Redesign
// ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerCard(
    post: LfgPost,
    isTeamLeader: Boolean,
    isMyPost: Boolean = false,
    onMessage: () -> Unit,
    onInvite: () -> Unit,
    onScreenshotClick: (String) -> Unit,
    onExpanded: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val roleColor = roleColor(post.role)

    // Stable gradient per player name
    val avatarGradient = remember(post.playerName) {
        val palettes = listOf(
            BlueGradient,
            PurpleGradient,
            listOf(AndroidTeal, Color(0xFF006064)),
            PremiumOrangeGradient,
            PremiumGreenGradient
        )
        palettes[Math.abs(post.playerName.hashCode()) % palettes.size]
    }

    // Time ago
    val timeAgo = remember(post.createdAt) {
        val diff = System.currentTimeMillis() - post.createdAt
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    }
    
    // Online status indicator based on recent post time (< 1 hr)
    val isOnline = System.currentTimeMillis() - post.createdAt < 3600_000

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceCard)
            .border(
                width = 1.dp,
                color = roleColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable {
                val wasExpanded = expanded
                expanded = !expanded
                // Increment view count when expanding for the first time
                if (!wasExpanded && expanded) onExpanded()
            }
    ) {
        Column {
            // Gradient Header Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Brush.horizontalGradient(listOf(roleColor, roleColor.copy(alpha = 0.5f))))
            )
            
            Column(modifier = Modifier.padding(18.dp)) {

                // ── Top Row: Avatar + Name + Stats ────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(avatarGradient))
                                .border(2.dp, roleColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!post.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = post.avatarUrl,
                                    contentDescription = post.playerName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = post.playerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = White
                                )
                            }
                        }
                        if (isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-2).dp, y = (-2).dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                                    .border(2.dp, SurfaceCard, CircleShape)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.playerName,
                                style = iOSHeadline.copy(color = TextPrimary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (post.useMic) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Mic, stringResource(R.string.content_desc_post),
                                    tint = BluePrimary, modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = timeAgo,
                                color = TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                    Spacer(Modifier.height(4.dp))

                    // City + Role + Rank row
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (post.city.isNotBlank()) {
                            CityBadge(post.city)
                        }
                        if (isMyPost) {
                            Box(
                                modifier = Modifier
                                    .background(SuccessGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .border(1.dp, SuccessGreen.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("My Post", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(roleColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .border(1.dp, roleColor.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(post.role.displayName, color = roleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (post.rank.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(GoldPrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .border(1.dp, GoldPrimary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(post.rank, color = GoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── Stats Bar ──────────────────────────────────────
            if (post.totalMatches > 0 || post.winRate.isNotBlank() || post.rankedWinRate.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (post.totalMatches > 0) {
                        StatBadge(
                            icon = Icons.Default.SportsEsports,
                            label = stringResource(R.string.games),
                            value = post.totalMatches.toString(),
                            color = BluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (post.winRate.isNotBlank()) {
                        StatBadge(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            label = stringResource(R.string.win_rate_abbrev),
                            value = post.winRate,
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (post.rankedWinRate.isNotBlank()) {
                        StatBadge(
                            icon = Icons.Default.EmojiEvents,
                            label = stringResource(R.string.ranked_wr),
                            value = post.rankedWinRate,
                            color = GoldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Actions (Message & Invite) ────────────────────
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onMessage,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Message, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.message_action), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                if (isTeamLeader) {
                    Button(
                        onClick = onInvite,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(SuccessGradient), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonAdd, null, tint = White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.invite_action), color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Expand indicator + View count ──────────────────
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View count badge
                if (post.viewCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(BluePrimary.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .border(1.dp, BluePrimary.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.views),
                            tint = BluePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = post.viewCount.toString(),
                            color = BluePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // ── Expanded: Full Profile Stats ────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    // Wins / Losses / PTS row
                    if (post.wins > 0 || post.losses > 0 || post.pts != 0) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBadge(
                                icon = Icons.Default.EmojiEvents,
                                label = stringResource(R.string.wins_label),
                                value = post.wins.toString(),
                                color = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                            StatBadge(
                                icon = Icons.Default.Close,
                                label = stringResource(R.string.losses_label),
                                value = post.losses.toString(),
                                color = ErrorRed,
                                modifier = Modifier.weight(1f)
                            )
                            StatBadge(
                                icon = Icons.Default.Star,
                                label = stringResource(R.string.pts_label),
                                value = (if (post.pts >= 0) "+" else "") + post.pts.toString(),
                                color = GoldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

            // ── In-Game ID ───────────────────────────────────────
            if (post.inGameId.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(SurfaceOverlay, RoundedCornerShape(10.dp))
                        .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Tag, null,
                        tint = TextSecondary, modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.in_game_id_label, post.inGameId),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Main Heroes ─────────────────────────────────────
            if (post.mainHeroes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    post.mainHeroes.take(3).forEach { hero ->
                        HeroPill(hero)
                    }
                }
            }

            // ── Screenshot ───────────────────────────────────────
            if (post.screenshotUrl.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onScreenshotClick(post.screenshotUrl) }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(post.screenshotUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.content_desc_profile_screenshot),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, null, tint = White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.tap_to_view), color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Bio / Message ────────────────────────────────────
            val displayBio = if (post.bio.isNotBlank()) post.bio else post.message
            if (displayBio.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = displayBio,
                    style = iOSFootnote.copy(color = TextSecondary),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Playstyle Tags ──────────────────────────────────
            if (post.playstyleTags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    post.playstyleTags.forEach { tag ->
                        TagPill(tag)
                    }
                }
            }

            // ── Social Links ─────────────────────────────────────
            if (post.discord.isNotBlank() || post.telegram.isNotBlank() || post.vk.isNotBlank() || post.facebook.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.discord.isNotBlank()) SocialIconBadge("Discord", Color(0xFF5865F2))
                    if (post.telegram.isNotBlank()) SocialIconBadge("TG", Color(0xFF0088CC))
                    if (post.vk.isNotBlank()) SocialIconBadge("VK", Color(0xFF0077FF))
                    if (post.facebook.isNotBlank()) SocialIconBadge("FB", Color(0xFF1877F2))
                }
            }

                } // end AnimatedVisibility Column
            } // end AnimatedVisibility
        } // end padded column
    } // end main card column
    } // end Box
} // end fun PlayerCard

@Composable
private fun CityBadge(city: String) {
    Box(
        modifier = Modifier
            .background(BluePrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(1.dp, BluePrimary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(city.uppercase(), color = BluePrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun StatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(SurfaceOverlay, RoundedCornerShape(10.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(value, color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HeroPill(hero: String) {
    Box(
        modifier = Modifier
            .background(GoldPrimary.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .border(1.dp, GoldPrimary.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = GoldPrimary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(hero, color = GoldPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TagPill(tag: String) {
    Box(
        modifier = Modifier
            .background(PurplePrimary.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .border(1.dp, PurplePrimary.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(tag, color = PurplePrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SocialIconBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// ────────────────────────────────────────────────────────────
// Create Post Bottom Sheet — Full Redesign
// ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreatePostSheet(
    currentUserProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSubmit: (LfgPost) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedRole by remember { mutableStateOf(GameRole.FLEX) }
    var selectedRegion by remember { mutableStateOf(Region.UTC) }
    var selectedSkill by remember { mutableStateOf(SkillLevel.ALL) }
    var heroesInput by remember { mutableStateOf(currentUserProfile?.mainHeroes?.joinToString(", ") ?: "") }
    var bio by remember { mutableStateOf(currentUserProfile?.bio ?: "") }
    var useMic by remember { mutableStateOf(false) }
    var rank by remember { mutableStateOf(currentUserProfile?.currentTier?.name ?: "") }
    var totalMatches by remember { mutableStateOf("${currentUserProfile?.totalMatches ?: 0}") }
    var winRate by remember { mutableStateOf("") }
    var rankedWinRate by remember { mutableStateOf("") }
    var inGameId by remember { mutableStateOf(currentUserProfile?.inGameId ?: "") }
    var city by remember { mutableStateOf("") }
    var screenshotUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    var discord by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var vk by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }

    val allTags = listOf("Aggressive", "Tactical", "Shotcaller", "Late Game", "Team Player", "Objective Focus")
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    val cities = listOf("MSK", "SPB", "KRD", "EKB", "KZN", "NSK", "UFA", "VVO", "OTHER")

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> screenshotUri = uri }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(White.copy(alpha = 0.2f), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(stringResource(R.string.create_player_post), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
            Text(stringResource(R.string.show_off_stats), fontSize = 13.sp, color = LightGray.copy(alpha = 0.6f))
            Spacer(Modifier.height(24.dp))

            // ── Role ──
            SectionLabel(stringResource(R.string.main_role))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GameRole.values()) { role ->
                    val rc = roleColor(role)
                    val selected = selectedRole == role
                    Box(
                        modifier = Modifier
                            .background(if (selected) rc.copy(alpha = 0.25f) else White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .border(1.dp, if (selected) rc else White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .clickable { selectedRole = role }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(role.displayName, color = if (selected) rc else LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── City ──
            SectionLabel(stringResource(R.string.your_city))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cities) { c ->
                    val selected = city == c
                    Box(
                        modifier = Modifier
                            .background(if (selected) BluePrimary.copy(alpha = 0.25f) else White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .border(1.dp, if (selected) BluePrimary else White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .clickable { city = c }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(c, color = if (selected) BluePrimary else LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── In-Game ID ──
            SectionLabel(stringResource(R.string.in_game_id))
            OutlinedTextField(
                value = inGameId,
                onValueChange = { inGameId = it },
                placeholder = { Text(stringResource(R.string.your_game_id), color = DimGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Tag, null, tint = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = White.copy(alpha = 0.2f),
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    cursorColor = GoldPrimary
                )
            )

            Spacer(Modifier.height(20.dp))

            // ── Stats: Total Games + Win Rate + Ranked WR (stacked for readability) ──
            SectionLabel(stringResource(R.string.player_stats))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = totalMatches,
                    onValueChange = { totalMatches = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.total_games), color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = White.copy(alpha = 0.2f),
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedLabelColor = BluePrimary,
                        cursorColor = BluePrimary
                    )
                )
                OutlinedTextField(
                    value = winRate,
                    onValueChange = { winRate = it },
                    label = { Text(stringResource(R.string.win_rate_percent), color = TextSecondary) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.win_rate_example), color = DimGray) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SuccessGreen,
                        unfocusedBorderColor = White.copy(alpha = 0.2f),
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedLabelColor = SuccessGreen,
                        cursorColor = SuccessGreen
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = rankedWinRate,
                onValueChange = { rankedWinRate = it },
                label = { Text(stringResource(R.string.ranked_win_rate_percent), color = TextSecondary) },
                placeholder = { Text(stringResource(R.string.ranked_win_rate_example), color = DimGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = White.copy(alpha = 0.2f),
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedLabelColor = GoldPrimary,
                    cursorColor = GoldPrimary
                )
            )

            Spacer(Modifier.height(20.dp))

            // ── Rank ──
            SectionLabel(stringResource(R.string.current_rank))
            OutlinedTextField(
                value = rank,
                onValueChange = { rank = it },
                placeholder = { Text(stringResource(R.string.rank_example), color = DimGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.EmojiEvents, null, tint = GoldPrimary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = White.copy(alpha = 0.2f),
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    cursorColor = GoldPrimary
                )
            )

            Spacer(Modifier.height(20.dp))

            // ── Hero Picker ──
            SectionLabel(stringResource(R.string.top_3_main_heroes_hint))
            OutlinedTextField(
                value = heroesInput,
                onValueChange = { heroesInput = it },
                placeholder = { Text(stringResource(R.string.hero_examples), color = DimGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Star, null, tint = GoldPrimary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = White.copy(alpha = 0.2f),
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    cursorColor = GoldPrimary
                )
            )

            Spacer(Modifier.height(20.dp))

            // ── Screenshot Upload ──
            SectionLabel(stringResource(R.string.profile_screenshot_optional))
            if (screenshotUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    AsyncImage(
                        model = screenshotUri,
                        contentDescription = stringResource(R.string.content_desc_selected_screenshot),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(ErrorRed.copy(alpha = 0.8f), CircleShape)
                            .clickable { screenshotUri = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = White, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceOverlay)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, null, tint = BluePrimary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.tap_upload_screenshot), color = TextSecondary, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.image_content_warning),
                    color = TextTertiary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Mic Preference ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.use_microphone), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = White)
                }
                Switch(
                    checked = useMic,
                    onCheckedChange = { useMic = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = BluePrimary,
                        uncheckedThumbColor = LightGray,
                        uncheckedTrackColor = White.copy(alpha = 0.1f)
                    )
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Playstyle Tags ──
            SectionLabel(stringResource(R.string.playstyle_tags))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTags.forEach { tag ->
                    val selected = selectedTags.contains(tag)
                    Box(
                        modifier = Modifier
                            .background(if (selected) PurplePrimary.copy(alpha = 0.3f) else White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, if (selected) PurplePrimary else White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .clickable { selectedTags = if (selected) selectedTags - tag else selectedTags + tag }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(tag, color = if (selected) PurplePrimary else LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Social Links ──
            SectionLabel(stringResource(R.string.social_links_optional))
            Spacer(Modifier.height(8.dp))
            SocialInputField(value = discord, onValueChange = { discord = it }, label = stringResource(R.string.discord_username), icon = Icons.Default.Language)
            Spacer(Modifier.height(8.dp))
            SocialInputField(value = telegram, onValueChange = { telegram = it }, label = stringResource(R.string.telegram_username), icon = Icons.AutoMirrored.Filled.Send)
            Spacer(Modifier.height(8.dp))
            SocialInputField(value = vk, onValueChange = { vk = it }, label = stringResource(R.string.vk_id), icon = Icons.Default.Language)
            Spacer(Modifier.height(8.dp))
            SocialInputField(value = facebook, onValueChange = { facebook = it }, label = stringResource(R.string.facebook_name), icon = Icons.Default.Facebook)

            Spacer(Modifier.height(20.dp))

            // ── Bio / Message ──
            SectionLabel(stringResource(R.string.bio_message))
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 200) bio = it },
                placeholder = { Text(stringResource(R.string.tell_about_yourself), color = DimGray) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = White.copy(alpha = 0.2f),
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    cursorColor = BluePrimary
                )
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    scope.launch {
                        var screenshotUrl = ""
                        screenshotUri?.let { uri ->
                            isUploading = true
                            try {
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                if (bytes != null) {
                                    val path = "lfg/${System.currentTimeMillis()}_${currentUserProfile?.id ?: "anon"}.jpg"
                                    val result = SupabaseStorageUpload.uploadFile(
                                        bucket = SupabaseConfig.BUCKET_LFG_SCREENSHOTS,
                                        path = path,
                                        fileBytes = bytes,
                                        contentType = "image/jpeg"
                                    )
                                    result.onSuccess { url -> screenshotUrl = url }
                                }
                            } catch (e: Exception) {
                                Timber.w("Screenshot upload failed: ${e.message}")
                            }
                            isUploading = false
                        }

                        val heroes = heroesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3)
                        onSubmit(
                            LfgPost(
                                id = "",
                                playerId = currentUserProfile?.id ?: "",
                                playerName = currentUserProfile?.username ?: "",
                                role = selectedRole,
                                region = selectedRegion,
                                skillLevel = selectedSkill,
                                message = bio,
                                mainHeroes = heroes,
                                bio = bio,
                                rank = rank,
                                totalMatches = totalMatches.toIntOrNull() ?: 0,
                                winRate = winRate,
                                rankedWinRate = rankedWinRate,
                                inGameId = inGameId,
                                city = city,
                                screenshotUrl = screenshotUrl,
                                useMic = useMic,
                                playstyleTags = selectedTags.toList(),
                                discord = discord,
                                telegram = telegram,
                                vk = vk,
                                facebook = facebook,
                                avatarUrl = currentUserProfile?.avatarUrl,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                enabled = !isUploading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(BlueGradient), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.post_profile), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = White)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SocialInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = DimGray, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = LightGray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BluePrimary.copy(alpha = 0.5f),
            unfocusedBorderColor = White.copy(alpha = 0.1f),
            focusedTextColor = White,
            unfocusedTextColor = White
        )
    )
}

// ────────────────────────────────────────────────────────────
// Empty State
// ────────────────────────────────────────────────────────────

@Composable
private fun EmptyPlayerFinderState(
    isTeamLeader: Boolean,
    hasFilters: Boolean,
    onPost: () -> Unit,
    onClearFilters: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(BluePrimary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonSearch, contentDescription = null, tint = BluePrimary.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (hasFilters) "No matching players" else "No solo players active",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (hasFilters) "Try resetting filters to find more players."
                    else if (isTeamLeader) "Solo players will appear here once they post their info."
                    else "Be the first to post your stats and get discovered by team leaders!",
                fontSize = 14.sp,
                color = LightGray.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            if (hasFilters) {
                TextButton(onClick = onClearFilters) {
                    Text("Reset Filters", color = BluePrimary, fontWeight = FontWeight.SemiBold)
                }
            } else if (!isTeamLeader) {
                Button(
                    onClick = onPost,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Create Post", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Role Color Helper ──
private fun roleColor(role: GameRole): Color = when (role) {
    GameRole.TANK -> Color(0xFF4CAF50)
    GameRole.FIGHTER -> Color(0xFFFF9800)
    GameRole.ASSASSIN -> Color(0xFFF44336)
    GameRole.MAGE -> Color(0xFF9C27B0)
    GameRole.MARKSMAN -> Color(0xFF2196F3)
    GameRole.SUPPORT -> Color(0xFF00BCD4)
    GameRole.FLEX -> Color(0xFF607D8B)
}

@Composable
fun TeamSelectionDialog(
    playerName: String,
    myTeams: List<Team>,
    onDismiss: () -> Unit,
    onTeamSelected: (Team) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavy,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Invite $playerName",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Select which team to invite them to",
                    fontSize = 13.sp,
                    color = LightGray
                )
            }
        },
        text = {
            if (myTeams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.GroupOff,
                            contentDescription = null,
                            tint = MidGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "You need to be a team leader",
                            color = MidGray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    myTeams.forEach { team ->
                        Surface(
                            onClick = { onTeamSelected(team) },
                            shape = RoundedCornerShape(14.dp),
                            color = White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.linearGradient(BlueGradient)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!team.logoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = team.logoUrl,
                                            contentDescription = team.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = team.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = White
                                    )
                                    Text(
                                        text = "${team.currentPlayerCount}/${team.maxPlayers} members",
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LightGray)
            }
        }
    )
}
