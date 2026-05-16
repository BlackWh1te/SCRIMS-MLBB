package com.mlbb.scrim.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlbb.scrim.data.model.*
import com.mlbb.scrim.data.model.UserProfile
import com.mlbb.scrim.data.model.Team
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.iOSChip
import com.mlbb.scrim.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerFinderScreen(
    posts: List<LfgPost>,
    isLoading: Boolean,
    currentUserId: String,
    currentUserName: String,
    currentUserProfile: UserProfile?,
    myTeams: List<Team>,
    onCreatePost: (GameRole, Region, SkillLevel, String, List<String>, String, Boolean, List<String>, String, String, String, String) -> Unit,
    onDeletePost: (String) -> Unit,
    onMessagePlayer: (LfgPost) -> Unit,
    onRefresh: () -> Unit
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedRoleFilter by remember { mutableStateOf<GameRole?>(null) }
    var selectedRegionFilter by remember { mutableStateOf<Region?>(null) }
    
    // Invite Dialog State
    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedPlayerForInvite by remember { mutableStateOf<LfgPost?>(null) }

    val myPost = posts.find { it.playerId == currentUserId }
    val isTeamLeader = myTeams.isNotEmpty()

    val filteredPosts = posts.filter { post ->
        post.playerId != currentUserId &&
        (selectedRoleFilter == null || post.role == selectedRoleFilter) &&
        (selectedRegionFilter == null || post.region == selectedRegionFilter)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──────────────────────────────────────────
            AnimatedEntrance(delayMillis = 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Player Finder",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                                Text(
                                    text = if (isTeamLeader) "Recruit pro players for your squad" else "Post your stats & get discovered",
                                    fontSize = 13.sp,
                                    color = LightGray.copy(alpha = 0.7f)
                                )
                            }

                            if (myPost == null) {
                                IconButton(
                                    onClick = { showCreateSheet = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            brush = Brush.linearGradient(listOf(BluePrimary, Color(0xFF0A5A9F))),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Post", tint = White, modifier = Modifier.size(22.dp))
                                }
                            } else {
                                IconButton(
                                    onClick = { onDeletePost(myPost.id) },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFFF3B30).copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove post", tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // My Post Banner
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
                                    Text(
                                        text = "Your post is live — players can see you!",
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

            // ── Role Filter Chips ────────────────────────────────
            AnimatedEntrance(delayMillis = 80) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        iOSChip(
                            text = "All Roles",
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
            }

            Spacer(Modifier.height(12.dp))

            // ── Post List ────────────────────────────────────────
            if (isLoading && posts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            } else if (filteredPosts.isEmpty()) {
                EmptyPlayerFinderState(
                    isTeamLeader = isTeamLeader,
                    hasFilters = selectedRoleFilter != null,
                    onPost = { showCreateSheet = true },
                    onClearFilters = { selectedRoleFilter = null }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPosts, key = { it.id }) { post ->
                        AnimatedEntrance(delayMillis = 0) {
                            PlayerCard(
                                post = post,
                                isTeamLeader = isTeamLeader,
                                onMessage = { onMessagePlayer(post) },
                                onInvite = { 
                                    selectedPlayerForInvite = post
                                    showInviteDialog = true
                                }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }

        // ── Bottom Sheet: Create Post ────────────────────────
        if (showCreateSheet) {
            CreatePostSheet(
                currentUserProfile = currentUserProfile,
                onDismiss = { showCreateSheet = false },
                onSubmit = { role, region, skill, msg, heroes, bio, useMic, tags, ds, tg, vk, fb ->
                    onCreatePost(role, region, skill, msg, heroes, bio, useMic, tags, ds, tg, vk, fb)
                    showCreateSheet = false
                }
            )
        }

        // ── Team Invitation Dialog ───────────────────────────
        if (showInviteDialog && selectedPlayerForInvite != null) {
            InviteToTeamDialog(
                player = selectedPlayerForInvite!!,
                myTeams = myTeams,
                onDismiss = { showInviteDialog = false },
                onSendInvite = { teamId ->
                    onMessagePlayer(selectedPlayerForInvite!!)
                    showInviteDialog = false
                }
            )
        }
    }
}

// ────────────────────────────────────────────────────────────
// Player Card
// ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerCard(
    post: LfgPost,
    isTeamLeader: Boolean,
    onMessage: () -> Unit,
    onInvite: () -> Unit
) {
    val roleColor = roleColor(post.role)
    val isOnline = remember { mutableStateOf(java.util.Random().nextBoolean()) }
    val uriHandler = LocalUriHandler.current

    // Stable gradient per player name
    val avatarGradient = remember(post.playerName) {
        val palettes = listOf(
            listOf(com.mlbb.scrim.ui.theme.BluePrimary, Color(0xFF0D47A1)),
            listOf(Color(0xFF7C4DFF), Color(0xFF4527A0)),
            listOf(Color(0xFF00BCD4), Color(0xFF006064)),
            listOf(Color(0xFFFF9800), Color(0xFFE65100)),
            listOf(Color(0xFF4CAF50), Color(0xFF1B5E20))
        )
        palettes[Math.abs(post.playerName.hashCode()) % palettes.size]
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(com.mlbb.scrim.ui.theme.SurfaceCard)
            .border(
                width = 1.dp,
                color = roleColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Top Row: Avatar + Name + Region ─────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(avatarGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.playerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = com.mlbb.scrim.ui.theme.White
                        )
                    }
                    // Online indicator
                    if (isOnline.value) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .background(com.mlbb.scrim.ui.theme.DarkNavy, CircleShape)
                                .padding(2.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(com.mlbb.scrim.ui.theme.SuccessGreen, CircleShape)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.playerName,
                            style = com.mlbb.scrim.ui.theme.iOSHeadline.copy(
                                color = com.mlbb.scrim.ui.theme.TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (post.useMic) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Mic, "Mic On",
                                tint = com.mlbb.scrim.ui.theme.BluePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    if (post.rank.isNotBlank()) {
                        Text(
                            post.rank,
                            style = com.mlbb.scrim.ui.theme.iOSCaption1.copy(
                                color = com.mlbb.scrim.ui.theme.GoldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Region chip
                Box(
                    modifier = Modifier
                        .background(
                            com.mlbb.scrim.ui.theme.SurfaceOverlay,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            com.mlbb.scrim.ui.theme.GlassBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        post.region.displayName,
                        style = com.mlbb.scrim.ui.theme.iOSCaption2.copy(
                            color = com.mlbb.scrim.ui.theme.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Role / Skill / Tag Chips ─────────────────────────
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Role chip
                Box(
                    modifier = Modifier
                        .background(roleColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .border(1.dp, roleColor.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(post.role.displayName, color = roleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                // Skill chip
                Box(
                    modifier = Modifier
                        .background(com.mlbb.scrim.ui.theme.SurfaceOverlay, RoundedCornerShape(8.dp))
                        .border(1.dp, com.mlbb.scrim.ui.theme.GlassBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        post.skillLevel.name,
                        color = com.mlbb.scrim.ui.theme.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Playstyle tags
                post.playstyleTags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF6C63FF).copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF8B85FF).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(tag, color = Color(0xFF9D97FF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                    if (post.discord.isNotBlank())  SocialBadge("Discord", Color(0xFF5865F2))
                    if (post.telegram.isNotBlank()) SocialBadge("TG",      Color(0xFF0088CC))
                    if (post.vk.isNotBlank())       SocialBadge("VK",      Color(0xFF0077FF))
                    if (post.facebook.isNotBlank()) SocialBadge("FB",      Color(0xFF1877F2))
                }
            }

            // ── Main Heroes ──────────────────────────────────────
            if (post.mainHeroes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Star, null,
                        tint = com.mlbb.scrim.ui.theme.GoldPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    post.mainHeroes.take(3).forEach { hero ->
                        Box(
                            modifier = Modifier
                                .background(
                                    com.mlbb.scrim.ui.theme.GoldPrimary.copy(alpha = 0.10f),
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    com.mlbb.scrim.ui.theme.GoldPrimary.copy(alpha = 0.22f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                hero,
                                color = com.mlbb.scrim.ui.theme.GoldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ── Bio ──────────────────────────────────────────────
            val displayBio = if (post.bio.isNotBlank()) post.bio else post.message
            if (displayBio.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = displayBio,
                    style = com.mlbb.scrim.ui.theme.iOSFootnote.copy(
                        color = com.mlbb.scrim.ui.theme.TextSecondary
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = com.mlbb.scrim.ui.theme.GlassBorder)
            Spacer(Modifier.height(12.dp))

            // ── Action Buttons ───────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Message
                OutlinedButton(
                    onClick = onMessage,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, com.mlbb.scrim.ui.theme.BluePrimary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = com.mlbb.scrim.ui.theme.BluePrimary
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Message, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Message", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                // Invite (team leaders only)
                if (isTeamLeader) {
                    Button(
                        onClick = onInvite,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(com.mlbb.scrim.ui.theme.SuccessGradient),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonAdd, null, tint = com.mlbb.scrim.ui.theme.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Invite", color = com.mlbb.scrim.ui.theme.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ────────────────────────────────────────────────────────────
// Create Post Bottom Sheet
// ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreatePostSheet(
    currentUserProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSubmit: (GameRole, Region, SkillLevel, String, List<String>, String, Boolean, List<String>, String, String, String, String) -> Unit
) {
    var selectedRole by remember { mutableStateOf(GameRole.FLEX) }
    var selectedRegion by remember { mutableStateOf(Region.UTC) }
    var selectedSkill by remember { mutableStateOf(SkillLevel.ALL) }
    var message by remember { mutableStateOf("") }
    var heroesInput by remember { mutableStateOf(currentUserProfile?.mainHeroes?.joinToString(", ") ?: "") }
    var bio by remember { mutableStateOf(currentUserProfile?.bio ?: "") }
    var useMic by remember { mutableStateOf(false) }
    
    // Social Links
    var discord by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var vk by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }

    val allTags = listOf("Aggressive", "Tactical", "Shotcaller", "Late Game", "Team Player", "Objective Focus")
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0D1B2E),
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
            Text("Post as Solo Player", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
            Text("Let team leaders find and invite you", fontSize = 13.sp, color = LightGray.copy(alpha = 0.6f))
            Spacer(Modifier.height(24.dp))

            // Role
            Text("Main Role", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = White)
            Spacer(Modifier.height(8.dp))
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

            // Mic Preference
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use Microphone?", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = White)
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

            // Playstyle Tags
            Text("Playstyle Tags", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = White)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allTags.forEach { tag ->
                    val selected = selectedTags.contains(tag)
                    Box(
                        modifier = Modifier
                            .background(if (selected) Color(0xFF6C63FF).copy(alpha = 0.3f) else White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .border(1.dp, if (selected) Color(0xFF8B85FF) else White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .clickable { selectedTags = if (selected) selectedTags - tag else selectedTags + tag }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(tag, color = if (selected) Color(0xFF8B85FF) else LightGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Social Links
            Text("Social Links (Optional)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = White)
            Spacer(Modifier.height(12.dp))
            
            SocialInputField(value = discord, onValueChange = { discord = it }, label = "Discord Username", icon = Icons.Default.Language)
            Spacer(Modifier.height(8.dp))
            SocialInputField(value = telegram, onValueChange = { telegram = it }, label = "Telegram Username", icon = Icons.Default.Send)
            Spacer(Modifier.height(8.dp))
            SocialInputField(value = vk, onValueChange = { vk = it }, label = "VK Profile ID/Link", icon = Icons.Default.Language)
            Spacer(Modifier.height(8.dp))
            SocialInputField(value = facebook, onValueChange = { facebook = it }, label = "Facebook Name/Link", icon = Icons.Default.Facebook)

            Spacer(Modifier.height(24.dp))

            // Heroes
            Text("Top 3 Main Heroes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = White)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = heroesInput,
                onValueChange = { heroesInput = it },
                placeholder = { Text("e.g. Fanny, Gusion, Lancelot", color = DimGray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = White.copy(alpha = 0.2f),
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    cursorColor = GoldPrimary
                )
            )

            Spacer(Modifier.height(16.dp))

            // Message
            Text("Message to Team Leaders", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = White)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= 160) message = it },
                placeholder = { Text("What are you looking for in a team?", color = DimGray) },
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
                    val heroes = heroesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3)
                    onSubmit(selectedRole, selectedRegion, selectedSkill, message, heroes, bio, useMic, selectedTags.toList(), discord, telegram, vk, facebook)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(BluePrimary, Color(0xFF0A5A9F))), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Post Profile", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = White)
                }
            }
        }
    }
}

@Composable
private fun SocialInputField(value: String, onValueChange: (String) -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
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
// Invite Dialog
// ────────────────────────────────────────────────────────────

@Composable
private fun InviteToTeamDialog(
    player: LfgPost,
    myTeams: List<Team>,
    onDismiss: () -> Unit,
    onSendInvite: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0D1B2E),
        title = {
            Text("Invite ${player.playerName}", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Select a team to invite this player to:", color = LightGray, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(myTeams) { team ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .clickable { onSendInvite(team.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(BluePrimary.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(team.name.firstOrNull()?.toString() ?: "", color = BluePrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(team.name, color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// ────────────────────────────────────────────────────────────
// Empty State & FlowRow Helper
// ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
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
                    Icon(Icons.Default.Add, contentDescription = null, tint = White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Post My Profile", fontWeight = FontWeight.Bold, color = White)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

private fun roleColor(role: GameRole) = when (role) {
    GameRole.TANK      -> Color(0xFF4CAF50)
    GameRole.FIGHTER   -> Color(0xFFFF9800)
    GameRole.ASSASSIN  -> Color(0xFFF44336)
    GameRole.MAGE      -> Color(0xFF9C27B0)
    GameRole.MARKSMAN  -> Color(0xFF2196F3)
    GameRole.SUPPORT   -> Color(0xFF00BCD4)
    GameRole.FLEX      -> Color(0xFF607D8B)
}
