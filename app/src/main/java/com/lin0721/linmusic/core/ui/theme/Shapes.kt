package com.lin0721.linmusic.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// MaterialTheme  of
val MimikyuShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// BottomSheet
val BottomSheetShape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

// BottomSheet  of
val DragHandleShape: Shape = RoundedCornerShape(2.dp)

// /
val RadiusCompact = 6.dp

// PlayDetails（//SongsDetails//About/Albums）
val InfoCardRadius = 16.dp

// /Chip
val PillRadius = 20.dp
val PillRadiusLarge = 25.dp
