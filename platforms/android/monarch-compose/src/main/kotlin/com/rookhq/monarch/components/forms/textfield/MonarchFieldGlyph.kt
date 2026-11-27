@file:Suppress("MagicNumber") // The 16-unit grid geometry of the web's glyphs, copied as it is.

package com.rookhq.monarch.components.forms.textfield

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.foundation.MonarchControlMetrics
import com.rookhq.monarch.internal.field.LocalMonarchFieldGlyphSize

/**
 * The few icons a field draws itself: the chevron of a select, the eye of a password, the error mark, copy,
 * clear, search, the calendar of a date field and the stepper signs. Monarch bundles no icon set for apps;
 * these are drawn on the 16-unit grid of the web's glyphs with round caps, so a field looks the same without
 * an icon dependency.
 */
internal enum class MonarchFieldGlyph {
    ChevronDown, Error, Eye, EyeOff, Copy, Check, Close, Search, Plus, Minus, Calendar,
}

/**
 * [glyph] in the surrounding content colour, at [size]: 20dp inside a field's box, 16dp elsewhere (see
 * [LocalMonarchFieldGlyphSize]). Decoration: it is hidden from TalkBack.
 */
@Composable
internal fun MonarchFieldGlyphIcon(
    glyph: MonarchFieldGlyph,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = LocalMonarchFieldGlyphSize.current,
) {
    Canvas(modifier.size(size)) { drawFieldGlyph(glyph, tint) }
}

private const val Grid = 16f
private const val StrokeUnits = 1.5f
/** The heaviest stroke, in units of the 16dp glyph's grid: 1.75dp, however large the glyph is drawn. */
private const val MaxStrokeUnits = 1.75f

/** A drawing surface on the 16-unit grid: [unit] pixels per grid unit and the round-capped [stroke]. */
private class GlyphPen(val scope: DrawScope, val color: Color) {
    val unit = scope.size.width / Grid
    private val gridDp = with(scope) { MonarchControlMetrics.IconSize.toPx() } / Grid

    // 1.5 grid units, at most 1.75dp, so a 20dp glyph in a field is no heavier than the outlined icons beside it.
    val stroke = Stroke(
        width = minOf(StrokeUnits * unit, MaxStrokeUnits * gridDp),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )

    fun at(x: Float, y: Float) = Offset(x * unit, y * unit)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        scope.drawLine(color, at(x1, y1), at(x2, y2), stroke.width, StrokeCap.Round)

    fun ring(x: Float, y: Float, radius: Float) = scope.drawCircle(color, radius * unit, at(x, y), style = stroke)

    fun dot(x: Float, y: Float, radius: Float) = scope.drawCircle(color, radius * unit, at(x, y))

    fun outline(path: Path) = scope.drawPath(path, color, style = stroke)
}

private fun DrawScope.drawFieldGlyph(glyph: MonarchFieldGlyph, color: Color) {
    val pen = GlyphPen(this, color)
    when (glyph) {
        MonarchFieldGlyph.ChevronDown -> pen.chevron()
        MonarchFieldGlyph.Error -> pen.error()
        MonarchFieldGlyph.Eye -> pen.eye()
        MonarchFieldGlyph.EyeOff -> {
            pen.eye()
            pen.line(2.75f, 2.75f, 13.25f, 13.25f)
        }
        MonarchFieldGlyph.Copy -> pen.copy()
        MonarchFieldGlyph.Check -> {
            pen.line(3.5f, 8.5f, 6.5f, 11.5f)
            pen.line(6.5f, 11.5f, 12.5f, 4.5f)
        }
        MonarchFieldGlyph.Close -> {
            pen.line(4f, 4f, 12f, 12f)
            pen.line(12f, 4f, 4f, 12f)
        }
        MonarchFieldGlyph.Search -> {
            pen.ring(7f, 7f, 4.75f)
            pen.line(10.5f, 10.5f, 13.75f, 13.75f)
        }
        MonarchFieldGlyph.Plus -> {
            pen.line(3.5f, 8f, 12.5f, 8f)
            pen.line(8f, 3.5f, 8f, 12.5f)
        }
        MonarchFieldGlyph.Minus -> pen.line(3.5f, 8f, 12.5f, 8f)
        MonarchFieldGlyph.Calendar -> pen.calendar()
    }
}

private fun GlyphPen.chevron() {
    line(4f, 6.25f, 8f, 10.25f)
    line(8f, 10.25f, 12f, 6.25f)
}

private fun GlyphPen.error() {
    ring(8f, 8f, 6.5f)
    line(8f, 4.9f, 8f, 8.4f)
    dot(8f, 10.9f, 0.85f)
}

private fun GlyphPen.eye() {
    val almond = Path().apply {
        moveTo(1.5f * unit, 8f * unit)
        cubicTo(3.2f * unit, 4.8f * unit, 5.4f * unit, 3.6f * unit, 8f * unit, 3.6f * unit)
        cubicTo(10.6f * unit, 3.6f * unit, 12.8f * unit, 4.8f * unit, 14.5f * unit, 8f * unit)
        cubicTo(12.8f * unit, 11.2f * unit, 10.6f * unit, 12.4f * unit, 8f * unit, 12.4f * unit)
        cubicTo(5.4f * unit, 12.4f * unit, 3.2f * unit, 11.2f * unit, 1.5f * unit, 8f * unit)
        close()
    }
    outline(almond)
    ring(8f, 8f, 2f)
}

private fun GlyphPen.copy() {
    scope.drawRoundRect(color, at(5.5f, 5.5f), Size(8f * unit, 8f * unit), CornerRadius(1.75f * unit), stroke)
    val back = Path().apply {
        moveTo(10.5f * unit, 4.25f * unit)
        lineTo(10.5f * unit, 4f * unit)
        quadraticTo(10.5f * unit, 2.5f * unit, 9f * unit, 2.5f * unit)
        lineTo(4f * unit, 2.5f * unit)
        quadraticTo(2.25f * unit, 2.5f * unit, 2.25f * unit, 4.25f * unit)
        lineTo(2.25f * unit, 9.25f * unit)
        quadraticTo(2.25f * unit, 10.5f * unit, 3.5f * unit, 10.5f * unit)
        lineTo(4.25f * unit, 10.5f * unit)
    }
    outline(back)
}

private fun GlyphPen.calendar() {
    scope.drawRoundRect(color, at(2.25f, 3.25f), Size(11.5f * unit, 10.5f * unit), CornerRadius(1.75f * unit), stroke)
    line(2.25f, 6.75f, 13.75f, 6.75f)
    line(5.25f, 1.75f, 5.25f, 4.25f)
    line(10.75f, 1.75f, 10.75f, 4.25f)
}
