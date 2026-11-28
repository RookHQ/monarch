package dev.monarchui.components.scheduling.timeoffpanel

import androidx.compose.runtime.Immutable

/**
 * What the add sheet of a `MonarchTimeOffPanel` produces.
 *
 * @property personId the chosen person, or `null` for a closure.
 * @property allDay whether [start] and [end] are days (UTC midnights, the end included) or instants.
 * @property start the first day, or the start instant.
 * @property end the last day (included), or the end instant.
 * @property type the chosen type's value, when types are offered.
 * @property reason the trimmed reason, when given.
 */
@Immutable
public data class MonarchTimeOffDraft(
    public val personId: String?,
    public val allDay: Boolean,
    public val start: Long,
    public val end: Long,
    public val type: String? = null,
    public val reason: String? = null,
)
