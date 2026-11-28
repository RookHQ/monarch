package dev.monarchui.components.scheduling.shiftroster

import androidx.compose.runtime.Immutable

/**
 * What one person does on one day in a `MonarchShiftRoster`: a shift or a day off. A day that is missing from
 * the value is not assigned.
 */
public sealed interface MonarchShiftRosterAssignment {
    /**
     * The person works the shift [shiftId].
     *
     * @property shiftId the `id` of a `MonarchShiftRosterShift`. An id that is not among the roster's shifts
     * reads as not assigned.
     */
    @Immutable
    public class Shift(public val shiftId: String) : MonarchShiftRosterAssignment {
        override fun equals(other: Any?): Boolean = other is Shift && other.shiftId == shiftId

        override fun hashCode(): Int = shiftId.hashCode()

        override fun toString(): String = "Shift($shiftId)"
    }

    /** The person has the day off. */
    public data object Off : MonarchShiftRosterAssignment
}
