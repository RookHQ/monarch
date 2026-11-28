package dev.monarchui.components.collaboration.commentspanel

import androidx.compose.runtime.Immutable
import dev.monarchui.components.collaboration.commentthread.MonarchCommentThreadComment

/**
 * One discussion of a `MonarchCommentsPanel`, anchored to a passage of the document or to the whole record.
 *
 * @property id a stable, unique id, passed back to the panel's callbacks.
 * @property comments the comments, oldest first. The first one starts the thread and names it in the list.
 * @property quote the passage the thread is about, shown as a quote. `null` for a general comment.
 * @property updatedAt the last activity in epoch milliseconds (UTC), shown in the list. `null` uses the newest
 * comment's time.
 * @property resolved the thread is resolved: it is listed under Resolved and takes no replies until reopened.
 * @property canResolve shows Resolve or Reopen for this thread when the panel has `onResolvedChange`.
 * @property canReply shows the reply field for this thread when the panel has `onReply`.
 */
@Immutable
public class MonarchCommentsPanelThread(
    public val id: String,
    public val comments: List<MonarchCommentThreadComment>,
    public val quote: String? = null,
    public val updatedAt: Long? = null,
    public val resolved: Boolean = false,
    public val canResolve: Boolean = true,
    public val canReply: Boolean = true,
)
