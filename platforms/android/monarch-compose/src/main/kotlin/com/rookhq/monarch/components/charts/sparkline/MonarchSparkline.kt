package com.rookhq.monarch.components.charts.sparkline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.R
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchChartValueFormat
import com.rookhq.monarch.theme.MonarchTheme
import java.text.NumberFormat

/**
 * A word-sized trend line, area or bar chart for tables, stat cards and lists.
 *
 * Built by Monarch with a Compose `Canvas`: Material 3 has no chart. With a
 * [label] the sparkline is one TalkBack element with the image role, named by
 * the label, whose state is a text alternative ("latest 11, low 3, high 11").
 * Without a label it is decoration and hidden, so put the numbers in text
 * nearby. The chart runs from the start edge, so it is mirrored in
 * right-to-left layouts. It does not animate.
 *
 * @param values the values in order. `null`, or a value that is not finite, is a gap: the line breaks there.
 * @param modifier the modifier of the sparkline.
 * @param tone a colour with meaning. `null` takes the surrounding content colour. Never the only signal:
 * show the value or the change beside it.
 * @param variant a line, an area or bars.
 * @param range fixes the value range, so sparklines in a table share one scale. `null` fits the values.
 * @param inverted draws smaller values higher, as for a rank.
 * @param showLastPoint puts a dot on the latest point. Lines and areas only.
 * @param fullWidth fills the available width instead of 80dp.
 * @param label the spoken name, such as "Sign-ups, last 8 days". Without it the sparkline is hidden from
 * TalkBack.
 * @param summary replaces the spoken text alternative.
 * @param formatValue writes the numbers of the text alternative. `null` writes a number in the current locale.
 * @param color a chart series colour, as the web's sparkline colour, instead of [tone]. `null` uses [tone].
 * @param notEnoughDataLabel replaces the built-in text "Not enough data" (`monarch_sparkline_not_enough_data`).
 */
@Composable
public fun MonarchSparkline(
    values: List<Double?>,
    modifier: Modifier = Modifier,
    tone: MonarchTone? = null,
    variant: MonarchSparklineVariant = MonarchSparklineVariant.Line,
    range: ClosedFloatingPointRange<Double>? = null,
    inverted: Boolean = false,
    showLastPoint: Boolean = true,
    fullWidth: Boolean = false,
    label: String? = null,
    summary: String? = null,
    formatValue: ((Double) -> String)? = null,
    notEnoughDataLabel: String? = null,
    color: MonarchChartColor? = null,
) {
    val clean = remember(values) { MonarchSparklineGeometry.clean(values) }
    val stats = remember(clean) { MonarchSparklineGeometry.stats(clean) }
    val sized = if (fullWidth) modifier.fillMaxWidth() else modifier.width(MonarchSparklineTokens.Width)
    if (stats == null || !MonarchSparklineGeometry.hasEnough(stats, variant)) {
        val text = monarchString(notEnoughDataLabel, R.string.monarch_sparkline_not_enough_data)
        Box(
            modifier = sized
                .heightIn(min = MonarchSparklineTokens.Height)
                .then(
                    if (label != null) {
                        Modifier.clearAndSetSemantics {
                            contentDescription = label
                            stateDescription = text
                        }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(text = text, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
        }
        return
    }
    val spoken = summary ?: spokenSummary(stats, tone, formatValue)
    val colors = MonarchSparklineTokens.colors(tone, MonarchTheme.colors, LocalContentColor.current, color)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val semantics = if (label != null) {
        Modifier.clearAndSetSemantics {
            contentDescription = label
            stateDescription = spoken
            role = Role.Image
        }
    } else {
        Modifier.clearAndSetSemantics { }
    }
    Canvas(modifier = sized.height(MonarchSparklineTokens.Height).then(semantics)) {
        val frame = MonarchSparklineFrame(
            width = size.width,
            height = size.height,
            pad = MonarchSparklineTokens.Padding.toPx(),
            domain = MonarchSparklineGeometry.domain(stats, range, variant),
            inverted = inverted,
        )
        val mirror = if (rtl) -1f else 1f
        scale(scaleX = mirror, scaleY = 1f) {
            if (variant == MonarchSparklineVariant.Bar) {
                drawBars(clean, frame, colors)
            } else {
                drawTrend(clean, frame, variant == MonarchSparklineVariant.Area, showLastPoint, colors)
            }
        }
    }
}

private fun DrawScope.drawBars(clean: List<Double?>, frame: MonarchSparklineFrame, colors: MonarchSparklineColors) {
    MonarchSparklineGeometry.bars(clean, frame).forEach { bar ->
        if (bar != null) drawRect(color = colors.stroke, topLeft = bar.topLeft, size = bar.size)
    }
}

private fun DrawScope.drawTrend(
    clean: List<Double?>,
    frame: MonarchSparklineFrame,
    area: Boolean,
    showLastPoint: Boolean,
    colors: MonarchSparklineColors,
) {
    val points = MonarchSparklineGeometry.points(clean, frame)
    val segments = MonarchSparklineGeometry.segments(points).filter { it.size > 1 }
    if (area) segments.forEach { drawPath(path = areaPath(it, frame.baseline), color = colors.fill) }
    val stroke = Stroke(
        width = MonarchSparklineTokens.StrokeWidth.toPx(),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    segments.forEach { drawPath(path = linePath(it), color = colors.stroke, style = stroke) }
    MonarchSparklineGeometry.dots(points, showLastPoint).forEach { dot ->
        drawCircle(color = colors.stroke, radius = MonarchSparklineTokens.DotRadius.toPx(), center = dot)
    }
}

private fun linePath(run: List<Offset>): Path = Path().apply {
    moveTo(run.first().x, run.first().y)
    run.drop(1).forEach { lineTo(it.x, it.y) }
}

private fun areaPath(run: List<Offset>, baseline: Float): Path = Path().apply {
    moveTo(run.first().x, baseline)
    run.forEach { lineTo(it.x, it.y) }
    lineTo(run.last().x, baseline)
    close()
}

@Composable
private fun spokenSummary(
    stats: MonarchSparklineStats,
    tone: MonarchTone?,
    formatValue: ((Double) -> String)?,
): String {
    val write = rememberMonarchChartValueFormat(formatValue)
    val text = stringResource(R.string.monarch_sparkline_summary, write(stats.last), write(stats.min), write(stats.max))
    return when (tone) {
        MonarchTone.Warning -> stringResource(R.string.monarch_sparkline_warning, text)
        MonarchTone.Danger -> stringResource(R.string.monarch_sparkline_critical, text)
        else -> text
    }
}
