package com.rookhq.monarch.components.files.pageoutline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.text.NumberFormat

/**
 * A document's headings as a nested list, with the section being read
 * marked as current. Picking a heading calls [onNavigate], where the app
 * scrolls its document to it.
 *
 * Built by Monarch on a `LazyColumn`: Compose has no outline. Each heading is
 * one tappable row with a 48dp minimum height, indented one step per nesting
 * level, with a bar at its start. The current heading is in the accent strong
 * colour and a stronger weight with an accent bar, and TalkBack reads it as
 * selected with the state "Current section", so it does not depend on colour.
 * When [currentId] changes, the outline scrolls itself so the current heading
 * is visible, without animation when the user has turned animations off.
 *
 * The app owns the document and its scroll position: it passes the headings
 * and the current id (for example from the document's
 * `LazyListState.firstVisibleItemIndex`), and in [onNavigate] calls
 * `LazyListState.animateScrollToItem` on the document and closes the sheet
 * the outline is in. Give the outline a bounded height (a sheet or a pane);
 * it scrolls on its own and cannot sit inside another vertical scroll.
 *
 * @param headings the headings in document order.
 * @param onNavigate called with the heading the person picked.
 * @param modifier the modifier of the outline.
 * @param currentId the id of the heading of the section being read, or `null`.
 * @param maxLevel the deepest heading level listed.
 * @param stats the word count and reading time shown under the list, for example
 * `MonarchPageOutlineStats.fromText(document)`.
 * @param enabled whether headings can be picked. Disabled headings use the disabled text colour.
 * @param showTitle shows the "On this page" title. Hide it when a sheet's title already says it; the outline keeps
 * it as its spoken name.
 * @param state the scroll state of the outline's own list.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchPageOutline(
    headings: List<MonarchPageOutlineHeading>,
    onNavigate: (MonarchPageOutlineHeading) -> Unit,
    modifier: Modifier = Modifier,
    currentId: String? = null,
    maxLevel: Int = 3,
    stats: MonarchPageOutlineStats? = null,
    enabled: Boolean = true,
    showTitle: Boolean = true,
    state: LazyListState = rememberLazyListState(),
    labels: MonarchPageOutlineLabels = MonarchPageOutlineLabels(),
) {
    val tokens = MonarchPageOutlineTokens
    val colors = MonarchTheme.colors
    val entries = remember(headings, maxLevel) { tokens.entries(headings, maxLevel) }
    val title = monarchString(labels.title, R.string.monarch_pageoutline_title)
    val currentLabel = monarchString(labels.current, R.string.monarch_pageoutline_current)
    val statsText = stats?.let { labels.stats?.invoke(it) ?: defaultStats(it) }
    val reducedMotion = MonarchTheme.motion.reducedMotion
    val firstHeading = if (showTitle) 1 else 0
    LaunchedEffect(currentId, entries) {
        val index = entries.indexOfFirst { it.heading.id == currentId }
        if (index >= 0) {
            val item = firstHeading + index
            val layout = state.layoutInfo
            val shown = layout.visibleItemsInfo.any {
                it.index == item && it.offset >= layout.viewportStartOffset &&
                    it.offset + it.size <= layout.viewportEndOffset
            }
            if (!shown) if (reducedMotion) state.scrollToItem(item) else state.animateScrollToItem(item)
        }
    }
    LazyColumn(
        modifier = modifier.semantics {
            contentDescription = title
            isTraversalGroup = true
        },
        state = state,
        verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
    ) {
        if (showTitle) {
            item(key = "monarch-pageoutline-title") {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MonarchTheme.typography.label,
                    color = colors.textDefault,
                )
            }
        }
        if (entries.isEmpty()) {
            item(key = "monarch-pageoutline-empty") {
                Text(
                    text = monarchString(labels.empty, R.string.monarch_pageoutline_empty),
                    style = MonarchTheme.typography.caption,
                    color = colors.textMuted,
                )
            }
        }
        items(entries, key = { it.heading.id }) { entry ->
            HeadingRow(
                entry = entry,
                current = entry.heading.id == currentId,
                enabled = enabled,
                currentLabel = currentLabel,
                onClick = { onNavigate(entry.heading) },
            )
        }
        if (statsText != null) {
            item(key = "monarch-pageoutline-stats") {
                Text(text = statsText, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun HeadingRow(
    entry: MonarchPageOutlineEntry,
    current: Boolean,
    enabled: Boolean,
    currentLabel: String,
    onClick: () -> Unit,
) {
    val tokens = MonarchPageOutlineTokens
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .height(IntrinsicSize.Min)
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                selected = current
                if (current) stateDescription = currentLabel
            }
            .padding(start = tokens.IndentStep * entry.depth),
        horizontalArrangement = Arrangement.spacedBy(tokens.BarGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(tokens.BarWidth)
                .fillMaxHeight()
                .background(tokens.barColor(current, enabled, colors)),
        )
        Text(
            text = entry.heading.text,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = tokens.RowPadding),
            style = if (current) MonarchTheme.typography.bodyStrong else MonarchTheme.typography.body,
            color = tokens.textColor(current, enabled, colors),
        )
    }
}

@Composable
private fun defaultStats(stats: MonarchPageOutlineStats): String {
    val locale = LocalConfiguration.current.locales[0]
    val words = pluralStringResource(
        R.plurals.monarch_pageoutline_words,
        stats.words,
        NumberFormat.getIntegerInstance(locale).format(stats.words),
    )
    val minutes = pluralStringResource(R.plurals.monarch_pageoutline_minutes, stats.minutes, stats.minutes)
    return stringResource(R.string.monarch_pageoutline_stats, words, minutes)
}
