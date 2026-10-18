package com.lin0721.linmusic.feature.message.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lin0721.linmusic.core.ui.components.EmptyState
import com.lin0721.linmusic.core.ui.components.SecondaryScreenScaffold

// Messages：、@、、。，
@Composable
fun MessageScreen(onBack: () -> Unit) {
    SecondaryScreenScaffold(title = "Messages", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.Notifications,
                title = "Messages feature in development",
                subtitle = "Will show notifications, mentions, replies, and messages"
            )
        }
    }
}
