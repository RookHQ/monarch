package dev.monarchui.components.forms.slider

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The colours of a slider in one state. */
internal data class MonarchSliderColors(val track: Color, val range: Color, val thumb: Color, val thumbBorder: Color)

/** Component tokens of MonarchSlider and MonarchRangeSlider, from `slider.css`. */
internal object MonarchSliderTokens {
    /** The thumb's ring: a 1dp hairline in the range colour (the web's 2dp reads heavy at native density). */
    val ThumbBorder: Dp = MonarchTokens.Space.px.dp

    /** The gap between the label row and the slider. */
    val LabelGap: Dp = MonarchTokens.Space.space1.dp

    /**
     * The colours: the muted-emphasis track with the accent range and a surface thumb with an accent border.
     * Invalid uses the danger solid; disabled the disabled text colour with a subtle thumb.
     */
    fun colors(enabled: Boolean, invalid: Boolean, colors: MonarchColors): MonarchSliderColors = when {
        !enabled -> MonarchSliderColors(colors.bgEmphasis, colors.textDisabled, colors.bgSubtle, colors.textDisabled)
        invalid -> MonarchSliderColors(colors.bgEmphasis, colors.dangerSolid, colors.bgSurface, colors.dangerSolid)
        else -> MonarchSliderColors(colors.bgEmphasis, colors.accentSolid, colors.bgSurface, colors.accentSolid)
    }

    /** The fraction of the track the range covers, for [value] in [range]. */
    fun fraction(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        val span = range.endInclusive - range.start
        return if (span <= 0f) 0f else ((value - range.start) / span).coerceIn(0f, 1f)
    }
}
