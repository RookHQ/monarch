package dev.monarchui.components.scheduling.shiftroster

import androidx.compose.runtime.Immutable

/**
 * The one change a person made in a `MonarchShiftRoster`, passed with the new value so the app can save it.
 *
 * @property memberId the `id` of the person.
 * @property day the day, as a day key: the epoch millisecond of its midnight in UTC.
 * @property assignment the new assignment, or `null` when it was cleared.
 */
@Immutable
public class MonarchShiftRosterChange(
    public val memberId: String,
    public val day: Long,
    public val assignment: MonarchShiftRosterAssignment?,
) {
    override fun equals(other: Any?): Boolean =
        other is MonarchShiftRosterChange && other.memberId == memberId && other.day == day &&
            other.assignment == assignment

    override fun hashCode(): Int = (memberId.hashCode() * HASH + day.hashCode()) * HASH + assignment.hashCode()

    override fun toString(): String = "MonarchShiftRosterChange($memberId, $day, $assignment)"

    private companion object {
        const val HASH = 31
    }
}
