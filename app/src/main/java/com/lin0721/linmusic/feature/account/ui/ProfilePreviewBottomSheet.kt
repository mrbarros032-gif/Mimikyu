package com.lin0721.linmusic.feature.account.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lin0721.linmusic.core.friend.FriendManager
import com.lin0721.linmusic.core.friend.FriendUser
import com.lin0721.linmusic.core.ui.components.ToastManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePreviewBottomSheet(
    targetUid: String?,
    targetUsername: String?,
    onDismiss: () -> Unit,
    friendManager: FriendManager = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var targetUser by remember { mutableStateOf<FriendUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFollowingLoading by remember { mutableStateOf(false) }

    val isAlreadyFollowing by if (targetUser != null) {
        friendManager.isFollowing(targetUser!!.uid).collectAsStateWithLifecycle(initialValue = false)
    } else {
        remember { mutableStateOf(false) }
    }

    LaunchedEffect(targetUid, targetUsername) {
        isLoading = true
        if (!targetUid.isNullOrBlank()) {
            targetUser = friendManager.getUserProfileByUid(targetUid)
        } else if (!targetUsername.isNullOrBlank()) {
            val results = friendManager.searchUsers(targetUsername)
            targetUser = results.firstOrNull { it.username.equals(targetUsername, ignoreCase = true) }
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Close Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                }
            } else if (targetUser == null) {
                Text(
                    text = "User not found",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "The shared profile link may be invalid or expired.",
                    color = Color(0xFF888888),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                val user = targetUser!!

                // Avatar
                if (user.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.displayName,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E2E2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = user.displayName.ifBlank { user.username },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (user.username.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@${user.username}",
                        color = Color(0xFF1DB954),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Instant Follow / Add Friend Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isFollowingLoading = true
                            if (isAlreadyFollowing) {
                                friendManager.unfollowTarget(user.uid, isArtist = false)
                                ToastManager.showToast("Unfollowed @${user.username}")
                            } else {
                                val res = friendManager.followUser(
                                    targetUid = user.uid,
                                    targetDisplayName = user.displayName.ifBlank { user.username },
                                    targetUsername = user.username,
                                    targetAvatarUrl = user.avatarUrl
                                )
                                if (res.isSuccess) {
                                    ToastManager.showToast("Following @${user.username}!")
                                } else {
                                    ToastManager.showToast(res.exceptionOrNull()?.message ?: "Failed to follow")
                                }
                            }
                            isFollowingLoading = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAlreadyFollowing) Color(0xFF333333) else Color(0xFF1DB954),
                        contentColor = if (isAlreadyFollowing) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !isFollowingLoading
                ) {
                    if (isFollowingLoading) {
                        CircularProgressIndicator(
                            color = if (isAlreadyFollowing) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (isAlreadyFollowing) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Following", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Follow / Add Friend", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
