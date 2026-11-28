@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.scheduling.calendartoolbar.MonarchCalendarToolbar
import dev.monarchui.components.scheduling.calendartoolbar.MonarchCalendarToolbarView
import dev.monarchui.components.scheduling.shiftroster.MonarchShiftRoster
import dev.monarchui.components.scheduling.shiftroster.MonarchShiftRosterAssignment
import dev.monarchui.components.scheduling.shiftroster.MonarchShiftRosterLayout
import dev.monarchui.components.scheduling.shiftroster.MonarchShiftRosterMember
import dev.monarchui.components.scheduling.shiftroster.MonarchShiftRosterShift
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** Monday 2 November 2026, 00:00 UTC. */
private const val RosterMonday = 1_793_577_600_000L
private const val RosterDay = 86_400_000L
private const val RosterWeekDays = 7

private val RosterShifts = listOf(
    MonarchShiftRosterShift("m", "Morning", start = 6 * 60, end = 14 * 60, shortName = "M"),
    MonarchShiftRosterShift("e", "Evening", start = 14 * 60, end = 22 * 60, shortName = "E"),
    MonarchShiftRosterShift("n", "Night", start = 22 * 60, end = 6 * 60, shortName = "N"),
)

private val RosterMembers = listOf(
    MonarchShiftRosterMember("a", "Alex Kim", description = "Support"),
    MonarchShiftRosterMember("b", "Sam Lee", description = "Support"),
    MonarchShiftRosterMember("c", "Priya Nair", description = "Operations"),
)

private val RosterStart = mapOf(
    "a" to mapOf(
        RosterMonday to MonarchShiftRosterAssignment.Shift("m"),
        RosterMonday + RosterDay to MonarchShiftRosterAssignment.Shift("m"),
        RosterMonday + 2 * RosterDay to MonarchShiftRosterAssignment.Off,
    ),
    "b" to mapOf(
        RosterMonday to MonarchShiftRosterAssignment.Shift("e"),
        RosterMonday + RosterDay to MonarchShiftRosterAssignment.Shift("n"),
    ),
    "c" to mapOf(RosterMonday to MonarchShiftRosterAssignment.Off),
)

/** The catalogue page of ShiftRoster. */
object MonarchShiftRosterSection : CataloguePage {
    override val title = "ShiftRoster"

    @Composable
    override fun Content() {
        var value by remember { mutableStateOf(RosterStart) }
        var week by rememberSaveable { mutableIntStateOf(0) }
        var layout by rememberSaveable { mutableStateOf(MonarchShiftRosterLayout.Automatic) }
        val from = RosterMonday + week * RosterWeekDays * RosterDay
        CatalogueCaption("Editable week, with a calendar toolbar")
        MonarchCalendarToolbar(
            title = if (week == 0) "This week" else "Week ${week + 1}",
            view = MonarchCalendarToolbarView.Week,
            onToday = { week = 0 },
            onPrevious = { week -= 1 },
            onNext = { week += 1 },
            previousIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextIcon = {
                MonarchIcon(
                    SampleIcons.ChevronRight,
                    contentDescription = null,
                )
            },
            todayEnabled = week != 0,
            actions = {
                MonarchButton(
                    text = layout.name,
                    onClick = {
                        val all = MonarchShiftRosterLayout.entries
                        layout = all[(layout.ordinal + 1) % all.size]
                    },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            },
        )
        MonarchShiftRoster(
            members = RosterMembers,
            shifts = RosterShifts,
            from = from,
            value = value,
            onValueChange = { next, _ -> value = next },
            layout = layout,
            today = RosterMonday,
        )
        CatalogueCaption("Read only, one person at a time")
        MonarchShiftRoster(
            members = RosterMembers,
            shifts = RosterShifts,
            from = RosterMonday,
            value = RosterStart,
            onValueChange = { _, _ -> },
            days = 3,
            layout = MonarchShiftRosterLayout.ByPerson,
            readOnly = true,
            today = RosterMonday,
        )
        CatalogueCaption("Loading, error and empty")
        MonarchShiftRoster(
            members = RosterMembers,
            shifts = RosterShifts,
            from = RosterMonday,
            value = emptyMap(),
            onValueChange = { _, _ -> },
            loading = true,
        )
        MonarchShiftRoster(
            members = RosterMembers,
            shifts = RosterShifts,
            from = RosterMonday,
            value = emptyMap(),
            onValueChange = { _, _ -> },
            error = "The roster could not be loaded.",
        )
        MonarchShiftRoster(
            members = emptyList(),
            shifts = RosterShifts,
            from = RosterMonday,
            value = emptyMap(),
            onValueChange = { _, _ -> },
        )
    }
}
