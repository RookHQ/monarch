package com.rookhq.monarch.components.workflow.stagejourney

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * Every stage move of an item, as a vertical timeline: from, to, when, who
 * and why. The record next to a `MonarchStageJourney`.
 *
 * Built by Monarch from a `Column`; the dots and the line are drawn at the
 * start edge and hidden from TalkBack. Each entry is one element read in
 * words: "Filed to Examination, (moved back), 3 days ago, Ana, 2 deadlines
 * added, Waiting for the client". It is not lazy; put it in a scrolling
 * container.
 *
 * @param entries the moves, newest first or oldest first as the product prefers.
 * @param modifier the modifier of the history.
 * @param now a fixed reference time for the relative times. `null` is the time of composition.
 * @param emptyMessage shown when there are no entries, replacing "No stage moves yet.".
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchStageHistory(
    entries: List<MonarchStageHistoryEntry>,
    modifier: Modifier = Modifier,
    now: Long? = null,
    emptyMessage: String? = null,
    labels: MonarchStageHistoryLabels = MonarchStageHistoryLabels(),
) {
    val colors = MonarchTheme.colors
    if (entries.isEmpty()) {
        Text(
            text = emptyMessage ?: monarchString(labels.empty, R.string.monarch_stagejourney_history_empty),
            modifier = modifier,
            style = MonarchTheme.typography.body,
            color = colors.textMuted,
        )
        return
    }
    val reference = now ?: System.currentTimeMillis()
    val to = monarchString(labels.to, R.string.monarch_stagejourney_history_to)
    val movedBack = monarchString(labels.movedBack, R.string.monarch_stagejourney_history_moved_back)
    val startedFormat = stringResource(R.string.monarch_stagejourney_history_started)
    val tokens = MonarchStageJourneyTokens
    val arrow = tokens.arrow(LocalLayoutDirection.current)
    Column(modifier = modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, entry ->
            val started = labels.started?.invoke(entry.to) ?: startedFormat.format(entry.to)
            val time = MonarchRelativeTime.format(entry.at, reference, LocalConfiguration.current.locales[0])
            val spoken = listOfNotNull(
                entry.from?.let { "$it $to ${entry.to}" } ?: started,
                movedBack.takeIf { entry.backward },
                time,
                entry.actor,
                entry.meta,
                entry.note,
            ).joinToString(", ")
            val (dot, line) = tokens.historyColors(colors, entry.backward)
            val last = index == entries.lastIndex
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val radius = tokens.DotSize.toPx() / 2
                        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - radius else radius
                        val top = tokens.DotTop.toPx() + radius
                        if (!last) {
                            drawLine(line, Offset(x, top), Offset(x, size.height), tokens.LineWidth.toPx())
                        }
                        drawCircle(dot, radius, Offset(x, top))
                    }
                    .padding(start = tokens.HistoryIndent, bottom = if (last) tokens.SmallGap else tokens.Gap)
                    .clearAndSetSemantics { contentDescription = spoken },
                verticalArrangement = Arrangement.spacedBy(tokens.SmallGap),
            ) {
                Text(
                    text = buildAnnotatedString {
                        val from = entry.from
                        if (from == null) {
                            append(started)
                        } else {
                            append("$from $arrow ")
                            withStyle(SpanStyle(fontWeight = FontWeight(MonarchTokens.FontWeight.semibold))) {
                                append(entry.to)
                            }
                        }
                        if (entry.backward) append(" $movedBack")
                    },
                    style = MonarchTheme.typography.body,
                    color = colors.textDefault,
                )
                Text(text = time, style = MonarchTheme.typography.caption, color = colors.textMuted)
                listOfNotNull(entry.actor, entry.meta).joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
                    Text(text = it, style = MonarchTheme.typography.caption, color = colors.textMuted)
                }
                entry.note?.let { Text(text = it, style = MonarchTheme.typography.body, color = colors.textDefault) }
            }
        }
    }
}
