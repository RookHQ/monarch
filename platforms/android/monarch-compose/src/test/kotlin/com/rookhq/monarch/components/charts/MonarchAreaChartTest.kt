package com.rookhq.monarch.components.charts

import androidx.compose.runtime.Composable
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
import com.rookhq.monarch.components.charts.areachart.MonarchAreaChart
import com.rookhq.monarch.components.charts.areachart.MonarchAreaChartLabels
import com.rookhq.monarch.components.charts.areachart.MonarchAreaChartStacking
import com.rookhq.monarch.components.charts.areachart.MonarchAreaChartStacks
import com.rookhq.monarch.components.charts.areachart.MonarchAreaChartTokens
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameLabels
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartCurve
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartReferenceLine
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartSeries
import com.rookhq.monarch.components.charts.linechart.MonarchLineChartTokens
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
import org.robolectric.annotation.Config
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val areaDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
private val web = MonarchLineChartSeries("web", "Web", listOf(420.0, 460.0, 445.0, 480.0, 610.0))
private val ios = MonarchLineChartSeries("ios", "iOS", listOf(210.0, 230.0, 220.0, 240.0, 300.0))
private val android = MonarchLineChartSeries("android", "Android", listOf(150.0, 160.0, 170.0, 160.0, 210.0))
private val forecast = MonarchLineChartSeries(
    "forecast",
    "Forecast",
    listOf(400.0, 420.0, null, 460.0, 500.0),
    color = MonarchChartColor.Neutral,
    dashed = true,
)
private val areaWhole: (Double) -> String = { it.roundToInt().toString() }
private const val AreaMarch1 = 1_772_323_200_000L
private const val AreaDay = 86_400_000L
private val AreaPlotHeight = 120.dp

internal object MonarchAreaChartFixtures : ComponentFixtures {
    override val id = "area-chart"
    override val fixtures = listOf(
        fixture("stacked, three series") {
            MonarchAreaChart(
                title = "Traffic",
                categories = areaDays,
                series = listOf(web, ios, android),
                description = "Daily sessions by platform",
                stacking = MonarchAreaChartStacking.Stacked,
                height = AreaPlotHeight,
            )
        },
        fixture("100% stack, monotone") {
            MonarchAreaChart(
                title = "Share of sessions",
                categories = areaDays,
                series = listOf(web, ios),
                stacking = MonarchAreaChartStacking.Expand,
                curve = MonarchLineChartCurve.Monotone,
                height = AreaPlotHeight,
            )
        },
        fixture("overlap, dashed with a gap, a goal") {
            MonarchAreaChart(
                title = "Sessions",
                categories = areaDays,
                series = listOf(web, forecast),
                referenceLines = listOf(MonarchLineChartReferenceLine(550.0, "Goal")),
                includeZero = false,
                legendToggle = false,
                height = AreaPlotHeight,
            )
        },
        fixture("dates, inspected, with details") {
            MonarchAreaChart(
                title = "Sign-ups",
                dates = listOf(AreaMarch1, AreaMarch1 + AreaDay, AreaMarch1 + 4 * AreaDay),
                series = listOf(MonarchLineChartSeries("s", "Sign-ups", listOf(42.0, 51.0, 47.0))),
                activeIndex = 1,
                onActiveIndexChange = {},
                tableToggle = false,
                height = AreaPlotHeight,
                onSelect = {},
            )
        },
        fixture("one long category and a long title") {
            MonarchAreaChart(
                title = "Weekly active people across every workspace in the organization",
                categories = listOf("The first week of the quarter"),
                series = listOf(MonarchLineChartSeries("a", "Active people", listOf(1204.0))),
                height = AreaPlotHeight,
            )
        },
        fixture("loading") {
            MonarchAreaChart(
                title = "Traffic",
                categories = areaDays,
                series = listOf(web),
                status = MonarchChartFrameStatus.Loading,
                height = AreaPlotHeight,
            )
        },
        fixture("empty") {
            MonarchAreaChart(
                title = "Traffic",
                categories = emptyList(),
                series = listOf(web),
                emptyMessage = "Sessions appear after the first visit.",
                height = AreaPlotHeight,
            )
        },
        fixture("error with retry") {
            MonarchAreaChart(
                title = "Traffic",
                categories = areaDays,
                series = listOf(web),
                status = MonarchChartFrameStatus.Error,
                height = AreaPlotHeight,
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAreaChartTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun Chart(
        series: List<MonarchLineChartSeries> = listOf(web, ios),
        stacking: MonarchAreaChartStacking = MonarchAreaChartStacking.Stacked,
        onSelect: ((Int) -> Unit)? = null,
        labels: MonarchAreaChartLabels = MonarchAreaChartLabels(),
    ) {
        MonarchAreaChart(
            title = "Traffic",
            categories = areaDays,
            series = series,
            xTitle = "Day",
            stacking = stacking,
            height = 160.dp,
            labels = labels,
            formatValue = areaWhole,
            onSelect = onSelect,
        )
    }

    @Test
    fun thePlotIsNamedByTheTitleAndSummarisesTheStackWithItsTotal() {
        rule.setMonarchContent { Chart() }
        val plot = rule.onNodeWithContentDescription("Traffic")
        plot.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        assertEquals(
            "5 points, Mon to Fri. Web: low 420, high 610, latest 610. iOS: low 210, high 300, latest 300. " +
                "Total: low 630, high 910, latest 910",
            plot.stateDescription(),
        )
    }

    @Test
    fun eachXValueReadsTheRealValuesAndTheTotalInOrder() {
        rule.setMonarchContent { Chart() }
        val mon = rule.onNodeWithContentDescription("Mon: Web 420, iOS 210, Total 630")
        val tue = rule.onNodeWithContentDescription("Tue: Web 460, iOS 230, Total 690")
        rule.onNodeWithContentDescription("Fri: Web 610, iOS 300, Total 910").assertExists()
        mon.assertIsBefore(tue, LayoutDirection.Ltr)
    }

    @Test
    fun aHundredPercentStackReadsSharesAndLeavesOutReferenceLines() {
        rule.setMonarchContent {
            MonarchAreaChart(
                title = "Traffic",
                categories = areaDays,
                series = listOf(web, ios),
                stacking = MonarchAreaChartStacking.Expand,
                referenceLines = listOf(MonarchLineChartReferenceLine(500.0, "Goal")),
                height = 160.dp,
                formatValue = areaWhole,
            )
        }
        rule.onNodeWithContentDescription("Mon: Web 420 (66.7%), iOS 210 (33.3%), Total 630").assertExists()
        val summary = rule.onNodeWithContentDescription("Traffic").stateDescription().orEmpty()
        assertTrue("Goal" !in summary, "reference lines are not read in a 100% stack: $summary")
    }

    @Test
    fun overlappingAreasHaveNoTotalAndReadTheReferenceLines() {
        rule.setMonarchContent {
            MonarchAreaChart(
                title = "Traffic",
                categories = areaDays,
                series = listOf(web, ios),
                referenceLines = listOf(MonarchLineChartReferenceLine(500.0, "Goal")),
                height = 160.dp,
                formatValue = areaWhole,
            )
        }
        rule.onNodeWithContentDescription("Mon: Web 420, iOS 210").assertExists()
        assertEquals(
            "5 points, Mon to Fri. Web: low 420, high 610, latest 610. iOS: low 210, high 300, latest 300. Goal 500",
            rule.onNodeWithContentDescription("Traffic").stateDescription(),
        )
    }

    @Test
    fun aGapIsReadAsNoValueAndLeftOutOfTheTotal() {
        val gappy = MonarchLineChartSeries("web", "Web", listOf(420.0, null, 445.0, Double.NaN, 610.0))
        rule.setMonarchContent { Chart(series = listOf(gappy, ios)) }
        rule.onNodeWithContentDescription("Tue: Web No value, iOS 230, Total 230").assertExists()
        rule.onNodeWithContentDescription("Thu: Web No value, iOS 240, Total 240").assertExists()
    }

    @Test
    fun rightToLeftMirrorsTheXValues() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Chart() }
        val mon = rule.onNodeWithContentDescription("Mon: Web 420, iOS 210, Total 630")
        val tue = rule.onNodeWithContentDescription("Tue: Web 460, iOS 230, Total 690")
        mon.assertIsBefore(tue, LayoutDirection.Rtl)
        val plot = rule.onNodeWithContentDescription("Traffic").getUnclippedBoundsInRoot()
        val first = mon.getUnclippedBoundsInRoot()
        assertTrue(first.left > (plot.left + plot.right) / 2, "Mon is on the right half in right to left: $first")
    }

    @Test
    fun aTapInspectsWithDetailsAndClear() {
        val selected = mutableListOf<Int>()
        rule.setMonarchContent { Chart(onSelect = { selected += it }) }
        val wed = "Wed: Web 445, iOS 220, Total 665"
        val action = rule.onNodeWithContentDescription(wed).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].single()
        assertEquals("Details, Wed", action.label)
        rule.onNodeWithContentDescription(wed).performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText(wed)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNode(hasText("Details") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(2), selected)
        rule.onNode(hasText("Clear") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNodeWithText(wed).assertDoesNotExist()
    }

    @Test
    fun hidingASeriesTakesItOutOfTheStackButNotTheTable() {
        rule.setMonarchContent { Chart() }
        rule.onNode(hasText("iOS") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Mon: Web 420").assertExists()
        rule.onNodeWithText("View as table").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Day, Web, iOS, Total").assertExists()
        rule.onNodeWithContentDescription("Mon, Web 420, iOS 210, Total 630").assertExists()
    }

    @Test
    fun aSingleSeriesHasNoLegendAndNoTotal() {
        rule.setMonarchContent { Chart(series = listOf(web)) }
        rule.onNodeWithText("Web").assertDoesNotExist()
        rule.onNodeWithContentDescription("Mon: Web 420").assertExists()
    }

    @Test
    fun noCategoriesShowTheEmptyState() {
        rule.setMonarchContent { MonarchAreaChart(title = "Traffic", categories = emptyList(), series = listOf(web)) }
        rule.onNodeWithText("No data yet").assertExists()
        rule.onNodeWithContentDescription("Traffic").assertDoesNotExist()
    }

    @Test
    fun datesAreWrittenInUtc() {
        rule.setMonarchContent {
            MonarchAreaChart(
                title = "Sign-ups",
                dates = listOf(AreaMarch1, AreaMarch1 + AreaDay),
                series = listOf(MonarchLineChartSeries("s", "Sign-ups", listOf(42.0, 51.0))),
                height = 160.dp,
                formatValue = areaWhole,
            )
        }
        rule.onNodeWithContentDescription("Mar 1: Sign-ups 42").assertExists()
        rule.onNodeWithContentDescription("Mar 2: Sign-ups 51").assertExists()
    }

    @Test
    fun valuesAreWrittenInTheLocaleByDefault() {
        rule.setMonarchContent {
            MonarchAreaChart(
                title = "Traffic",
                categories = areaDays,
                series = listOf(MonarchLineChartSeries("big", "Big", listOf(1290.0, 1300.0, 1310.0, 1320.0, 1330.0))),
            )
        }
        rule.onNodeWithContentDescription("Mon: Big 1,290").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Chart(
                stacking = MonarchAreaChartStacking.Expand,
                labels = MonarchAreaChartLabels(
                    frame = MonarchChartFrameLabels(viewTable = "Als Tabelle"),
                    category = "Tag",
                    total = "Summe",
                    share = { value, share -> "$value · $share" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Mon: Web 420 · 66.7%, iOS 210 · 33.3%, Summe 630").assertExists()
        rule.onNodeWithText("Als Tabelle").assertExists()
    }

    @Test
    fun theInspectionRowWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAreaChart(
                title = "Traffic",
                categories = areaDays,
                series = listOf(web, ios),
                stacking = MonarchAreaChartStacking.Stacked,
                legendToggle = false,
                height = 200.dp,
                formatValue = areaWhole,
            )
        }
        val wed = "Wed: Web 445, iOS 220, Total 665"
        rule.onNodeWithContentDescription(wed).performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText(wed).assertTextFullyVisible()
    }

    @Test
    fun theStackPilesTheSeriesInOrder() {
        val series = listOf(web, ios)
        val overlap = MonarchAreaChartStacks.stack(series, listOf(0, 1), 5, MonarchAreaChartStacking.Overlap)
        assertEquals(listOf(210.0, 230.0, 220.0, 240.0, 300.0), overlap.edges[1])
        val stacked = MonarchAreaChartStacks.stack(series, listOf(0, 1), 5, MonarchAreaChartStacking.Stacked)
        assertEquals(listOf(420.0, 460.0, 445.0, 480.0, 610.0), stacked.edges[0])
        assertEquals(listOf(630.0, 690.0, 665.0, 720.0, 910.0), stacked.edges[1])
        assertEquals(listOf(630.0, 690.0, 665.0, 720.0, 910.0), stacked.totals)
        val expand = MonarchAreaChartStacks.stack(series, listOf(0, 1), 5, MonarchAreaChartStacking.Expand)
        assertEquals(2.0 / 3.0, checkNotNull(expand.edges[0][0]), 1e-9)
        assertEquals(1.0, checkNotNull(expand.edges[1][0]), 1e-9)
    }

    @Test
    fun aGapKeepsTheSeriesAboveInPlaceAndNegativesCountAsZero() {
        val low = MonarchLineChartSeries("a", "A", listOf(10.0, null, -5.0))
        val high = MonarchLineChartSeries("b", "B", listOf(5.0, 5.0, 5.0))
        val stack = MonarchAreaChartStacks.stack(listOf(low, high), listOf(0, 1), 3, MonarchAreaChartStacking.Stacked)
        assertEquals(listOf(10.0, null, 0.0), stack.edges[0])
        assertEquals(listOf(15.0, 5.0, 5.0), stack.edges[1])
        assertEquals(listOf(15.0, 5.0, 5.0), stack.totals)
        val zero = MonarchLineChartSeries("z", "Z", listOf(0.0))
        val shares = MonarchAreaChartStacks.stack(listOf(zero), listOf(0), 1, MonarchAreaChartStacking.Expand)
        assertEquals(listOf(0.0), shares.edges[0])
        assertEquals(0.0, MonarchAreaChartStacks.share(-3.0, 10.0))
        assertTrue(MonarchAreaChartStacks.showsTotal(MonarchAreaChartStacking.Stacked, 2))
        assertTrue(!MonarchAreaChartStacks.showsTotal(MonarchAreaChartStacking.Overlap, 2))
        assertTrue(!MonarchAreaChartStacks.showsTotal(MonarchAreaChartStacking.Expand, 1))
    }

    @Test
    fun fillsAreOpaqueShadesOfTheSeriesColourInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val overlap = MonarchAreaChartTokens.areaColor(MonarchChartColor.Data1, false, false, colors)
            assertEquals(MonarchLineChartTokens.areaColor(MonarchChartColor.Data1, colors), overlap, theme.id)
            val band = MonarchAreaChartTokens.areaColor(MonarchChartColor.Data1, true, false, colors)
            assertEquals(lerp(colors.data1, colors.bgSurface, MonarchAreaChartTokens.STACK_MIX), band, theme.id)
            val faint = MonarchAreaChartTokens.areaColor(MonarchChartColor.Data2, false, true, colors)
            assertEquals(lerp(colors.data2, colors.bgSurface, MonarchAreaChartTokens.DASHED_MIX), faint, theme.id)
            listOf(overlap, band, faint).forEach { assertEquals(1f, it.alpha, "fills are opaque in ${theme.id}") }
            assertNotEquals(overlap, band, "a stacked band is stronger than an overlapping fill in ${theme.id}")
            assertNotEquals(colors.bgSurface, faint, theme.id)
        }
    }
}
