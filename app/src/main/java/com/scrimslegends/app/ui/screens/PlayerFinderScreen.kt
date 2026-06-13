package com.scrimslegends.app.ui.screens

import androidx.compose.material3.MaterialTheme
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
    onInvitePlayer: ((LfgPost) -> Unit)? = null,
    onJoinDiscord: ((String) -> Unit)? = null,
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedRoleFilter by remember { mutableStateOf<GameRole?>(null) }
    var selectedCityFilter by remember { mutableStateOf<String?>(null) }
    var showScreenshotDialog by remember { mutableStateOf<String?>(null) }

    // New Advanced Filter States
    var searchQuery by remember { mutableStateOf("") }
    var useMicOnly by remember { mutableStateOf(false) }
    var selectedRankFilter by remember { mutableStateOf<String?>(null) }
    var selectedSort by remember { mutableStateOf(PlayerSortOption.NEWEST) }

    val myPost = posts.find { it.playerId == currentUserId }
    val isTeamLeader = myTeams.isNotEmpty()

    val cities = remember(posts) {
        posts.map { it.city }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val ranks = remember(posts) {
        posts.map { it.rank }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredPosts = remember(posts, selectedRoleFilter, selectedCityFilter, searchQuery, useMicOnly, selectedRankFilter, selectedSort) {
        posts.filter { post ->
            val matchesRole = selectedRoleFilter == null || post.role == selectedRoleFilter
            val matchesCity = selectedCityFilter == null || post.city == selectedCityFilter
            val matchesMic = !useMicOnly || post.useMic
            val matchesRank = selectedRankFilter == null || post.rank == selectedRankFilter
            val matchesSearch = searchQuery.isBlank() ||
                post.playerName.contains(searchQuery, ignoreCase = true) ||
                post.inGameId.contains(searchQuery, ignoreCase = true)
            matchesRole && matchesCity && matchesMic && matchesRank && matchesSearch
        }.let { list ->
            when (selectedSort) {
                PlayerSortOption.NEWEST -> list.sortedByDescending { it.createdAt }
                PlayerSortOption.WIN_RATE -> list.sortedByDescending {
                    it.winRate.replace("%", "").toFloatOrNull() ?: 0f
                }
                PlayerSortOption.POINTS -> list.sortedByDescending { it.pts }
                PlayerSortOption.MOST_VIEWED -> list.sortedByDescending { it.viewCount }
            }
        }
    }
    val activeFilterCount = listOf(
        selectedRoleFilter != null,
        selectedCityFilter != null,
        selectedRankFilter != null,
        searchQuery.isNotBlank(),
        useMicOnly
    ).count { it }
    val livePostCount = remember(posts) {
        val oneHourAgo = System.currentTimeMillis() - 3600_000
        posts.count { it.createdAt >= oneHourAgo }
    }
    val canClearFilters = activeFilterCount > 0

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
                        .padding(top = 18.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.player_finder_title),
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold,
                                color = appTextPrimaryColor()
                            )
                            Text(
                                text = if (isTeamLeader) stringResource(R.string.recruit_players)
                                    else stringResource(R.string.post_stats_discovered),
                                fontSize = 13.sp,
                                color = appTextSecondaryColor()
                            )
                        }

                        PlayerFinderPrimaryAction(
                            hasPost = myPost != null,
                            onPost = { showCreateSheet = true },
                            onDelete = { showDeleteDialog = true }
                        )
                    }

                    if (myPost != null) {
                        LivePostBanner()
                    }
                }
            }

            AnimatedEntrance(delayMillis = 60) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(appSurfaceColor())
                        .border(1.dp, appBorderColor(), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FinderStatPill(
                        icon = Icons.Default.Groups,
                        value = posts.size.toString(),
                        label = "players",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    FinderStatPill(
                        icon = Icons.Default.Bolt,
                        value = livePostCount.toString(),
                        label = "live",
                        tint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    FinderStatPill(
                        icon = Icons.Default.FilterList,
                        value = filteredPosts.size.toString(),
                        label = "shown",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            var isFilterExpanded by remember { mutableStateOf(true) }
            // ── Advanced Search & Filters ─────────────────────────────────────────
            AnimatedEntrance(delayMillis = 80) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFilterExpanded = !isFilterExpanded }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.filters_and_search), color = appTextPrimaryColor(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            if (activeFilterCount > 0) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "$activeFilterCount active",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canClearFilters) {
                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        useMicOnly = false
                                        selectedRoleFilter = null
                                        selectedCityFilter = null
                                        selectedRankFilter = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Reset", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Icon(
                                imageVector = if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Filters",
                                tint = appTextSecondaryColor()
                            )
                        }
                    }

                    AnimatedVisibility(visible = isFilterExpanded) {
                        Column {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        placeholder = { Text("Search name or MLBB ID", color = appTextSecondaryColor()) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = appTextSecondaryColor()) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null, tint = appTextSecondaryColor())
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = appSurfaceColor(),
                            unfocusedContainerColor = appSurfaceColor(),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = appBorderColor(),
                            focusedTextColor = appTextPrimaryColor(),
                            unfocusedTextColor = appTextPrimaryColor()
                        ),
                        singleLine = true
                    )

                    // Sort Options
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            Icon(Icons.AutoMirrored.Filled.Sort, null, tint = appTextSecondaryColor(), modifier = Modifier.padding(end = 4.dp))
                        }
                        items(PlayerSortOption.values()) { sortOption ->
                            iOSChip(
                                text = sortOption.displayName,
                                selected = selectedSort == sortOption,
                                onClick = { selectedSort = sortOption }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Role filter & Mic
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            iOSChip(
                                text = "Mic ready",
                                selected = useMicOnly,
                                onClick = { useMicOnly = !useMicOnly },
                                icon = Icons.Default.Mic
                            )
                        }
                        item {
                            Spacer(Modifier.width(8.dp))
                        }
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

                    // Rank filter
                    if (ranks.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                iOSChip(
                                    text = "All Ranks",
                                    selected = selectedRankFilter == null,
                                    onClick = { selectedRankFilter = null }
                                )
                            }
                            items(ranks) { rank ->
                                iOSChip(
                                    text = rank,
                                    selected = selectedRankFilter == rank,
                                    onClick = { selectedRankFilter = if (selectedRankFilter == rank) null else rank }
                                )
                            }
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                } else if (error != null && posts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = ErrorRed, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.error_loading_data), color = appTextPrimaryColor(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(error, color = appTextSecondaryColor(), fontSize = 13.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.retry), color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                } else if (filteredPosts.isEmpty()) {
                    EmptyPlayerFinderState(
                        isTeamLeader = isTeamLeader,
                        hasFilters = canClearFilters,
                        onPost = { showCreateSheet = true },
                        onClearFilters = {
                            searchQuery = ""
                            useMicOnly = false
                            selectedRoleFilter = null
                            selectedCityFilter = null
                            selectedRankFilter = null
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredPosts, key = { it.id }) { post ->
                            AnimatedEntrance(delayMillis = 0) {
                                PlayerCard(
                                    post = post,
                                    isTeamLeader = isTeamLeader,
                                    isMyPost = post.playerId == currentUserId,
                                    onMessage = { onMessagePlayer(post) },
                                    onInvite = { onInvitePlayer?.invoke(post) },
                                    onJoinDiscord = { url -> onJoinDiscord?.invoke(url) },
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
                        Text(stringResource(R.string.close), color = appTextPrimaryColor())
                    }
                }
            )
        }

        // ── Delete Confirmation Dialog ────────────────────────
        if (showDeleteDialog && myPost != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = appSurfaceColor(),
                title = { Text(stringResource(R.string.delete_post_title), color = appTextPrimaryColor()) },
                text = { Text(stringResource(R.string.delete_post_confirmation), color = appTextSecondaryColor()) },
                confirmButton = {
                    TextButton(onClick = {
                        onDeletePost(myPost.id)
                        showDeleteDialog = false
                    }) { Text(stringResource(R.string.delete_action), color = ErrorRed) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
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
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars)
        )
    }
}

// ────────────────────────────────────────────────────────────
// Player Card — Premium Redesign
// ────────────────────────────────────────────────────────────

@Composable
private fun PlayerFinderPrimaryAction(
    hasPost: Boolean,
    onPost: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(shape)
            .background(
                brush = if (hasPost) Brush.horizontalGradient(listOf(ErrorRed.copy(alpha = 0.22f), ErrorRed.copy(alpha = 0.10f)))
                else Brush.horizontalGradient(BlueGradient),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = if (hasPost) ErrorRed.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = shape
            )
            .clickable { if (hasPost) onDelete() else onPost() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (hasPost) Icons.Default.Delete else Icons.Default.Add,
                contentDescription = if (hasPost) "Remove post" else "Create post",
                tint = if (hasPost) ErrorRed else White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (hasPost) "Remove" else "Post",
                color = if (hasPost) ErrorRed else White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LivePostBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SuccessGreen.copy(alpha = 0.11f))
            .border(1.dp, SuccessGreen.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(SuccessGreen, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.your_post_is_live),
                color = SuccessGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.lfg_post_limit_reached),
                color = appTextSecondaryColor(),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FinderStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Column {
            Text(value, color = appTextPrimaryColor(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(label, color = appTextSecondaryColor(), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerCard(
    post: LfgPost,
    isTeamLeader: Boolean,
    isMyPost: Boolean = false,
    onMessage: () -> Unit,
    onInvite: () -> Unit,
    onJoinDiscord: (String) -> Unit,
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
            diff <= 0 -> "Just now"
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    }


    var hasViewed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(appSurfaceColor())
            .border(
                width = 1.dp,
                color = roleColor.copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable {
                val wasExpanded = expanded
                expanded = !expanded
                // Increment view count when expanding for the first time
                if (!wasExpanded && expanded && !hasViewed) {
                    hasViewed = true
                    onExpanded()
                }
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

            Column(modifier = Modifier.padding(16.dp)) {

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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.playerName,
                                style = iOSHeadline.copy(color = appTextPrimaryColor()),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (post.useMic) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Mic, stringResource(R.string.content_desc_post),
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = timeAgo,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(post.rank, color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── Stats Bar ──────────────────────────────────────
            if (post.totalMatches > 0 || post.winRate.isNotBlank() || post.rankedWinRate.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (post.totalMatches > 0) {
                        StatBadge(
                            icon = Icons.Default.SportsEsports,
                            label = stringResource(R.string.games),
                            value = post.totalMatches.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    if (post.winRate.isNotBlank()) {
                        StatBadge(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            label = stringResource(R.string.win_rate_abbrev),
                            value = post.winRate,
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                    if (post.rankedWinRate.isNotBlank()) {
                        StatBadge(
                            icon = Icons.Default.EmojiEvents,
                            label = stringResource(R.string.ranked_wr),
                            value = post.rankedWinRate,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }

            // ── Actions (Message & Invite) ────────────────────
            if (!isMyPost) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onMessage,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
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
                                    Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.invite_action), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = stringResource(R.string.views),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = post.viewCount.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (expanded) "Hide profile" else "View profile",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBadge(
                                icon = Icons.Default.EmojiEvents,
                                label = stringResource(R.string.wins_label),
                                value = post.wins.toString(),
                                color = SuccessGreen,
                                modifier = Modifier.weight(1f).fillMaxHeight()
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
                                color = MaterialTheme.colorScheme.secondary,
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
                        .background(appElevatedSurfaceColor(), RoundedCornerShape(10.dp))
                        .border(1.dp, appBorderColor(), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Tag, null,
                        tint = appTextSecondaryColor(), modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.in_game_id_label, post.inGameId),
                        color = appTextSecondaryColor(),
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
                            Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.tap_to_view), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                    style = iOSFootnote.copy(color = appTextSecondaryColor()),
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
            } // end Playstyle Tags

            // ── Social Links ─────────────────────────────────────
            if (post.discord.isNotBlank() || post.telegram.isNotBlank() || post.vk.isNotBlank() || post.facebook.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.discord.isNotBlank()) SocialIconBadge("Discord", Color(0xFF5865F2)) { onJoinDiscord(post.discord) }
                    if (post.telegram.isNotBlank()) SocialIconBadge("TG", Color(0xFF0088CC)) {}
                    if (post.vk.isNotBlank()) SocialIconBadge("VK", Color(0xFF0077FF)) {}
                    if (post.facebook.isNotBlank()) SocialIconBadge("FB", Color(0xFF1877F2)) {}
                }
            }

            // ── Quick Actions ─────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isTeamLeader && !isMyPost) {
                    Button(
                        onClick = { onInvite() },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.invite), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
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
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(city.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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
            .background(appElevatedSurfaceColor(), RoundedCornerShape(10.dp))
            .border(1.dp, appBorderColor(), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                color = appTextSecondaryColor(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(value, color = appTextPrimaryColor(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HeroPill(hero: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(hero, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
private fun SocialIconBadge(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
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
    var uploadError by remember { mutableStateOf<String?>(null) }

    var discord by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var vk by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }

    val allTags = listOf("Aggressive", "Tactical", "Shotcaller", "Late Game", "Team Player", "Objective Focus")
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    val cities = listOf("MSK", "SPB", "KRD", "EKB", "KZN", "NSK", "UFA", "VVO", "OTHER")

    fun validateRequiredFields(): String? {
        val totalMatchesValue = totalMatches.toIntOrNull()
        return when {
            currentUserProfile?.id.isNullOrBlank() -> "Profile is still loading. Try again in a moment."
            currentUserProfile?.username.isNullOrBlank() -> "Username is required before posting."
            city.isBlank() -> "Select your city before posting."
            inGameId.isBlank() -> "In-game ID is required."
            totalMatchesValue == null || totalMatchesValue <= 0 -> "Total games must be greater than 0."
            winRate.isBlank() -> "Win rate is required."
            rankedWinRate.isBlank() -> "Ranked win rate is required."
            rank.isBlank() -> "Current rank is required."
            heroesInput.split(",").none { it.trim().isNotEmpty() } -> "Add at least one main hero."
            bio.isBlank() -> "Bio/message is required."
            else -> null
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> screenshotUri = uri }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = appSurfaceColor(),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
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
            Text(stringResource(R.string.create_player_post), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = appTextPrimaryColor())
            Text(stringResource(R.string.show_off_stats), fontSize = 13.sp, color = appTextSecondaryColor())
            Spacer(Modifier.height(24.dp))

            // ── Role ──
            SectionLabel(stringResource(R.string.main_role))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GameRole.values()) { role ->
                    val rc = roleColor(role)
                    val selected = selectedRole == role
                    Box(
                        modifier = Modifier
                            .background(if (selected) rc.copy(alpha = 0.25f) else appElevatedSurfaceColor(), RoundedCornerShape(10.dp))
                            .border(1.dp, if (selected) rc else appBorderColor(), RoundedCornerShape(10.dp))
                            .clickable { selectedRole = role }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(role.displayName, color = if (selected) rc else appTextSecondaryColor(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else appElevatedSurfaceColor(), RoundedCornerShape(10.dp))
                            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else appBorderColor(), RoundedCornerShape(10.dp))
                            .clickable { city = c }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(c, color = if (selected) MaterialTheme.colorScheme.primary else appTextSecondaryColor(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── In-Game ID ──
            SectionLabel(stringResource(R.string.in_game_id))
            OutlinedTextField(
                value = inGameId,
                onValueChange = { inGameId = it },
                placeholder = { Text(stringResource(R.string.your_game_id), color = appTextSecondaryColor()) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Tag, null, tint = appTextSecondaryColor()) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = appBorderColor(),
                    focusedTextColor = appTextPrimaryColor(),
                    unfocusedTextColor = appTextPrimaryColor(),
                    cursorColor = MaterialTheme.colorScheme.secondary
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
                    label = { Text(stringResource(R.string.total_games), color = appTextSecondaryColor()) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = appBorderColor(),
                        focusedTextColor = appTextPrimaryColor(),
                        unfocusedTextColor = appTextPrimaryColor(),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                OutlinedTextField(
                    value = winRate,
                    onValueChange = { winRate = it },
                    label = { Text(stringResource(R.string.win_rate_percent), color = appTextSecondaryColor()) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.win_rate_example), color = appTextSecondaryColor()) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SuccessGreen,
                        unfocusedBorderColor = appBorderColor(),
                        focusedTextColor = appTextPrimaryColor(),
                        unfocusedTextColor = appTextPrimaryColor(),
                        focusedLabelColor = SuccessGreen,
                        cursorColor = SuccessGreen
                    )
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = rankedWinRate,
                onValueChange = { rankedWinRate = it },
                label = { Text(stringResource(R.string.ranked_win_rate_percent), color = appTextSecondaryColor()) },
                placeholder = { Text(stringResource(R.string.ranked_win_rate_example), color = appTextSecondaryColor()) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = appBorderColor(),
                    focusedTextColor = appTextPrimaryColor(),
                    unfocusedTextColor = appTextPrimaryColor(),
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
            )

            Spacer(Modifier.height(20.dp))

            // ── Rank ──
            SectionLabel(stringResource(R.string.current_rank))
            OutlinedTextField(
                value = rank,
                onValueChange = { rank = it },
                placeholder = { Text(stringResource(R.string.rank_example), color = appTextSecondaryColor()) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.secondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = appBorderColor(),
                    focusedTextColor = appTextPrimaryColor(),
                    unfocusedTextColor = appTextPrimaryColor(),
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
            )

            Spacer(Modifier.height(20.dp))

            // ── Hero Picker ──
            SectionLabel(stringResource(R.string.top_3_main_heroes_hint))
            OutlinedTextField(
                value = heroesInput,
                onValueChange = { heroesInput = it },
                placeholder = { Text(stringResource(R.string.hero_examples), color = appTextSecondaryColor()) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.secondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = appBorderColor(),
                    focusedTextColor = appTextPrimaryColor(),
                    unfocusedTextColor = appTextPrimaryColor(),
                    cursorColor = MaterialTheme.colorScheme.secondary
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
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(appElevatedSurfaceColor())
                        .border(1.dp, appBorderColor(), RoundedCornerShape(14.dp))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.tap_upload_screenshot), color = appTextSecondaryColor(), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.image_content_warning),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.use_microphone), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = appTextPrimaryColor())
                }
                Switch(
                    checked = useMic,
                    onCheckedChange = { useMic = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = appTextSecondaryColor(),
                        uncheckedTrackColor = appElevatedSurfaceColor()
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
                            .background(if (selected) PurplePrimary.copy(alpha = 0.3f) else appElevatedSurfaceColor(), RoundedCornerShape(8.dp))
                            .border(1.dp, if (selected) PurplePrimary else appBorderColor(), RoundedCornerShape(8.dp))
                            .clickable { selectedTags = if (selected) selectedTags - tag else selectedTags + tag }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(tag, color = if (selected) PurplePrimary else appTextSecondaryColor(), fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                placeholder = { Text(stringResource(R.string.tell_about_yourself), color = appTextSecondaryColor()) },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = appBorderColor(),
                    focusedTextColor = appTextPrimaryColor(),
                    unfocusedTextColor = appTextPrimaryColor(),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = submit@{
                    val validationError = validateRequiredFields()
                    if (validationError != null) {
                        uploadError = validationError
                        return@submit
                    }

                    scope.launch {
                        uploadError = null
                        var screenshotUrl = ""
                        var uploadSuccess = true

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
                                    result.onFailure {
                                        uploadSuccess = false
                                        uploadError = "Upload failed: ${it.message}"
                                    }
                                }
                            } catch (e: Exception) {
                                uploadSuccess = false
                                uploadError = "Upload failed: ${e.message}"
                            }
                            isUploading = false
                        }

                        if (!uploadSuccess) return@launch

                        val heroes = heroesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3)
                        onSubmit(
                            LfgPost(
                                id = "",
                                playerId = currentUserProfile?.id ?: "",
                                playerName = currentUserProfile?.username ?: "",
                                role = selectedRole,
                                region = selectedRegion,
                                skillLevel = selectedSkill,
                                message = bio.trim(),
                                mainHeroes = heroes,
                                bio = bio.trim(),
                                rank = rank.trim(),
                                totalMatches = totalMatches.toIntOrNull() ?: 0,
                                winRate = winRate.trim(),
                                rankedWinRate = rankedWinRate.trim(),
                                inGameId = inGameId.trim(),
                                city = city,
                                screenshotUrl = screenshotUrl,
                                useMic = useMic,
                                playstyleTags = selectedTags.toList(),
                                discord = discord.trim(),
                                telegram = telegram.trim(),
                                vk = vk.trim(),
                                facebook = facebook.trim(),
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
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.post_profile), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            uploadError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = appTextPrimaryColor())
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
        placeholder = { Text(label, color = appTextSecondaryColor(), fontSize = 13.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = appTextSecondaryColor(), modifier = Modifier.size(18.dp)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = appBorderColor(),
            focusedTextColor = appTextPrimaryColor(),
            unfocusedTextColor = appTextPrimaryColor()
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonSearch, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (hasFilters) stringResource(R.string.no_matching_players) else stringResource(R.string.no_solo_players_active),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = appTextPrimaryColor(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (hasFilters) stringResource(R.string.no_matching_players_hint)
                    else if (isTeamLeader) stringResource(R.string.no_solo_players_team_leader_hint)
                    else stringResource(R.string.no_solo_players_solo_hint),
                fontSize = 14.sp,
                color = appTextSecondaryColor(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            if (hasFilters) {
                TextButton(onClick = onClearFilters) {
                    Text(stringResource(R.string.reset_filters), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            } else if (!isTeamLeader) {
                Button(
                    onClick = onPost,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.create_post), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Role Color Helper ──
internal fun roleColor(role: GameRole): Color = when (role) {
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
        containerColor = appSurfaceColor(),
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Invite $playerName",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = appTextPrimaryColor()
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Select which team to invite them to",
                    fontSize = 13.sp,
                    color = appTextSecondaryColor()
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "You need to be a team leader",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = appElevatedSurfaceColor(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
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
                                        .background(MaterialTheme.colorScheme.primary),
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
                                            tint = MaterialTheme.colorScheme.onSurface,
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
                                        color = appTextPrimaryColor()
                                    )
                                    Text(
                                        text = "${team.currentPlayerCount}/${team.maxPlayers} members",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                Text("Cancel", color = appTextSecondaryColor())
            }
        }
    )
}

enum class PlayerSortOption(val displayName: String) {
    NEWEST("Newest"),
    WIN_RATE("Win Rate"),
    POINTS("Points"),
    MOST_VIEWED("Most Viewed")
}
