package com.rookhq.monarch.components.scheduling

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.scheduling.agendalist.MonarchAgendaEvent
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus
import com.rookhq.monarch.components.scheduling.timegrid.MonarchTimeGrid
import com.rookhq.monarch.components.scheduling.timegrid.MonarchTimeGridLabels
import com.rookhq.monarch.components.scheduling.timegrid.MonarchTimeGridMove
import com.rookhq.monarch.components.scheduling.timegrid.MonarchTimeGridSlot
import com.rookhq.monarch.components.scheduling.timegrid.MonarchTimeGridTokens
import com.rookhq.monarch.components.scheduling.timegrid.MonarchTimeGridUnavailable
import com.rookhq.monarch.components.scheduling.timegrid.MonarchTimeGridWorkingHours
import com.rookhq.monarch.components.scheduling.timegrid.TimeGridLayout
import com.rookhq.monarch.components.scheduling.timegrid.TimeGridModel
import com.rookhq.monarch.components.scheduling.timegrid.TimeGridSpan
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Wednesday 4 November 2026, 00:00 UTC. */
private const val Day0 = 1_793_750_400_000L
private const val Day = 86_400_000L
private const val Hour = 3_600_000L
private const val Minute = 60_000L
/** ICU puts a narrow no-break space before AM and PM in en-US. */
private const val Nb = "\u202F"
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")
private val NewYork: TimeZone = TimeZone.getTimeZone("America/New_York")
private val GridHeight = 320.dp

private val Review = MonarchAgendaEvent(
    "review",
    "Design review",
    Day0 + 9 * Hour + 30 * Minute,
    Day0 + 11 * Hour,
    detail = "Room 4",
)
private val Interview = MonarchAgendaEvent(
    "interview",
    "Interview",
    Day0 + 10 * Hour,
    Day0 + 11 * Hour,
    status = MonarchEventChipStatus.Tentative,
)
private val Conference = MonarchAgendaEvent("conference", "Conference", Day0, allDay = true)
private val WorkWeek = (1..5).map { MonarchTimeGridWorkingHours(it, 9 * 60, 17 * 60) }

/** The day key of a date. */
private fun dayKey(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance(Utc, Locale.ROOT).apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis

internal object MonarchTimeGridFixtures : ComponentFixtures {
    override val id = "time-grid"
    override val fixtures = listOf(
        fixture("day with slots, events, working hours, closure and now") {
            MonarchTimeGrid(
                days = listOf(Day0),
                modifier = Modifier.height(GridHeight),
                events = listOf(Review, Interview),
                now = Day0 + 10 * Hour + 40 * Minute,
                workingHours = WorkWeek,
                unavailable = listOf(MonarchTimeGridUnavailable("lunch", Day0 + 12 * Hour, Day0 + 13 * Hour, "Closed")),
                scrollToMinutes = 9 * 60,
                timeZone = Utc.id,
                onSelectSlot = {},
                onSelectEvent = {},
                onMoveEvent = {},
            )
        },
        fixture("work week with day buttons and an all-day row") {
            MonarchTimeGrid(
                days = (0 until 5).map { Day0 - 2 * Day + it * Day },
                modifier = Modifier.height(GridHeight),
                events = listOf(Review, Interview, Conference),
                now = Day0 + 10 * Hour,
                scrollToMinutes = 9 * 60,
                timeZone = Utc.id,
                onSelectDay = {},
                onSelectEvent = {},
            )
        },
        fixture("read only, short and cancelled events") {
            MonarchTimeGrid(
                days = listOf(Day0, Day0 + Day),
                modifier = Modifier.height(GridHeight),
                events = listOf(
                    MonarchAgendaEvent("standup", "Stand-up", Day0 + 9 * Hour, Day0 + 9 * Hour + 15 * Minute),
                    MonarchAgendaEvent(
                        "retro",
                        "Retrospective",
                        Day0 + Day + 9 * Hour,
                        status = MonarchEventChipStatus.Cancelled,
                    ),
                ),
                now = null,
                startHour = 8,
                endHour = 18,
                timeZone = Utc.id,
            )
        },
        fixture("three overlapping events, all-day row shown") {
            MonarchTimeGrid(
                days = listOf(Day0),
                modifier = Modifier.height(GridHeight),
                events = listOf(
                    Review,
                    Interview,
                    MonarchAgendaEvent("call", "Customer call with a long title", Day0 + 10 * Hour, Day0 + 12 * Hour),
                ),
                now = null,
                scrollToMinutes = 9 * 60,
                showAllDayRow = true,
                timeZone = Utc.id,
                onSelectEvent = {},
            )
        },
        fixture("daylight-saving day in New York") {
            val day = dayKey(2026, 11, 1)
            MonarchTimeGrid(
                days = listOf(day),
                modifier = Modifier.height(GridHeight),
                events = listOf(MonarchAgendaEvent("late", "Night shift", day + 5 * Hour, day + 8 * Hour)),
                now = null,
                scrollToMinutes = 0,
                timeZone = NewYork.id,
                onSelectSlot = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchTimeGridTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun wallClockTimesAndInstantsFollowTheZone() {
        assertEquals(Day0, TimeGridModel.wall(Day0 + 9 * Hour, Utc).day)
        assertEquals(9 * 60, TimeGridModel.wall(Day0 + 9 * Hour, Utc).minutes)
        // 01:00 UTC on 4 November is 20:00 on 3 November in New York.
        val ny = TimeGridModel.wall(Day0 + Hour, NewYork)
        assertEquals(Day0 - Day, ny.day)
        assertEquals(20 * 60, ny.minutes)
        assertEquals(Day0 + 14 * Hour, TimeGridModel.instant(Day0, 9 * 60, NewYork))
        assertEquals(3, TimeGridModel.isoWeekday(Day0))
        assertEquals(7, TimeGridModel.isoWeekday(Day0 + 4 * Day))
        assertEquals(Day0, TimeGridModel.dayKey(Day0 + 13 * Hour))
    }

    @Test
    fun aSkippedHourMovesForwardAndARepeatedHourKeepsWallClockTime() {
        val spring = dayKey(2026, 3, 8)
        // 02:30 does not exist in New York on 8 March 2026; it becomes 03:30 EDT, 07:30 UTC.
        assertEquals(spring + 7 * Hour + 30 * Minute, TimeGridModel.instant(spring, 2 * 60 + 30, NewYork))
        val autumn = dayKey(2026, 11, 1)
        // 05:00 to 08:00 UTC on 1 November is 01:00 EDT to 03:00 EST: drawn from 01:00 to 03:00.
        val night = TimeGridModel.span(autumn + 5 * Hour, autumn + 8 * Hour, autumn, NewYork, 1)
        assertEquals(TimeGridSpan(60, 180), night)
    }

    @Test
    fun spansAreClippedToTheDayAndAnEndAtMidnightDoesNotTouchTheNextDay() {
        val late = TimeGridModel.span(Day0 + 22 * Hour, Day0 + Day + 2 * Hour, Day0, Utc, 1)
        assertEquals(TimeGridSpan(22 * 60, 1_440), late)
        val next = TimeGridModel.span(Day0 + 22 * Hour, Day0 + Day + 2 * Hour, Day0 + Day, Utc, 1)
        assertEquals(TimeGridSpan(0, 120), next)
        assertNull(TimeGridModel.span(Day0 + 22 * Hour, Day0 + Day, Day0 + Day, Utc, 1))
        assertEquals(TimeGridSpan(9 * 60, 9 * 60 + 15), TimeGridModel.span(Day0 + 9 * Hour, null, Day0, Utc, 15))
    }

    @Test
    fun overlappingEventsShareColumnsInTheirCluster() {
        val a = MonarchAgendaEvent("a", "A", 0)
        val b = MonarchAgendaEvent("b", "B", 0)
        val c = MonarchAgendaEvent("c", "C", 0)
        val d = MonarchAgendaEvent("d", "D", 0)
        val packed = TimeGridLayout.pack(
            listOf(
                a to TimeGridSpan(540, 600),
                b to TimeGridSpan(570, 630),
                c to TimeGridSpan(600, 660),
                d to TimeGridSpan(720, 780),
            ),
        ).associateBy { it.event.id }
        assertEquals(0, packed.getValue("a").col)
        assertEquals(1, packed.getValue("b").col)
        assertEquals(0, packed.getValue("c").col)
        assertEquals(2, packed.getValue("a").cols)
        assertEquals(1, packed.getValue("d").cols)
    }

    @Test
    fun shortEventsAreDrawnLongEnoughForATargetAndPackedByTheirDrawnLength() {
        val range = TimeGridModel.range(0, 24)
        val first = MonarchAgendaEvent("a", "A", Day0 + 9 * Hour, Day0 + 9 * Hour + 15 * Minute)
        val second = MonarchAgendaEvent("b", "B", Day0 + 9 * Hour + 15 * Minute, Day0 + 9 * Hour + 30 * Minute)
        val column =
            TimeGridLayout.columns(listOf(Day0), listOf(first, second), emptyList(), null, range, Utc, 45).single()
        assertEquals(listOf(540 to 585, 555 to 600), column.timed.map { it.top to it.bottom })
        assertEquals(2, column.maxCols)
        val late = MonarchAgendaEvent("c", "C", Day0 + 23 * Hour + 45 * Minute, Day0 + Day)
        val end = TimeGridLayout.columns(listOf(Day0), listOf(late), emptyList(), null, range, Utc, 45).single()
        assertEquals(1_440, end.timed.single().bottom)
        assertEquals(1_395, end.timed.single().top)
    }

    @Test
    fun timeOutsideWorkingHoursIsShaded() {
        val range = TimeGridModel.range(8, 20)
        val split = listOf(
            MonarchTimeGridWorkingHours(3, 9 * 60, 12 * 60),
            MonarchTimeGridWorkingHours(3, 13 * 60, 17 * 60),
        )
        assertEquals(
            listOf(TimeGridSpan(480, 540), TimeGridSpan(720, 780), TimeGridSpan(1_020, 1_200)),
            TimeGridLayout.shade(split, 3, range),
        )
        assertEquals(listOf(TimeGridSpan(480, 1_200)), TimeGridLayout.shade(split, 6, range))
        assertEquals(emptyList(), TimeGridLayout.shade(null, 3, range))
    }

    @Test
    fun movesKeepTheDurationStayInsideTheHoursAndSkipNoChange() {
        val range = TimeGridModel.range(8, 18)
        assertEquals(8 * 60, TimeGridLayout.clampStart(7 * 60, 60, range, 15))
        assertEquals(17 * 60, TimeGridLayout.clampStart(17 * 60 + 30, 60, range, 15))
        val move = TimeGridLayout.move(Review, Day0 + Day, 13 * 60, Utc, 15)
        assertEquals(MonarchTimeGridMove(Review, Day0 + Day + 13 * Hour, Day0 + Day + 14 * Hour + 30 * Minute), move)
        assertNull(TimeGridLayout.move(Review, Day0, 9 * 60 + 30, Utc, 15))
        assertEquals(9 * 60 + 15, TimeGridLayout.snapDown(80f, TimeGridModel.range(8, 18), 15))
        assertEquals(8 * 60, TimeGridLayout.scrollTarget(null, emptyList()))
    }

    @Test
    fun colorsAndSizesComeFromTheThemeAndTokens() {
        AllThemes.forEach { theme ->
            val roles = MonarchColors(theme.colors)
            val colors = MonarchTimeGridTokens.colors(roles)
            assertEquals(roles.bgSurface, colors.background)
            assertEquals(roles.accentSolid, colors.todayFill)
            assertEquals(roles.accentOnSolid, colors.todayText)
            assertEquals(roles.dangerSolid, colors.now)
            assertEquals(roles.bgSubtle, colors.shade)
            assertEquals(roles.accentSolid, MonarchTimeGridTokens.ghostBorder(roles, Color.Unspecified))
        }
        assertEquals(64.dp, MonarchTimeGridTokens.hourHeight(Dp.Unspecified, 1f))
        assertEquals(96.dp, MonarchTimeGridTokens.hourHeight(Dp.Unspecified, LargestFontScale))
        assertEquals(45, MonarchTimeGridTokens.minMinutes(64.dp, interactive = true, snap = 15))
        assertEquals(15, MonarchTimeGridTokens.minMinutes(64.dp, interactive = false, snap = 15))
        val widths = MonarchTimeGridTokens.dayWidths(250.dp, listOf(0, 3, 1))
        assertEquals(250.dp / 3, widths[0])
        assertTrue(widths[1] >= (48.dp + MonarchTimeGridTokens.EventGap) * 3)
    }

    @Test
    fun headingsReadTheLongDateWithTodayAndOpenTheDayWhenAsked() {
        var opened: Long? = null
        rule.setMonarchContent {
            MonarchTimeGrid(
                days = listOf(Day0 - Day, Day0),
                modifier = Modifier.height(GridHeight),
                now = Day0 + 10 * Hour,
                timeZone = Utc.id,
                onSelectDay = { opened = it },
            )
        }
        rule.onNodeWithContentDescription("Wednesday, November 4, Today")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(Day0, opened)
        val tuesday = rule.onNode(hasContentDescription("Tuesday, November 3") and hasClickAction())
        val clickLabel = tuesday.fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.OnClick)?.label
        assertEquals("Open Tuesday, November 3", clickLabel)
    }

    @Test
    fun aDayViewHeadingIsAHeadingNotAButton() {
        rule.setMonarchContent {
            MonarchTimeGrid(
                days = listOf(Day0),
                modifier = Modifier.height(GridHeight),
                now = null,
                timeZone = Utc.id,
                onSelectDay = {},
            )
        }
        rule.onNode(hasContentDescription("Wednesday, November 4") and isHeading).assertHasNoClickAction()
    }

    @Test
    fun slotsAreNamedButtonsThatSnapTheTappedTime() {
        val slots = mutableListOf<MonarchTimeGridSlot>()
        rule.setMonarchContent {
            MonarchTimeGrid(
                days = listOf(Day0),
                modifier = Modifier.height(GridHeight),
                now = null,
                scrollToMinutes = 9 * 60,
                timeZone = Utc.id,
                onSelectSlot = { slots += it },
            )
        }
        val slot = rule.onNodeWithContentDescription("Create event, Wednesday, November 4, 9:00${Nb}AM")
        slot.assertHasClickAction().assertTouchTargetAtLeast48dp(rule.density)
        slot.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(MonarchTimeGridSlot(Day0, 9 * 60, Day0 + 9 * Hour), slots.last())
        slot.performTouchInput { click(Offset(centerX, height * 0.6f)) }
        assertEquals(9 * 60 + 30, slots.last().minutes)
        assertEquals(Day0 + 9 * Hour + 30 * Minute, slots.last().start)
    }

    @Test
    fun eventsAreButtonsWithSpokenTimesAndTalkBackMoves() {
        var opened: String? = null
        val moves = mutableListOf<MonarchTimeGridMove>()
        rule.setMonarchContent {
            MonarchTimeGrid(
                days = listOf(Day0 - Day, Day0),
                modifier = Modifier.height(GridHeight),
                events = listOf(Review),
                now = null,
                timeZone = Utc.id,
                onSelectEvent = { opened = it.id },
                onMoveEvent = { moves += it },
            )
        }
        val event = rule.onNode(hasContentDescription("Design review, 9:30${Nb}AM to 11:00${Nb}AM", substring = true))
        event.assertHasClickAction().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals("review", opened)
        val actions = event.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        assertEquals(listOf("Move earlier", "Move later", "Move to previous day"), actions.map { it.label })
        actions.first { it.label == "Move later" }.action()
        assertEquals(Day0 + 9 * Hour + 45 * Minute, moves.last().start)
        assertEquals(Day0 + 11 * Hour + 15 * Minute, moves.last().end)
        actions.first { it.label == "Move to previous day" }.action()
        assertEquals(Day0 - Day + 9 * Hour + 30 * Minute, moves.last().start)
    }

    @Test
    fun aLongPressAndDragMovesTheEventByWholeSnaps() {
        val moves = mutableListOf<MonarchTimeGridMove>()
        rule.setMonarchContent {
            MonarchTimeGrid(
                days = listOf(Day0),
                modifier = Modifier.height(GridHeight),
                events = listOf(Review),
                now = null,
                scrollToMinutes = 9 * 60,
                timeZone = Utc.id,
                onMoveEvent = { moves += it },
            )
        }
        val hourPx = with(rule.density) { 64.dp.toPx() }
        rule.onNode(hasContentDescription("Design review", substring = true)).performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, hourPx / 2))
            moveBy(Offset(0f, hourPx / 2))
            up()
        }
        rule.waitForIdle()
        assertEquals(Day0 + 10 * Hour + 30 * Minute, moves.single().start)
        assertEquals(Day0 + 12 * Hour, moves.single().end)
    }

    @Test
    fun withoutCallbacksEventsAndHoursAreNotButtons() {
        rule.setMonarchContent {
            MonarchTimeGrid(
                days = listOf(Day0),
                modifier = Modifier.height(GridHeight),
                events = listOf(Review),
                now = null,
                timeZone = Utc.id,
            )
        }
        rule.onNode(hasContentDescription("Design review", substring = true)).assertHasNoClickAction()
        rule.onAllNodes(hasContentDescription("Create event", substring = true)).assertCountEquals(0)
    }

    @Test
    fun allDayEventsAndUnavailableBlocksAreRead() {
        rule.setMonarchContent {
            MonarchTimeGrid(
                days = listOf(Day0),
                modifier = Modifier.height(GridHeight),
                events = listOf(Conference),
                unavailable = listOf(MonarchTimeGridUnavailable("lunch", Day0 + 12 * Hour, Day0 + 13 * Hour, "Closed")),
                now = null,
                scrollToMinutes = 11 * 60,
                timeZone = Utc.id,
            )
        }
        rule.onNodeWithContentDescription("Conference, All day").assertExists()
        rule.onNodeWithContentDescription("Closed, 12:00${Nb}PM to 1:00${Nb}PM").assertExists()
    }

    @Test
    fun rightToLeftPutsTheFirstDayAtTheRight() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchTimeGrid(
                days = listOf(Day0, Day0 + Day),
                modifier = Modifier.height(GridHeight),
                now = null,
                timeZone = Utc.id,
            )
        }
        rule.onNode(hasContentDescription("Wednesday, November 4") and isHeading)
            .assertIsBefore(
                rule.onNode(hasContentDescription("Thursday, November 5") and isHeading),
                LayoutDirection.Rtl,
            )
    }

    @Test
    fun theLargestFontScaleMakesHoursAndEventsTaller() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTimeGrid(
                days = listOf(Day0),
                modifier = Modifier.height(GridHeight),
                events = listOf(Interview),
                now = null,
                scrollToMinutes = 10 * 60,
                timeZone = Utc.id,
                onSelectEvent = {},
            )
        }
        val node = rule.onNode(hasContentDescription("Interview", substring = true)).fetchSemanticsNode()
        assertTrue(with(rule.density) { node.size.height.toDp() } >= 90.dp)
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchTimeGrid(
                days = listOf(Day0),
                modifier = Modifier.height(GridHeight),
                events = listOf(Review),
                now = Day0,
                scrollToMinutes = 9 * 60,
                timeZone = Utc.id,
                labels = MonarchTimeGridLabels(
                    createEvent = { "New at $it" },
                    today = "Now",
                    range = { start, end -> "$start until $end" },
                    moveLater = "Later",
                ),
                onSelectSlot = {},
                onMoveEvent = {},
            )
        }
        rule.onNodeWithContentDescription("Wednesday, November 4, Now").assertExists()
        rule.onNodeWithContentDescription("New at Wednesday, November 4, 9:00${Nb}AM").assertExists()
        val name = "Design review, 9:30${Nb}AM until 11:00${Nb}AM"
        val event = rule.onNode(hasContentDescription(name, substring = true))
        val actions = event.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions).orEmpty()
        val labels = actions.map { it.label }
        assertTrue("Later" in labels)
    }
}
