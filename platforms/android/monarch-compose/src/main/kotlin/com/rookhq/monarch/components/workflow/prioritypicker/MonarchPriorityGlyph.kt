package com.rookhq.monarch.components.workflow.prioritypicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The shape of a priority level, as `MonarchPriorityPicker` draws it, for a
 * list row or a board card that shows the priority without changing it: a
 * tile with an exclamation mark for urgent, three, two or one filled bars
 * for high, medium and low, and three dashes for no priority.
 *
 * Decorative: TalkBack does not read it, so put the level's name in the
 * row's text or content description. Each level has its own shape as well as
 * its own colour, so the shape alone tells the levels apart.
 *
 * @param level the level to draw.
 * @param modifier the modifier of the glyph. Its default size is 14dp.
 */
@Composable
public fun MonarchPriorityGlyph(level: MonarchPriorityLevel, modifier: Modifier = Modifier) {
    val colors = MonarchPriorityPickerTokens.glyphColors(MonarchTheme.colors, level)
    Canvas(modifier = modifier.size(MonarchPriorityPickerTokens.GlyphSize).clearAndSetSemantics {}) {
        val unit = size.minDimension / MonarchPriorityPickerTokens.GRID
        when (level) {
            MonarchPriorityLevel.Urgent -> drawUrgent(unit, colors)
            MonarchPriorityLevel.None -> drawDashes(unit, colors)
            else -> drawBars(unit, MonarchPriorityPickerTokens.filledBars(level), colors)
        }
    }
}

private fun DrawScope.drawUrgent(unit: Float, colors: PriorityGlyphColors) {
    val tokens = MonarchPriorityPickerTokens
    val inset = tokens.TILE_INSET * unit
    drawRoundRect(
        color = colors.mark,
        topLeft = Offset(inset, inset),
        size = Size(tokens.GRID * unit - 2 * inset, tokens.GRID * unit - 2 * inset),
        cornerRadius = CornerRadius(tokens.TILE_RADIUS * unit),
    )
    listOf(tokens.MarkLine, tokens.MarkDot).forEach { (from, to) ->
        drawLine(colors.cutout, from * unit, to * unit, tokens.MARK_STROKE * unit, StrokeCap.Round)
    }
}

private fun DrawScope.drawDashes(unit: Float, colors: PriorityGlyphColors) {
    MonarchPriorityPickerTokens.Dashes.forEach { (from, to) ->
        drawLine(colors.mark, from * unit, to * unit, MonarchPriorityPickerTokens.DASH_STROKE * unit, StrokeCap.Round)
    }
}

private fun DrawScope.drawBars(unit: Float, filled: Int, colors: PriorityGlyphColors) {
    val tokens = MonarchPriorityPickerTokens
    repeat(tokens.BAR_COUNT) { index ->
        val (corner, height) = tokens.bar(index)
        val radius = CornerRadius(tokens.BAR_RADIUS * unit)
        val barSize = Size(tokens.BAR_WIDTH * unit, height * unit)
        if (index < filled) {
            drawRoundRect(colors.mark, corner * unit, barSize, radius)
        } else {
            val stroke = tokens.EMPTY_STROKE * unit
            drawRoundRect(
                color = colors.empty,
                topLeft = corner * unit + Offset(stroke / 2, stroke / 2),
                size = Size(barSize.width - stroke, barSize.height - stroke),
                cornerRadius = radius,
                style = Stroke(stroke),
            )
        }
    }
}
