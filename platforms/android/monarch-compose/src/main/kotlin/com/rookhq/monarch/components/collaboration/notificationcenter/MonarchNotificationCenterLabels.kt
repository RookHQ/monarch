package com.rookhq.monarch.components.collaboration.notificationcenter

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMoreLabels

/**
 * Replacements for the built-in strings of a `MonarchNotificationCenter`.
 * Every `null` keeps the built-in string `monarch_notificationcenter_<key>`.
 *
 * @property title the heading: "Notifications".
 * @property all the first tab: "All".
 * @property unread the second tab: "Unread".
 * @property unreadTab what TalkBack reads for the second tab, given the unread total: "Unread, 3 unread".
 * @property markAllRead the button that marks everything read: "Mark all as read".
 * @property markRead the swipe and TalkBack action that marks one notification read: "Mark as read".
 * @property unreadItem read after the title of an unread notification: "Unread".
 * @property today the heading of today's notifications.
 * @property yesterday the heading of yesterday's notifications.
 * @property emptyTitle shown when there is nothing: "You're all caught up".
 * @property emptyMessage the line under [emptyTitle].
 * @property emptyUnreadTitle shown in the Unread tab when nothing is unread: "No unread notifications".
 * @property loading what TalkBack reads for the first load: "Loading notifications".
 * @property loadMore the strings of the load-more row.
 */
@Immutable
public class MonarchNotificationCenterLabels(
    public val title: String? = null,
    public val all: String? = null,
    public val unread: String? = null,
    public val unreadTab: ((Int) -> String)? = null,
    public val markAllRead: String? = null,
    public val markRead: String? = null,
    public val unreadItem: String? = null,
    public val today: String? = null,
    public val yesterday: String? = null,
    public val emptyTitle: String? = null,
    public val emptyMessage: String? = null,
    public val emptyUnreadTitle: String? = null,
    public val loading: String? = null,
    public val loadMore: MonarchLoadMoreLabels = MonarchLoadMoreLabels(),
)
