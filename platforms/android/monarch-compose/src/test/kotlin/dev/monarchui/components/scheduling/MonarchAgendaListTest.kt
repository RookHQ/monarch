package dev.monarchui.components.scheduling

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.scheduling.agendalist.AgendaListDays
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaEvent
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaList
import dev.monarchui.components.scheduling.agendalist.MonarchCalendarEvent
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaListLabels
import dev.monarchui.components.scheduling.agendalist.MonarchAgendaListTokens
import dev.monarchui.components.scheduling.eventchip.MonarchEventChipStatus
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

/** Wednesday 4 November 2026, 00:00 UTC. */
private const val Day0 = 1_793_750_400_000L
private const val Hour = 3_600_000L
private const val Minute = 60_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private val SampleEvents = listOf(
    MonarchAgendaEvent("standup", "Stand-up", Day0 + 9 * Hour + 30 * Minute, Day0 + 10 * Hour + 15 * Minute),
    MonarchAgendaEvent("offsite", "Team offsite", Day0, allDay = true),
    MonarchAgendaEvent(
        "review",
        "Design review",
        Day0 + 14 * Hour,
        Day0 + 15 * Hour,
        detail = "Room 4",
        status = MonarchEventChipStatus.Tentative,
    ),
)

internal object MonarchAgendaListFixtures : ComponentFixtures {
    override val id = "agenda-list"
    override val fixtures = listOf(
        fixture("days with today, as buttons") {
            MonarchAgendaList(
                events = SampleEvents,
                modifier = Modifier.height(400.dp),
                today = Day0,
                timeZone = Utc.id,
                onSelect = {},
            )
        },
        fixture("multi-day, notes, statuses, labels") {
            MonarchAgendaList(
                events = listOf(
                    MonarchAgendaEvent("late", "Release night", Day0 + 22 * Hour, Day0 + 2 * 24 * Hour + 2 * Hour),
                    MonarchAgendaEvent(
                        "cancelled",
                        "Retro",
                        Day0 + 16 * Hour,
                        status = MonarchEventChipStatus.Cancelled,
                    ),
                ),
                modifier = Modifier.height(400.dp),
                today = null,
                timeZone = Utc.id,
                dayNotes = mapOf(Day0 + Hour to listOf("Ana is out of office")),
            )
        },
        fixture("empty days shown") {
            MonarchAgendaList(
                events = SampleEvents.take(1),
                modifier = Modifier.height(400.dp),
                from = Day0,
                days = 3,
                showEmptyDays = true,
                today = Day0 + AgendaListDays.DAY_MILLIS,
                timeZone = Utc.id,
            )
        },
        fixture("empty") {
            MonarchAgendaList(events = emptyList(), modifier = Modifier.height(300.dp), timeZone = Utc.id)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAgendaListTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun aCalendarEventIsAnAgendaEvent() {
        val event: MonarchCalendarEvent = MonarchCalendarEvent("review", "Design review", Day0 + 14 * Hour)
        val agenda: List<MonarchAgendaEvent> = listOf(event)
        assertEquals("Design review", agenda.single().title)
    }

    @Test
    fun eventsAreGroupedByDayWithAllDayFirstAndMultiDayEventsOnEveryDay() {
        val late = MonarchAgendaEvent("late", "Late", Day0 + 22 * Hour, Day0 + 2 * 24 * Hour + 2 * Hour)
        val events = SampleEvents + late
        val range = AgendaListDays.range(events, null, null, Utc)
        assertEquals(listOf(Day0, Day0 + 24 * Hour, Day0 + 48 * Hour), range)
        val days = AgendaListDays.days(events, range, Utc, emptyMap(), showEmptyDays = false)
        assertEquals(listOf("offsite", "standup", "review", "late"), days[0].events.map { it.id })
        assertEquals(listOf("late"), days[1].events.map { it.id })
        assertEquals(listOf("late"), days[2].events.map { it.id })
    }

    @Test
    fun anEventEndingAtMidnightDoesNotTouchTheNextDayAndAnEndBeforeTheStartIsNoEnd() {
        val toMidnight = MonarchAgendaEvent("a", "A", Day0 + 22 * Hour, Day0 + 24 * Hour)
        assertEquals(Day0, AgendaListDays.span(toMidnight, Utc).last)
        val backwards = MonarchAgendaEvent("b", "B", Day0 + 22 * Hour, Day0 + Hour)
        assertEquals(Day0, AgendaListDays.span(backwards, Utc).last)
        val allDay = MonarchAgendaEvent("c", "C", Day0, Day0 + 24 * Hour, allDay = true)
        assertEquals(Day0 + 24 * Hour, AgendaListDays.span(allDay, Utc).last)
    }

    @Test
    fun daysFollowTheTimeZone() {
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        val event = MonarchAgendaEvent("a", "A", Day0 + 20 * Hour)
        assertEquals(Day0, AgendaListDays.dayOf(event.start, Utc))
        assertEquals(Day0 + 24 * Hour, AgendaListDays.dayOf(event.start, tokyo))
    }

    @Test
    fun aLongerEventReadsItsStartAndEndThenAllDayThenUntil() {
        val late = MonarchAgendaEvent("late", "Late", Day0 + 22 * Hour, Day0 + 2 * 24 * Hour + 2 * Hour)
        val format: (Long) -> String = { "${(it / Hour) % 24}h" }
        val range: (String, String) -> String = { a, b -> "$a to $b" }
        val until: (String) -> String = { "until $it" }
        val first = AgendaListDays.time(late, Day0, Utc, "All day", range, until, format)
        assertEquals("22h – 2h", first.visible)
        assertEquals("22h to 2h", first.spoken)
        val middle = AgendaListDays.time(late, Day0 + 24 * Hour, Utc, "All day", range, until, format)
        assertEquals("All day", middle.visible)
        val lastDay = AgendaListDays.time(late, Day0 + 48 * Hour, Utc, "All day", range, until, format)
        assertEquals("– 2h", lastDay.visible)
        assertEquals("until 2h", lastDay.spoken)
    }

    @Test
    fun aDayHeadingIsOneHeadingWithTheDateTodayAndTheCount() {
        rule.setMonarchContent {
            MonarchAgendaList(
                events = SampleEvents,
                modifier = Modifier.height(400.dp),
                today = Day0,
                timeZone = Utc.id,
            )
        }
        rule.onNodeWithContentDescription("Wednesday, November 4, Today, 3 events")
            .assert(isHeading)
    }

    @Test
    fun eventsAreButtonsWithSpokenTimesOnlyWithOnSelect() {
        var opened: String? = null
        rule.setMonarchContent {
            MonarchAgendaList(
                events = SampleEvents,
                modifier = Modifier.height(400.dp),
                today = null,
                timeZone = Utc.id,
                onSelect = { opened = it.id },
            )
        }
        rule.onNode(hasContentDescription("Stand-up, 9:30", substring = true))
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("standup", opened)
        rule.onNodeWithContentDescription("Team offsite, All day").assertHasClickAction()
        rule.onNode(hasContentDescription("Design review, 2:00", substring = true))
            .assert(hasContentDescription("Tentative, Room 4", substring = true))
        rule.onAllNodes(hasContentDescription("Today", substring = true)).assertCountEquals(0)
    }

    @Test
    fun withoutOnSelectEventsAreLabels() {
        rule.setMonarchContent {
            MonarchAgendaList(events = SampleEvents, modifier = Modifier.height(400.dp), timeZone = Utc.id)
        }
        rule.onNode(hasContentDescription("Stand-up, 9:30", substring = true)).assertHasNoClickAction()
    }

    @Test
    fun notesAreShownFirstAndADayWithNotesIsListed() {
        rule.setMonarchContent {
            MonarchAgendaList(
                events = SampleEvents,
                modifier = Modifier.height(400.dp),
                from = Day0,
                today = null,
                timeZone = Utc.id,
                dayNotes = mapOf(Day0 + 24 * Hour + 5 * Hour to listOf("Holiday")),
                days = 2,
            )
        }
        rule.onNodeWithText("Holiday").assertExists()
        rule.onNodeWithContentDescription("Thursday, November 5, 0 events").assertExists()
    }

    @Test
    fun emptyShowsTheEmptyStateAndItsTitleCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAgendaList(events = emptyList(), modifier = Modifier.height(300.dp))
        }
        rule.onNodeWithText("Nothing scheduled").assertExists()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAgendaList(
                events = SampleEvents,
                modifier = Modifier.height(400.dp),
                today = Day0,
                timeZone = Utc.id,
                labels = MonarchAgendaListLabels(
                    today = "Hoy",
                    allDay = "Todo el día",
                    count = { "$it citas" },
                    range = { a, b -> "de $a a $b" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Wednesday, November 4, Hoy, 3 citas").assertExists()
        rule.onNodeWithContentDescription("Team offsite, Todo el día").assertExists()
        rule.onNode(hasContentDescription("Stand-up, de 9:30", substring = true)).assertExists()
    }

    @Test
    fun theEmptyTitleCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAgendaList(
                events = emptyList(),
                modifier = Modifier.height(300.dp),
                labels = MonarchAgendaListLabels(emptyTitle = "Free day"),
            )
        }
        rule.onNodeWithText("Free day").assertExists()
    }

    @Test
    fun theHeadingTextIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAgendaList(
                events = SampleEvents,
                modifier = Modifier.height(450.dp),
                today = Day0,
                timeZone = Utc.id,
            )
        }
        rule.onNodeWithText("Wednesday, November 4", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun headingColoursComeFromTheThemeAndTodayIsNotAnAccent() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            val heading = MonarchAgendaListTokens.headingColors(colors)
            assertEquals(colors.bgSurface, heading.container)
            // The date is the text colour on every day; today carries the Today badge instead of an accent.
            assertEquals(colors.textDefault, heading.date)
            assertEquals(colors.textMuted, heading.count)
            assertEquals(colors.textMuted, MonarchAgendaListTokens.noteColor(colors))
        }
    }

    @Test
    fun theCountFollowsTheDateRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAgendaList(
                events = SampleEvents,
                modifier = Modifier.height(400.dp),
                today = null,
                timeZone = Utc.id,
            )
        }
        rule.onNodeWithText("Wednesday, November 4", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("3 events", useUnmergedTree = true), LayoutDirection.Rtl)
    }
}
