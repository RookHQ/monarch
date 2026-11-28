package com.rookhq.monarch.components.collaboration.notificationcenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMore
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateKind
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.navigation.tabs.MonarchTabs
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.currentIcuTimeZone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchClock
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The in-app list of notifications: a heading with Mark all as read, All and
 * Unread tabs, rows grouped under Today, Yesterday and dates, paging, and
 * loading, error and empty states.
 *
 * A Monarch pattern built from `MonarchTabs` and a
 * `LazyColumn` of `MonarchInboxRow`s, with `MonarchLoadMore`,
 * `MonarchLoadingState`, `MonarchErrorState` and `MonarchEmptyState`. It is
 * the content of a screen or a `ModalBottomSheet` opened from
 * `MonarchNotificationsBell`; it does not present itself. Give it a bounded
 * height, since the list scrolls. Marking one notification read is a swipe
 * toward the end, also offered as a TalkBack action. System notifications
 * are a separate concern of the app.
 *
 * @param items the loaded notifications, newest first.
 * @param modifier the modifier of the column.
 * @param unreadCount the unread total from the server, which may exceed the loaded items. The unread loaded items by
 * default.
 * @param initialFilter the tab shown first. The component keeps the selection.
 * @param now the current time in epoch milliseconds, for the ages and the day headings. `null` reads the clock.
 * @param loading the first load: three placeholder rows, read as "Loading notifications".
 * @param errorMessage the first load failed: `MonarchErrorState` with this title replaces the list.
 * @param hasMore another page exists: `MonarchLoadMore` ends the list.
 * @param loadingMore a page is loading.
 * @param loadMoreFailed the last page failed; the load-more row offers a retry.
 * @param groupByDay groups the notifications under Today, Yesterday and dates, in the device's time zone.
 * @param markReadOnSelect calls [onMarkRead] when an unread notification is opened.
 * @param labels replacements for the built-in strings.
 * @param onFilterChange receives the tab, for apps that fetch each tab from the server.
 * @param onMarkRead marks one notification read; adds the swipe and TalkBack action on unread rows.
 * @param onMarkAllRead adds Mark all as read, disabled when nothing is unread.
 * @param onLoadMore loads the next page; also retries a failed page.
 * @param onRetry retries a failed first load.
 * @param onSelect a notification was opened. Navigate to what it is about.
 * @param footer content after the list, such as a link to notification settings.
 */
@Suppress("LongParameterList")
@Composable
public fun MonarchNotificationCenter(
    items: List<MonarchNotificationCenterItem>,
    modifier: Modifier = Modifier,
    unreadCount: Int? = null,
    initialFilter: MonarchNotificationCenterFilter = MonarchNotificationCenterFilter.All,
    now: Long? = null,
    loading: Boolean = false,
    errorMessage: String? = null,
    hasMore: Boolean = false,
    loadingMore: Boolean = false,
    loadMoreFailed: Boolean = false,
    groupByDay: Boolean = true,
    markReadOnSelect: Boolean = true,
    labels: MonarchNotificationCenterLabels = MonarchNotificationCenterLabels(),
    onFilterChange: ((MonarchNotificationCenterFilter) -> Unit)? = null,
    onMarkRead: ((String) -> Unit)? = null,
    onMarkAllRead: (() -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onSelect: ((MonarchNotificationCenterItem) -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchNotificationCenterTokens
    var filter by rememberSaveable { mutableStateOf(initialFilter) }
    val unread = tokens.unreadTotal(items, unreadCount)
    val visible = tokens.visible(items, filter)
    Column(modifier = modifier) {
        Header(unread, labels, onMarkAllRead)
        FilterTabs(filter, unread, labels) { next ->
            if (next != filter) {
                filter = next
                onFilterChange?.invoke(next)
            }
        }
        Box(modifier = Modifier.weight(1f, fill = false)) {
            when {
                loading -> MonarchLoadingState(
                    label = monarchString(labels.loading, R.string.monarch_notificationcenter_loading),
                    modifier = Modifier.padding(tokens.StatePadding),
                    rows = tokens.PLACEHOLDER_ROWS,
                )
                errorMessage != null -> MonarchErrorState(
                    title = errorMessage,
                    modifier = Modifier.padding(tokens.StatePadding),
                    size = MonarchEmptyStateSize.Sm,
                    onRetry = onRetry,
                )
                visible.isEmpty() && !hasMore -> EmptyList(filter, labels)
                else -> NotificationList(
                    visible = visible,
                    now = now ?: System.currentTimeMillis(),
                    fixedNow = now,
                    groupByDay = groupByDay,
                    labels = labels,
                    paging = NotificationCenterPaging(hasMore, loadingMore, loadMoreFailed, onLoadMore),
                    onOpen = { item ->
                        if (markReadOnSelect && !item.read) onMarkRead?.invoke(item.id)
                        onSelect?.invoke(item)
                    },
                    onMarkRead = onMarkRead,
                )
            }
        }
        footer?.invoke()
    }
}

/** The state of the last page. */
private class NotificationCenterPaging(
    val hasMore: Boolean,
    val loadingMore: Boolean,
    val failed: Boolean,
    val onLoadMore: (() -> Unit)?,
) {
    val shown: Boolean get() = onLoadMore != null && (hasMore || loadingMore || failed)
}

@Composable
private fun Header(unread: Int, labels: MonarchNotificationCenterLabels, onMarkAllRead: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MonarchNotificationCenterTokens.HeaderPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchNotificationCenterTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(
            text = monarchString(labels.title, R.string.monarch_notificationcenter_title),
            modifier = Modifier.weight(1f),
            style = MonarchTextStyle.BodyStrong,
            heading = true,
        )
        if (onMarkAllRead != null) {
            MonarchButton(
                text = monarchString(labels.markAllRead, R.string.monarch_notificationcenter_mark_all_read),
                onClick = onMarkAllRead,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                leadingIcon = MonarchDefaultIcons.Check,
                enabled = unread > 0,
            )
        }
    }
}

@Composable
private fun FilterTabs(
    filter: MonarchNotificationCenterFilter,
    unread: Int,
    labels: MonarchNotificationCenterLabels,
    onFilter: (MonarchNotificationCenterFilter) -> Unit,
) {
    val unreadText = monarchString(labels.unread, R.string.monarch_notificationcenter_unread)
    val unreadName = labels.unreadTab?.invoke(unread)
        ?: pluralStringResource(R.plurals.monarch_notificationcenter_unread_tab, unread, unread)
    val allText = monarchString(labels.all, R.string.monarch_notificationcenter_all)
    MonarchTabs(
        selectedIndex = filter.ordinal,
        onSelectedIndexChange = { onFilter(MonarchNotificationCenterFilter.entries[it]) },
    ) {
        tab(allText)
        tab(
            text = unreadText,
            count = if (unread > 0) MonarchNotificationCenterTokens.countText(unread) else null,
            countTone = MonarchTone.Accent,
            contentDescription = if (unread > 0) unreadName else null,
        )
    }
}

@Composable
private fun EmptyList(filter: MonarchNotificationCenterFilter, labels: MonarchNotificationCenterLabels) {
    val unreadTab = filter == MonarchNotificationCenterFilter.Unread
    MonarchEmptyState(
        title = if (unreadTab) {
            monarchString(labels.emptyUnreadTitle, R.string.monarch_notificationcenter_empty_unread_title)
        } else {
            monarchString(labels.emptyTitle, R.string.monarch_notificationcenter_empty_title)
        },
        modifier = Modifier.padding(MonarchNotificationCenterTokens.StatePadding),
        kind = MonarchEmptyStateKind.Success,
        size = MonarchEmptyStateSize.Sm,
        message = monarchString(labels.emptyMessage, R.string.monarch_notificationcenter_empty_message),
    )
}

@Suppress("LongParameterList")
@Composable
private fun NotificationList(
    visible: List<MonarchNotificationCenterItem>,
    now: Long,
    fixedNow: Long?,
    groupByDay: Boolean,
    labels: MonarchNotificationCenterLabels,
    paging: NotificationCenterPaging,
    onOpen: (MonarchNotificationCenterItem) -> Unit,
    onMarkRead: ((String) -> Unit)?,
) {
    val tokens = MonarchNotificationCenterTokens
    val locale = LocalConfiguration.current.locales[0]
    val timeZone = currentIcuTimeZone()
    val today = monarchString(labels.today, R.string.monarch_notificationcenter_today)
    val yesterday = monarchString(labels.yesterday, R.string.monarch_notificationcenter_yesterday)
    val unreadItem = monarchString(labels.unreadItem, R.string.monarch_notificationcenter_unread_item)
    val markRead = monarchString(labels.markRead, R.string.monarch_notificationcenter_mark_read)
    val row: LazyListScope.(List<MonarchNotificationCenterItem>) -> Unit = { group ->
        items(group, key = { it.id }) { item ->
            NotificationRow(item, fixedNow, unreadItem, markRead, onOpen, onMarkRead)
        }
    }
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        if (groupByDay) {
            tokens.groupByDay(visible, timeZone).forEach { day ->
                item(key = "day-${day.day}-${day.items.first().id}") {
                    val heading = when (tokens.dayName(day.day, now, timeZone)) {
                        NotificationCenterDayName.Today -> today
                        NotificationCenterDayName.Yesterday -> yesterday
                        NotificationCenterDayName.Date ->
                            tokens.dateHeading(day.items.first().time, now, locale, timeZone)
                    }
                    MonarchText(
                        text = heading,
                        modifier = Modifier.padding(tokens.HeadingPadding),
                        style = MonarchTextStyle.Caption,
                        color = MonarchTheme.colors.textMuted,
                        heading = true,
                    )
                }
                row(day.items)
            }
        } else {
            row(visible)
        }
        if (paging.shown) {
            item(key = "load-more") {
                val load = paging.onLoadMore ?: {}
                MonarchLoadMore(
                    hasMore = paging.hasMore,
                    onLoadMore = load,
                    modifier = Modifier.fillMaxWidth(),
                    loading = paging.loadingMore,
                    failed = paging.failed,
                    showEnd = false,
                    onRetry = load,
                    labels = labels.loadMore,
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: MonarchNotificationCenterItem,
    now: Long?,
    unreadText: String,
    markReadText: String,
    onOpen: (MonarchNotificationCenterItem) -> Unit,
    onMarkRead: ((String) -> Unit)?,
) {
    val colors = MonarchTheme.colors
    val clock = rememberMonarchClock(now)
    val locale = LocalConfiguration.current.locales[0]
    val age = MonarchRelativeTime.format(item.time, clock, locale, future = false)
    val unread = !item.read
    val unreadName = unreadText
    val titleStyle = MonarchTheme.typography.label.copy(
        fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
    )
    val caption = MonarchTheme.typography.caption
    // As on the web: the avatar or the source icon, the title wrapping in full with the detail and the age under it,
    // then the unread dot and a check that marks the notification read.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onOpen(item) })
            .padding(horizontal = MonarchTheme.spacing.space4, vertical = MonarchTheme.spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.semantics(mergeDescendants = true) { }) {
            if (item.actorName != null) {
                MonarchAvatar(
                    image = item.actorImage,
                    name = item.actorName,
                    size = MonarchAvatarSize.Sm,
                    decorative = true,
                )
            } else if (item.icon != null) {
                Box(
                    modifier = Modifier
                        .size(MonarchTheme.spacing.space8)
                        .background(colors.bgMuted, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides colors.textMuted) { item.icon.invoke() }
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    // TalkBack offers Mark as read as an action of an unread row, besides the check button.
                    if (unread && onMarkRead != null) {
                        customActions = listOf(
                            CustomAccessibilityAction(markReadText) {
                                onMarkRead(item.id)
                                true
                            },
                        )
                    }
                    contentDescription = listOfNotNull(
                        item.title,
                        item.message,
                        age,
                        unreadName.takeIf { unread },
                    ).joinToString(", ")
                },
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space0_5),
        ) {
            Text(text = item.title, style = titleStyle, color = colors.textDefault)
            item.message?.let { Text(text = it, style = caption, color = colors.textMuted) }
            Text(text = age, style = caption, color = colors.textMuted)
        }
        if (unread) {
            Box(
                modifier = Modifier
                    .padding(top = MonarchTheme.spacing.space2)
                    .size(MonarchTheme.spacing.space2)
                    .background(colors.accentSolid, CircleShape)
                    .clearAndSetSemantics { },
            )
        }
        if (unread && onMarkRead != null) {
            MonarchIconButton(
                label = markReadText,
                onClick = { onMarkRead(item.id) },
                size = MonarchButtonSize.Sm,
            ) { MonarchDefaultIcons.Check() }
        }
    }
}
