package com.rookhq.monarch.internal

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * The small marks drawn inside status markers (steps, timeline dots, progress
 * markers): a check, a cross, an exclamation mark, a dash and a dot. Monarch
 * bundles no icons, so these few are drawn, on the 16-unit grid of the web's
 * SVGs, with round caps.
 *
 * A glyph is decoration next to text or a state description; it never carries
 * the meaning alone.
 */
internal enum class MonarchStrokeGlyph {
    /** Done, approved, valid. */
    Check,

    /** Rejected, failed, removed. */
    Cross,

    /** Needs attention, failed with a message. */
    Exclamation,

    /** Skipped, not applicable, indeterminate. */
    Dash,

    /** Current, in progress. */
    Dot,
}

/** The geometry of the glyphs on the 16-unit grid. */
internal object MonarchStrokeGlyphGeometry {
    /** The size of the grid the points are given on. */
    const val GRID: Float = 16f

    /** The stroke width, in grid units. */
    const val STROKE: Float = 2f

    /** The radius of the dot glyph, in grid units. */
    const val DOT_RADIUS: Float = 3f

    val Check: List<Offset> = listOf(Offset(3.5f, 8.5f), Offset(6.5f, 11.5f), Offset(12.5f, 4.5f))
    val Cross: List<Pair<Offset, Offset>> = listOf(
        Offset(5f, 5f) to Offset(11f, 11f),
        Offset(11f, 5f) to Offset(5f, 11f),
    )
    val ExclamationBar: Pair<Offset, Offset> = Offset(8f, 4f) to Offset(8f, 9f)
    val ExclamationDot: Offset = Offset(8f, 11.75f)
    val Dash: Pair<Offset, Offset> = Offset(5f, 8f) to Offset(11f, 8f)
    val Centre: Offset = Offset(8f, 8f)
}

/**
 * Draws [glyph] in [color], scaled so the 16-unit grid fills the width of the
 * draw scope and centred vertically.
 *
 * @param strokeUnits the stroke width in grid units, 2 by default.
 */
internal fun DrawScope.drawMonarchStrokeGlyph(
    glyph: MonarchStrokeGlyph,
    color: Color,
    strokeUnits: Float = MonarchStrokeGlyphGeometry.STROKE,
) {
    val unit = size.width / MonarchStrokeGlyphGeometry.GRID
    val top = (size.height - size.width) / 2
    fun at(point: Offset) = Offset(point.x * unit, point.y * unit + top)
    val width = strokeUnits * unit
    fun line(from: Offset, to: Offset) = drawLine(color, at(from), at(to), width, StrokeCap.Round)
    when (glyph) {
        MonarchStrokeGlyph.Check ->
            MonarchStrokeGlyphGeometry.Check.zipWithNext().forEach { (a, b) -> line(a, b) }
        MonarchStrokeGlyph.Cross -> MonarchStrokeGlyphGeometry.Cross.forEach { (a, b) -> line(a, b) }
        MonarchStrokeGlyph.Exclamation -> {
            val (a, b) = MonarchStrokeGlyphGeometry.ExclamationBar
            line(a, b)
            drawCircle(color, width / 2, at(MonarchStrokeGlyphGeometry.ExclamationDot))
        }
        MonarchStrokeGlyph.Dash -> {
            val (a, b) = MonarchStrokeGlyphGeometry.Dash
            line(a, b)
        }
        MonarchStrokeGlyph.Dot ->
            drawCircle(color, MonarchStrokeGlyphGeometry.DOT_RADIUS * unit, at(MonarchStrokeGlyphGeometry.Centre))
    }
}

/**
 * [glyph] as a decorative element: give it a size with [modifier]. It is not
 * read by TalkBack; say the state in text or a state description.
 */
@Composable
internal fun MonarchStrokeGlyphMark(glyph: MonarchStrokeGlyph, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) { drawMonarchStrokeGlyph(glyph, color) }
}
