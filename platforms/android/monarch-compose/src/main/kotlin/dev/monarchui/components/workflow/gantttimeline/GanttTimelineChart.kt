package dev.monarchui.components.workflow.gantttimeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme

private enum class GanttTimelinePart { Probe, Corner, Lane, Labels, Grid }

/** The deepest nesting that still indents the row label. */
private const val GanttTimelineMaxIndentDepth = 3

/**
 * The chart: the row labels at the start, measured first, and the calendar grid beside them, which scrolls
 * sideways. Rows are as tall as their label and at least 48dp.
 */
@Composable
internal fun GanttTimelineChart(model: GanttTimelineModel) {
    val tokens = MonarchGanttTimelineTokens
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.lg
    val line = tokens.lineColor(colors)
    val scroll = rememberScrollState()
    SubcomposeLayout(
        modifier = Modifier
            .fillMaxWidth()
            .border(tokens.LineWidth, line, shape)
            .clip(shape)
            .background(colors.bgSurface)
            .semantics { contentDescription = model.text.timeline }
            .drawBehind {
                val at = tokens.labelWidth(size.width.toDp()).toPx()
                val x = if (layoutDirection == LayoutDirection.Ltr) at else size.width - at
                drawLine(line, Offset(x, 0f), Offset(x, size.height), tokens.LineWidth.toPx())
            },
    ) { constraints ->
        val width = constraints.maxWidth
        val labelWidth = tokens.labelWidth(width.toDp()).roundToPx()
        val probe = subcompose(GanttTimelinePart.Probe) { GanttTimelineHeaderProbe() }.first().measure(Constraints())
        val headerMin = maxOf(tokens.HeaderMinHeight.roundToPx(), probe.height)
        val corner = subcompose(GanttTimelinePart.Corner) { GanttTimelineCorner(model.text.rowHeader) }
            .first()
            .measure(Constraints(minWidth = labelWidth, maxWidth = labelWidth, minHeight = headerMin))
        val cell = Constraints(
            minWidth = labelWidth,
            maxWidth = labelWidth,
            minHeight = tokens.RowMinHeight.roundToPx(),
        )
        val lane = if (model.laneMilestones.isNotEmpty()) {
            subcompose(GanttTimelinePart.Lane) { GanttTimelineLaneLabel(model.text.milestones) }.first().measure(cell)
        } else {
            null
        }
        val labels = subcompose(GanttTimelinePart.Labels) {
            model.lines.lines.forEach { row -> key(rowKey(row)) { GanttTimelineRowLabel(model, row) } }
        }.map { it.measure(cell) }
        val header = corner.height
        val laneHeight = lane?.height ?: 0
        val tops = IntArray(labels.size)
        val heights = IntArray(labels.size)
        var top = header + laneHeight
        labels.forEachIndexed { i, placeable ->
            tops[i] = top
            heights[i] = placeable.height
            top += placeable.height
        }
        val geometry = GanttTimelineGeometry(header, laneHeight, tops, heights, top)
        val gridWidth = (width - labelWidth).coerceAtLeast(0)
        val grid = subcompose(GanttTimelinePart.Grid) { GanttTimelineGrid(model, geometry, scroll) }
            .first()
            .measure(Constraints.fixed(gridWidth, top))
        layout(width, top) {
            corner.placeRelative(0, 0)
            lane?.placeRelative(0, header)
            labels.forEachIndexed { i, placeable -> placeable.placeRelative(0, tops[i]) }
            grid.placeRelative(labelWidth, 0)
        }
    }
}

private fun rowKey(row: GanttTimelineLine): String = when (row) {
    is GanttTimelineLine.Group -> "g:${row.group.id}"
    is GanttTimelineLine.Item -> "i:${row.item.id}"
}

/** Two caption lines: how tall the header must be for the month and day ticks at the current font size. */
@Composable
private fun GanttTimelineHeaderProbe() {
    Column(Modifier.clearAndSetSemantics {}.padding(vertical = MonarchGanttTimelineTokens.TickPadding)) {
        Text(GanttTimelineProbeText, style = MonarchTheme.typography.caption)
        Text(GanttTimelineProbeText, style = MonarchTheme.typography.caption)
    }
}

private const val GanttTimelineProbeText = "0"

/** A line along the bottom of a row. */
private fun Modifier.ganttTimelineRowLine(color: Color): Modifier = drawBehind {
    val y = size.height - MonarchGanttTimelineTokens.LineWidth.toPx() / 2
    drawLine(color, Offset(0f, y), Offset(size.width, y), MonarchGanttTimelineTokens.LineWidth.toPx())
}

@Composable
private fun GanttTimelineCorner(text: String) {
    val tokens = MonarchGanttTimelineTokens
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier
            .background(tokens.shadeColor(colors))
            .ganttTimelineRowLine(tokens.lineColor(colors))
            .padding(horizontal = tokens.LabelPadding, vertical = tokens.TickPadding),
        verticalArrangement = Arrangement.Bottom,
    ) {
        MonarchText(text = text, style = MonarchTextStyle.Caption, color = tokens.mutedColor(colors))
    }
}

@Composable
private fun GanttTimelineLaneLabel(text: String) {
    val tokens = MonarchGanttTimelineTokens
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier
            .ganttTimelineRowLine(tokens.lineColor(colors))
            .padding(horizontal = tokens.LabelPadding, vertical = tokens.LabelVerticalPadding),
        verticalArrangement = Arrangement.Center,
    ) {
        MonarchText(text = text, style = MonarchTextStyle.Caption, color = tokens.mutedColor(colors))
    }
}

/** The label of a row: a group heading, or an item, which toggles its children when it has some. */
@Composable
private fun GanttTimelineRowLabel(model: GanttTimelineModel, row: GanttTimelineLine) {
    val tokens = MonarchGanttTimelineTokens
    val colors = MonarchTheme.colors
    when (row) {
        is GanttTimelineLine.Group -> Column(
            modifier = Modifier
                .background(tokens.shadeColor(colors))
                .ganttTimelineRowLine(tokens.lineColor(colors))
                .semantics(mergeDescendants = true) { heading() }
                .padding(horizontal = tokens.LabelPadding, vertical = tokens.LabelVerticalPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            MonarchText(text = row.group.label, style = MonarchTextStyle.Label)
            GanttTimelineMetaLine(row.group.meta, model.groupMeta?.let { slot -> { slot(row.group) } })
        }
        is GanttTimelineLine.Item -> {
            val item = row.item
            val parent = row.children > 0
            val expanded = item.id !in model.state.collapsed
            val text = model.text
            val toggle = if (parent) {
                Modifier
                    .clickable(role = Role.Button, onClickLabel = text.children(row.children, expanded)) {
                        model.state.toggle(item.id)
                    }
                    .semantics { stateDescription = if (expanded) text.expanded else text.collapsed }
            } else {
                Modifier
            }
            // As on the web: a chevron before a parent's name instead of a caption under it.
            Row(
                modifier = Modifier
                    .ganttTimelineRowLine(tokens.lineColor(colors))
                    .then(toggle)
                    .padding(
                        start = tokens.LabelPadding +
                            tokens.DepthIndent * row.depth.coerceAtMost(GanttTimelineMaxIndentDepth),
                        end = tokens.LabelPadding,
                        top = tokens.LabelVerticalPadding,
                        bottom = tokens.LabelVerticalPadding,
                    ),
                horizontalArrangement = Arrangement.spacedBy(tokens.LabelVerticalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (parent) {
                    CompositionLocalProvider(LocalContentColor provides tokens.mutedColor(colors)) {
                        Box(
                            Modifier
                                .graphicsLayer { rotationZ = if (expanded) 0f else CollapsedChevronTurn }
                                .clearAndSetSemantics { },
                        ) { MonarchDefaultIcons.ChevronDown() }
                    }
                }
                Column(verticalArrangement = Arrangement.Center) {
                    MonarchText(text = item.label, style = MonarchTextStyle.Label)
                    GanttTimelineMetaLine(item.meta, model.itemMeta?.let { slot -> { slot(item) } })
                }
            }
        }
    }
}

/** The meta text of a row and the app's meta content (a status chip) on one line, wrapping when it is narrow. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GanttTimelineMetaLine(meta: String?, extra: (@Composable () -> Unit)?) {
    if (meta == null && extra == null) return
    val tokens = MonarchGanttTimelineTokens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.LabelVerticalPadding),
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        meta?.let {
            MonarchText(text = it, style = MonarchTextStyle.Caption, color = tokens.mutedColor(MonarchTheme.colors))
        }
        extra?.invoke()
    }
}

/** The turn of the chevron of a collapsed parent: it points to the end. */
private const val CollapsedChevronTurn = -90f
