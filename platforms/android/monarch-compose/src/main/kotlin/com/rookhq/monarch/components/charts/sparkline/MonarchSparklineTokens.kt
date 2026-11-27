package com.rookhq.monarch.components.charts.sparkline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The colours of a sparkline.
 *
 * @property stroke the line, the dots and the bars.
 * @property fill the area under the line.
 */
@Immutable
internal data class MonarchSparklineColors(val stroke: Color, val fill: Color)

/** Component tokens of MonarchSparkline, built from primitives and roles only. */
internal object MonarchSparklineTokens {
    /** 80dp: the width of a word-sized sparkline. */
    val Width: Dp = MonarchTokens.Space.space20.dp

    /** 24dp. */
    val Height: Dp = MonarchTokens.Space.space6.dp
    val StrokeWidth: Dp = MonarchTokens.Space.space0_5.dp
    val DotRadius: Dp = MonarchTokens.Space.space0_5.dp
    val Padding: Dp = MonarchTokens.Space.space0_5.dp

    /**
     * Without a tone the line takes the surrounding content colour and the
     * area the muted background; with one, the strong colour of the tone and
     * its subtle background.
     */
    fun colors(
        tone: MonarchTone?,
        colors: MonarchColors,
        contentColor: Color,
        series: MonarchChartColor? = null,
    ): MonarchSparklineColors {
        if (series != null) {
            val stroke = MonarchChartLegendTokens.color(series, colors)
            return MonarchSparklineColors(stroke, lerp(stroke, colors.bgSurface, FILL_SOFTEN))
        }
        if (tone == null) return MonarchSparklineColors(contentColor, colors.bgMuted)
        val palette = MonarchTonePalette.of(tone, colors)
        return MonarchSparklineColors(palette.strong, palette.subtle)
    }

    private const val FILL_SOFTEN = 0.8f
}
