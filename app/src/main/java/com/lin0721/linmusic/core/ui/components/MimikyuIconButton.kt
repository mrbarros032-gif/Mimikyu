package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.PressStyle

// and  M3 IconButton ， size
private val IconButtonSize = 40.dp
private const val DisabledContentAlpha = 0.38f

// M3 IconButton  of ：，。Play MimikyuPress.Transport
@Composable
fun MimikyuIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: PressStyle = MimikyuPress.Icon,
    // ： of  modifier ， background
    containerColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(IconButtonSize)
            .pressable(
                style = style,
                shape = CircleShape,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = LocalContentColor.current
        CompositionLocalProvider(
            LocalContentColor provides
                if (enabled) contentColor else contentColor.copy(alpha = DisabledContentAlpha),
            content = content
        )
    }
}
