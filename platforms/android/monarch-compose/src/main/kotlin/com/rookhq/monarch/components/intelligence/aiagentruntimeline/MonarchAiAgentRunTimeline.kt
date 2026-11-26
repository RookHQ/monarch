package com.rookhq.monarch.components.intelligence.aiagentruntimeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.typography.tabularnumberstyle.MonarchTabularNumber
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The trace of one agent run: every model call, tool call and hand-off as a
 * bar on a shared time axis, totals for time, tokens and cost, and details
 * that open per event, with a table view of the same data.
 *
 * Built by Monarch: rows with bars drawn on a Compose `Canvas`, a
 * `MonarchSegmentedControl` for the view, and a table that scrolls
 * sideways. Each event is one button that opens its details, read as its
 * kind, name and duration ("Tool, search_invoices, 1.6 s"); the bars and the
 * axis are hidden from TalkBack, so nothing depends on their colour or length.
 * The trace never reads the clock: running events end at [now], else at the
 * latest time any event knows.
 *
 * @param events the run's events in any order.
 * @param modifier the modifier of the trace.
 * @param currency the ISO 4217 currency of the costs.
 * @param now the end of events that are still running, in epoch milliseconds.
 * @param mode the view. `null` lets the component keep it, starting from [initialMode].
 * @param onModeChange called when the person switches the view.
 * @param initialMode the first view when the component keeps it.
 * @param onOpenPart adds "Show in conversation" to an open event that has a part or a message id.
 * @param labels replacements for the built-in strings and the duration formats.
 */
@Composable
public fun MonarchAiAgentRunTimeline(
    events: List<MonarchAiRunEvent>,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    now: Long? = null,
    mode: MonarchAiAgentRunTimelineMode? = null,
    onModeChange: ((MonarchAiAgentRunTimelineMode) -> Unit)? = null,
    initialMode: MonarchAiAgentRunTimelineMode = MonarchAiAgentRunTimelineMode.Timeline,
    onOpenPart: ((MonarchAiRunEvent) -> Unit)? = null,
    labels: MonarchAiAgentRunTimelineLabels = MonarchAiAgentRunTimelineLabels(),
) {
    val tokens = MonarchAiAgentRunTimelineTokens
    val format = MonarchAiAgentRunTimelineFormat
    val texts = rememberMonarchAiAgentRunTimelineTexts(labels)
    var keptMode by rememberSaveable { mutableStateOf(initialMode) }
    val shownMode = mode ?: keptMode
    val sorted = tokens.sorted(events)
    val totals = tokens.totals(sorted, now)
    Column(
        modifier = modifier.semantics {
            contentDescription = texts.label
            isTraversalGroup = true
        },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        Text(
            text = texts.label,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.subheading,
            color = MonarchTheme.colors.textDefault,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        ) {
            Total(texts.totalDuration, texts.formatDuration(totals.durationMillis), texts)
            totals.inputTokens?.let { Total(texts.inputTokens, format.formatCount(it), texts) }
            totals.outputTokens?.let { Total(texts.outputTokens, format.formatCount(it), texts) }
            totals.costMicros?.let { Total(texts.cost, format.formatCost(it, currency), texts) }
        }
        MonarchSegmentedControl(modifier = Modifier.semantics { contentDescription = texts.modeLabel }) {
            MonarchAiAgentRunTimelineMode.entries.forEach { option ->
                Segment(
                    selected = shownMode == option,
                    onClick = {
                        keptMode = option
                        onModeChange?.invoke(option)
                    },
                    label = texts.mode(option),
                )
            }
        }
        when {
            sorted.isEmpty() -> Text(
                text = texts.empty,
                style = MonarchTheme.typography.body,
                color = MonarchTheme.colors.textMuted,
            )
            shownMode == MonarchAiAgentRunTimelineMode.Timeline ->
                MonarchAiAgentRunTimelineTrace(sorted, now, currency, onOpenPart, texts)
            else -> MonarchAiAgentRunTimelineTable(sorted, now, currency, texts)
        }
    }
}

/** One total: the value in the strong body style over its name, read as one element. */
@Composable
private fun Total(name: String, value: String, texts: MonarchAiAgentRunTimelineTexts) {
    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = texts.fact(name, value) }) {
        MonarchTabularNumber(text = value, style = MonarchTextStyle.BodyStrong)
        Text(text = name, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
    }
}
