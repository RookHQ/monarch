package dev.monarchui.components.scheduling.shiftroster

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.internal.monarchString

/** The built-in strings of MonarchShiftRoster, resolved once with the app's replacements. */
@Suppress("LongParameterList")
internal class ShiftRosterText(
    val roster: String,
    val person: String,
    val day: String,
    val off: String,
    val unassigned: String,
    val clear: String,
    val overnight: String,
    val hours: String,
    val working: String,
    val today: String,
    val menuTitle: (String, String) -> String,
    val cell: (String, String, String) -> String,
    val range: (String, String) -> String,
    val hoursValue: (String) -> String,
    val workingValue: (Int) -> String,
)

/** Resolves [labels] against the built-in strings. */
@Composable
internal fun shiftRosterText(labels: MonarchShiftRosterLabels): ShiftRosterText {
    val menuTitle = stringResource(R.string.monarch_shiftroster_menu_title)
    val cell = stringResource(R.string.monarch_shiftroster_cell)
    val range = stringResource(R.string.monarch_shiftroster_range)
    val hoursValue = stringResource(R.string.monarch_shiftroster_hours_value)
    val workingOne = pluralStringResource(R.plurals.monarch_shiftroster_working_value, 1)
    val workingOther = pluralStringResource(R.plurals.monarch_shiftroster_working_value, 2)
    return ShiftRosterText(
        roster = monarchString(labels.roster, R.string.monarch_shiftroster_roster),
        person = monarchString(labels.person, R.string.monarch_shiftroster_person),
        day = monarchString(labels.day, R.string.monarch_shiftroster_day),
        off = monarchString(labels.off, R.string.monarch_shiftroster_off),
        unassigned = monarchString(labels.unassigned, R.string.monarch_shiftroster_unassigned),
        clear = monarchString(labels.clear, R.string.monarch_shiftroster_clear),
        overnight = monarchString(labels.overnight, R.string.monarch_shiftroster_overnight),
        hours = monarchString(labels.hours, R.string.monarch_shiftroster_hours),
        working = monarchString(labels.working, R.string.monarch_shiftroster_working),
        today = monarchString(labels.today, R.string.monarch_shiftroster_today),
        menuTitle = labels.menuTitle ?: { name, day -> menuTitle.format(name, day) },
        cell = labels.cell ?: { name, day, assignment -> cell.format(name, day, assignment) },
        range = labels.range ?: { start, end -> range.format(start, end) },
        hoursValue = labels.hoursValue ?: { hours -> hoursValue.format(hours) },
        workingValue = labels.workingValue ?: { count -> (if (count == 1) workingOne else workingOther).format(count) },
    )
}

/** Everything the layouts of MonarchShiftRoster share: the data, the state flags, the strings and the formats. */
@Suppress("LongParameterList")
internal class ShiftRosterModel(
    val members: List<MonarchShiftRosterMember>,
    val shifts: List<MonarchShiftRosterShift>,
    val range: List<Long>,
    val value: ShiftRosterValue,
    val onValueChange: (ShiftRosterValue, MonarchShiftRosterChange) -> Unit,
    val enabled: Boolean,
    val readOnly: Boolean,
    val showTotals: Boolean,
    val today: Long?,
    val onMemberClick: ((MonarchShiftRosterMember) -> Unit)?,
    val text: ShiftRosterText,
    val formats: ShiftRosterFormats,
) {
    private val shiftsById = shifts.associateBy { it.id }

    fun assignment(member: MonarchShiftRosterMember, day: Long): MonarchShiftRosterAssignment? =
        ShiftRosterRules.assignmentOf(value, member.id, day)

    fun shiftOf(assignment: MonarchShiftRosterAssignment?): MonarchShiftRosterShift? =
        ShiftRosterRules.shiftOf(assignment, shiftsById)

    /** Sets or clears one cell and reports the change. Does nothing when read only or disabled. */
    fun assign(member: MonarchShiftRosterMember, day: Long, assignment: MonarchShiftRosterAssignment?) {
        if (readOnly || !enabled) return
        val next = ShiftRosterRules.assign(value, member.id, day, assignment)
        onValueChange(next, MonarchShiftRosterChange(member.id, day, assignment))
    }

    /** "37.5 h". */
    fun hours(member: MonarchShiftRosterMember): String =
        text.hoursValue(formats.hours(ShiftRosterRules.hours(value, member.id, range, shiftsById)))

    fun working(day: Long): Int = ShiftRosterRules.working(value, members, day, shiftsById)

    /** "6:00 AM – 2:00 PM", as shown. */
    fun visibleRange(shift: MonarchShiftRosterShift): String =
        "${formats.time(shift.start)} – ${formats.time(shift.end)}"

    /** "Night, 10:00 PM to 6:00 AM, overnight", as TalkBack reads a shift. [short] adds the short name shown. */
    fun spokenShift(shift: MonarchShiftRosterShift, short: Boolean = false): String {
        val shortName = shift.shortName?.takeIf { short && it != shift.name }
        return listOfNotNull(
            shift.name,
            shortName,
            text.range(formats.time(shift.start), formats.time(shift.end)),
            text.overnight.takeIf { ShiftRosterRules.overnight(shift) },
        ).joinToString(", ")
    }

    /** What a cell's assignment reads as. */
    fun describe(assignment: MonarchShiftRosterAssignment?, short: Boolean): String {
        val shift = shiftOf(assignment)
        return when {
            shift != null -> spokenShift(shift, short)
            assignment == MonarchShiftRosterAssignment.Off -> text.off
            else -> text.unassigned
        }
    }

    /** "Wednesday, November 4", with ", Today" for today. */
    fun spokenDay(day: Long): String =
        if (day == today) "${formats.fullDay(day)}, ${text.today}" else formats.fullDay(day)
}
