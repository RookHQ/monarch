package dev.monarchui.components.collaboration.commentthread

import androidx.compose.runtime.Immutable
import dev.monarchui.components.collaboration.messagerow.MonarchMessageRowLabels

/**
 * Replacements for the built-in strings of a `MonarchCommentThread`. Every
 * `null` keeps the built-in string `monarch_commentthread_<key>`.
 *
 * @property label the TalkBack name of the thread, given the first author: "Thread started by Ana Ruiz".
 * @property quote what TalkBack reads for the quote, given its text: "Quoted text: …".
 * @property showEarlier the button that shows folded replies, given their number: "Show 3 earlier replies".
 * @property reply the TalkBack name of the reply field: "Reply".
 * @property replyPlaceholder shown in the empty reply field: "Reply…".
 * @property send the send button of the reply field: "Send reply".
 * @property resolve the button that resolves the thread.
 * @property reopen the button that reopens a resolved thread.
 * @property resolved the badge of a resolved thread.
 * @property resolvedHint shown instead of the reply field while resolved.
 * @property resolvedNotice announced after resolving.
 * @property reopenedNotice announced after reopening.
 * @property sentNotice announced after a reply is posted.
 * @property actionFailed shown when replying, resolving, reopening or deleting fails.
 * @property deleteComment the menu action and TalkBack action that deletes a comment.
 * @property deleteTitle the title of the delete confirmation.
 * @property deleteDescription the text of the delete confirmation.
 * @property deleteConfirm the confirm button of the delete confirmation.
 * @property cancel the cancel button of the delete confirmation.
 * @property messageRow the strings of the comments, which are `MonarchMessageRow`s.
 */
@Immutable
public class MonarchCommentThreadLabels(
    public val label: ((author: String) -> String)? = null,
    public val quote: ((quote: String) -> String)? = null,
    public val showEarlier: ((count: Int) -> String)? = null,
    public val reply: String? = null,
    public val replyPlaceholder: String? = null,
    public val send: String? = null,
    public val resolve: String? = null,
    public val reopen: String? = null,
    public val resolved: String? = null,
    public val resolvedHint: String? = null,
    public val resolvedNotice: String? = null,
    public val reopenedNotice: String? = null,
    public val sentNotice: String? = null,
    public val actionFailed: String? = null,
    public val deleteComment: String? = null,
    public val deleteTitle: String? = null,
    public val deleteDescription: String? = null,
    public val deleteConfirm: String? = null,
    public val cancel: String? = null,
    public val messageRow: MonarchMessageRowLabels = MonarchMessageRowLabels(),
)
