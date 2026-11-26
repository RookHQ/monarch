package com.rookhq.monarch.components.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameLabels
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.linechart.MonarchLineChart
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartCurve
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartGeometry
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartLabels
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartPaths
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartReferenceLine
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartSeries
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartTokens
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartVariant
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
private val current = MonarchLineChartSeries("current", "This week", listOf(820.0, 932.0, 901.0, 1290.0, 1330.0))
private val previous = MonarchLineChartSeries(
    "previous",
    "Last week",
    listOf(700.0, 760.0, 810.0, 1020.0, 1100.0),
    color = MonarchChartColor.Neutral,
    dashed = true,
)
private val gappy = MonarchLineChartSeries("p95", "p95", listOf(210.0, null, 260.0, Double.NaN, 235.0))
private val wholeNumber: (Double) -> String = { it.roundToInt().toString() }
private const val March1 = 1_772_323_200_000L
private const val Day = 86_400_000L
private val PlotHeight = 120.dp

internal object MonarchLineChartFixtures : ComponentFixtures {
    override val id = "line-chart"
    override val fixtures = listOf(
        fixture("two series, dashed, with a target") {
            MonarchLineChart(
                title = "Sessions",
                categories = days,
                series = listOf(current, previous),
                description = "This week against last week",
                referenceLines = listOf(MonarchLineChartReferenceLine(1200.0, "Target")),
                height = PlotHeight,
            )
        },
        fixture("area, monotone, gaps, without zero") {
            MonarchLineChart(
                title = "p95 latency",
                categories = days,
                series = listOf(gappy),
                variant = MonarchLineChartVariant.Area,
                curve = MonarchLineChartCurve.Monotone,
                includeZero = false,
                height = PlotHeight,
                formatValue = { "${it.roundToInt()} ms" },
            )
        },
        fixture("dates spaced by time, every point") {
            MonarchLineChart(
                title = "Sign-ups",
                dates = listOf(March1, March1 + Day, March1 + 4 * Day, March1 + 5 * Day),
                series = listOf(MonarchLineChartSeries("s", "Sign-ups", listOf(42.0, 51.0, 47.0, 88.0))),
                showPoints = true,
                valueRange = 0.0..100.0,
                height = PlotHeight,
            )
        },
        fixture("inspected, with details") {
            MonarchLineChart(
                title = "Sessions",
                categories = days,
                series = listOf(current, previous),
                activeIndex = 1,
                onActiveIndexChange = {},
                legendToggle = false,
                tableToggle = false,
                height = PlotHeight,
                onSelect = {},
            )
        },
        fixture("one long category and a long title") {
            MonarchLineChart(
                title = "Weekly active people across every workspace in the organization",
                categories = listOf("The first week of the quarter"),
                series = listOf(MonarchLineChartSeries("a", "Active people", listOf(1204.0))),
                height = PlotHeight,
            )
        },
        fixture("loading") {
            MonarchLineChart(
                title = "Sessions",
                categories = days,
                series = listOf(current),
                status = MonarchChartFrameStatus.Loading,
                height = PlotHeight,
            )
        },
        fixture("empty") {
            MonarchLineChart(
                title = "Sessions",
                categories = emptyList(),
                series = listOf(current),
                emptyMessage = "Sessions appear after the first visit.",
                height = PlotHeight,
            )
        },
        fixture("error with retry") {
            MonarchLineChart(
                title = "Sessions",
                categories = days,
                series = listOf(current),
                status = MonarchChartFrameStatus.Error,
                height = PlotHeight,
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchLineChartTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun Chart(
        series: List<MonarchLineChartSeries> = listOf(current, previous),
        onSelect: ((Int) -> Unit)? = null,
        labels: MonarchLineChartLabels = MonarchLineChartLabels(),
    ) {
        MonarchLineChart(
            title = "Sessions",
            categories = days,
            series = series,
            xTitle = "Day",
            height = 160.dp,
            labels = labels,
            formatValue = wholeNumber,
            onSelect = onSelect,
        )
    }

    @Test
    fun thePlotIsNamedByTheTitleAndReadsTheSummary() {
        rule.setMonarchContent {
            MonarchLineChart(
                title = "Sessions",
                categories = days,
                series = listOf(current, previous),
                referenceLines = listOf(MonarchLineChartReferenceLine(1200.0, "Target")),
                height = 160.dp,
                formatValue = wholeNumber,
            )
        }
        val plot = rule.onNodeWithContentDescription("Sessions")
        plot.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        assertEquals(
            "5 points, Mon to Fri. This week: low 820, high 1330, latest 1330. " +
                "Last week: low 700, high 1100, latest 1100. Target 1200",
            plot.stateDescription(),
        )
    }

    @Test
    fun theSummaryCanBeReplaced() {
        rule.setMonarchContent {
            MonarchLineChart(title = "Sessions", categories = days, series = listOf(current), summary = "Rising")
        }
        assertEquals("Rising", rule.onNodeWithContentDescription("Sessions").stateDescription())
    }

    @Test
    fun eachPointIsAnElementInOrder() {
        rule.setMonarchContent { Chart() }
        val mon = rule.onNodeWithContentDescription("Mon: This week 820, Last week 700")
        val tue = rule.onNodeWithContentDescription("Tue: This week 932, Last week 760")
        rule.onNodeWithContentDescription("Fri: This week 1330, Last week 1100").assertExists()
        mon.assertIsBefore(tue, LayoutDirection.Ltr)
    }

    @Test
    fun rightToLeftMirrorsThePoints() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Chart() }
        val mon = rule.onNodeWithContentDescription("Mon: This week 820, Last week 700")
        val tue = rule.onNodeWithContentDescription("Tue: This week 932, Last week 760")
        mon.assertIsBefore(tue, LayoutDirection.Rtl)
        val plot = rule.onNodeWithContentDescription("Sessions").getUnclippedBoundsInRoot()
        val first = mon.getUnclippedBoundsInRoot()
        assertTrue(first.left > (plot.left + plot.right) / 2, "Mon is on the right half in right to left: $first")
        assertTrue(first.right <= plot.right + 1.dp, "Mon lies inside the plot")
    }

    @Test
    fun gapsAreReadAsNoValueAndLeftOutOfTheSummary() {
        rule.setMonarchContent { Chart(series = listOf(gappy)) }
        rule.onNodeWithContentDescription("Tue: p95 No value").assertExists()
        rule.onNodeWithContentDescription("Thu: p95 No value").assertExists()
        assertEquals(
            "5 points, Mon to Fri. p95: low 210, high 260, latest 235",
            rule.onNodeWithContentDescription("Sessions").stateDescription(),
        )
    }

    @Test
    fun theDetailsActionOfAPointCallsOnSelect() {
        val selected = mutableListOf<Int>()
        rule.setMonarchContent { Chart(onSelect = { selected += it }) }
        val node = rule.onNodeWithContentDescription("Wed: This week 901, Last week 810").fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].single()
        assertEquals("Details, Wed", action.label)
        rule.runOnIdle { action.action() }
        assertEquals(listOf(2), selected)
    }

    @Test
    fun withoutOnSelectPointsHaveNoAction() {
        rule.setMonarchContent { Chart() }
        val node = rule.onNodeWithContentDescription("Wed: This week 901, Last week 810").fetchSemanticsNode()
        assertTrue(SemanticsActions.CustomActions !in node.config)
        assertTrue(SemanticsActions.OnClick !in node.config)
    }

    @Test
    fun aTapInspectsTheNearestPointWithDetailsAndClear() {
        val selected = mutableListOf<Int>()
        rule.setMonarchContent { Chart(onSelect = { selected += it }) }
        rule.onNodeWithContentDescription("Thu: This week 1290, Last week 1020").performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText("Thu: This week 1290, Last week 1020")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNode(hasText("Details") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(3), selected)
        rule.onNode(hasText("Clear") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Thu: This week 1290, Last week 1020").assertDoesNotExist()
    }

    @Test
    fun aTapInRightToLeftInspectsThePointUnderTheFinger() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Chart() }
        rule.onNodeWithContentDescription("Mon: This week 820, Last week 700").performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText("Mon: This week 820, Last week 700").assertExists()
        rule.onNodeWithText("Details").assertDoesNotExist()
    }

    @Test
    fun aControlledInspectionIsSharedThroughTheApp() {
        var active by mutableStateOf<Int?>(2)
        val reported = mutableListOf<Int?>()
        rule.setMonarchContent {
            MonarchLineChart(
                title = "Sessions",
                categories = days,
                series = listOf(current),
                activeIndex = active,
                onActiveIndexChange = { reported += it },
                height = 160.dp,
                formatValue = wholeNumber,
            )
        }
        rule.onNodeWithText("Wed: This week 901").assertExists()
        rule.onNodeWithContentDescription("Fri: This week 1330").performTouchInput { click(center) }
        rule.waitForIdle()
        assertEquals(listOf<Int?>(4), reported)
        rule.onNodeWithText("Wed: This week 901").assertExists()
        active = 4
        rule.waitForIdle()
        rule.onNodeWithText("Fri: This week 1330").assertExists()
        rule.onNode(hasText("Clear") and hasClickAction()).performClick()
        assertEquals(listOf(4, null), reported)
    }

    @Test
    fun anInspectionOutsideTheValuesIsIgnored() {
        rule.setMonarchContent {
            MonarchLineChart(
                title = "Sessions",
                categories = days,
                series = listOf(current),
                activeIndex = 9,
                onActiveIndexChange = {},
                formatValue = wholeNumber,
            )
        }
        rule.onNodeWithText("Clear").assertDoesNotExist()
    }

    @Test
    fun theLegendHidesASeries() {
        rule.setMonarchContent { Chart() }
        rule.onNode(hasText("Last week") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Mon: This week 820").assertExists()
        rule.onNode(hasText("Last week") and hasClickAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Hidden"),
        )
    }

    @Test
    fun aSingleSeriesHasNoLegend() {
        rule.setMonarchContent { Chart(series = listOf(current)) }
        rule.onNodeWithText("This week").assertDoesNotExist()
    }

    @Test
    fun theTableListsEverySeries() {
        rule.setMonarchContent { Chart(series = listOf(current, gappy)) }
        rule.onNodeWithText("View as table").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Day, This week, p95").assertExists()
        rule.onNodeWithContentDescription("Tue, This week 932, p95 No value").assertExists()
    }

    @Test
    fun datesAreWrittenInUtcAndSpacedByTime() {
        rule.setMonarchContent {
            MonarchLineChart(
                title = "Sign-ups",
                dates = listOf(March1, March1 + Day, March1 + 4 * Day),
                series = listOf(MonarchLineChartSeries("s", "Sign-ups", listOf(42.0, 51.0, 47.0))),
                height = 160.dp,
                formatValue = wholeNumber,
            )
        }
        rule.onNodeWithContentDescription("Mar 1: Sign-ups 42").assertExists()
        val first = rule.onNodeWithContentDescription("Mar 1: Sign-ups 42").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithContentDescription("Mar 2: Sign-ups 51").getUnclippedBoundsInRoot()
        rule.onNodeWithContentDescription("Mar 5: Sign-ups 47").assertExists()
        val firstWidth = first.right - first.left
        assertTrue(second.right - second.left > firstWidth * 2, "three days widen the strip: $first, $second")
        assertTrue(first.right <= second.left + 1.dp)
        rule.onNodeWithText("View as table").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Date, Sign-ups").assertExists()
    }

    @Test
    fun noCategoriesShowTheEmptyState() {
        rule.setMonarchContent {
            MonarchLineChart(title = "Sessions", categories = emptyList(), series = listOf(current))
        }
        rule.onNodeWithText("No data yet").assertExists()
        rule.onNodeWithContentDescription("Sessions").assertDoesNotExist()
    }

    @Test
    fun valuesAreWrittenInTheLocaleByDefault() {
        rule.setMonarchContent { MonarchLineChart(title = "Sessions", categories = days, series = listOf(current)) }
        rule.onNodeWithContentDescription("Thu: This week 1,290").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchLineChart(
                title = "Sitzungen",
                categories = days,
                series = listOf(gappy),
                formatValue = wholeNumber,
                onSelect = {},
                labels = MonarchLineChartLabels(
                    frame = MonarchChartFrameLabels(viewTable = "Als Tabelle", noValue = "Kein Wert"),
                    category = "Tag",
                    details = "Öffnen",
                    clearSelection = "Schließen",
                ),
            )
        }
        val tue = rule.onNodeWithContentDescription("Tue: p95 Kein Wert")
        assertEquals("Öffnen, Tue", tue.fetchSemanticsNode().config[SemanticsActions.CustomActions].single().label)
        tue.performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText("Öffnen").assertExists()
        rule.onNodeWithText("Schließen").assertExists()
        rule.onNodeWithText("Als Tabelle").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Tag, p95").assertExists()
    }

    @Test
    fun theInspectionRowWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLineChart(
                title = "Sessions",
                categories = days,
                series = listOf(current, previous),
                legendToggle = false,
                height = 200.dp,
                formatValue = wholeNumber,
            )
        }
        rule.onNodeWithContentDescription("Wed: This week 901, Last week 810").performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText("Wed: This week 901, Last week 810").assertTextFullyVisible()
    }

    @Test
    fun theAxisCanLeaveOutZero() {
        val withZero = MonarchLineChartGeometry.axis(listOf(210.0, 260.0), 4, null, includeZero = true)
        assertEquals(0.0, withZero.domain.start)
        val without = MonarchLineChartGeometry.axis(listOf(210.0, 260.0), 2, null, includeZero = false)
        // The web's steps (1, 2, 2.5, 5): a 50 span in 2 parts steps by 25.
        assertEquals(200.0..275.0, without.domain)
        assertEquals(listOf(200.0, 225.0, 250.0, 275.0), without.ticks)
        val flat = MonarchLineChartGeometry.axis(listOf(50.0, 50.0), 4, null, includeZero = false)
        assertTrue(flat.domain.start < 50.0 && flat.domain.endInclusive > 50.0, "a flat series is padded")
        val fixed = MonarchLineChartGeometry.axis(listOf(42.0), 4, 0.0..100.0, includeZero = false)
        assertEquals(0.0..100.0, fixed.domain)
        assertEquals(200.0, MonarchLineChartGeometry.baseline(200.0..260.0))
        assertEquals(0.0, MonarchLineChartGeometry.baseline(-10.0..260.0))
        assertEquals(-5.0, MonarchLineChartGeometry.baseline(-20.0..-5.0))
    }

    @Test
    fun positionsAreEvenForCategoriesAndByTimeForDates() {
        assertEquals(listOf(0f, 0.5f, 1f), MonarchLineChartGeometry.evenFractions(3))
        assertEquals(listOf(0.5f), MonarchLineChartGeometry.evenFractions(1))
        assertEquals(listOf(0f, 0.25f, 1f), MonarchLineChartGeometry.timeFractions(listOf(0L, Day, 4 * Day)))
        assertEquals(listOf(0.5f, 0.5f), MonarchLineChartGeometry.timeFractions(listOf(Day, Day)))
        assertEquals(2, MonarchLineChartGeometry.nearestIndex(listOf(0f, 50f, 100f), 80f))
        assertEquals(null, MonarchLineChartGeometry.nearestIndex(emptyList(), 80f))
        assertEquals(25f to 75f, MonarchLineChartGeometry.strip(listOf(0f, 50f, 100f), 1, 0f, 100f))
        assertEquals(0f to 25f, MonarchLineChartGeometry.strip(listOf(0f, 50f, 100f), 0, 0f, 100f))
    }

    @Test
    fun gapsSplitTheLineAndLoneValuesBecomeDots() {
        val a = Offset(0f, 1f)
        val b = Offset(1f, 2f)
        val c = Offset(3f, 1f)
        val points = listOf(a, b, null, c, null)
        assertEquals(listOf(listOf(a, b), listOf(c)), MonarchLineChartPaths.runs(points))
        assertEquals(listOf(c), MonarchLineChartPaths.isolated(points))
    }

    @Test
    fun theMonotoneCurveIsFlatAtATurnAndKeepsTheSlopeOfStraightRuns() {
        val turn = MonarchLineChartPaths.tangents(listOf(Offset(0f, 0f), Offset(1f, 10f), Offset(2f, 0f)))
        assertEquals(0f, turn[1])
        assertEquals(10f, turn[0])
        val straight = MonarchLineChartPaths.tangents(listOf(Offset(0f, 0f), Offset(1f, 2f), Offset(2f, 4f)))
        assertTrue(straight.all { abs(it - 2f) < 0.0001f }, "a straight run keeps its slope: $straight")
    }

    @Test
    fun xLabelsAreSpacedAndKeepTheEnds() {
        assertEquals(listOf(0, 1, 2), MonarchLineChartGeometry.labelIndices(3, 300f, 50f))
        assertEquals(listOf(0, 3, 6, 9), MonarchLineChartGeometry.labelIndices(10, 300f, 100f))
        assertEquals(listOf(0, 4, 11), MonarchLineChartGeometry.labelIndices(12, 300f, 100f))
        assertEquals(listOf(0), MonarchLineChartGeometry.labelIndices(1, 10f, 100f))
        assertEquals(emptyList(), MonarchLineChartGeometry.labelIndices(0, 300f, 100f))
    }

    @Test
    fun coloursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val parts = MonarchLineChartTokens.colors(colors)
            assertEquals(colors.borderSubtle, parts.grid, theme.id)
            assertEquals(colors.borderStrong, parts.zero, theme.id)
            assertEquals(colors.textMuted, parts.axisText, theme.id)
            assertEquals(colors.textSubtle, parts.reference, theme.id)
            assertEquals(colors.borderStrong, parts.crosshair, theme.id)
            assertEquals(colors.bgSurface, parts.dotRing, theme.id)
            assertEquals(colors.data2, MonarchLineChartTokens.lineColor(MonarchChartColor.Data2, colors), theme.id)
            assertEquals(colors.textSubtle, MonarchLineChartTokens.lineColor(MonarchChartColor.Neutral, colors))
            val area = MonarchLineChartTokens.areaColor(MonarchChartColor.Data1, colors)
            assertEquals(lerp(colors.data1, colors.bgSurface, MonarchLineChartTokens.AREA_MIX), area, theme.id)
            assertEquals(1f, area.alpha, "the fill is opaque in ${theme.id}")
            assertNotEquals(colors.bgSurface, area, theme.id)
            assertEquals(MonarchChartColor.Data3, MonarchLineChartTokens.slot(null, 2))
        }
    }
}
