package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.theme.DragHandleShape
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// ModalBottomSheet ， dragHandle
@Composable
fun MimikyuDragHandle() {
    Box(modifier = Modifier.padding(top = 12.dp, bottom = MimikyuSpacing.xs)) {
        Surface(
            modifier = Modifier.width(40.dp).height(4.dp),
            shape = DragHandleShape,
            color = Color.White.copy(alpha = 0.3f)
        ) {}
    }
}
