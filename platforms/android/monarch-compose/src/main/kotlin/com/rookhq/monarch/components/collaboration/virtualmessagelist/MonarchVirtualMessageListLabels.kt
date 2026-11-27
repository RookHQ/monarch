package com.rookhq.monarch.components.collaboration.virtualmessagelist

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchVirtualMessageList`.
 * Every `null` keeps the built-in string `monarch_virtualmessagelist_<key>`.
 *
 * @property label the TalkBack name of the list: "Messages".
 * @property newMessages the divider above the first unread message: "New messages".
 * @property today the day separator of today.
 * @property yesterday the day separator of yesterday.
 * @property jumpToLatest the button that scrolls to the newest message.
 * @property unseen the same button while new messages arrived below, given their number: "3 new messages".
 * @property arrived announced when messages arrive, given their number: "1 new message".
 * @property loadOlder the button that loads older messages.
 * @property loadingOlder shown while older messages load.
 * @property olderFailed shown when loading older messages failed.
 * @property retryOlder the button that retries loading older messages.
 * @property loading the TalkBack name of the placeholder of the first load.
 */
@Immutable
public class MonarchVirtualMessageListLabels(
    public val label: String? = null,
    public val newMessages: String? = null,
    public val today: String? = null,
    public val yesterday: String? = null,
    public val jumpToLatest: String? = null,
    public val unseen: ((count: Int) -> String)? = null,
    public val arrived: ((count: Int) -> String)? = null,
    public val loadOlder: String? = null,
    public val loadingOlder: String? = null,
    public val olderFailed: String? = null,
    public val retryOlder: String? = null,
    public val loading: String? = null,
)
