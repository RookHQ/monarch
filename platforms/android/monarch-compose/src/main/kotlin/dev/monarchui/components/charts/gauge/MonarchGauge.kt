package dev.monarchui.components.charts.gauge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.R
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchTonePalette
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchChartValueFormat
import dev.monarchui.theme.MonarchTheme
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A half-circle meter for one reading against a scale: utilisation, a health
 * score, a quota used.
 *
 * Built by Monarch with a Compose `Canvas`: Material 3 has no gauge. Bands
 * give the scale a meaning, a target is marked with a tick, and the value
 * and the band's name are written as text, so the reading never depends on
 * the arc or on colour. The gauge is one TalkBack element with a progress
 * range. The scale starts at the start edge, so the dial is mirrored in
 * right-to-left layouts. Changes of the value are animated, and immediate
 * when the user has removed animations.
 *
 * @param value the reading. `null`, or a value that is not finite, shows an empty track and a dash. Values
 * outside the range are drawn at the nearest end; the text shows the real value.
 * @param modifier the modifier of the gauge.
 * @param range the scale. A range whose ends are equal is widened by one.
 * @param bands stretches of the scale with a tone and a name.
 * @param target a target on the scale, marked with a tick and added to the spoken value.
 * @param tone the colour of the value. `null` takes the tone of the value's band, or the accent without bands.
 * @param indicator an arc filled to the value, or a needle over the bands.
 * @param size the width of the dial.
 * @param showRange writes the ends of the scale under the dial.
 * @param label the visible label under the gauge and its spoken name.
 * @param description a second line under the label.
 * @param formatValue writes every number. `null` writes a number in the current locale.
 * @param valueLabel replaces the value TalkBack reads.
 * @param noValueLabel replaces the built-in text "No data" (`monarch_gauge_no_value`).
 */
@Composable
public fun MonarchGauge(
    value: Double?,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Double> = 0.0..DefaultMaximum,
    bands: List<MonarchGaugeBand> = emptyList(),
    target: Double? = null,
    tone: MonarchTone? = null,
    indicator: MonarchGaugeIndicator = MonarchGaugeIndicator.Arc,
    size: MonarchGaugeSize = MonarchGaugeSize.Md,
    showRange: Boolean = true,
    label: String? = null,
    description: String? = null,
    formatValue: ((Double) -> String)? = null,
    valueLabel: String? = null,
    noValueLabel: String? = null,
) {
    val scale = MonarchGaugeScale.of(range)
    val reading = value?.takeIf { it.isFinite() }
    val band = reading?.let { MonarchGaugeTokens.bandAt(bands, it, scale) }
    val resolved = MonarchGaugeTokens.resolvedTone(tone, reading != null, band, bands.isNotEmpty())
    val colors = MonarchTheme.colors
    val dialColors = MonarchGaugeTokens.colors(resolved, colors)
    // As the web: the value and the band name take the strong colour of a toned reading.
    val readingColor = resolved?.takeIf { it != MonarchTone.Neutral }
        ?.let { MonarchTonePalette.of(it, colors).strong } ?: colors.textDefault
    val write = rememberMonarchChartValueFormat(formatValue)
    val noValue = monarchString(noValueLabel, R.string.monarch_gauge_no_value)
    val name = label ?: stringResource(R.string.monarch_gauge_name)
    val spoken = valueLabel ?: spokenValue(reading, band, target, scale, write, noValue)
    val share by animateFloatAsState(
        targetValue = reading?.let(scale::share) ?: 0f,
        animationSpec = MonarchTheme.motion.spec(),
        label = "gauge",
    )
    val width = MonarchGaugeTokens.width(size)
    val valueInside = MonarchGaugeTokens.valueInside(indicator, LocalDensity.current.fontScale)
    val valueText = reading?.let(write) ?: Dash
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val bandSpans = bands.map { band ->
        (scale.share(band.range.start) to scale.share(band.range.endInclusive)) to
            MonarchGaugeTokens.toneColor(band.tone, colors)
    }
    Column(
        modifier = modifier
            .width(width)
            .clearAndSetSemantics {
                contentDescription = name
                stateDescription = spoken
                reading?.let { progressBarRangeInfo = scale.rangeInfo(it) }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MonarchGaugeTokens.TextGap),
    ) {
        Box(modifier = Modifier.size(width, width / 2), contentAlignment = Alignment.BottomCenter) {
            Canvas(modifier = Modifier.fillMaxWidth().height(width / 2)) {
                scale(scaleX = if (rtl) -1f else 1f, scaleY = 1f) {
                    drawDial(
                        share = if (reading != null) share else null,
                        bands = bandSpans,
                        target = target?.let(scale::share),
                        indicator = indicator,
                        colors = dialColors,
                    )
                }
            }
            if (valueInside) ValueText(valueText, readingColor)
        }
        if (showRange) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Caption(write(scale.low))
                Caption(write(scale.high))
            }
        }
        GaugeText(
            value = valueText.takeIf { !valueInside },
            valueColor = readingColor,
            band = band?.label,
            label = label,
            description = description,
        )
    }
}

/** The text under the dial: the value when it is not inside, the band's name, the label and the description. */
@Composable
private fun GaugeText(value: String?, valueColor: Color, band: String?, label: String?, description: String?) {
    value?.let { ValueText(it, valueColor) }
    band?.let { Caption(it, valueColor.takeIf { c -> c != MonarchTheme.colors.textDefault }) }
    label?.let {
        Text(
            text = it,
            style = MonarchTheme.typography.label,
            color = MonarchTheme.colors.textDefault,
            textAlign = TextAlign.Center,
        )
    }
    description?.let { Caption(it) }
}

/** The end of the default scale. */
private const val DefaultMaximum = 100.0

/** A dash for a missing value; TalkBack reads the no-value text instead. */
private const val Dash = "–"

@Composable
private fun ValueText(text: String, color: Color) {
    Text(
        text = text,
        style = MonarchTheme.typography.heading,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Caption(text: String, color: Color? = null) {
    Text(
        text = text,
        style = MonarchTheme.typography.caption,
        color = color ?: MonarchTheme.colors.textMuted,
        textAlign = TextAlign.Center,
    )
}

@Composable
@Suppress("LongParameterList")
private fun spokenValue(
    reading: Double?,
    band: MonarchGaugeBand?,
    target: Double?,
    scale: MonarchGaugeScale,
    write: (Double) -> String,
    noValue: String,
): String {
    if (reading == null) return noValue
    val text = write(reading)
    val bandLabel = band?.label
    val base = if (bandLabel != null) {
        stringResource(R.string.monarch_gauge_value_band, text, bandLabel)
    } else {
        stringResource(R.string.monarch_gauge_value_range, text, write(scale.low), write(scale.high))
    }
    return if (target != null) stringResource(R.string.monarch_gauge_target, base, write(target)) else base
}

/** Draws the dial left to right; the caller mirrors it in right-to-left layouts. */
private fun DrawScope.drawDial(
    share: Float?,
    bands: List<Pair<Pair<Float, Float>, Color>>,
    target: Float?,
    indicator: MonarchGaugeIndicator,
    colors: MonarchGaugeColors,
) {
    val stroke = size.width * MonarchGaugeTokens.THICKNESS
    val radius = (size.width - stroke) / 2
    val center = Offset(size.width / 2, size.height)
    val gap = MonarchGaugeTokens.Gap.toPx()
    arc(center, radius, 0f, 1f, colors.track, stroke)
    val bandRadius = if (indicator == MonarchGaugeIndicator.Arc) radius - stroke / 2 - gap else radius
    val bandStroke = if (indicator == MonarchGaugeIndicator.Arc) MonarchGaugeTokens.BandStroke.toPx() else stroke
    bands.forEach { (span, color) -> arc(center, bandRadius, span.first, span.second, color, bandStroke) }
    if (share != null && indicator == MonarchGaugeIndicator.Arc && share > 0f) {
        arc(center, radius, 0f, share, colors.value, stroke)
    }
    val mark = MonarchGaugeTokens.MarkStroke.toPx()
    target?.let { at ->
        drawLine(
            color = colors.mark,
            start = point(center, radius - stroke / 2 - gap, at),
            end = point(center, radius + stroke / 2, at),
            strokeWidth = mark,
        )
    }
    if (share != null && indicator == MonarchGaugeIndicator.Needle) {
        drawLine(
            color = colors.mark,
            start = center,
            end = point(center, radius - stroke / 2 - gap, share),
            strokeWidth = mark,
            cap = StrokeCap.Round,
        )
        drawCircle(color = colors.mark, radius = stroke / 2, center = center)
    }
}

/** The point at [share] of the half circle, from the left end (0) over the top to the right end (1). */
private fun point(center: Offset, radius: Float, share: Float): Offset {
    val angle = PI * (1 - share)
    return Offset(center.x + radius * cos(angle).toFloat(), center.y - radius * sin(angle).toFloat())
}

@Suppress("LongParameterList")
private fun DrawScope.arc(center: Offset, radius: Float, from: Float, to: Float, color: Color, stroke: Float) {
    if (to <= from) return
    drawArc(
        color = color,
        startAngle = HalfTurnDegrees + from * HalfTurnDegrees,
        sweepAngle = (to - from) * HalfTurnDegrees,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = stroke),
    )
}

private const val HalfTurnDegrees = 180f
