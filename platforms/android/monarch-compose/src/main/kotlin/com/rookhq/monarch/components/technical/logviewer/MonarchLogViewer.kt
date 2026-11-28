package com.rookhq.monarch.components.technical.logviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.tag.MonarchChip
import com.rookhq.monarch.components.datadisplay.tag.MonarchTagSize
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateKind
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingStateVariant
import com.rookhq.monarch.components.search.searchbar.MonarchSearchBar
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale
import java.util.TimeZone

/**
 * A streaming log with level filters and counts, search that filters or
 * highlights, follow mode that keeps the newest line in view, and a wrap
 * toggle.
 *
 * Built on a `LazyColumn` of monospaced lines, so only the lines in view are
 * composed however long the log grows, with a `MonarchSearchBar`, `MonarchChip`s
 * for the levels, Follow and Wrap lines, and `MonarchEmptyState`
 * and `MonarchLoadingState` for the empty, filtered and loading states. While
 * following, every change of the lines scrolls to the newest one. Dragging
 * toward older lines pauses following and shows a Jump to latest button, which
 * counts the lines that arrive; scrolling back to the end resumes it. Each
 * state with an `on…Change` callback can be controlled by the app; with `null`
 * the viewer keeps it itself.
 *
 * The viewer takes the height it is given and scrolls inside it: give it a
 * height, or the rest of the screen, and do not put it in another vertical
 * scroll.
 *
 * @param lines the lines, oldest first. Append to the list to stream.
 * @param modifier the modifier of the viewer. Give it a bounded height.
 * @param levels the levels shown. `null` keeps them inside, with every level on at first.
 * @param onLevelsChange called with the new set when a level filter is toggled or the filters are cleared.
 * @param query the search text. `null` keeps it inside. Trimmed and matched against the message and the source,
 * ignoring case.
 * @param onQueryChange called with the new text on every edit and when the filters are cleared.
 * @param following whether the newest line is kept in view. `null` keeps it inside, on at first.
 * @param onFollowingChange called with `false` when the reader drags toward older lines or turns Follow off,
 * and with `true` when they return to the end, tap Jump to latest or turn Follow on.
 * @param wrapped whether long lines wrap. `null` keeps it inside, off at first. Unwrapped lines scroll sideways.
 * @param onWrappedChange called with the new state when Wrap lines is toggled.
 * @param searchMode filter hides lines without a match; highlight keeps every line. Both mark the matches.
 * @param showTimestamps shows each line's time as 24-hour `HH:mm:ss.SSS` in the device's time zone.
 * @param showLineNumbers shows each line's 1-based position in [lines], which stays the same when filters hide
 * lines.
 * @param loading shows Loading log while [lines] is empty.
 * @param state the scroll state of the log.
 * @param clearSearchIcon an icon for the search field's clear button; without it the field has none.
 * @param labels replacements for the built-in strings.
 * @param actions extra controls after Follow and Wrap lines, such as a share or clear button.
 */
@Composable
public fun MonarchLogViewer(
    lines: List<MonarchLogLine>,
    modifier: Modifier = Modifier,
    levels: Set<MonarchLogLevel>? = null,
    onLevelsChange: ((Set<MonarchLogLevel>) -> Unit)? = null,
    query: String? = null,
    onQueryChange: ((String) -> Unit)? = null,
    following: Boolean? = null,
    onFollowingChange: ((Boolean) -> Unit)? = null,
    wrapped: Boolean? = null,
    onWrappedChange: ((Boolean) -> Unit)? = null,
    searchMode: MonarchLogViewerSearchMode = MonarchLogViewerSearchMode.Filter,
    showTimestamps: Boolean = true,
    showLineNumbers: Boolean = false,
    loading: Boolean = false,
    state: LazyListState = rememberLazyListState(),
    clearSearchIcon: (@Composable () -> Unit)? = null,
    labels: MonarchLogViewerLabels = MonarchLogViewerLabels(),
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MonarchLogViewerTokens
    val texts = monarchLogViewerTexts(labels)
    val locale = LocalConfiguration.current.locales[0]
    val controls = rememberMonarchLogViewerControls()
    controls.update(
        MonarchLogViewerValues(
            levels = levels,
            query = query,
            following = following,
            wrapped = wrapped,
            onLevelsChange = onLevelsChange,
            onQueryChange = onQueryChange,
            onFollowingChange = onFollowingChange,
            onWrappedChange = onWrappedChange,
        ),
        texts,
    )
    val shownLevels = controls.levels
    val shownQuery = controls.query
    // Not remembered by the list: a SnapshotStateList that grows stays the same object.
    val visible = MonarchLogViewerLogic.visible(lines, shownLevels, shownQuery, searchMode)
    val counts = MonarchLogViewerLogic.counts(lines)
    FollowEffects(controls, state, lines.size, visible.size)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        MonarchSearchBar(
            query = shownQuery,
            onQueryChange = controls::setQuery,
            onSearch = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = texts.search,
            clearIcon = clearSearchIcon,
            clearLabel = texts.clearSearch,
        )
        LevelFilters(shownLevels, counts, locale, texts, controls::setLevels)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            MonarchChip(
                text = texts.follow,
                selected = controls.following,
                onSelectedChange = { controls.setFollowing(it, byDrag = false) },
                neutral = true,
            )
            MonarchChip(
                text = texts.wrap,
                selected = controls.wrapped,
                onSelectedChange = { controls.setWrapped(it) },
                neutral = true,
            )
            actions?.invoke(this)
        }
        LogArea(
            lines = lines,
            visible = visible,
            controls = controls,
            state = state,
            settings = MonarchLogViewerLineSettings(
                query = MonarchLogViewerLogic.normalized(shownQuery),
                wrapped = controls.wrapped,
                showTimestamps = showTimestamps,
                showLineNumbers = showLineNumbers,
                zone = remember { TimeZone.getDefault() },
            ),
            loading = loading,
            locale = locale,
            texts = texts,
        )
        MonarchText(
            text = texts.count(visible.size, lines.size, locale),
            style = MonarchTextStyle.Caption,
            color = tokens.metaColor(MonarchTheme.colors),
        )
        MonarchLiveStatus(text = controls.announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun LevelFilters(
    levels: Set<MonarchLogLevel>,
    counts: Map<MonarchLogLevel, Int>,
    locale: Locale,
    texts: MonarchLogViewerTexts,
    onLevelsChange: (Set<MonarchLogLevel>) -> Unit,
) {
    val tokens = MonarchLogViewerTokens
    FlowRow(
        modifier = Modifier.semantics {
            isTraversalGroup = true
            contentDescription = texts.levels
        },
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        MonarchLogLevel.entries.forEach { level ->
            val count = MonarchLogViewerLogic.count(counts[level] ?: 0, locale)
            MonarchChip(
                text = "${texts.levelName(level)} $count",
                selected = level in levels,
                onSelectedChange = { onLevelsChange(MonarchLogViewerLogic.toggled(levels, level)) },
                size = MonarchTagSize.Sm,
                neutral = true,
            )
        }
    }
}

/** Keeps the newest line in view while following, pauses on a drag toward older lines and resumes at the end. */
@Composable
private fun FollowEffects(controls: MonarchLogViewerControls, state: LazyListState, total: Int, shown: Int) {
    var totalAtPause by remember { mutableIntStateOf(total) }
    controls.newLines = MonarchLogViewerLogic.newLines(total, totalAtPause)
    LaunchedEffect(controls.following) {
        if (!controls.following) totalAtPause = total
    }
    LaunchedEffect(controls.following, shown, total) {
        if (controls.following && shown > 0) state.scrollToItem(shown - 1)
    }
    val currentControls by rememberUpdatedState(controls)
    LaunchedEffect(state) {
        snapshotFlow { !state.isScrollInProgress && !state.canScrollForward }.collect { atEnd ->
            if (atEnd && currentControls.pausedByDrag) currentControls.setFollowing(true, byDrag = false)
        }
    }
}

@Composable
private fun ColumnScope.LogArea(
    lines: List<MonarchLogLine>,
    visible: List<Int>,
    controls: MonarchLogViewerControls,
    state: LazyListState,
    settings: MonarchLogViewerLineSettings,
    loading: Boolean,
    locale: Locale,
    texts: MonarchLogViewerTexts,
) {
    val tokens = MonarchLogViewerTokens
    val shape = MonarchShapeDefaults.Container
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.surface(MonarchTheme.colors), shape),
    ) {
        when {
            lines.isEmpty() && loading -> MonarchLogViewerStateBox {
                MonarchLoadingState(label = texts.loading, variant = MonarchLoadingStateVariant.Spinner)
            }
            lines.isEmpty() -> MonarchLogViewerStateBox {
                MonarchEmptyState(title = texts.empty, size = MonarchEmptyStateSize.Sm)
            }
            visible.isEmpty() -> MonarchLogViewerStateBox {
                MonarchEmptyState(
                    title = texts.noMatches,
                    kind = MonarchEmptyStateKind.Filtered,
                    size = MonarchEmptyStateSize.Sm,
                    onClearFilters = controls::clearFilters,
                    clearFiltersLabel = texts.clearFilters,
                )
            }
            else -> MonarchLogViewerLines(
                lines = lines,
                visible = visible,
                state = state,
                settings = settings,
                texts = texts,
                onDragToOlder = { controls.setFollowing(false, byDrag = true) },
            )
        }
        if (!controls.following && visible.isNotEmpty()) {
            val count = controls.newLines
            MonarchButton(
                text = if (count > 0) texts.newLines(count, locale) else texts.jumpToLatest,
                onClick = { controls.setFollowing(true, byDrag = false) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = tokens.JumpPadding),
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}
