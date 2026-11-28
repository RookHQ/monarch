package com.rookhq.monarch.components.charts.uptimebar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The body of a period's details: its state with the marker and its uptime, each detail line and the note.
 *
 * @param status the name of the state.
 * @param colors the colours of the state, for the marker.
 * @param uptime the uptime line, such as "Uptime 99.42%", or `null` without one.
 * @param lines the detail lines, such as "Checks 1,440".
 * @param note the note, or `null`.
 */
@Composable
internal fun MonarchUptimeBarDetailsBody(
    status: String,
    colors: MonarchUptimeBarStatusColors,
    uptime: String?,
    lines: List<String>,
    note: String?,
) {
    val style = MonarchTheme.typography.caption
    Column(verticalArrangement = Arrangement.spacedBy(MonarchUptimeBarTokens.ThinSegment)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchUptimeBarTokens.LegendGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchUptimeBarSwatch(colors)
            Text(text = status, style = style)
        }
        if (uptime != null) Text(text = uptime, style = style)
        lines.forEach { Text(text = it, style = style) }
        if (note != null) Text(text = note, style = style)
    }
}

/** The marker of a state: a small rounded square in its colour, outlined for no data. Decoration. */
@Composable
internal fun MonarchUptimeBarSwatch(colors: MonarchUptimeBarStatusColors) {
    Canvas(Modifier.size(MonarchUptimeBarTokens.Swatch)) {
        val radius = CornerRadius(MonarchUptimeBarTokens.SegmentRadius.toPx())
        drawRoundRect(colors.fill, cornerRadius = radius)
        val outline = colors.outline ?: return@Canvas
        val width = MonarchUptimeBarTokens.Hairline.toPx()
        drawRoundRect(
            color = outline,
            topLeft = Offset(width / 2, width / 2),
            size = Size(size.width - width, size.height - width),
            cornerRadius = radius,
            style = Stroke(width),
        )
    }
}
