package com.rookhq.monarch.components.intelligence.aitraceview

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.typography.tabularnumberstyle.MonarchTabularNumber
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * The technical trace of one agent run: every model call, tool call,
 * retrieval, sub-agent and guardrail as a row in a tree, with its duration,
 * tokens, cost and a bar on a shared time axis, the totals of the run, and
 * the details of a span in a sheet.
 *
 * A Monarch pattern drawn as rows in a `Column` (put it in a scrolling
 * parent), with a `MonarchSegmentedControl` for the filter
 * (All, Errors only, Slowest first) and a `MonarchBottomSheet` for
 * the details: status, kind, timings, tokens, cost, model, error, attributes
 * and the plain-text input and output with copy buttons. The web's tree grid
 * does not fit a phone, so each row shows the name with a facts line and the
 * bar under it. Each span is one TalkBack button read as its name, status,
 * kind, tokens, cost and timing; its open and close button is a separate
 * element. The bars and the axis are hidden from TalkBack, so nothing
 * depends on their colour or length. The view never reads the clock:
 * running spans end at [now], else at the latest time any span knows.
 * Top-level spans, failed spans and their ancestors start open.
 *
 * @param spans the spans of one run, in any order. Children are found through `parentId` and shown in start order.
 * @param expandIcon a chevron that points to the end, such as a `MonarchIcon` of size `Sm` that mirrors in right
 * to left. It turns to point down when the span is open. Monarch bundles no icons.
 * @param modifier the modifier of the trace.
 * @param currency the ISO 4217 currency of spans that name none.
 * @param now the end of running spans, in milliseconds from the start of the run.
 * @param selection the id of the span whose details are open. The view keeps the person's choice and starts again
 * from this value when it changes.
 * @param onSelectionChange called with the span the person opens, and with `null` when the details close.
 * @param filter the filter. `null` lets the view keep it, starting from All.
 * @param onFilterChange called when the person changes the filter.
 * @param expanded the ids of the open spans. `null` keeps them inside the view.
 * @param onExpandedChange called with the new set of open ids.
 * @param locale the locale of numbers, durations and money. `null` uses the configuration's locale.
 * @param labels replacements for the built-in strings and the duration format.
 * @param onOpenPart adds Show in conversation to the details of a span with a part or message id.
 */
@Suppress("LongParameterList", "LongMethod")
@ExperimentalMonarchApi
@Composable
public fun MonarchAiTraceView(
    spans: List<MonarchAiTraceSpan>,
    expandIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    currency: String = "USD",
    now: Long? = null,
    selection: String? = null,
    onSelectionChange: ((String?) -> Unit)? = null,
    filter: MonarchAiTraceFilter? = null,
    onFilterChange: ((MonarchAiTraceFilter) -> Unit)? = null,
    expanded: Set<String>? = null,
    onExpandedChange: ((Set<String>) -> Unit)? = null,
    locale: Locale? = null,
    labels: MonarchAiTraceViewLabels = MonarchAiTraceViewLabels(),
    onOpenPart: ((MonarchAiTraceSpan) -> Unit)? = null,
) {
    val tokens = MonarchAiTraceViewTokens
    val shownLocale = locale ?: LocalConfiguration.current.locales[0]
    val texts = rememberMonarchAiTraceViewTexts(labels, shownLocale, currency)
    var selectedId by rememberSaveable(selection) { mutableStateOf(selection) }
    var keptFilter by rememberSaveable { mutableStateOf(MonarchAiTraceFilter.All) }
    var inside by rememberSaveable(saver = traceIdSetSaver) { mutableStateOf(tokens.defaultExpanded(spans)) }
    val shownFilter = filter ?: keptFilter
    val open = expanded ?: inside
    val totals = remember(spans, now, currency) { tokens.totals(spans, now, currency) }
    val lines = remember(spans, shownFilter, open, totals) { tokens.lines(spans, shownFilter, open, totals.end) }
    val select: (String?) -> Unit = { id ->
        selectedId = id
        onSelectionChange?.invoke(id)
    }
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = texts.label,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.subheading,
                color = MonarchTheme.colors.textDefault,
            )
            if (spans.isNotEmpty()) {
                MonarchSegmentedControl(modifier = Modifier.semantics { contentDescription = texts.filterLabel }) {
                    MonarchAiTraceFilter.entries.forEach { option ->
                        Segment(
                            selected = shownFilter == option,
                            onClick = {
                                keptFilter = option
                                onFilterChange?.invoke(option)
                            },
                            label = texts.filter(option),
                        )
                    }
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
        ) {
            Total(texts.totalDuration, texts.duration(totals.duration), texts)
            val tokenText = if (totals.tokensKnown) texts.count(totals.totalTokens) else texts.notAvailable
            Total(texts.totalTokens, tokenText, texts)
            Total(texts.totalCost, texts.costs(totals.costs), texts)
            Total(texts.errors, texts.count(totals.errorCount.toLong()), texts)
        }
        when {
            spans.isEmpty() -> Muted(texts.empty)
            lines.isEmpty() -> Muted(texts.noErrors)
            else -> BoxWithConstraints {
                val compact = maxWidth < MonarchAiTraceViewTokens.TableWidth
                Column(
                    modifier = (
                        if (compact) Modifier.fillMaxWidth() else Modifier.horizontalScroll(rememberScrollState())
                            .width(MonarchAiTraceViewTokens.TableWidth)
                        )
                        .semantics {
                            contentDescription = texts.label
                            isTraversalGroup = true
                        },
                ) {
                    MonarchAiTraceViewHeader(texts, compact)
                    lines.forEach { line ->
                        MonarchAiTraceViewRow(
                            line = line,
                            totals = totals,
                            selected = line.span.id == selectedId,
                            expandIcon = expandIcon,
                            texts = texts,
                            onOpen = { select(line.span.id) },
                            onToggle = {
                                val next = tokens.toggled(open, line.span.id)
                                if (expanded == null) inside = next
                                onExpandedChange?.invoke(next)
                            },
                            compact = compact,
                        )
                    }
                }
            }
        }
    }
    val selected = spans.firstOrNull { it.id == selectedId }
    if (selected != null) {
        MonarchAiTraceViewDetails(
            span = selected,
            totals = totals,
            texts = texts,
            onOpenPart = onOpenPart?.takeIf { tokens.opensPart(selected) },
            onDismiss = { select(null) },
        )
    }
}

private val traceIdSetSaver = listSaver<MutableState<Set<String>>, String>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)

/** One total: the value over its name, read as one element. */
@Composable
private fun Total(name: String, value: String, texts: MonarchAiTraceViewTexts) {
    Column(modifier = Modifier.clearAndSetSemantics { contentDescription = texts.fact(name, value) }) {
        Text(text = name, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
        MonarchTabularNumber(text = value, style = MonarchTextStyle.BodyStrong)
    }
}

@Composable
private fun Muted(text: String) {
    Text(text = text, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
}
