package com.rookhq.monarch.components.intelligence.aiagentruntimeline

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import com.rookhq.monarch.theme.MonarchTheme

/** One cell of the table: its text, its width and what TalkBack reads for it, or `null` when unknown. */
private class Cell(val text: String, val width: Dp, val spoken: String?)

/** The events as a table that scrolls sideways; each row is read as one element with its known values. */
@Composable
internal fun MonarchAiAgentRunTimelineTable(
    events: List<MonarchAiRunEvent>,
    now: Long?,
    currency: String,
    texts: MonarchAiAgentRunTimelineTexts,
) {
    val tokens = MonarchAiAgentRunTimelineTokens
    val format = MonarchAiAgentRunTimelineFormat
    val scale = LocalDensity.current.fontScale
    val wide = tokens.NameColumnWidth * scale
    val narrow = tokens.ColumnWidth * scale
    val start = tokens.runStart(events)
    val end = tokens.runEnd(events, now)
    val showsCost = events.any { it.costMicros != null }
    val header = buildList {
        add(texts.eventColumn to wide)
        add(texts.kindColumn to narrow)
        add(texts.start to narrow)
        add(texts.duration to narrow)
        add(texts.inputTokens to narrow)
        add(texts.outputTokens to narrow)
        if (showsCost) add(texts.cost to narrow)
    }
    Column(
        modifier = Modifier
            .semantics { contentDescription = texts.events }
            .horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
    ) {
        Row(modifier = Modifier.clearAndSetSemantics { }) {
            header.forEach { (title, width) ->
                Text(
                    text = title,
                    modifier = Modifier.width(width),
                    style = MonarchTheme.typography.caption,
                    color = MonarchTheme.colors.textMuted,
                )
            }
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
        events.forEach { event ->
            fun known(name: String, value: String?, width: Dp) =
                Cell(value ?: texts.notAvailable, width, value?.let { texts.fact(name, it) })
            val duration = if (event.endedAt == null) {
                texts.running
            } else {
                texts.formatDuration(tokens.duration(event, end))
            }
            val cells = buildList {
                add(Cell(event.name, wide, event.name))
                add(Cell(texts.kind(event.kind), narrow, texts.kind(event.kind)))
                add(known(texts.start, texts.formatOffset(event.startedAt - start), narrow))
                add(known(texts.duration, duration, narrow))
                add(known(texts.inputTokens, event.inputTokens?.let(format::formatCount), narrow))
                add(known(texts.outputTokens, event.outputTokens?.let(format::formatCount), narrow))
                if (showsCost) add(known(texts.cost, event.costMicros?.let { format.formatCost(it, currency) }, narrow))
            }
            val spoken = cells.mapNotNull { it.spoken }.joinToString(", ")
            Row(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
                cells.forEach { cell ->
                    Text(
                        text = cell.text,
                        modifier = Modifier.width(cell.width),
                        style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.body),
                        color = MonarchTheme.colors.textDefault,
                    )
                }
            }
        }
    }
}

