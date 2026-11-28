package dev.monarchui.components.technical.domainprogress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import dev.monarchui.theme.MonarchTheme

/** The check mark on a 16-unit grid, as the web's `M3.5 8.5l3 3 6-7`. */
private val CheckPoints = listOf(Offset(3.5f, 8.5f), Offset(6.5f, 11.5f), Offset(12.5f, 4.5f))

/** The bar and the dot of the exclamation mark on a 16-unit grid. */
private val ExclamationBar = Offset(8f, 3.5f) to Offset(8f, 9f)
private val ExclamationDot = Offset(8f, 12.25f)
private const val Grid = 16f
private const val GlyphStroke = 2f

/**
 * The marker of a step: a check when done, a spinner in progress (a still open ring when animations are off), an
 * exclamation mark when it needs attention, an empty circle before it starts. Decoration: the state is spoken.
 */
@Composable
internal fun MonarchDomainProgressMarker(state: MonarchDomainStepState, side: Dp) {
    val colors = MonarchDomainProgressTokens.marker(state, MonarchTheme.colors)
    Box(
        modifier = Modifier
            .clearAndSetSemantics { }
            .size(side)
            .background(colors.fill, CircleShape)
            .border(MonarchDomainProgressTokens.Line, colors.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            MonarchDomainStepState.Done, MonarchDomainStepState.Error -> Glyph(state, colors.content, side)
            MonarchDomainStepState.Current -> if (MonarchTheme.motion.reducedMotion) {
                CircularProgressIndicator(
                    progress = { MonarchDomainProgressTokens.StillArc },
                    modifier = Modifier.size(side * MonarchDomainProgressTokens.SpinnerRatio),
                    color = colors.content,
                    strokeWidth = MonarchDomainProgressTokens.Line,
                    trackColor = Color.Transparent,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(side * MonarchDomainProgressTokens.SpinnerRatio),
                    color = colors.content,
                    strokeWidth = MonarchDomainProgressTokens.Line,
                    trackColor = Color.Transparent,
                )
            }
            MonarchDomainStepState.Todo -> Unit
        }
    }
}

@Composable
private fun Glyph(state: MonarchDomainStepState, color: Color, side: Dp) {
    Canvas(modifier = Modifier.size(side * MonarchDomainProgressTokens.GlyphRatio)) {
        val unit = size.width / Grid
        val stroke = Stroke(width = GlyphStroke * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        if (state == MonarchDomainStepState.Done) {
            val path = Path().apply {
                moveTo(CheckPoints[0].x * unit, CheckPoints[0].y * unit)
                CheckPoints.drop(1).forEach { lineTo(it.x * unit, it.y * unit) }
            }
            drawPath(path, color, style = stroke)
        } else {
            drawLine(color, ExclamationBar.first * unit, ExclamationBar.second * unit, stroke.width, StrokeCap.Round)
            drawCircle(color, radius = stroke.width / 2, center = ExclamationDot * unit)
        }
    }
}
