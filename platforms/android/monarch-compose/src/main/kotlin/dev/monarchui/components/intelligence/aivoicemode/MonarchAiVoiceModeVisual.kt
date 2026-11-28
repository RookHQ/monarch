package dev.monarchui.components.intelligence.aivoicemode

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.monarchui.theme.MonarchTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * The calm central visual of `MonarchAiVoiceMode`, drawn on a `Canvas`: a
 * halo that grows with the level, an orb and, in it, level bars, waiting dots,
 * an error mark or a resting line. Decorative and hidden from TalkBack: the
 * state text carries the meaning. It holds still when animations are off.
 */
@Composable
internal fun MonarchAiVoiceModeVisual(
    state: MonarchAiVoiceModeState,
    level: Float,
    size: MonarchAiVoiceModeSize,
    modifier: Modifier = Modifier,
) {
    val tokens = MonarchAiVoiceModeTokens
    val reduced = MonarchTheme.motion.reducedMotion
    val shown by animateFloatAsState(
        targetValue = MonarchAiVoiceModeRules.shownLevel(level, state, reduced),
        animationSpec = MonarchTheme.motion.spec(MonarchTheme.motion.fast),
        label = "voice level",
    )
    val colors = tokens.visualColors(state, MonarchTheme.colors)
    val glyph = MonarchAiVoiceModeRules.glyph(state)
    val phase = if (glyph == MonarchAiVoiceModeGlyph.Dots && !reduced) wavePhase() else null
    val orb = tokens.orbSize(size)
    Canvas(
        modifier = modifier
            .size(orb * (tokens.HALO_REST + tokens.HALO_GROWTH))
            .clearAndSetSemantics { },
    ) {
        val orbRadius = orb.toPx() / 2
        drawCircle(color = colors.halo, radius = orbRadius * tokens.haloScale(shown))
        drawCircle(color = colors.orb, radius = orbRadius)
        colors.ring?.let {
            val stroke = tokens.RingStroke.toPx()
            drawCircle(color = it, radius = orbRadius - stroke / 2, style = Stroke(width = stroke))
        }
        val area = orbRadius * 2 * tokens.GLYPH_AREA
        when (glyph) {
            MonarchAiVoiceModeGlyph.Bars -> {
                val heights = if (reduced) tokens.ReducedBarHeights else tokens.barHeights(shown)
                drawBars(area, heights, colors.glyph)
            }
            MonarchAiVoiceModeGlyph.Dots -> drawDots(area, phase?.value, colors.glyph)
            MonarchAiVoiceModeGlyph.Error -> drawErrorMark(area, colors.glyph)
            MonarchAiVoiceModeGlyph.Muted, MonarchAiVoiceModeGlyph.Rest -> drawRestLine(area, colors.glyph)
        }
    }
}

@Composable
private fun wavePhase(): State<Float> = rememberInfiniteTransition(label = "voice dots").animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(MonarchAiVoiceModeTokens.DOT_CYCLE_MILLIS, easing = LinearEasing)),
    label = "phase",
)

/** Five rounded bars, centred, with the gap as wide as a bar. Symmetric, so the same in both directions. */
private fun DrawScope.drawBars(area: Float, heights: List<Float>, color: Color) {
    val count = heights.size
    val bar = area / (count * 2 - 1)
    val left = center.x - area / 2
    heights.forEachIndexed { index, share ->
        val height = (area * share).coerceAtLeast(bar)
        drawRoundRect(
            color = color,
            topLeft = Offset(left + index * bar * 2, center.y - height / 2),
            size = Size(bar, height),
            cornerRadius = CornerRadius(bar / 2),
        )
    }
}

/** Three dots; while animations run they rise in turn. */
private fun DrawScope.drawDots(area: Float, phase: Float?, color: Color) {
    val count = MonarchAiVoiceModeTokens.DOT_COUNT
    val step = area / count
    val radius = step / DotStepPerRadius
    val first = center.x - step * (count - 1) / 2
    repeat(count) { index ->
        val rise = phase?.let { sin(2 * PI * (it - index.toFloat() / count)).toFloat().coerceAtLeast(0f) } ?: 0f
        drawCircle(color = color, radius = radius, center = Offset(first + index * step, center.y - rise * radius))
    }
}

/** An exclamation mark: a rounded stroke and a dot under it. */
private fun DrawScope.drawErrorMark(area: Float, color: Color) {
    val width = area / MarkWidthDivisor
    val top = center.y - area / 2
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - width / 2, top),
        size = Size(width, area * ErrorStrokeShare),
        cornerRadius = CornerRadius(width / 2),
    )
    drawCircle(color = color, radius = width / 2, center = Offset(center.x, top + area - width / 2))
}

/** A short resting line, for a muted or ended call. */
private fun DrawScope.drawRestLine(area: Float, color: Color) {
    val height = area / MarkWidthDivisor
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - area / 2, center.y - height / 2),
        size = Size(area, height),
        cornerRadius = CornerRadius(height / 2),
    )
}

private const val DotStepPerRadius = 3f
private const val MarkWidthDivisor = 6f
private const val ErrorStrokeShare = 0.65f
