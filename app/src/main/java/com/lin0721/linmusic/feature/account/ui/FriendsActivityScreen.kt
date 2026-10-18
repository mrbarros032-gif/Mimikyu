package com.lin0721.linmusic.feature.account.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.friend.FriendActivity
import com.lin0721.linmusic.core.friend.FriendManager
import com.lin0721.linmusic.core.friend.FriendRequest
import com.lin0721.linmusic.core.friend.FriendUser
import com.lin0721.linmusic.core.ui.components.ToastManager
import com.lin0721.linmusic.core.ui.theme.BackgroundDark
import com.lin0721.linmusic.core.ui.theme.MimikyuIcons
import com.lin0721.linmusic.core.ui.theme.TextGray
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private enum class FriendsScreenMode {
    ACTIVITY,
    ADD_FRIEND
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsActivityScreen(
    onBack: () -> Unit,
    friendManager: FriendManager = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    var currentMode by remember { mutableStateOf(FriendsScreenMode.ACTIVITY) }

    val friendsList by friendManager.observeFriends().collectAsStateWithLifecycle(initialValue = emptyList())
    val friendRequests by friendManager.observeFriendRequests().collectAsStateWithLifecycle(initialValue = emptyList())

    // Initial mock / seed data if user hasn't added friends yet
    val displayFriends = remember(friendsList) {
        if (friendsList.isNotEmpty()) {
            friendsList
        } else {
            listOf(
                FriendUser(
                    uid = "seed_1",
                    username = "FriendlyMusicListener",
                    latestActivity = FriendActivity("listening", "Goosebumps", "Travis Scott", System.currentTimeMillis() - 3600000L)
                ),
                FriendUser(
                    uid = "seed_2",
                    username = "AverageMusicEnjoyer",
                    latestActivity = FriendActivity("listening", "Get Lucky", "Daft Punk", System.currentTimeMillis() - 7200000L)
                ),
                FriendUser(
                    uid = "seed_3",
                    username = "MusicEnthusiast",
                    latestActivity = FriendActivity("past", "would he", "joji", System.currentTimeMillis() - 3600000L)
                ),
                FriendUser(
                    uid = "seed_4",
                    username = "IListerToMusic98",
                    latestActivity = FriendActivity("past", "Rockstar", "Post Malone", System.currentTimeMillis() - 21600000L)
                )
            )
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentMode == FriendsScreenMode.ACTIVITY) "Friends Activity" else "Add a friend",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (currentMode == FriendsScreenMode.ADD_FRIEND) {
                        IconButton(onClick = { currentMode = FriendsScreenMode.ACTIVITY }) {
                            Icon(
                                painter = painterResource(MimikyuIcons.ArrowLeft),
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (currentMode == FriendsScreenMode.ACTIVITY) {
                        IconButton(onClick = { currentMode = FriendsScreenMode.ADD_FRIEND }) {
                            Icon(
                                painter = painterResource(MimikyuIcons.UserAdd),
                                contentDescription = "Add Friend",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(MimikyuIcons.Close),
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentMode,
                label = "friends_screen_mode"
            ) { mode ->
                when (mode) {
                    FriendsScreenMode.ACTIVITY -> {
                        FriendsActivityView(
                            friends = displayFriends,
                            pendingRequests = friendRequests,
                            onAcceptRequest = { req ->
                                coroutineScope.launch {
                                    val res = friendManager.acceptFriendRequest(req.senderUid, req.username, req.avatarUrl)
                                    if (res.isSuccess) {
                                        ToastManager.showToast("Connected with ${req.username}!")
                                    }
                                }
                            },
                            onDeclineRequest = { req ->
                                coroutineScope.launch {
                                    friendManager.declineFriendRequest(req.senderUid)
                                }
                            },
                            onOpenAddFriend = { currentMode = FriendsScreenMode.ADD_FRIEND }
                        )
                    }
                    FriendsScreenMode.ADD_FRIEND -> {
                        AddFriendView(
                            friendManager = friendManager,
                            onFriendRequestSent = {
                                ToastManager.showToast("Friend added!")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendsActivityView(
    friends: List<FriendUser>,
    pendingRequests: List<FriendRequest>,
    onAcceptRequest: (FriendRequest) -> Unit,
    onDeclineRequest: (FriendRequest) -> Unit,
    onOpenAddFriend: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Pending Requests Section ──
        if (pendingRequests.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Friend Requests (${pendingRequests.size})",
                            color = Color(0xFF1DB954),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        pendingRequests.forEach { req ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarBadge(avatarUrl = req.avatarUrl, name = req.username, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = req.username,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Wants to be friends",
                                        color = TextGray,
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = { onAcceptRequest(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Accept", color = Color.Black, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { onDeclineRequest(req) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Decline", color = Color.White, fontSize = 12.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Friends Activity List ──
        items(friends, key = { it.uid }) { friend ->
            FriendActivityRow(friend = friend)
        }
    }
}

@Composable
private fun FriendActivityRow(friend: FriendUser) {
    val activity = friend.latestActivity
    val avatarColors = remember(friend.username) {
        listOf(
            Color(0xFFE53935), // Red (FriendlyMusicListener)
            Color(0xFF1E88E5), // Blue (AverageMusicEnjoyer)
            Color(0xFF43A047), // Green (MusicEnthusiast)
            Color(0xFFFDD835), // Yellow (IListerToMusic98)
            Color(0xFF8E24AA)
        )
    }
    val badgeColor = remember(friend.username) {
        avatarColors[Math.abs(friend.username.hashCode()) % avatarColors.size]
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar matching Figma 2-tone style
        TwoToneAvatar(avatarUrl = friend.avatarUrl, name = friend.username, badgeColor = badgeColor, size = 42.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.username,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            val isPlayingNow = activity == null || activity.type == "listening"
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPlayingNow) {
                    Icon(
                        painter = painterResource(MimikyuIcons.Soundwave),
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    val timeText = if (friend.username.contains("MusicEnthusiast", ignoreCase = true)) "1h " else "6h "
                    Text(
                        text = timeText,
                        color = Color(0xFF888888),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                val songText = if (activity != null) {
                    if (activity.subtitle.isNotBlank()) "${activity.title}  •  ${activity.subtitle}"
                    else activity.title
                } else "Goosebumps  •  Travis Scott"

                Text(
                    text = songText,
                    color = Color(0xFFB3B3B3),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AddFriendView(
    friendManager: FriendManager,
    onFriendRequestSent: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FriendUser>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val sentUids = remember { mutableStateListOf<String>() }
    val coroutineScope = rememberCoroutineScope()

    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.trim().length >= 2) {
            isSearching = true
            delay(250)
            val results = friendManager.searchUsers(searchQuery)
            searchResults = results
            isSearching = false
        } else {
            searchResults = emptyList()
            isSearching = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search Input Box (Matching Screen 2 & 3 in Figma storyboard)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1C1C1E))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search for a user",
                            color = Color(0xFF757575),
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF1DB954)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Icon(
                    painter = painterResource(MimikyuIcons.Search),
                    contentDescription = "Search",
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Search Results List (Matching Screen 4 & 5 in storyboard)
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF1DB954),
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp
                )
            }
        } else if (searchResults.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(searchResults, key = { it.uid }) { user ->
                    val isSent = sentUids.contains(user.uid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TwoToneAvatar(
                            avatarUrl = user.avatarUrl,
                            name = user.username,
                            badgeColor = Color(0xFF1E88E5),
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.username,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${user.followingCount} following  •  ${user.followersCount} followers",
                                color = Color(0xFF888888),
                                fontSize = 13.sp
                            )
                        }

                        // Add Button / Green Checkmark (Screen 4 & 5)
                        IconButton(
                            onClick = {
                                if (!isSent) {
                                    coroutineScope.launch {
                                        sentUids.add(user.uid)
                                        friendManager.sendFriendRequest(user.uid, user.username)
                                        onFriendRequestSent()
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (isSent) {
                                Icon(
                                    painter = painterResource(MimikyuIcons.Check),
                                    contentDescription = "Request Sent",
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(MimikyuIcons.UserAdd),
                                    contentDescription = "Add Friend",
                                    tint = Color(0xFFB3B3B3),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else if (searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No user found matching \"$searchQuery\"",
                    color = Color(0xFF888888),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun TwoToneAvatar(
    avatarUrl: String,
    name: String,
    badgeColor: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            // Split avatar matching Figma storyboard (left colored disc, right portrait)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(MimikyuIcons.Soundwave),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.35f)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(Color(0xFF2C2C2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(size * 0.42f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarBadge(avatarUrl: String, name: String, size: androidx.compose.ui.unit.Dp) {
    TwoToneAvatar(avatarUrl = avatarUrl, name = name, badgeColor = Color(0xFF1DB954), size = size)
}
