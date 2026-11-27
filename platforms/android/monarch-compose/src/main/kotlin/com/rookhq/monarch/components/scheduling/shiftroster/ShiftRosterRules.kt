package com.rookhq.monarch.components.scheduling.shiftroster

import java.util.TimeZone

/** A roster value: assignments by member id, then by day key. */
internal typealias ShiftRosterValue = Map<String, Map<Long, MonarchShiftRosterAssignment>>

/**
 * The pure rules of MonarchShiftRoster. A day is a "day key": the epoch
 * millisecond of a date's midnight in UTC, as in the Material 3
 * `DatePickerState`. `java.time` needs API 26, so this is plain arithmetic.
 */
internal object ShiftRosterRules {
    const val DAY_MILLIS: Long = 86_400_000L
    const val MINUTES_PER_DAY: Int = 1440
    private const val MINUTES_PER_HOUR: Double = 60.0

    /** The most days a roster shows, about two months, so a wrong count does not build thousands of cells. */
    const val MAX_DAYS: Int = 62

    /** The day key of the UTC date that contains [millis]. */
    fun dayKey(millis: Long): Long = Math.floorDiv(millis, DAY_MILLIS) * DAY_MILLIS

    /** The day key of the date that contains [millis] in [zone]. */
    fun localDay(millis: Long, zone: TimeZone): Long = dayKey(millis + zone.getOffset(millis))

    /** The days shown: [days] days (at least one, at most [MAX_DAYS]) from the date of [from]. */
    fun range(from: Long, days: Int): List<Long> {
        val first = dayKey(from)
        return List(days.coerceIn(1, MAX_DAYS)) { first + it * DAY_MILLIS }
    }

    /** The assignment of [memberId] on [day], or `null` when the day is not assigned. Keys may be any time. */
    fun assignmentOf(value: ShiftRosterValue, memberId: String, day: Long): MonarchShiftRosterAssignment? =
        value[memberId]?.entries?.lastOrNull { dayKey(it.key) == day }?.value

    /** [value] with [assignment] on [day] for [memberId], or with the day removed when [assignment] is `null`. */
    fun assign(
        value: ShiftRosterValue,
        memberId: String,
        day: Long,
        assignment: MonarchShiftRosterAssignment?,
    ): ShiftRosterValue {
        val row = value[memberId].orEmpty().filterKeys { dayKey(it) != day }
        val next = if (assignment == null) row else row + (day to assignment)
        return value + (memberId to next)
    }

    /** The shift an assignment refers to, or `null` for a day off, no assignment or an unknown id. */
    fun shiftOf(
        assignment: MonarchShiftRosterAssignment?,
        shifts: Map<String, MonarchShiftRosterShift>,
    ): MonarchShiftRosterShift? = (assignment as? MonarchShiftRosterAssignment.Shift)?.let { shifts[it.shiftId] }

    /** Whether [shift] ends the next day: its end is at or before its start. */
    fun overnight(shift: MonarchShiftRosterShift): Boolean =
        shift.end.mod(MINUTES_PER_DAY) <= shift.start.mod(MINUTES_PER_DAY)

    /** The length of [shift] in minutes. A shift that ends when it starts lasts a whole day. */
    fun durationMinutes(shift: MonarchShiftRosterShift): Int {
        val start = shift.start.mod(MINUTES_PER_DAY)
        val end = shift.end.mod(MINUTES_PER_DAY)
        return if (end > start) end - start else end + MINUTES_PER_DAY - start
    }

    /** The hours [memberId] works on the days of [range]. */
    fun hours(
        value: ShiftRosterValue,
        memberId: String,
        range: List<Long>,
        shifts: Map<String, MonarchShiftRosterShift>,
    ): Double = range.sumOf { day ->
        shiftOf(assignmentOf(value, memberId, day), shifts)?.let(::durationMinutes) ?: 0
    } / MINUTES_PER_HOUR

    /** How many of [members] work a known shift on [day]. */
    fun working(
        value: ShiftRosterValue,
        members: List<MonarchShiftRosterMember>,
        day: Long,
        shifts: Map<String, MonarchShiftRosterShift>,
    ): Int = members.count { shiftOf(assignmentOf(value, it.id, day), shifts) != null }
}
