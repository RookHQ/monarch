package com.rookhq.monarch.components.scheduling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent
import com.rookhq.monarch.components.scheduling.calendarmonthview.CalendarMonthViewCellState
import com.rookhq.monarch.components.scheduling.calendarmonthview.CalendarMonthViewDrag
import com.rookhq.monarch.components.scheduling.calendarmonthview.CalendarMonthViewEvents
import com.rookhq.monarch.components.scheduling.calendarmonthview.MonarchCalendarMonthView
import com.rookhq.monarch.components.scheduling.calendarmonthview.MonarchCalendarMonthViewDayState
import com.rookhq.monarch.components.scheduling.calendarmonthview.MonarchCalendarMonthViewLabels
import com.rookhq.monarch.components.scheduling.calendarmonthview.MonarchCalendarMonthViewTokens
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus.Confirmed
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus.Declined
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus.Tentative
import com.rookhq.monarch.internal.scheduling.MonarchMonthGrid
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Day = 86_400_000L
private const val Hour = 3_600_000L

/** 1 November 2026, a Sunday, UTC midnight. */
private const val November1 = 1_793_491_200_000L
private const val November4 = November1 + 3 * Day
private const val November9 = November1 + 8 * Day
private const val November10 = November1 + 9 * Day
private const val November14 = November1 + 13 * Day
private const val December1 = November1 + 30 * Day

/** 1 February 2026, a Sunday: with Sunday first, the month is exactly four weeks. */
private const val February1 = 1_769_904_000_000L

/** 15 June 1811, before the first month the pager offers. */
private const val EarlyDate = -5_000_000_000_000L

private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private val MonthEvents = listOf(
    MonarchAgendaEvent(id = "release", title = "Release day", start = November4, allDay = true),
    MonarchAgendaEvent("standup", "Stand-up", November4 + 9 * Hour, November4 + 9 * Hour + Hour / 4),
    MonarchAgendaEvent("review", "Design review", November4 + 11 * Hour, November4 + 12 * Hour),
    MonarchAgendaEvent("lunch", "Lunch and learn", November4 + 12 * Hour, November4 + 13 * Hour),
    MonarchAgendaEvent(
        id = "retro",
        title = "Retrospective",
        start = November4 + 16 * Hour,
        end = November4 + 17 * Hour,
        status = MonarchEventChipStatus.Cancelled,
    ),
    MonarchAgendaEvent(
        id = "interview",
        title = "Interview",
        start = November9 + 10 * Hour,
        end = November9 + 11 * Hour,
        color = Color.Unspecified,
        status = MonarchEventChipStatus.Tentative,
    ),
    MonarchAgendaEvent("deploy", "Overnight deploy", November9 + 22 * Hour, November10 + 2 * Hour),
    MonarchAgendaEvent(
        id = "conference",
        title = "Conference",
        start = November1 + 23 * Day,
        end = November1 + 25 * Day,
        allDay = true,
    ),
)

private val FebruaryEvents = listOf(
    MonarchAgendaEvent("f1", "Planning", February1 + 2 * Day + 9 * Hour, February1 + 2 * Day + 10 * Hour),
    MonarchAgendaEvent("f2", "Offsite", February1 + 10 * Day, February1 + 11 * Day, allDay = true),
    MonarchAgendaEvent(
        id = "f3",
        title = "Vendor call",
        start = February1 + 10 * Day + 14 * Hour,
        status = MonarchEventChipStatus.Declined,
    ),
)

@Suppress("LongParameterList")
@Composable
private fun TestMonthView(
    month: Long = November1,
    events: List<MonarchAgendaEvent> = MonthEvents,
    onMonthChange: (Long) -> Unit = {},
    selectedDay: Long? = null,
    onDayClick: ((Long) -> Unit)? = {},
    onEventClick: ((MonarchAgendaEvent) -> Unit)? = null,
    onEventMove: ((MonarchAgendaEvent, Int) -> Unit)? = null,
    fixedWeeks: Boolean = false,
    dayState: ((Long) -> MonarchCalendarMonthViewDayState?)? = null,
    labels: MonarchCalendarMonthViewLabels = MonarchCalendarMonthViewLabels(),
) {
    MonarchCalendarMonthView(
        month = month,
        events = events,
        onMonthChange = onMonthChange,
        selectedDay = selectedDay,
        onDayClick = onDayClick,
        onEventClick = onEventClick,
        onEventMove = onEventMove,
        today = November14,
        timeZone = Utc.id,
        firstDayOfWeek = Calendar.SUNDAY,
        fixedWeeks = fixedWeeks,
        dayState = dayState,
        labels = labels,
    )
}

internal object MonarchCalendarMonthViewFixtures : ComponentFixtures {
    override val id = "calendar-month-view"
    override val fixtures = listOf(
        fixture("four weeks with events, dots, overflow and a badge") {
            TestMonthView(
                month = February1,
                events = FebruaryEvents + List(5) { index ->
                    MonarchAgendaEvent("busy$index", "Busy $index", February1 + 4 * Day + index * Hour)
                },
                dayState = { day ->
                    val holiday = day == February1 + 6 * Day
                    if (holiday) MonarchCalendarMonthViewDayState(hatched = true, badge = "Holiday") else null
                },
            )
        },
        fixture("selected day with events that can be opened and moved") {
            TestMonthView(
                month = February1,
                events = FebruaryEvents,
                selectedDay = February1 + 2 * Day,
                onEventClick = {},
                onEventMove = { _, _ -> },
            )
        },
        fixture("selected day without events, days not tappable") {
            TestMonthView(month = February1, events = FebruaryEvents, selectedDay = February1, onDayClick = null)
        },
        fixture("six fixed weeks with outside days and hatching") {
            TestMonthView(
                fixedWeeks = true,
                dayState = { day ->
                    if (MonarchMonthGrid.dayOfWeek(day) == Calendar.SATURDAY) {
                        MonarchCalendarMonthViewDayState(hatched = true)
                    } else {
                        null
                    }
                },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchCalendarMonthViewTest {
    @get:Rule
    val rule = createComposeRule()

    private var savedLocale: Locale = Locale.getDefault()
    private var savedZone: TimeZone = TimeZone.getDefault()

    @Before
    fun pinLocale() {
        savedLocale = Locale.getDefault()
        savedZone = TimeZone.getDefault()
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(Utc)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(savedLocale)
        TimeZone.setDefault(savedZone)
    }

    private fun day(text: String) = rule.onNodeWithContentDescription(text, substring = true)

    @Test
    fun aDayIsReadAsItsDateItsEventsAndToday() {
        rule.setMonarchContent { TestMonthView() }
        day("Wednesday, November 4, 2026, 5 events").assertExists()
        day("Monday, November 9, 2026, 2 events").assertExists()
        day("Tuesday, November 10, 2026, 1 event").assertExists()
        day("Wednesday, November 25, 2026, 1 event").assertExists()
        day("Saturday, November 14, 2026, 0 events, Today").assertExists()
    }

    @Test
    fun theMonthIsAGridWithWeekdayHeaders() {
        rule.setMonarchContent { TestMonthView() }
        val grid = SemanticsMatcher.expectValue(SemanticsProperties.CollectionInfo, CollectionInfo(6, 7))
        rule.onNode(grid).assertExists()
        rule.onNodeWithContentDescription("Sunday").assertExists()
        rule.onNodeWithContentDescription("Saturday").assertExists()
    }

    @Test
    fun tappingADayReportsItsUtcMidnightAndTheSelectedDayIsSelected() {
        var tapped: Long? = null
        rule.setMonarchContent { TestMonthView(selectedDay = November9, onDayClick = { tapped = it }) }
        day("November 9, 2026").assertIsSelected()
        day("November 10, 2026").assertIsNotSelected().performClick()
        assertEquals(November10, tapped)
    }

    @Test
    fun withoutOnDayClickDaysAreNotButtons() {
        rule.setMonarchContent { TestMonthView(onDayClick = null) }
        day("November 4, 2026").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun theSelectedDayListsItsEventsAllDayFirst() {
        val heading = "Wednesday, November 4"
        rule.setMonarchContent { TestMonthView(selectedDay = November4) }
        rule.onNode(isHeading() and SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(AnnotatedString(heading))))
        val release = day("Release day, All day").getUnclippedBoundsInRoot()
        val standup = day("Stand-up, 9:00").getUnclippedBoundsInRoot()
        assertTrue(release.top < standup.top)
        day("Retrospective").assert(
            SemanticsMatcher("cancelled is said") { node ->
                node.config[SemanticsProperties.ContentDescription].first().contains("Cancelled")
            },
        )
    }

    @Test
    fun aLongerEventIsOnEveryDayItTouchesAndReadsUntil() {
        rule.setMonarchContent { TestMonthView(selectedDay = November10) }
        day("Overnight deploy, until 2:00").assertExists()
    }

    @Test
    fun anEmptySelectedDaySaysSo() {
        rule.setMonarchContent { TestMonthView(selectedDay = November14) }
        rule.onNodeWithText("No events").assertExists()
    }

    @Test
    fun openingAnEventReportsIt() {
        var opened: MonarchAgendaEvent? = null
        rule.setMonarchContent { TestMonthView(selectedDay = November9, onEventClick = { opened = it }) }
        day("Interview").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals("interview", opened?.id)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun talkBackActionsMoveAnEventByADayOrAWeek() {
        val moves = mutableListOf<Pair<String, Int>>()
        rule.setMonarchContent {
            TestMonthView(selectedDay = November9, onEventMove = { event, days -> moves += event.id to days })
        }
        day("Interview").performCustomAccessibilityActionWithLabel("Move one day earlier")
        day("Interview").performCustomAccessibilityActionWithLabel("Move one week later")
        assertEquals(listOf("interview" to -1, "interview" to 7), moves)
    }

    @Test
    fun withoutOnEventMoveThereAreNoMoveActions() {
        rule.setMonarchContent { TestMonthView(selectedDay = November9) }
        day("Interview").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun draggingAnEventOntoADayMovesItByTheDaysBetween() {
        val moves = mutableListOf<Pair<String, Int>>()
        rule.setMonarchContent {
            TestMonthView(selectedDay = November9, onEventMove = { event, days -> moves += event.id to days })
        }
        val from = day("Interview").getUnclippedBoundsInRoot()
        val to = day("Wednesday, November 11, 2026").getUnclippedBoundsInRoot()
        dragOnRoot(from, to)
        assertEquals(listOf("interview" to 2), moves)
    }

    @Test
    fun droppingAnEventOnItsOwnDayDoesNotMoveIt() {
        val moves = mutableListOf<Pair<String, Int>>()
        rule.setMonarchContent {
            TestMonthView(selectedDay = November9, onEventMove = { event, days -> moves += event.id to days })
        }
        val from = day("Interview").getUnclippedBoundsInRoot()
        val to = day("Monday, November 9, 2026").getUnclippedBoundsInRoot()
        dragOnRoot(from, to)
        assertTrue(moves.isEmpty())
    }

    private fun dragOnRoot(from: DpRect, to: DpRect) {
        val density = rule.density
        fun DpRect.centre(): Offset = with(density) {
            Offset(((left + right) / 2).toPx(), ((top + bottom) / 2).toPx())
        }
        val start = from.centre()
        val end = to.centre()
        rule.onRoot().performTouchInput {
            down(start)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + LongPressMargin)
            val steps = 10
            for (step in 1..steps) moveTo(start + (end - start) * (step.toFloat() / steps))
            up()
        }
        rule.waitForIdle()
    }

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp")
    fun swipingToTheNextMonthReportsItsFirstDay() {
        var month by mutableLongStateOf(November1)
        rule.setMonarchContent { TestMonthView(month = month, onMonthChange = { month = it }) }
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
            .performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(December1, month)
        day("Tuesday, December 15, 2026").assertExists()
    }

    @Test
    fun changingTheMonthMovesThePager() {
        var month by mutableLongStateOf(November1)
        var reported: Long? = null
        rule.setMonarchContent { TestMonthView(month = month, onMonthChange = { reported = it }) }
        month = December1 + 14 * Day
        rule.waitForIdle()
        day("Tuesday, December 15, 2026").assertExists()
        assertNull(reported)
    }

    @Test
    @Config(qualifiers = "en-rUS-w800dp-h1280dp")
    fun wideDaysAreTallerToListEventsByTitle() {
        rule.setMonarchContent { TestMonthView() }
        val cell = day("Wednesday, November 4, 2026").getUnclippedBoundsInRoot()
        assertTrue(cell.bottom - cell.top >= MonarchCalendarMonthViewTokens.WideCellMinHeight - 1.dp)
        assertTrue(cell.right - cell.left >= MonarchCalendarMonthViewTokens.WideCellWidth)
    }

    @Test
    fun narrowDaysKeepTheirTouchTargetAndScrollSideways() {
        rule.setMonarchContent { TestMonthView() }
        day("Sunday, November 1, 2026").assertTouchTargetAtLeast48dp(rule.density)
        day("Monday, November 2, 2026").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun rightToLeftPutsTheFirstDayOfTheWeekAtTheRight() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { TestMonthView() }
        day("Sunday, November 1, 2026").assertIsBefore(day("Monday, November 2, 2026"), LayoutDirection.Rtl)
    }

    @Test
    fun theDayHeadingIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) { TestMonthView(selectedDay = November14) }
        rule.onNode(isHeading()).assertTextFullyVisible()
    }

    @Test
    fun aBadgeIsReadWithTheDay() {
        rule.setMonarchContent {
            TestMonthView(
                dayState = { day ->
                    val holiday = day == November1 + 25 * Day
                    if (holiday) MonarchCalendarMonthViewDayState(hatched = true, badge = "Holiday") else null
                },
            )
        }
        day("Thursday, November 26, 2026, 1 event, Holiday").assertExists()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            TestMonthView(
                selectedDay = November14,
                labels = MonarchCalendarMonthViewLabels(
                    today = "Aujourd'hui",
                    day = { date, count -> "$date: $count" },
                    noEvents = "Rien",
                ),
            )
        }
        day("Saturday, November 14, 2026: 0, Aujourd'hui").assertExists()
        rule.onNodeWithText("Rien").assertExists()
        rule.onAllNodesWithContentDescription("events", substring = true).assertCountEquals(0)
    }

    @Test
    fun cellColoursComeFromTheirRolesInEveryTheme() {
        val tokens = MonarchCalendarMonthViewTokens
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            val plain = tokens.cellColors(CalendarMonthViewCellState(true, false, false, false), colors)
            assertEquals(Color.Transparent, plain.container)
            assertEquals(colors.textDefault, plain.number)
            assertNull(plain.markBorder)
            assertNull(plain.outline)
            val selected = tokens.cellColors(CalendarMonthViewCellState(true, true, true, false), colors)
            assertEquals(colors.accentSolid, selected.mark)
            assertEquals(colors.accentOnSolid, selected.number)
            assertNull(selected.markBorder)
            val today = tokens.cellColors(CalendarMonthViewCellState(true, true, false, false), colors)
            assertEquals(colors.accentBorder, today.markBorder)
            assertEquals(colors.accentStrong, today.number)
            val outside = tokens.cellColors(CalendarMonthViewCellState(false, false, false, true), colors)
            assertEquals(colors.bgSubtle, outside.container)
            assertEquals(colors.textSubtle, outside.number)
            assertEquals(colors.accentSolid, outside.outline)
            assertEquals(colors.accentSolid, tokens.dotColor(Color.Unspecified, Confirmed, colors))
            assertEquals(colors.borderStrong, tokens.dotColor(Color.Unspecified, Declined, colors))
            assertEquals(colors.dangerSolid, tokens.dotColor(colors.dangerSolid, Tentative, colors))
            assertEquals(colors.borderSubtle, tokens.gridLine(colors))
        }
        assertTrue(tokens.isHollow(MonarchEventChipStatus.Tentative))
        assertFalse(tokens.isHollow(MonarchEventChipStatus.Confirmed))
    }

    @Test
    fun weeksFollowTheFirstDayOfTheWeek() {
        val sunday = MonarchMonthGrid.weeks(November1, Calendar.SUNDAY, fixedWeeks = false)
        assertEquals(5, sunday.size)
        assertEquals(November1, sunday.first().first())
        val monday = MonarchMonthGrid.weeks(November1 + 10 * Day, Calendar.MONDAY, fixedWeeks = true)
        assertEquals(6, monday.size)
        assertEquals(November1 - 6 * Day, monday.first().first())
        assertEquals(4, MonarchMonthGrid.weeks(February1, Calendar.SUNDAY, fixedWeeks = false).size)
    }

    @Test
    fun pagesAndMonthsRoundTrip() {
        val page = MonarchMonthGrid.pageOf(November14)
        assertEquals(November1, MonarchMonthGrid.monthOfPage(page))
        assertEquals(December1, MonarchMonthGrid.monthOfPage(page + 1))
        assertEquals(0, MonarchMonthGrid.pageOf(EarlyDate))
    }

    @Test
    fun eventsArePlacedOnTheDaysTheyTouchInTheZone() {
        val days = MonarchMonthGrid.weeks(November1, Calendar.SUNDAY, fixedWeeks = false).flatten()
        val byDay = CalendarMonthViewEvents.eventsByDay(MonthEvents, days, Utc)
        assertEquals(listOf("release", "standup", "review", "lunch", "retro"), byDay.getValue(November4).map { it.id })
        assertEquals(listOf("deploy"), byDay.getValue(November10).map { it.id })
        assertEquals(3, (23..25).count { byDay.getValue(November1 + it * Day).any { e -> e.id == "conference" } })
        val newYork = TimeZone.getTimeZone("America/New_York")
        val shifted = CalendarMonthViewEvents.eventsByDay(MonthEvents, days, newYork)
        assertTrue(shifted.getValue(November4).any { it.id == "standup" })
        assertTrue(shifted.getValue(November4 - Day).any { it.id == "release" })
        val midnight = MonarchAgendaEvent("m", "To midnight", November4 + 22 * Hour, November4 + Day)
        assertEquals(November4, CalendarMonthViewEvents.span(midnight, Utc).last)
    }

    @Test
    fun overflowLeavesTheLastPlaceForTheMoreLine() {
        assertEquals(3, CalendarMonthViewEvents.shownCount(3, 3))
        assertEquals(2, CalendarMonthViewEvents.shownCount(5, 3))
        assertEquals(0, CalendarMonthViewEvents.shownCount(2, 1))
        assertTrue(MonarchCalendarMonthViewTokens.isWide(MonarchCalendarMonthViewTokens.WideCellWidth))
        assertFalse(MonarchCalendarMonthViewTokens.isWide(MonarchCalendarMonthViewTokens.CellMinWidth))
        assertEquals(MonarchCalendarMonthViewTokens.CellMinWidth, MonarchCalendarMonthViewTokens.cellWidth(300.dp))
    }

    @Test
    fun dragTargetsTheCellUnderTheFinger() {
        val drag = CalendarMonthViewDrag()
        drag.place(0, November4, Rect(0f, 0f, 10f, 10f))
        drag.place(0, November10, Rect(10f, 0f, 20f, 10f))
        drag.begin("x", Offset(5f, 5f), 0)
        assertEquals(November4, drag.target)
        drag.move(Offset(10f, 0f))
        assertEquals(November10, drag.target)
        assertEquals(November10, drag.end())
        assertNull(drag.eventId)
        assertEquals(6, drag.daysBetween(November4, November10))
    }

    private companion object {
        const val LongPressMargin = 100L
    }
}
