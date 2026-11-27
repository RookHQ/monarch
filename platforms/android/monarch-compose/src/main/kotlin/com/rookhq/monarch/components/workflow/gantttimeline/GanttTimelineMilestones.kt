package com.rookhq.monarch.components.workflow.gantttimeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.components.overlays.tooltip.MonarchTooltip
import com.rookhq.monarch.theme.MonarchTheme
import kotlin.math.roundToInt

/** A milestone's diamond: a square turned a quarter, filled when done. */
@Composable
internal fun GanttTimelineDiamond(done: Boolean, modifier: Modifier = Modifier) {
    val tokens = MonarchGanttTimelineTokens
    val colors = tokens.milestoneColors(MonarchTheme.colors, done)
    Box(
        modifier = modifier
            .size(tokens.DiamondSize)
            .rotate(MonarchGanttTimelineTokens.DIAMOND_DEGREES)
            .background(colors.fill)
            .border(tokens.DiamondBorder, colors.border),
    )
}

/**
 * The milestones without an item, in the lane under the header: a diamond and the name, cut to a short width. The
 * full name and date are in a Material 3 plain tooltip (touch and hold) and read by TalkBack.
 */
@Composable
internal fun GanttTimelineLane(model: GanttTimelineModel, scale: GanttTimelineScale, geometry: GanttTimelineGeometry) {
    if (geometry.laneHeight == 0) return
    val tokens = MonarchGanttTimelineTokens
    val half = with(LocalDensity.current) { tokens.DiamondSize.toPx() / 2 }
    val center = geometry.header + geometry.laneHeight / 2
    model.laneMilestones.forEach { milestone ->
        val day = GanttTimelineDays.day(milestone.date)
        if (day < model.anchor || day > model.rangeEnd) return@forEach
        val date = model.date(day)
        val spoken = model.text.milestone(milestone.label, date, milestone.done)
        key(milestone.id) {
            // The tooltip emits more than one layout node, so the slot is on a box around it.
            Box(
                Modifier.layoutId(
                    GanttTimelineSlot((scale.x(day) + scale.dayWidth / 2 - half).roundToInt(), center, centerY = true),
                ),
            ) {
                GanttTimelineLaneMilestone(milestone.label, date, spoken, milestone.done)
            }
        }
    }
}

@Composable
private fun GanttTimelineLaneMilestone(label: String, date: String, spoken: String, done: Boolean) {
    val tokens = MonarchGanttTimelineTokens
    MonarchTooltip(text = "$label · $date") {
        Row(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Image
            },
            horizontalArrangement = Arrangement.spacedBy(tokens.LaneGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GanttTimelineDiamond(done)
            Text(
                text = label,
                modifier = Modifier.widthIn(max = tokens.LaneLabelMaxWidth),
                style = MonarchTheme.typography.caption,
                color = tokens.mutedColor(MonarchTheme.colors),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The milestones of an item, as diamonds at the bottom of its row, read by TalkBack with their name and date. */
@Composable
internal fun GanttTimelineRowMilestones(
    model: GanttTimelineModel,
    scale: GanttTimelineScale,
    geometry: GanttTimelineGeometry,
) {
    val tokens = MonarchGanttTimelineTokens
    val density = LocalDensity.current
    val half = with(density) { tokens.DiamondSize.toPx() / 2 }
    val inset = with(density) { tokens.TickPadding.roundToPx() }
    val rows = model.lines.lines.withIndex()
        .mapNotNull { (i, row) -> (row as? GanttTimelineLine.Item)?.let { it.item.id to i } }
        .toMap()
    model.milestones.forEach { milestone ->
        val index = milestone.itemId?.let(rows::get) ?: return@forEach
        val day = GanttTimelineDays.day(milestone.date)
        if (day < model.anchor || day > model.rangeEnd) return@forEach
        val spoken = model.text.milestone(milestone.label, model.date(day), milestone.done)
        key(milestone.id) {
            GanttTimelineDiamond(
                done = milestone.done,
                modifier = Modifier
                    .layoutId(
                        GanttTimelineSlot(
                            x = (scale.x(day) + scale.dayWidth / 2 - half).roundToInt(),
                            y = geometry.rowTops[index] + geometry.rowHeights[index] - inset,
                            bottom = true,
                        ),
                    )
                    .clearAndSetSemantics {
                        contentDescription = spoken
                        role = Role.Image
                    },
            )
        }
    }
}
