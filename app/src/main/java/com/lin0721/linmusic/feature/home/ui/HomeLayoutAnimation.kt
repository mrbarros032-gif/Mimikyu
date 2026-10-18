package com.lin0721.linmusic.feature.home.ui

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import com.lin0721.linmusic.core.ui.theme.LayoutReflowDurationMs

// of  LookaheadScope。Play and ，
// and ，，
val LocalHomeLookaheadScope = staticCompositionLocalOf<LookaheadScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
private val HomeReflowBoundsTransform = BoundsTransform { _, _ ->
    tween(LayoutReflowDurationMs, easing = FastOutSlowInEasing)
}

// ； and （animateBounds ）
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.homeReflowBounds(): Modifier {
    val scope = LocalHomeLookaheadScope.current ?: return this
    return this.animateBounds(lookaheadScope = scope, boundsTransform = HomeReflowBoundsTransform)
}
