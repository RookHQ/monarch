package dev.monarchui.components.files.versionhistorypanel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One saved version in a `MonarchVersionHistoryPanel`.
 *
 * @property id a stable id: the panel's selection and the argument of `onRestore`.
 * @property date when it was saved, in epoch milliseconds (UTC).
 * @property authorName who saved it.
 * @property authorImage the author's picture, loaded by the app; `null` shows the initials.
 * @property note the name someone gave this version.
 * @property automatic saved automatically rather than by a person.
 */
@Immutable
public data class MonarchVersionHistoryPanelVersion(
    public val id: String,
    public val date: Long,
    public val authorName: String,
    public val authorImage: Painter? = null,
    public val note: String? = null,
    public val automatic: Boolean = false,
)
