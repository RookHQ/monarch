package com.rookhq.monarch.components.collaboration.inboxrow

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchInboxRow`. Every `null`
 * keeps the built-in string `monarch_inboxrow_<key>`.
 *
 * @property unread read after the title of an unread row, given the count or `null`: "Unread", "3 unread".
 * @property age the compact age, given the unit and the number: "5m". Older than 30 days shows a short date.
 * @property select what a long press does when the row can be selected: "Select".
 * @property markRead the swipe and TalkBack action of an unread row: "Read".
 * @property markUnread the same action on a read row: "Unread".
 * @property archive the archive action.
 * @property delete the delete action.
 */
@Immutable
public class MonarchInboxRowLabels(
    public val unread: ((Int?) -> String)? = null,
    public val age: ((MonarchInboxRowAgeUnit, Int) -> String)? = null,
    public val select: String? = null,
    public val markRead: String? = null,
    public val markUnread: String? = null,
    public val archive: String? = null,
    public val delete: String? = null,
)
