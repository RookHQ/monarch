package dev.monarchui.components.datadisplay.progress

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.theme.MonarchTheme

/**
 * The Monarch progress bar, drawn by Monarch (not the Material 3 indicator, which leaves a gap between the fill
 * and the track and draws a stop dot at the end): a pill track in the emphasis role and a pill fill from the start
 * edge, as on the web. Without [fraction] a 40 % segment slides across the track every 1.4 s; with "Remove
 * animations" on it stands still at the start. The node carries the range (or "indeterminate") and [description].
 */
@Composable
internal fun ProgressBar(
    modifier: Modifier,
    height: Dp,
    fraction: Float?,
    description: String?,
    colors: MonarchProgressColors,
) {
    val still = fraction == null && MonarchTheme.motion.reducedMotion
    val slide = if (fraction == null && !still) slidingSegment() else remember { mutableFloatStateOf(0f) }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .progressSemantics(fraction, description),
    ) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(colors.track, cornerRadius = radius)
        val (start, length) = when {
            fraction != null -> 0f to size.width * fraction
            still -> 0f to size.width * MonarchProgressTokens.SEGMENT_FRACTION
            else -> {
                val segment = size.width * MonarchProgressTokens.SEGMENT_FRACTION
                // From fully before the start edge to fully after the end edge.
                (-segment + (size.width + segment) * slide.value) to segment
            }
        }
        drawFill(start, length, colors, radius)
    }
}

/** The fill from [start] (from the start edge) for [length] px, clipped to the track, mirrored in RTL. */
private fun DrawScope.drawFill(start: Float, length: Float, colors: MonarchProgressColors, radius: CornerRadius) {
    val from = start.coerceIn(0f, size.width)
    val to = (start + length).coerceIn(0f, size.width)
    if (to - from <= 0f) return
    val left = if (layoutDirection == LayoutDirection.Rtl) size.width - to else from
    drawRoundRect(
        color = colors.indicator,
        topLeft = Offset(left, 0f),
        size = Size(to - from, size.height),
        cornerRadius = radius,
    )
}

/**
 * The Monarch progress ring: a full track circle and a round-capped arc from 12 o'clock, clockwise (mirrored in
 * RTL), with no gap between them. Without [fraction] a quarter arc turns once every 0.7 s like the spinner; with
 * "Remove animations" on it stands still.
 */
@Composable
internal fun ProgressRing(
    modifier: Modifier,
    diameter: Dp,
    stroke: Dp,
    fraction: Float?,
    description: String?,
    colors: MonarchProgressColors,
    semantics: Boolean = true,
) {
    val still = fraction == null && MonarchTheme.motion.reducedMotion
    val turn = if (fraction == null && !still) turning() else remember { mutableFloatStateOf(0f) }
    val node = if (semantics) Modifier.progressSemantics(fraction, description) else Modifier.clearAndSetSemantics { }
    Canvas(modifier.size(diameter).then(node)) {
        val width = stroke.toPx()
        val inset = width / 2
        val arcSize = Size(size.width - width, size.height - width)
        val topLeft = Offset(inset, inset)
        drawArc(colors.track, 0f, FullTurn, false, topLeft, arcSize, style = Stroke(width))
        val sweep = FullTurn * (fraction ?: MonarchProgressTokens.STILL_FRACTION)
        if (sweep <= 0f) return@Canvas
        val rtl = layoutDirection == LayoutDirection.Rtl
        val startAngle = TopAngle + turn.value * FullTurn
        drawArc(
            color = colors.indicator,
            startAngle = if (rtl) TopAngle - (startAngle - TopAngle) - sweep else startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width, cap = StrokeCap.Round),
        )
    }
}

private fun Modifier.progressSemantics(fraction: Float?, description: String?): Modifier = clearAndSetSemantics {
    progressBarRangeInfo = if (fraction == null) {
        ProgressBarRangeInfo.Indeterminate
    } else {
        ProgressBarRangeInfo(fraction, 0f..1f)
    }
    if (description != null) contentDescription = description
}

@Composable
private fun slidingSegment(): State<Float> = rememberInfiniteTransition(label = "monarch-progress-bar").animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        tween(MonarchProgressTokens.SLIDE_MILLIS, easing = MonarchTheme.motion.standard),
    ),
    label = "segment",
)

@Composable
private fun turning(): State<Float> = rememberInfiniteTransition(label = "monarch-progress-ring").animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(MonarchProgressTokens.TURN_MILLIS, easing = LinearEasing)),
    label = "turn",
)

private const val FullTurn = 360f
private const val TopAngle = -90f
