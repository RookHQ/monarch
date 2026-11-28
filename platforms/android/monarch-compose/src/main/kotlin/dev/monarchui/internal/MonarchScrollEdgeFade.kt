package dev.monarchui.internal

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Fades the content to transparent over [width] at an edge that has more to scroll to, so a row that does not fit
 * ends in a soft edge instead of a cut-off control. Nothing is drawn at an edge that is fully scrolled. Apply it
 * to the scrolling viewport (before `horizontalScroll`), so a background set earlier is not faded.
 */
internal fun Modifier.monarchScrollEdgeFade(state: ScrollState, opaque: Color, width: Dp = 24.dp): Modifier {
    // Only the alpha of the mask matters: any opaque colour and the same colour fully transparent.
    val clear = Color(opaque.red, opaque.green, opaque.blue, 0f)
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val edge = width.toPx().coerceAtMost(size.width / 2)
            val rtl = layoutDirection == LayoutDirection.Rtl
            val fadeLeft = if (rtl) state.canScrollForward else state.canScrollBackward
            val fadeRight = if (rtl) state.canScrollBackward else state.canScrollForward
            if (fadeLeft) {
                drawRect(
                    brush = Brush.horizontalGradient(listOf(clear, opaque), 0f, edge),
                    size = Size(edge, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (fadeRight) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(opaque, clear),
                        size.width - edge,
                        size.width,
                    ),
                    topLeft = Offset(size.width - edge, 0f),
                    size = Size(edge, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
}
