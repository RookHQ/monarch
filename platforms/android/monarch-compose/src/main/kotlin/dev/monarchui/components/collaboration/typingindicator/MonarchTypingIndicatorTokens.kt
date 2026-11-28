package dev.monarchui.components.collaboration.typingindicator

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchTypingIndicator, built from primitives and roles only. */
internal object MonarchTypingIndicatorTokens {
    val HorizontalPadding: Dp = MonarchTokens.Space.space1.dp
    val ContentGap: Dp = MonarchTokens.Space.space1_5.dp
    val DotGap: Dp = MonarchTokens.Space.space1.dp

    /** The diameter of a dot at the default font size. The dots grow with the font size setting. */
    val DotSize: Dp = MonarchTokens.Space.space1.dp

    /** How far a dot rises at the top of its bounce. */
    val BounceHeight: Dp = MonarchTokens.Space.space0_5.dp

    /** The number of dots. */
    const val DOT_COUNT: Int = 3

    /** One bounce cycle: three quarters of the shimmer duration, as on the web. */
    const val CYCLE_MILLIS: Int = MonarchTokens.Duration.shimmer * 3 / 4

    /** Each dot starts this much after the one before it. */
    const val DOT_DELAY_MILLIS: Int = MonarchTokens.Duration.shimmer * 3 / 32

    /** A dot is at the top of its bounce at this fraction of the cycle and back down at twice it. */
    const val PEAK_FRACTION: Float = 0.3f

    /** The colour of a resting dot. */
    fun restingDotColor(colors: MonarchColors): Color = colors.textSubtle

    /** The colour of a dot at the top of its bounce, and of every dot when animations are off. */
    fun activeDotColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the sentence. */
    fun textColor(colors: MonarchColors): Color = colors.textMuted

    /**
     * The sentence for [names]: empty for nobody, then one, two or several
     * people. The wording is passed in, so it can come from resources or the
     * caller.
     */
    fun sentence(
        names: List<String>,
        one: (String) -> String,
        two: (String, String) -> String,
        several: () -> String,
    ): String = when (names.size) {
        0 -> ""
        1 -> one(names[0])
        2 -> two(names[0], names[1])
        else -> several()
    }
}
