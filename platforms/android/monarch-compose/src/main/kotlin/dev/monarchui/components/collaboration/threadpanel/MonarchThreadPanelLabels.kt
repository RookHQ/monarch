package dev.monarchui.components.collaboration.threadpanel

import androidx.compose.runtime.Immutable
import dev.monarchui.components.collaboration.virtualmessagelist.MonarchVirtualMessageListLabels

/**
 * Replacements for the built-in strings of a `MonarchThreadPanel`. Every `null` keeps the built-in string
 * `monarch_threadpanel_<key>`.
 *
 * @property title the heading of the panel: "Thread".
 * @property close the button that closes the panel.
 * @property follow the follow chip while not following.
 * @property following the follow chip while following.
 * @property followedNotice announced after following.
 * @property unfollowedNotice announced after unfollowing.
 * @property followFailed announced when following or unfollowing fails.
 * @property replyCount the divider between the root message and the replies: "3 replies".
 * @property replies the TalkBack name of the list of replies.
 * @property loading what TalkBack reads for the placeholders while the thread loads.
 * @property error shown when the thread could not be loaded.
 * @property deleted shown when the thread was deleted.
 * @property list the strings of the list of replies, a `MonarchVirtualMessageList`. `null` keeps its built-in
 * strings and names the list with [replies].
 */
@Immutable
public class MonarchThreadPanelLabels(
    public val title: String? = null,
    public val close: String? = null,
    public val follow: String? = null,
    public val following: String? = null,
    public val followedNotice: String? = null,
    public val unfollowedNotice: String? = null,
    public val followFailed: String? = null,
    public val replyCount: ((count: Int) -> String)? = null,
    public val replies: String? = null,
    public val loading: String? = null,
    public val error: String? = null,
    public val deleted: String? = null,
    public val list: MonarchVirtualMessageListLabels? = null,
)
