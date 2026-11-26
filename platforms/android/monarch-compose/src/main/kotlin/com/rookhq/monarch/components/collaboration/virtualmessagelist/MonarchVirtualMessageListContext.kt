package com.rookhq.monarch.components.collaboration.virtualmessagelist

import androidx.compose.runtime.Immutable

/**
 * What the row of a `MonarchVirtualMessageList` receives next to its item.
 *
 * @property grouped the message continues the previous one's group: same author, same day, within the group
 * window, and no "New messages" divider between them. Draw a compact row, such as `MonarchMessageRow` with
 * `grouped = true`.
 * @property index the item's index in the list's items, oldest first.
 * @property previous the item before, if any.
 */
@Immutable
public class MonarchVirtualMessageListContext<out T>(
    public val grouped: Boolean,
    public val index: Int,
    public val previous: T?,
)
