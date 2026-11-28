package dev.monarchui.components.intelligence.aiconversationlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateKind
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchTimeZone
import dev.monarchui.theme.MonarchTheme

/**
 * The history of an AI chat: conversations grouped into Pinned, Today, Previous 7 days and Older, with search, the
 * open conversation marked, and Rename, Pin and Delete on each row.
 *
 * Built by Monarch on a Compose `LazyColumn` with a `stickyHeader` per group, so give it a bounded height (a
 * navigation drawer, a pane, or `Modifier.fillMaxSize()`). A `MonarchSearchBar` filters by title and preview, or
 * hands the search to [onSearch]. Each row opens its conversation on a tap; a long press, the optional [menuIcon]
 * button and the TalkBack actions offer Rename, Pin or Unpin and Delete. Rename asks for the title in a
 * `MonarchDialog` with a `MonarchTextField`; Delete asks for confirmation in another.
 *
 * Groups are decided by calendar days in [timeZone] counted from [nowMillis]; pass a fixed time to keep tests and
 * previews stable.
 *
 * @param conversations the history, in any order.
 * @param onSelect opens a conversation, given its id.
 * @param modifier the modifier of the list.
 * @param activeId the open conversation, marked with a start bar, a bold title and the selected state.
 * @param searchable shows the search field.
 * @param visibleCount how many rows are shown at first, and how many "Show more" adds.
 * @param nowMillis the time the groups are counted from, in epoch milliseconds (UTC). `null` uses the clock.
 * @param timeZone the time zone that decides the day boundaries. An IANA id such as `"Europe/Berlin"`. `null`, or an id
 * the device does not know, is the device's zone.
 * @param onSearch runs the search in the app; the list then shows [conversations] as given instead of filtering.
 * @param onNew shows a New conversation button at the top.
 * @param onRename offers Rename; receives the trimmed new title, never an empty or unchanged one.
 * @param onPin offers Pin or Unpin; receives the id and whether to pin.
 * @param onDelete offers Delete, after a confirmation.
 * @param menuIcon the icon of a menu button at the end of each row, such as a vertical ellipsis. Without it the menu
 * opens on a long press and from the TalkBack actions. Monarch bundles no icons.
 * @param labels replaces any of the built-in strings.
 */
@Composable
public fun MonarchAiConversationList(
    conversations: List<MonarchAiConversationSummary>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    activeId: String? = null,
    searchable: Boolean = true,
    visibleCount: Int = 200,
    nowMillis: Long? = null,
    timeZone: String? = null,
    onSearch: ((String) -> Unit)? = null,
    onNew: (() -> Unit)? = null,
    onRename: ((id: String, title: String) -> Unit)? = null,
    onPin: ((id: String, pinned: Boolean) -> Unit)? = null,
    onDelete: ((id: String) -> Unit)? = null,
    menuIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.More,
    labels: MonarchAiConversationListLabels = MonarchAiConversationListLabels(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable(visibleCount) { mutableIntStateOf(visibleCount) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = if (onSearch != null) conversations else MonarchAiConversationListTokens.filter(conversations, query)
    val untitled = monarchString(labels.untitled, R.string.monarch_aiconversationlist_untitled)
    val callbacks = ListCallbacks(onSelect, onRename, onPin, onDelete)
    val body = ListBody(
        conversations = conversations,
        shown = shown,
        groups = MonarchAiConversationListTokens.grouped(
            shown,
            nowMillis ?: System.currentTimeMillis(),
            monarchTimeZone(timeZone),
            limit,
        ),
        query = query,
        setQuery = {
            query = it
            onSearch?.invoke(it)
        },
        searchable = searchable,
        onNew = onNew,
        hidden = shown.size - limit,
        showMore = { limit += visibleCount.coerceAtLeast(1) },
    )
    val row = RowOptions(activeId, untitled, labels, callbacks, menuIcon, { renaming = it }) { deleting = it }
    Box(modifier = modifier) {
        ConversationLazyList(body, row, labels)
        // Always present, so the number found is announced when a local search changes.
        val results = if (onSearch == null && query.isNotBlank()) resultsText(shown.size, labels) else null
        Box(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (results != null) contentDescription = results
            },
        )
    }
    ListDialogs(
        renamed = conversations.firstOrNull { it.id == renaming },
        deleted = conversations.firstOrNull { it.id == deleting },
        untitled = untitled,
        labels = labels,
        callbacks = callbacks,
        closeRename = { renaming = null },
        closeDelete = { deleting = null },
    )
}

/** The app's callbacks. */
private class ListCallbacks(
    val onSelect: (String) -> Unit,
    val onRename: ((String, String) -> Unit)?,
    val onPin: ((String, Boolean) -> Unit)?,
    val onDelete: ((String) -> Unit)?,
)

/** What the lazy list shows. */
private class ListBody(
    val conversations: List<MonarchAiConversationSummary>,
    val shown: List<MonarchAiConversationSummary>,
    val groups: List<Pair<MonarchAiConversationGroup, List<MonarchAiConversationSummary>>>,
    val query: String,
    val setQuery: (String) -> Unit,
    val searchable: Boolean,
    val onNew: (() -> Unit)?,
    val hidden: Int,
    val showMore: () -> Unit,
)

@Composable
private fun ConversationLazyList(body: ListBody, row: RowOptions, labels: MonarchAiConversationListLabels) {
    val listName = monarchString(labels.label, R.string.monarch_aiconversationlist_label)
    val newText = monarchString(labels.newConversation, R.string.monarch_aiconversationlist_new_conversation)
    val searchText = monarchString(labels.search, R.string.monarch_aiconversationlist_search)
    LazyColumn(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = listName },
        verticalArrangement = Arrangement.spacedBy(MonarchAiConversationListTokens.Gap),
    ) {
        body.onNew?.let { onNew ->
            item(key = "new", contentType = "new") {
                MonarchButton(
                    text = newText,
                    onClick = onNew,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = MonarchDefaultIcons.Plus,
                )
            }
        }
        if (body.searchable && (body.conversations.isNotEmpty() || body.query.isNotEmpty())) {
            item(key = "search", contentType = "search") {
                MonarchSearchBar(
                    query = body.query,
                    onQueryChange = body.setQuery,
                    onSearch = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = searchText,
                )
            }
        }
        when {
            body.conversations.isEmpty() && body.query.isEmpty() -> item(key = "empty", contentType = "empty") {
                MonarchEmptyState(
                    title = monarchString(labels.empty, R.string.monarch_aiconversationlist_empty),
                    size = MonarchEmptyStateSize.Sm,
                )
            }
            body.shown.isEmpty() -> item(key = "no-results", contentType = "empty") {
                val query = body.query.trim()
                MonarchEmptyState(
                    title = labels.noResults?.invoke(query)
                        ?: stringResource(R.string.monarch_aiconversationlist_no_results, query),
                    kind = MonarchEmptyStateKind.Filtered,
                    size = MonarchEmptyStateSize.Sm,
                    onClearFilters = { body.setQuery("") },
                )
            }
            else -> body.groups.forEach { (group, items) -> groupItems(group, items, row) }
        }
        if (body.hidden > 0) {
            item(key = "show-more", contentType = "show-more") {
                MonarchButton(
                    text = labels.showMore?.invoke(body.hidden) ?: pluralStringResource(
                        R.plurals.monarch_aiconversationlist_show_more,
                        body.hidden,
                        body.hidden,
                    ),
                    onClick = body.showMore,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}

@Composable
private fun ListDialogs(
    renamed: MonarchAiConversationSummary?,
    deleted: MonarchAiConversationSummary?,
    untitled: String,
    labels: MonarchAiConversationListLabels,
    callbacks: ListCallbacks,
    closeRename: () -> Unit,
    closeDelete: () -> Unit,
) {
    val onRename = callbacks.onRename
    if (renamed != null && onRename != null) {
        MonarchAiConversationListRenameDialog(renamed.title, labels, onDismiss = closeRename) { title ->
            closeRename()
            onRename(renamed.id, title)
        }
    }
    val onDelete = callbacks.onDelete
    if (deleted != null && onDelete != null) {
        MonarchAiConversationListDeleteDialog(
            title = MonarchAiConversationListTokens.title(deleted.title, untitled),
            labels = labels,
            onDismiss = closeDelete,
        ) {
            closeDelete()
            onDelete(deleted.id)
        }
    }
}

/** What every row needs. */
private class RowOptions(
    val activeId: String?,
    val untitled: String,
    val labels: MonarchAiConversationListLabels,
    val callbacks: ListCallbacks,
    val menuIcon: (@Composable () -> Unit)?,
    val startRename: (String) -> Unit,
    val startDelete: (String) -> Unit,
)

/** One command of a row's menu. */
private class RowAction(val label: String, val run: () -> Unit)

private fun LazyListScope.groupItems(
    group: MonarchAiConversationGroup,
    items: List<MonarchAiConversationSummary>,
    row: RowOptions,
) {
    stickyHeader(key = "group-${group.name}", contentType = "group") { GroupHeading(group, row.labels) }
    items(items, key = { "conversation-${it.id}" }, contentType = { "conversation" }) { summary ->
        ConversationRow(summary, row)
    }
}

@Composable
private fun GroupHeading(group: MonarchAiConversationGroup, labels: MonarchAiConversationListLabels) {
    val colors = MonarchTheme.colors
    val text = labels.group?.invoke(group) ?: stringResource(
        when (group) {
            MonarchAiConversationGroup.Pinned -> R.string.monarch_aiconversationlist_group_pinned
            MonarchAiConversationGroup.Today -> R.string.monarch_aiconversationlist_group_today
            MonarchAiConversationGroup.Previous7Days -> R.string.monarch_aiconversationlist_group_previous_7_days
            MonarchAiConversationGroup.Older -> R.string.monarch_aiconversationlist_group_older
        },
    )
    MonarchText(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(MonarchAiConversationListTokens.headingBackground(colors))
            .padding(vertical = MonarchAiConversationListTokens.HeadingPadding),
        style = MonarchTextStyle.Label,
        color = MonarchAiConversationListTokens.muted(colors),
        heading = true,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(summary: MonarchAiConversationSummary, row: RowOptions) {
    val colors = MonarchTheme.colors
    val active = summary.id == row.activeId
    val title = MonarchAiConversationListTokens.title(summary.title, row.untitled)
    val actions = rowActions(summary, row)
    var menuOpen by rememberSaveable(summary.id) { mutableStateOf(false) }
    val shape = MonarchTheme.shapes.sm
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .heightIn(min = MonarchTouchTarget.Minimum)
                .clip(shape)
                .background(if (active) MonarchAiConversationListTokens.activeBackground(colors) else Color.Transparent)
                .monarchFocusRing(shape)
                .combinedClickable(
                    role = Role.Button,
                    onLongClick = if (actions.isEmpty()) null else ({ menuOpen = true }),
                    onClick = { row.callbacks.onSelect(summary.id) },
                )
                .semantics(mergeDescendants = true) {
                    selected = active
                    customActions = actions.map { action ->
                        CustomAccessibilityAction(action.label) {
                            action.run()
                            true
                        }
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(MonarchAiConversationListTokens.RowGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(MonarchAiConversationListTokens.MarkerWidth)
                    .fillMaxHeight()
                    .background(if (active) MonarchAiConversationListTokens.marker(colors) else Color.Transparent),
            )
            if (summary.pinned) {
                // The pin mark of a pinned conversation, as the web's star. Decoration.
                Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Star() }
            }
            Column(
                modifier = Modifier.weight(1f).padding(vertical = MonarchAiConversationListTokens.RowPadding),
            ) {
                MonarchText(
                    text = title,
                    style = if (active) MonarchTextStyle.BodyStrong else MonarchTextStyle.Body,
                    maxLines = MonarchAiConversationListTokens.TitleLines,
                    overflow = TextOverflow.Ellipsis,
                )
                summary.preview?.takeIf { it.isNotBlank() }?.let {
                    MonarchText(
                        text = it,
                        style = MonarchTextStyle.Caption,
                        color = MonarchAiConversationListTokens.muted(colors),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val icon = row.menuIcon
            if (icon != null && actions.isNotEmpty()) {
                val name = row.labels.moreActions?.invoke(title)
                    ?: stringResource(R.string.monarch_aiconversationlist_more_actions, title)
                MonarchIconButton(label = name, onClick = { menuOpen = true }) { icon() }
            }
        }
        MonarchMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            actions.forEach { action ->
                MonarchMenuItem(
                    text = action.label,
                    onClick = {
                        menuOpen = false
                        action.run()
                    },
                )
            }
        }
    }
}

@Composable
private fun rowActions(summary: MonarchAiConversationSummary, row: RowOptions): List<RowAction> {
    val labels = row.labels
    val rename = monarchString(labels.rename, R.string.monarch_aiconversationlist_rename)
    val pin = if (summary.pinned) {
        monarchString(labels.unpin, R.string.monarch_aiconversationlist_unpin)
    } else {
        monarchString(labels.pin, R.string.monarch_aiconversationlist_pin)
    }
    val delete = monarchString(labels.delete, R.string.monarch_aiconversationlist_delete)
    return listOfNotNull(
        row.callbacks.onRename?.let { RowAction(rename) { row.startRename(summary.id) } },
        row.callbacks.onPin?.let { onPin -> RowAction(pin) { onPin(summary.id, !summary.pinned) } },
        row.callbacks.onDelete?.let { RowAction(delete) { row.startDelete(summary.id) } },
    )
}

@Composable
private fun resultsText(count: Int, labels: MonarchAiConversationListLabels): String =
    labels.results?.invoke(count) ?: pluralStringResource(R.plurals.monarch_aiconversationlist_results, count, count)
