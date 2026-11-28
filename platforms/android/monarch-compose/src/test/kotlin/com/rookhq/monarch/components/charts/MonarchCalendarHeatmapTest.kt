package com.rookhq.monarch.components.charts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.charts.calendarheatmap.CalendarHeatmapDates
import com.rookhq.monarch.components.charts.calendarheatmap.CalendarHeatmapModel
import com.rookhq.monarch.components.charts.calendarheatmap.CalendarHeatmapOutline
import com.rookhq.monarch.components.charts.calendarheatmap.CalendarHeatmapText
import com.rookhq.monarch.components.charts.calendarheatmap.CalendarHeatmapWords
import com.rookhq.monarch.components.charts.calendarheatmap.MonarchCalendarHeatmap
import com.rookhq.monarch.components.charts.calendarheatmap.MonarchCalendarHeatmapDay
import com.rookhq.monarch.components.charts.calendarheatmap.MonarchCalendarHeatmapLabels
import com.rookhq.monarch.components.charts.calendarheatmap.MonarchCalendarHeatmapLayout
import com.rookhq.monarch.components.charts.calendarheatmap.MonarchCalendarHeatmapTokens
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameLabels
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.heatmap.MonarchHeatmapTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Day = 86_400_000L

/** 2026-01-01, a Thursday. */
private const val Jan1 = 20_454L * Day

private val week = listOf(
    MonarchCalendarHeatmapDay(Jan1, 3.0),
    MonarchCalendarHeatmapDay(Jan1 + Day, 0.0),
    MonarchCalendarHeatmapDay(Jan1 + 2 * Day, 8.0, note = "Holiday", marker = true),
    MonarchCalendarHeatmapDay(Jan1 + 3 * Day, null, inactive = true),
    MonarchCalendarHeatmapDay(Jan1 + 5 * Day, 12.0, valueLabel = "12 commits"),
    MonarchCalendarHeatmapDay(Jan1 + 7 * Day, 5.0),
)

private val half = (0 until 181).map { MonarchCalendarHeatmapDay(Jan1 + it * Day, (it % 9).toDouble()) }

internal object MonarchCalendarHeatmapFixtures : ComponentFixtures {
    override val id = "calendar-heatmap"
    override val fixtures = listOf(
        fixture("year, a week with today, notes, a marker and a gap") {
            MonarchCalendarHeatmap(title = "Contributions", days = week, today = Jan1)
        },
        fixture("year, half a year scrolling, drill-in") {
            MonarchCalendarHeatmap(
                title = "Contributions",
                days = half,
                description = "January to June 2026",
                today = Jan1 + 180 * Day,
                onSelect = {},
            )
        },
        fixture("month, two weeks with values") {
            MonarchCalendarHeatmap(
                title = "Hours",
                days = week,
                from = Jan1,
                to = Jan1 + 9 * Day,
                layout = MonarchCalendarHeatmapLayout.Month,
                today = Jan1 + 2 * Day,
                firstDayOfWeek = Calendar.MONDAY,
            )
        },
        fixture("custom thresholds, color, no legend") {
            MonarchCalendarHeatmap(
                title = "Deploys",
                days = week,
                color = MonarchChartColor.Data3,
                thresholds = listOf(1.0, 4.0, 10.0),
                showLegend = false,
            )
        },
        fixture("loading") {
            MonarchCalendarHeatmap(title = "Contributions", days = week, status = MonarchChartFrameStatus.Loading)
        },
        fixture("empty") {
            MonarchCalendarHeatmap(title = "Contributions", days = emptyList(), emptyMessage = "Nothing yet")
        },
        fixture("error with retry") {
            MonarchCalendarHeatmap(
                title = "Contributions",
                days = week,
                status = MonarchChartFrameStatus.Error,
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchCalendarHeatmapTest {
    @get:Rule
    val rule = createComposeRule()

    private val notLive = SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion)
    private val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun day(spoken: String) = rule.onNode(hasContentDescription(spoken) and notLive)

    @Test
    fun theGridIsNamedByTheGeneratedSummary() {
        rule.setMonarchContent { MonarchCalendarHeatmap(title = "Contributions", days = week) }
        rule.onNodeWithContentDescription(
            "8 days from Thu, Jan 1, 2026 to Thu, Jan 8, 2026. Total 28. Highest: Tue, Jan 6, 2026: 12 commits",
        ).assertExists()
    }

    @Test
    fun theSummaryCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCalendarHeatmap(title = "Contributions", days = week, summary = "A busy first week")
        }
        rule.onNodeWithContentDescription("A busy first week").assertExists()
    }

    @Test
    fun eachDayReadsItsDateValueNotesAndToday() {
        rule.setMonarchContent { MonarchCalendarHeatmap(title = "Contributions", days = week, today = Jan1) }
        day("Thu, Jan 1, 2026: 3, Today").assertExists()
        day("Fri, Jan 2, 2026: 0").assertExists()
        day("Sat, Jan 3, 2026: 8, Holiday, Marked").assertExists()
        day("Sun, Jan 4, 2026: No data, Non-working day").assertExists()
        day("Mon, Jan 5, 2026: No data").assertExists()
        day("Tue, Jan 6, 2026: 12 commits").assertExists()
    }

    @Test
    fun aTapInspectsTheDayAndAnnouncesIt() {
        val selected = mutableListOf<MonarchCalendarHeatmapDay>()
        rule.setMonarchContent {
            MonarchCalendarHeatmap(title = "Contributions", days = week, onSelect = { selected += it })
        }
        day("Sat, Jan 3, 2026: 8, Holiday, Marked").performTouchInput { click(center) }
        rule.waitForIdle()
        day("Sat, Jan 3, 2026: 8, Holiday, Marked").assertIsSelected()
        rule.onNode(hasContentDescription("Sat, Jan 3, 2026: 8, Holiday, Marked") and live).assertExists()
        assertEquals(listOf(8.0), selected.map { it.value })
    }

    @Test
    fun theSelectActionGivesADayWithoutAnEntryWithANullValue() {
        val selected = mutableListOf<MonarchCalendarHeatmapDay>()
        rule.setMonarchContent {
            MonarchCalendarHeatmap(title = "Contributions", days = week, onSelect = { selected += it })
        }
        val node = day("Mon, Jan 5, 2026: No data").fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].single()
        assertEquals("Select", action.label)
        rule.runOnIdle { action.action() }
        assertEquals(Jan1 + 4 * Day, selected.single().date)
        assertNull(selected.single().value)
    }

    @Test
    fun withoutOnSelectDaysHaveNoActionAndNoClick() {
        rule.setMonarchContent { MonarchCalendarHeatmap(title = "Contributions", days = week) }
        val node = day("Fri, Jan 2, 2026: 0").fetchSemanticsNode()
        assertFalse(SemanticsActions.CustomActions in node.config)
        assertFalse(SemanticsActions.OnClick in node.config)
    }

    @Test
    fun theYearOpensAtTheLatestWeek() {
        rule.setMonarchContent { MonarchCalendarHeatmap(title = "Contributions", days = half) }
        rule.waitForIdle()
        day("Tue, Jun 30, 2026: 0").assertIsDisplayed()
        day("Thu, Jan 1, 2026: 0").assertIsNotDisplayed()
    }

    @Test
    fun weeksRunFromTheStartEdgeInBothDirections() {
        rule.setMonarchContent { MonarchCalendarHeatmap(title = "Contributions", days = week) }
        day("Thu, Jan 1, 2026: 3").assertIsBefore(day("Thu, Jan 8, 2026: 5"), LayoutDirection.Ltr)
    }

    @Test
    fun rightToLeftMirrorsTheWeeks() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchCalendarHeatmap(title = "Contributions", days = week)
        }
        day("Thu, Jan 1, 2026: 3").assertIsBefore(day("Thu, Jan 8, 2026: 5"), LayoutDirection.Rtl)
    }

    @Test
    fun theMonthLayoutPutsAWeekInARowFromTheFirstDayOfTheWeek() {
        rule.setMonarchContent {
            MonarchCalendarHeatmap(
                title = "Hours",
                days = week,
                layout = MonarchCalendarHeatmapLayout.Month,
                firstDayOfWeek = Calendar.MONDAY,
            )
        }
        val thursday = day("Thu, Jan 1, 2026: 3")
        val friday = day("Fri, Jan 2, 2026: 0")
        val monday = day("Mon, Jan 5, 2026: No data")
        thursday.assertIsBefore(friday, LayoutDirection.Ltr)
        monday.assertIsBefore(thursday, LayoutDirection.Ltr)
        assertTrue(monday.fetchSemanticsNode().boundsInRoot.top > thursday.fetchSemanticsNode().boundsInRoot.top)
    }

    @Test
    fun theTableListsEveryEntryWithItsNote() {
        rule.setMonarchContent { MonarchCalendarHeatmap(title = "Contributions", days = week) }
        rule.onNodeWithText("View as table").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Date, Value, Note").assertExists()
        rule.onNodeWithContentDescription("Sat, Jan 3, 2026, Value 8, Note Holiday").assertExists()
        rule.onNodeWithContentDescription("Tue, Jan 6, 2026, Value 12 commits, Note No value").assertExists()
    }

    @Test
    fun theLegendReadsEveryStep() {
        rule.setMonarchContent { MonarchCalendarHeatmap(title = "Contributions", days = week) }
        rule.onNodeWithContentDescription(
            "Legend, No data, 0, Below 3, 3 to 6, 6 to 9, 9 or more",
        ).assertExists()
    }

    @Test
    fun noEntryInTheRangeShowsTheEmptyState() {
        rule.setMonarchContent {
            MonarchCalendarHeatmap(title = "Contributions", days = week, from = Jan1 + 30 * Day, to = Jan1 + 40 * Day)
        }
        rule.onNodeWithText("No data yet").assertExists()
    }

    @Test
    fun theTextIsFullyVisibleAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCalendarHeatmap(title = "Contributions this year", days = week, description = "First week")
        }
        rule.onNodeWithText("Contributions this year").assertTextFullyVisible()
        rule.onNodeWithText("First week").assertTextFullyVisible()
        day("Sat, Jan 3, 2026: 8, Holiday, Marked").performTouchInput { click(center) }
        rule.waitForIdle()
        val line = rule.onNode(hasContentDescription("Sat, Jan 3, 2026: 8, Holiday, Marked") and live)
        assertTrue(line.fetchSemanticsNode().size.height > 0)
    }

    @Test
    fun weekdayNamesFitAtTheLargestFontSize() {
        val tokens = MonarchCalendarHeatmapTokens
        // Month layout: the short names while they fit their column, the narrow ones once one would be cut.
        val short = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val narrow = listOf("S", "M", "T", "W", "T", "F", "S")
        assertEquals(short, tokens.weekdayNames(short, narrow, widest = 30.dp, cell = 42.dp))
        assertEquals(narrow, tokens.weekdayNames(short, narrow, widest = 46.dp, cell = 42.dp))
        // Year layout: a day keeps 14dp until a name is taller than the two rows and the gap it labels.
        assertEquals(tokens.YearCell, tokens.yearRow(16.dp))
        assertEquals((40.dp - tokens.Gap) / 2, tokens.yearRow(40.dp))
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCalendarHeatmap(title = "Contributions", days = week)
        }
        val side = with(rule.density) { day("Fri, Jan 2, 2026: 0").fetchSemanticsNode().size.height.toDp() }
        // Android scales large text less than linearly, so at 2.0 a caption still fits two 14dp rows: the days
        // keep their size, and only a taller name would make them grow.
        assertTrue(side >= tokens.YearCell, "A day is never smaller than 14dp: $side")
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCalendarHeatmap(
                title = "Beiträge",
                days = week,
                today = Jan1,
                onSelect = {},
                labels = MonarchCalendarHeatmapLabels(
                    frame = MonarchChartFrameLabels(viewTable = "Als Tabelle"),
                    noData = "Keine Daten",
                    today = "Heute",
                    inactive = "Frei",
                    marker = "Markiert",
                    date = "Datum",
                    dayCount = { "$it Tage" },
                    summary = "%1\$s, %2\$s bis %3\$s, Summe %4\$s",
                    highest = "Spitze: %1\$s",
                    select = "Wählen",
                ),
            )
        }
        day("Thu, Jan 1, 2026: 3, Heute").assertExists()
        day("Sun, Jan 4, 2026: Keine Daten, Frei").assertExists()
        val marked = day("Sat, Jan 3, 2026: 8, Holiday, Markiert").fetchSemanticsNode()
        assertEquals("Wählen", marked.config[SemanticsActions.CustomActions].single().label)
        rule.onNodeWithContentDescription(
            "8 Tage, Thu, Jan 1, 2026 bis Thu, Jan 8, 2026, Summe 28. Spitze: Tue, Jan 6, 2026: 12 commits",
        ).assertExists()
        rule.onNode(hasText("Als Tabelle")).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Datum, Value, Note").assertExists()
    }

    @Test
    fun datesAreWholeDaysInUtc() {
        assertEquals(20_454L, CalendarHeatmapDates.dayNumber(Jan1))
        assertEquals(20_454L, CalendarHeatmapDates.dayNumber(Jan1 + Day - 1))
        assertEquals(-1L, CalendarHeatmapDates.dayNumber(-1L))
        assertEquals(4, CalendarHeatmapDates.weekday(20_454L))
        assertEquals(4, CalendarHeatmapDates.weekday(0L))
        assertEquals(3, CalendarHeatmapDates.weekday(-1L))
        assertEquals(1, CalendarHeatmapDates.dayOfMonth(20_454L))
        assertEquals(31, CalendarHeatmapDates.dayOfMonth(-1L))
        // 2024-02-29 and 2024-03-01.
        assertEquals(29, CalendarHeatmapDates.dayOfMonth(19_782L))
        assertEquals(1, CalendarHeatmapDates.dayOfMonth(19_783L))
    }

    @Test
    fun theModelPlacesTheRangeInWholeWeeks() {
        val model = CalendarHeatmapModel(week, from = null, to = null, firstWeekday = 0, thresholds = null)
        assertEquals(20_454L..20_461L, model.range)
        assertEquals(8, model.count)
        assertEquals(4, model.lead)
        assertEquals(20_450L, model.gridStart)
        assertEquals(2, model.weeks)
        assertEquals(28.0, model.total)
        assertEquals(20_459L, model.highest)
        assertTrue(model.hasGaps)
        assertTrue(model.hasNotes)
        assertEquals(listOf(3.0, 6.0, 9.0), model.scale.thresholds)
        assertEquals(listOf(2, 0, 3, null, null, 4), (20_454L..20_459L).map(model::level))
        val monday = CalendarHeatmapModel(week, null, null, firstWeekday = 1, thresholds = null)
        assertEquals(3, monday.lead)
    }

    @Test
    fun theModelIgnoresEntriesOutsideTheRangeAndKeepsTheLaterOfTwo() {
        val twice = week + MonarchCalendarHeatmapDay(Jan1 + 3_600_000L, 7.0)
        val model = CalendarHeatmapModel(twice, from = Jan1, to = Jan1 + Day, firstWeekday = 0, thresholds = null)
        assertEquals(listOf(20_454L, 20_455L), model.entries.map { it.first })
        assertEquals(7.0, model.entry(20_454L)?.value)
        assertFalse(model.inRange(20_456L))
        val empty = CalendarHeatmapModel(week, from = Jan1 + Day, to = Jan1, firstWeekday = 0, thresholds = null)
        assertFalse(empty.valid)
        assertEquals(0, empty.weeks)
        assertFalse(empty.hasData)
    }

    @Test
    fun theTextIsPure() {
        val model = CalendarHeatmapModel(week, null, null, firstWeekday = 0, thresholds = null)
        val words = CalendarHeatmapWords(
            locale = Locale.US,
            format = { String.format(Locale.US, "%.0f", it) },
            fullDate = { "day $it" },
            noData = "No data",
            today = "Today",
            inactive = "Non-working day",
            marker = "Marked",
            day = "%1\$s: %2\$s",
            dayCount = { "$it days" },
            summary = "%1\$s from %2\$s to %3\$s. Total %4\$s",
            highest = "Highest: %1\$s",
        )
        assertEquals(
            "day 20456: 8, Holiday, Marked, Today",
            CalendarHeatmapText.describe(model, 20_456L, 20_456L, words),
        )
        assertEquals(
            "8 days from day 20454 to day 20461. Total 28. Highest: day 20459: 12 commits",
            CalendarHeatmapText.summary(model, words),
        )
        val empty = CalendarHeatmapModel(emptyList(), null, null, 0, null)
        assertEquals("No data", CalendarHeatmapText.summary(empty, words))
    }

    @Test
    fun coloursComeFromTheThemeInEveryTheme() {
        val tokens = MonarchCalendarHeatmapTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val slot = MonarchChartColor.Data2
            val strong = tokens.dayStyle(4, false, false, false, false, slot, colors)
            assertEquals(MonarchHeatmapTokens.levelColor(4, slot, colors), strong.fill, theme.id)
            assertEquals(CalendarHeatmapOutline.None, strong.outlineWidth)
            assertTrue(strong.onChip)
            val gap = tokens.dayStyle(null, false, false, false, false, slot, colors)
            assertEquals(Color.Transparent, gap.fill)
            assertEquals(colors.borderSubtle, gap.outline)
            assertEquals(CalendarHeatmapOutline.Thin, gap.outlineWidth)
            val quiet = tokens.dayStyle(0, true, false, false, true, slot, colors)
            assertEquals(colors.bgSubtle, quiet.fill)
            assertEquals(colors.borderDefault, quiet.outline)
            assertTrue(quiet.dashed)
            assertEquals(colors.warningSolid, quiet.marker)
            val today = tokens.dayStyle(2, false, true, false, false, slot, colors)
            assertEquals(colors.accentSolid, today.outline)
            assertEquals(CalendarHeatmapOutline.Thick, today.outlineWidth)
            val inspected = tokens.dayStyle(2, false, true, true, false, slot, colors)
            assertEquals(colors.textDefault, inspected.outline)
            assertFalse(inspected.dashed)
            assertNull(inspected.marker)
            assertEquals(colors.textMuted, tokens.dayNumberColor(true, colors))
            assertEquals(colors.textDefault, tokens.dayNumberColor(false, colors))
            assertEquals(colors.bgSurface, tokens.chipColor(colors))
        }
        assertEquals(tokens.YearMinHeight, tokens.minHeight(MonarchCalendarHeatmapLayout.Year))
        assertEquals(tokens.MonthMinHeight, tokens.minHeight(MonarchCalendarHeatmapLayout.Month))
        assertEquals(MonarchChartFrameStatus.Empty, tokens.status(MonarchChartFrameStatus.Ready, hasData = false))
        assertEquals(MonarchChartFrameStatus.Ready, tokens.status(MonarchChartFrameStatus.Ready, hasData = true))
        assertEquals(MonarchChartFrameStatus.Error, tokens.status(MonarchChartFrameStatus.Error, hasData = false))
    }

    @Test
    fun theInspectionLineIsAPoliteLiveRegion() {
        rule.setMonarchContent { MonarchCalendarHeatmap(title = "Contributions", days = week) }
        rule.onNode(live).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }
}
