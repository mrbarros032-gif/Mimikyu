package com.lin0721.linmusic.feature.home.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.lin0721.linmusic.core.auth.UserProfile
import com.lin0721.linmusic.core.ui.interaction.pressable
import com.lin0721.linmusic.core.ui.theme.FilterPillInactive
import com.lin0721.linmusic.core.ui.theme.MimikyuPress
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing
import com.lin0721.linmusic.core.ui.theme.PillRadius
import com.lin0721.linmusic.core.ui.theme.PressStyle
import com.lin0721.linmusic.core.ui.theme.RobotoFontFamily

@Composable
fun HomeSharedHeader(
    userProfile: UserProfile? = null,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    secondarySelected: Boolean = false,
    onSecondarySelected: () -> Unit = {},
    showFilterPills: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    onAvatarClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(2f)
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MimikyuSpacing.sm, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showFilterPills) {
                FilterPills(
                    selectedIndex = selectedTab,
                    onSelected = onTabSelected,
                    secondarySelected = secondarySelected,
                    onSecondarySelected = onSecondarySelected,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                )
            }
        }
    }
}

private val PillShapeFull = RoundedCornerShape(PillRadius)

@Composable
fun FilterPills(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    secondarySelected: Boolean = false,
    onSecondarySelected: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp)
) {
    val labels = remember { listOf("Tudo", "Curtidas", "Descobrir") }

    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(labels, key = { _, label -> label }) { index, label ->
            FilterPillChip(
                text = label,
                selected = index == selectedIndex,
                pressStyle = MimikyuPress.Pill,
                onClick = { onSelected(index) },
                modifier = Modifier.padding(start = if (index == 0) 0.dp else 12.dp)
            )
        }
    }
}

@Composable
private fun FilterPillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape = PillShapeFull,
    pressStyle: PressStyle = MimikyuPress.Pill,
    animateColors: Boolean = true,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = FilterPillInactive,
    modifier: Modifier = Modifier
) {
    val targetBackground = if (selected) activeColor else inactiveColor
    val targetContent = if (selected) Color.Black else Color.White
    val animatedBackground by animateColorAsState(targetValue = targetBackground, label = "pillBackground")
    val animatedContent by animateColorAsState(targetValue = targetContent, label = "pillContent")
    val backgroundColor = if (animateColors) animatedBackground else targetBackground
    val contentColor = if (animateColors) animatedContent else targetContent
    Box(
        modifier = modifier
            .height(36.dp)
            .pressable(pressStyle, onClick = onClick)
            .clip(shape)
            .background(backgroundColor)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 13.5.sp,
            fontFamily = RobotoFontFamily,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
