package com.rookhq.monarch.components.collaboration.commentspanel

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.collaboration.commentthread.MonarchCommentThreadLabels

/**
 * Replacements for the built-in strings of a `MonarchCommentsPanel`. Every `null` keeps the built-in string
 * `monarch_commentspanel_<key>`.
 *
 * @property title the heading of the panel: "Comments".
 * @property close the button that closes the panel.
 * @property open the filter segment of open threads.
 * @property resolved the filter segment of resolved threads.
 * @property back the button that returns from a thread to the list: "All comments".
 * @property loading what TalkBack reads for the placeholders while the threads load.
 * @property emptyOpen the title shown when there is no open thread.
 * @property emptyResolved the title shown when there is no resolved thread.
 * @property emptyHint the text under the empty title of the open threads.
 * @property commentCount the number of comments of a thread in the list: "2 comments".
 * @property quote the quote of a thread in the list, given its text: "“…”".
 * @property resolvedNotice announced after a thread is resolved.
 * @property reopenedNotice announced after a thread is reopened.
 * @property thread the strings of the open thread, a `MonarchCommentThread`.
 */
@Immutable
public class MonarchCommentsPanelLabels(
    public val title: String? = null,
    public val close: String? = null,
    public val open: String? = null,
    public val resolved: String? = null,
    public val back: String? = null,
    public val loading: String? = null,
    public val emptyOpen: String? = null,
    public val emptyResolved: String? = null,
    public val emptyHint: String? = null,
    public val commentCount: ((count: Int) -> String)? = null,
    public val quote: ((quote: String) -> String)? = null,
    public val resolvedNotice: String? = null,
    public val reopenedNotice: String? = null,
    public val thread: MonarchCommentThreadLabels = MonarchCommentThreadLabels(),
)
