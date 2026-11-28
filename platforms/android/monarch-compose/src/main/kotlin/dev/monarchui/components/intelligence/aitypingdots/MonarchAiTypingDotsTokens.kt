package dev.monarchui.components.intelligence.aitypingdots

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchAiTypingDots, built from primitives and roles only. */
internal object MonarchAiTypingDotsTokens {
    /** The number of dots. */
    const val DOT_COUNT: Int = 3

    /** The gap between dots, and how far a dot rises, as a fraction of the dot size (the web's 0.6). */
    const val GAP_FRACTION: Float = 0.6f

    /** The room above and below the dots, as a fraction of the dot size (the web's 0.5). */
    const val PADDING_FRACTION: Float = 0.5f

    /** One cycle: three quarters of the shimmer duration, as on the web. */
    const val CYCLE_MILLIS: Int = MonarchTokens.Duration.shimmer * 3 / 4

    /** Each dot starts an eighth of the shimmer duration after the one before it, as on the web. */
    const val DOT_DELAY_MILLIS: Int = MonarchTokens.Duration.shimmer / 8

    /** A dot is at the top at this fraction of the cycle and back at rest at twice it (the web's 30% and 60%). */
    const val PEAK_FRACTION: Float = 0.3f

    /** The diameter of a dot at the default font size. The dots grow with the font size setting. */
    fun dotSize(size: MonarchAiTypingDotsSize): Dp = when (size) {
        MonarchAiTypingDotsSize.Sm -> MonarchTokens.Space.space1.dp
        MonarchAiTypingDotsSize.Md -> MonarchTokens.Space.space1_5.dp
    }

    /** The colour of a dot at rest: the subtle text colour, which keeps its contrast on the light canvas. */
    fun restingColor(colors: MonarchColors): Color = colors.textSubtle

    /** The colour of a dot at the top of its rise, and of every dot when animations are off. */
    fun activeColor(colors: MonarchColors): Color = colors.aiStrong
}
