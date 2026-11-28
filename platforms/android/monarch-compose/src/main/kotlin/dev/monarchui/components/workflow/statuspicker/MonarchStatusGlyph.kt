package dev.monarchui.components.workflow.statuspicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.monarchui.theme.MonarchTheme

/**
 * The glyph of a status category, as `MonarchStatusPicker` draws it, for a
 * list row or a board card that shows a status without changing it: a dashed
 * ring for backlog, a ring for not started, a half-filled ring for in
 * progress, a disc with a check mark for completed and a disc with a cross
 * for cancelled.
 *
 * Decorative: TalkBack does not read it, so put the status's name in the
 * row's text or content description. Each category has its own shape as well
 * as its own colour, so the shape alone tells the categories apart.
 *
 * @param category the category to draw.
 * @param modifier the modifier of the glyph. Its default size is 14dp.
 */
@Composable
public fun MonarchStatusGlyph(category: MonarchStatusCategory, modifier: Modifier = Modifier) {
    val colors = MonarchStatusPickerTokens.glyphColors(MonarchTheme.colors, category)
    Canvas(modifier = modifier.size(MonarchStatusPickerTokens.GlyphSize).clearAndSetSemantics {}) {
        val unit = size.minDimension / MonarchStatusPickerTokens.GRID
        when (category) {
            MonarchStatusCategory.Backlog -> drawRing(unit, colors, dashed = true)
            MonarchStatusCategory.Unstarted -> drawRing(unit, colors, dashed = false)
            MonarchStatusCategory.Started -> {
                drawRing(unit, colors, dashed = false)
                drawHalf(unit, colors)
            }
            MonarchStatusCategory.Completed -> {
                drawDisc(unit, colors)
                drawCheck(unit, colors)
            }
            MonarchStatusCategory.Cancelled -> {
                drawDisc(unit, colors)
                drawCross(unit, colors)
            }
        }
    }
}

private fun DrawScope.drawRing(unit: Float, colors: StatusGlyphColors, dashed: Boolean) {
    val tokens = MonarchStatusPickerTokens
    drawCircle(
        color = colors.mark,
        radius = tokens.RING_RADIUS * unit,
        center = Offset(tokens.CENTRE * unit, tokens.CENTRE * unit),
        style = Stroke(
            width = tokens.STROKE * unit,
            pathEffect = if (dashed) {
                PathEffect.dashPathEffect(floatArrayOf(tokens.DASH * unit, tokens.DASH_GAP * unit))
            } else {
                null
            },
        ),
    )
}

/** The end half of a smaller disc, `M7 3.5a3.5 3.5 0 0 1 0 7z`. */
private fun DrawScope.drawHalf(unit: Float, colors: StatusGlyphColors) {
    val tokens = MonarchStatusPickerTokens
    val radius = tokens.HALF_RADIUS * unit
    val centre = tokens.CENTRE * unit
    drawArc(
        color = colors.mark,
        startAngle = -QuarterTurn,
        sweepAngle = HalfTurn,
        useCenter = true,
        topLeft = Offset(centre - radius, centre - radius),
        size = Size(radius * 2, radius * 2),
    )
}

private fun DrawScope.drawDisc(unit: Float, colors: StatusGlyphColors) {
    val tokens = MonarchStatusPickerTokens
    drawCircle(colors.mark, tokens.DISC_RADIUS * unit, Offset(tokens.CENTRE * unit, tokens.CENTRE * unit))
}

private fun DrawScope.drawCheck(unit: Float, colors: StatusGlyphColors) {
    val points = MonarchStatusPickerTokens.Check.map { it * unit }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        color = colors.cutout,
        style = Stroke(MonarchStatusPickerTokens.STROKE * unit, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawCross(unit: Float, colors: StatusGlyphColors) {
    MonarchStatusPickerTokens.Cross.forEach { (from, to) ->
        drawLine(colors.cutout, from * unit, to * unit, MonarchStatusPickerTokens.STROKE * unit, StrokeCap.Round)
    }
}

private const val QuarterTurn = 90f
private const val HalfTurn = 180f
