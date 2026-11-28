package dev.monarchui.components.workflow.gantttimeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.collapsible.MonarchCollapsible
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme

/**
 * The list view: a row per group and item in chart order, then the unscheduled items and the milestones. Every
 * date, length, progress, dependency and conflict is written; editable items have a Change dates button.
 */
@Composable
internal fun GanttTimelineList(model: GanttTimelineModel) {
    Column(modifier = Modifier.fillMaxWidth().semantics { contentDescription = model.text.timeline }) {
        model.lines.lines.forEachIndexed { i, row ->
            if (i > 0) MonarchDivider()
            when (row) {
                is GanttTimelineLine.Group -> key("g:${row.group.id}") { GanttTimelineGroupRow(model, row.group) }
                is GanttTimelineLine.Item -> key("i:${row.item.id}") {
                    GanttTimelineItemRow(model, row.item, row.depth, row.children)
                }
            }
        }
        val unscheduled = model.lines.unscheduled
        if (unscheduled.isNotEmpty()) {
            GanttTimelineListHeading(model.text.unscheduled(unscheduled.size))
            unscheduled.forEachIndexed { i, item ->
                if (i > 0) MonarchDivider()
                key("u:${item.id}") { GanttTimelineItemRow(model, item, depth = 0, children = 0) }
            }
        }
        if (model.milestones.isNotEmpty()) {
            GanttTimelineListHeading(model.text.milestones)
            model.milestones.forEachIndexed { i, milestone ->
                if (i > 0) MonarchDivider()
                key("m:${milestone.id}") { GanttTimelineMilestoneRow(model, milestone) }
            }
        }
    }
}

/** The items without dates under the chart, in a `MonarchCollapsible` that starts open, as on the web. */
@Composable
internal fun GanttTimelineUnscheduled(model: GanttTimelineModel) {
    // Collapsed at first, as the web's Unscheduled list is.
    var expanded by rememberSaveable { mutableStateOf(false) }
    val items = model.lines.unscheduled
    val shape = MonarchTheme.shapes.lg
    val line = MonarchGanttTimelineTokens.lineColor(MonarchTheme.colors)
    MonarchCollapsible(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        label = model.text.unscheduled(items.size),
        modifier = Modifier
            .fillMaxWidth()
            .border(MonarchGanttTimelineTokens.LineWidth, line, shape)
            .clip(shape)
            .padding(horizontal = MonarchGanttTimelineTokens.LabelPadding),
        trailingIcon = MonarchDefaultIcons.ChevronDown,
    ) {
        items.forEachIndexed { i, item ->
            if (i > 0) MonarchDivider()
            key(item.id) { GanttTimelineItemRow(model, item, depth = 0, children = 0) }
        }
    }
}

@Composable
private fun GanttTimelineListHeading(text: String) {
    MonarchText(
        text = text,
        modifier = Modifier.padding(top = MonarchGanttTimelineTokens.Gap, bottom = MonarchGanttTimelineTokens.ListGap),
        style = MonarchTextStyle.Subheading,
        heading = true,
    )
}

@Composable
private fun GanttTimelineGroupRow(model: GanttTimelineModel, group: MonarchGanttTimelineGroup) {
    val tokens = MonarchGanttTimelineTokens
    val colors = MonarchTheme.colors
    val span = GanttTimelineRules.span(group.start, group.end)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.shadeColor(colors))
            .semantics(mergeDescendants = true) { heading() }
            .padding(horizontal = tokens.LabelPadding, vertical = tokens.LabelPadding),
    ) {
        MonarchText(text = group.label, style = MonarchTextStyle.BodyStrong)
        val range = span?.let { model.text.range(model.date(it.start), model.date(it.end)) }
        val details = listOfNotNull(group.meta, range)
        if (details.isNotEmpty()) {
            MonarchText(
                text = details.joinToString(" · "),
                style = MonarchTextStyle.Caption,
                color = tokens.mutedColor(colors),
            )
        }
        model.groupMeta?.invoke(group)
    }
}

/** An item: its name, dates, length, progress, dependencies and conflict, with Change dates when editable. */
@Composable
private fun GanttTimelineItemRow(model: GanttTimelineModel, item: MonarchGanttTimelineItem, depth: Int, children: Int) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val open = model.onOpen
    MonarchListRow(
        headlineContent = { MonarchText(text = item.label, style = MonarchTextStyle.BodyStrong) },
        modifier = Modifier.padding(start = MonarchGanttTimelineTokens.ListIndent * depth),
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(MonarchGanttTimelineTokens.ListGap)) {
                GanttTimelineItemFacts(model, item)
                GanttTimelineItemButtons(model, item, children) { picking = true }
            }
        },
        onClick = open?.let { { it(item) } },
    )
    if (picking) GanttTimelineDatesDialog(model, item) { picking = false }
}

/** The written facts of an item: dates, then length, progress and meta, then dependencies, then the conflict. */
@Composable
private fun GanttTimelineItemFacts(model: GanttTimelineModel, item: MonarchGanttTimelineItem) {
    val tokens = MonarchGanttTimelineTokens
    val colors = MonarchTheme.colors
    val text = model.text
    val span = GanttTimelineRules.span(item.start, item.end)
    val dates = span?.let {
        if (it.start == it.end) model.date(it.start) else text.range(model.date(it.start), model.date(it.end))
    } ?: text.noDates
    val facts = listOfNotNull(
        span?.let { text.duration(it.days) },
        model.percent(item)?.let(text.progress),
        item.meta,
    )
    val dependsOn = model.dependsOn(item)
    val lines = listOfNotNull(
        dates,
        facts.takeIf { it.isNotEmpty() }?.joinToString(" · "),
        dependsOn.takeIf { it.isNotEmpty() }?.let { text.dependsOn(it.joinToString(", ")) },
    )
    lines.forEach { MonarchText(text = it, style = MonarchTextStyle.Caption, color = tokens.mutedColor(colors)) }
    item.conflict?.let {
        MonarchText(text = text.conflict(it), style = MonarchTextStyle.Caption, color = tokens.conflictColor(colors))
    }
    model.itemMeta?.invoke(item)
}

/** Change dates when the item is editable, and Hide or Show for a parent. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GanttTimelineItemButtons(
    model: GanttTimelineModel,
    item: MonarchGanttTimelineItem,
    children: Int,
    onChangeDates: () -> Unit,
) {
    val editable = model.editable(item)
    if (!editable && children == 0) return
    val text = model.text
    FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchGanttTimelineTokens.ListGap)) {
        if (editable) MonarchButton(text = text.changeDates, onClick = onChangeDates, size = MonarchButtonSize.Sm)
        if (children > 0) {
            val expanded = item.id !in model.state.collapsed
            MonarchButton(
                text = text.children(children, expanded),
                onClick = { model.state.toggle(item.id) },
                modifier = Modifier.semantics { stateDescription = if (expanded) text.expanded else text.collapsed },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

@Composable
private fun GanttTimelineMilestoneRow(model: GanttTimelineModel, milestone: MonarchGanttTimelineMilestone) {
    val day = GanttTimelineDays.day(milestone.date)
    val on = milestone.itemId?.let { model.itemsById[it]?.label }
    MonarchListRow(
        headlineContent = {
            MonarchText(text = model.text.milestone(milestone.label, model.date(day), milestone.done))
        },
        supportingContent = on?.let {
            {
                MonarchText(
                    text = it,
                    style = MonarchTextStyle.Caption,
                    color = MonarchGanttTimelineTokens.mutedColor(MonarchTheme.colors),
                )
            }
        },
        leadingContent = { GanttTimelineDiamond(milestone.done) },
    )
}
