package com.rookhq.monarch.components.search.searchresults

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateKind
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorState
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeletonShape
import com.rookhq.monarch.components.search.searchresultitem.MonarchSearchResultItem
import com.rookhq.monarch.components.search.searchresultitem.monarchQueryTerms
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * Search results in labelled groups with counts, highlighted matches, a
 * summary that TalkBack reads when it changes, Show all buttons, and loading,
 * empty and error states.
 *
 * Built on a `LazyColumn` of `MonarchSearchResultItem` rows (the Material 3
 * `ListItem`) under group headings with heading semantics. While a search
 * runs with no results yet, skeleton rows stand in; when results are already
 * shown they stay and the summary reads "Searching…". With no results the
 * filtered `MonarchEmptyState` names the query; when the search fails,
 * `MonarchErrorState` replaces the results. The list scrolls itself: give it
 * a bounded height, such as the rest of the screen, and do not put it in
 * another vertical scroll.
 *
 * @param groups the results in groups. A group with a `null` title has no heading, for results without
 * groups. Groups without results are left out.
 * @param onSelect opens a result.
 * @param modifier the modifier of the results.
 * @param query what the person searched for: its words are highlighted and it is named in the summary and
 * the empty title.
 * @param loading a search is running.
 * @param failed the search failed: the error state replaces the results.
 * @param errorMessage the reason, shown under the error title.
 * @param loadingRows the number of skeleton rows while loading with no results.
 * @param onRetry adds Try again to the error state.
 * @param onShowAll adds "Show all 24 in Documents" after a titled group whose total is larger than its results.
 * @param state the scroll state of the list.
 * @param emptyIcon an icon for the empty state, such as a magnifying glass. Decorative.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchSearchResults(
    groups: List<MonarchSearchResultGroup>,
    onSelect: (MonarchSearchResult, MonarchSearchResultGroup) -> Unit,
    modifier: Modifier = Modifier,
    query: String = "",
    loading: Boolean = false,
    failed: Boolean = false,
    errorMessage: String? = null,
    loadingRows: Int = 4,
    onRetry: (() -> Unit)? = null,
    onShowAll: ((MonarchSearchResultGroup) -> Unit)? = null,
    state: LazyListState = rememberLazyListState(),
    emptyIcon: (@Composable () -> Unit)? = null,
    labels: MonarchSearchResultsLabels = MonarchSearchResultsLabels(),
) {
    val tokens = MonarchSearchResultsTokens
    val visible = tokens.visibleGroups(groups)
    val status = tokens.status(failed, visible.isNotEmpty(), loading)
    val shownQuery = tokens.shownQuery(query)
    val locale = LocalConfiguration.current.locales[0]
    val emptyTitle = emptyTitle(shownQuery, labels)
    val errorTitle = monarchString(labels.errorTitle, R.string.monarch_searchresults_error_title)
    val summary = summaryText(status, tokens.total(visible), shownQuery, locale, labels, emptyTitle, errorTitle)
    val summaryShown = status == MonarchSearchResultsStatus.Ready || status == MonarchSearchResultsStatus.Refreshing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        // One node that stays in the tree, so every change of the summary is announced. When the summary is
        // not drawn, its text is the node's description.
        Box(
            modifier = Modifier
                .padding(horizontal = tokens.InlinePadding)
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    if (!summaryShown) contentDescription = summary
                },
        ) {
            if (summaryShown) {
                MonarchText(
                    text = summary,
                    style = MonarchTextStyle.Caption,
                    color = tokens.mutedColor(MonarchTheme.colors),
                )
            }
        }
        when (status) {
            MonarchSearchResultsStatus.Ready, MonarchSearchResultsStatus.Refreshing -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                state = state,
            ) {
                val context = ResultsContext(monarchQueryTerms(query), onSelect, onShowAll, locale, labels)
                visible.forEach { group -> resultGroup(group, context) }
            }
            MonarchSearchResultsStatus.Loading -> SkeletonRows(loadingRows)
            MonarchSearchResultsStatus.Empty -> StateBox {
                MonarchEmptyState(
                    title = emptyTitle,
                    kind = MonarchEmptyStateKind.Filtered,
                    size = MonarchEmptyStateSize.Sm,
                    message = monarchString(labels.emptyMessage, R.string.monarch_searchresults_empty_message),
                    icon = emptyIcon,
                )
            }
            MonarchSearchResultsStatus.Error -> StateBox {
                MonarchErrorState(
                    title = errorTitle,
                    size = MonarchEmptyStateSize.Sm,
                    message = errorMessage,
                    onRetry = onRetry,
                    retryLabel = monarchString(labels.retry, R.string.monarch_searchresults_retry),
                )
            }
        }
    }
}

@Composable
private fun emptyTitle(query: String, labels: MonarchSearchResultsLabels): String =
    labels.emptyTitle?.invoke(query) ?: if (query.isEmpty()) {
        stringResource(R.string.monarch_searchresults_empty_title)
    } else {
        stringResource(R.string.monarch_searchresults_empty_title_query, query)
    }

@Composable
private fun summaryText(
    status: MonarchSearchResultsStatus,
    total: Int,
    query: String,
    locale: Locale,
    labels: MonarchSearchResultsLabels,
    emptyTitle: String,
    errorTitle: String,
): String = when (status) {
    MonarchSearchResultsStatus.Ready -> labels.summary?.invoke(total, query) ?: countSummary(total, query, locale)
    MonarchSearchResultsStatus.Refreshing, MonarchSearchResultsStatus.Loading ->
        monarchString(labels.loading, R.string.monarch_searchresults_loading)
    MonarchSearchResultsStatus.Empty -> emptyTitle
    MonarchSearchResultsStatus.Error -> errorTitle
}

@Composable
private fun countSummary(total: Int, query: String, locale: Locale): String {
    val count = MonarchSearchResultsTokens.count(total, locale)
    return if (query.isEmpty()) {
        pluralStringResource(R.plurals.monarch_searchresults_summary, total, count)
    } else {
        pluralStringResource(R.plurals.monarch_searchresults_summary_query, total, count, query)
    }
}

/** What every row and heading of the results needs. */
private class ResultsContext(
    val terms: List<String>,
    val onSelect: (MonarchSearchResult, MonarchSearchResultGroup) -> Unit,
    val onShowAll: ((MonarchSearchResultGroup) -> Unit)?,
    val locale: Locale,
    val labels: MonarchSearchResultsLabels,
)

private fun LazyListScope.resultGroup(group: MonarchSearchResultGroup, context: ResultsContext) {
    val tokens = MonarchSearchResultsTokens
    val terms = context.terms
    val onSelect = context.onSelect
    val onShowAll = context.onShowAll
    val locale = context.locale
    val labels = context.labels
    val total = tokens.groupTotal(group)
    group.title?.let { title ->
        item(key = "heading:${group.id}", contentType = "heading") {
            GroupHeading(title, total, group.icon, locale, labels)
        }
    }
    items(items = group.results, key = { "result:${group.id}:${it.id}" }, contentType = { "result" }) { result ->
        MonarchSearchResultItem(
            title = result.title,
            path = result.path,
            snippet = result.snippet,
            terms = terms,
            titleRanges = result.titleRanges,
            snippetRanges = result.snippetRanges,
            snippetMarkTags = result.snippetMarkTags,
            fileName = result.fileName,
            onClick = { onSelect(result, group) },
            icon = result.icon,
            destructive = result.destructive,
            meta = result.meta?.let { meta -> { MonarchText(text = meta, style = MonarchTextStyle.Caption) } },
        )
    }
    val title = group.title
    if (onShowAll != null && title != null && tokens.showsAll(group, canShowAll = true)) {
        item(key = "more:${group.id}", contentType = "more") {
            val text = labels.showAll?.invoke(title, total)
                ?: stringResource(R.string.monarch_searchresults_show_all, tokens.count(total, locale), title)
            Box(Modifier.padding(horizontal = tokens.InlinePadding)) {
                MonarchButton(
                    text = text,
                    onClick = { onShowAll(group) },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}

@Composable
private fun GroupHeading(
    title: String,
    total: Int,
    icon: (@Composable () -> Unit)?,
    locale: Locale,
    labels: MonarchSearchResultsLabels,
) {
    val tokens = MonarchSearchResultsTokens
    val count = tokens.count(total, locale)
    val name = labels.groupName?.invoke(title, total)
        ?: pluralStringResource(R.plurals.monarch_searchresults_group_name, total, title, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = tokens.InlinePadding, end = tokens.InlinePadding, top = tokens.GroupGap)
            .clearAndSetSemantics {
                heading()
                contentDescription = name
            },
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Box(Modifier.size(tokens.HeadingIconSize)) { icon() }
        MonarchText(
            text = title,
            modifier = Modifier.weight(1f, fill = false),
            style = MonarchTextStyle.Subheading,
            color = tokens.headingColor(MonarchTheme.colors),
        )
        MonarchText(
            text = labels.groupCount?.invoke(total) ?: count,
            style = MonarchTextStyle.Caption,
            color = tokens.mutedColor(MonarchTheme.colors),
        )
    }
}

@Composable
private fun SkeletonRows(rows: Int) {
    val tokens = MonarchSearchResultsTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.InlinePadding)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(tokens.GroupGap),
    ) {
        repeat(rows.coerceAtLeast(0)) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                MonarchSkeleton(
                    modifier = Modifier.fillMaxWidth(tokens.SKELETON_TITLE_FRACTION),
                    shape = MonarchSkeletonShape.Text,
                )
                MonarchSkeleton(
                    modifier = Modifier.fillMaxWidth(tokens.SKELETON_LINE_FRACTION),
                    shape = MonarchSkeletonShape.Text,
                )
            }
        }
    }
}

/** The empty and error states, scrolling inside the results when they are taller than the space. */
@Composable
private fun StateBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) { content() }
}
