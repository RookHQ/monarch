package com.rookhq.monarch.components.feedback.skeleton

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchShapes
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchSkeleton, built from primitives and roles only. */
internal object MonarchSkeletonTokens {
    /** The default height of a block and the default diameter of a circle. */
    val DefaultSize: Dp = MonarchTokens.Space.space10.dp

    /** A text line is this fraction of the body font size high, as on the web (0.75em). */
    const val TEXT_HEIGHT_FRACTION: Float = 0.75f

    /** One half of the pulse (resting to light, then back): 0.8s, so a full cycle is the web's 1.6s. */
    const val PULSE_MILLIS: Int = MonarchTokens.Duration.slower * 2

    fun shape(shape: MonarchSkeletonShape, shapes: MonarchShapes): Shape = when (shape) {
        MonarchSkeletonShape.Text -> shapes.xs
        MonarchSkeletonShape.Rect -> shapes.md
        MonarchSkeletonShape.Circle -> shapes.full
    }

    /**
     * The resting fill, and the lighter fill the pulse reaches. The web fills a skeleton with `bgMuted`
     * and pulses its opacity; on a phone the canvas is that colour almost (1.08 to 1 in the light theme), so
     * the resting fill is the emphasis role (1.16 to 1 in light, 1.9 to 1 in dark). The pulse goes only half way to
     * `bgMuted`: all the way, the placeholder nearly vanishes into the canvas for a moment (and a still picture taken
     * at that moment shows an empty region).
     */
    fun colors(colors: MonarchColors): Pair<Color, Color> =
        colors.bgEmphasis to lerp(colors.bgEmphasis, colors.bgMuted, PULSE_DEPTH)

    /** How far the pulse travels from the resting fill towards `bgMuted`: 0 stays put, 1 reaches it. */
    const val PULSE_DEPTH: Float = 0.5f
}
