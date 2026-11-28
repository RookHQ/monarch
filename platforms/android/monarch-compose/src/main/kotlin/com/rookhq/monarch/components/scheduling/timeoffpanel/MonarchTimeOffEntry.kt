package com.rookhq.monarch.components.scheduling.timeoffpanel

import androidx.compose.runtime.Immutable

/**
 * One period of time off in a `MonarchTimeOffPanel`.
 *
 * @property id identifies the entry.
 * @property personId whose time off, or `null` for a closure that applies to everyone.
 * @property personName the name shown for the person.
 * @property allDay `true`: [start] and [end] are days as their UTC midnight, the end included. `false`: instants
 * in epoch milliseconds.
 * @property start the first day, or the start instant.
 * @property end the last day (included), or the end instant.
 * @property type the kind, for example "Annual leave", shown as a badge.
 * @property reason a note in the person's own words, for example "Conference".
 * @property status a state shown as a badge, for example "Pending".
 */
@Immutable
public data class MonarchTimeOffEntry(
    public val id: String,
    public val personId: String?,
    public val allDay: Boolean,
    public val start: Long,
    public val end: Long,
    public val personName: String? = null,
    public val type: String? = null,
    public val reason: String? = null,
    public val status: String? = null,
)
