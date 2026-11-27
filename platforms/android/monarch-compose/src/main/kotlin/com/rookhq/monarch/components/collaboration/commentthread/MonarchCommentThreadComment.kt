package com.rookhq.monarch.components.collaboration.commentthread

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One comment of a `MonarchCommentThread`.
 *
 * @property id a stable, unique id, passed back to `onDeleteComment`.
 * @property author the author's display name.
 * @property body the comment text, shown as given.
 * @property createdAt when the comment was written, in epoch milliseconds (UTC).
 * @property authorImage the author's avatar picture, loaded by the app. `null` shows initials.
 * @property authorSeed a stable id for the avatar colour. The name by default.
 * @property canDelete the person may delete this comment: Delete is offered when the thread has `onDeleteComment`.
 */
@Immutable
public class MonarchCommentThreadComment(
    public val id: String,
    public val author: String,
    public val body: String,
    public val createdAt: Long,
    public val authorImage: Painter? = null,
    public val authorSeed: String? = null,
    public val canDelete: Boolean = false,
)
