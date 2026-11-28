package dev.monarchui.components.technical.logviewer

import androidx.compose.runtime.Immutable

/**
 * One line of a `MonarchLogViewer`.
 *
 * @property id a stable, unique id: the key of the line's row.
 * @property message the text of the line, shown as plain text.
 * @property level the severity.
 * @property time when it was written, in epoch milliseconds. Without it the line has no time column.
 * @property source where it came from (a service, pod or step), shown before the message.
 */
@Immutable
public data class MonarchLogLine(
    public val id: String,
    public val message: String,
    public val level: MonarchLogLevel = MonarchLogLevel.Info,
    public val time: Long? = null,
    public val source: String? = null,
)
