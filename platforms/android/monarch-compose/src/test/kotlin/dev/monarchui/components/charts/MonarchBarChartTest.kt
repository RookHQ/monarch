package dev.monarchui.components.charts

import androidx.compose.runtime.Composable
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
import dev.monarchui.components.charts.barchart.MonarchBarChart
import dev.monarchui.components.charts.barchart.MonarchBarChartGeometry
import dev.monarchui.components.charts.barchart.MonarchBarChartLabels
import dev.monarchui.components.charts.barchart.MonarchBarChartLayout
import dev.monarchui.components.charts.barchart.MonarchBarChartOrientation
import dev.monarchui.components.charts.barchart.MonarchBarChartReferenceLine
import dev.monarchui.components.charts.barchart.MonarchBarChartScale
import dev.monarchui.components.charts.barchart.MonarchBarChartSeries
import dev.monarchui.components.charts.barchart.MonarchBarChartTicks
import dev.monarchui.components.charts.barchart.MonarchBarChartTokens
import dev.monarchui.components.charts.chartframe.MonarchChartFrameLabels
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val months = listOf("Jan", "Feb", "Mar")
private val web = MonarchBarChartSeries("web", "Web", listOf(1200.0, 1450.0, 1390.0))
private val ios = MonarchBarChartSeries("ios", "iOS", listOf(800.0, 920.0, 1010.0))
private val gaps = MonarchBarChartSeries("web", "Web", listOf(1200.0, null, Double.NaN))
private val whole: (Double) -> String = { it.roundToInt().toString() }
private val PlotHeight = 96.dp

internal object MonarchBarChartFixtures : ComponentFixtures {
    override val id = "bar-chart"
    override val fixtures = listOf(
        fixture("grouped columns with a reference line") {
            MonarchBarChart(
                title = "Revenue",
                categories = months,
                series = listOf(web, ios),
                referenceLines = listOf(MonarchBarChartReferenceLine(1300.0, "Target")),
                height = PlotHeight,
            )
        },
        fixture("stacked horizontal bars with value labels") {
            MonarchBarChart(
                title = "Tickets by team",
                categories = listOf("Support", "Sales"),
                series = listOf(web, ios),
                orientation = MonarchBarChartOrientation.Horizontal,
                layout = MonarchBarChartLayout.Stacked,
                showValueLabels = true,
                height = PlotHeight,
            )
        },
        fixture("one series, highlighted, value labels, drill-in") {
            MonarchBarChart(
                title = "Signups",
                categories = months,
                series = listOf(web),
                highlightedIndex = 2,
                showValueLabels = true,
                height = PlotHeight,
                onSelect = {},
            )
        },
        fixture("negative values and gaps") {
            MonarchBarChart(
                title = "Net change",
                categories = months,
                series = listOf(MonarchBarChartSeries("net", "Net", listOf(-40.0, null, 25.0))),
                height = PlotHeight,
            )
        },
        fixture("hidden series, fixed range, legend not toggleable") {
            MonarchBarChart(
                title = "Revenue",
                categories = months,
                series = listOf(web, ios),
                hiddenSeries = setOf("ios"),
                valueRange = 0.0..2000.0,
                legendToggle = false,
                height = PlotHeight,
            )
        },
        fixture("many long categories") {
            MonarchBarChart(
                title = "Revenue by region",
                categories = (1..12).map { "Region number $it" },
                series = listOf(
                    MonarchBarChartSeries("r", "Revenue", (1..12).map { it * 10.0 }, MonarchChartColor.Data3),
                ),
                height = PlotHeight,
            )
        },
        fixture("loading") {
            MonarchBarChart(
                title = "Revenue",
                categories = months,
                series = listOf(web),
                status = MonarchChartFrameStatus.Loading,
                height = PlotHeight,
            )
        },
        fixture("empty") {
            MonarchBarChart(title = "Revenue", categories = emptyList(), series = listOf(web), height = PlotHeight)
        },
        fixture("error with retry") {
            MonarchBarChart(
                title = "Revenue",
                categories = months,
                series = listOf(web),
                status = MonarchChartFrameStatus.Error,
                height = PlotHeight,
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchBarChartTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun Chart(
        series: List<MonarchBarChartSeries> = listOf(web, ios),
        layout: MonarchBarChartLayout = MonarchBarChartLayout.Grouped,
        references: List<MonarchBarChartReferenceLine> = emptyList(),
        highlighted: Int? = null,
        onSelect: ((Int) -> Unit)? = null,
        labels: MonarchBarChartLabels = MonarchBarChartLabels(),
    ) {
        MonarchBarChart(
            title = "Revenue",
            categories = months,
            series = series,
            categoryTitle = "Month",
            layout = layout,
            highlightedIndex = highlighted,
            referenceLines = references,
            height = 160.dp,
            labels = labels,
            formatValue = whole,
            onSelect = onSelect,
        )
    }

    @Test
    fun thePlotIsNamedByTheTitleAndReadsTheSummary() {
        rule.setMonarchContent {
            MonarchBarChart(
                title = "Revenue",
                categories = months,
                series = listOf(web, ios),
                referenceLines = listOf(MonarchBarChartReferenceLine(1300.0, "Target")),
                height = 160.dp,
                formatValue = whole,
            )
        }
        val plot = rule.onNodeWithContentDescription("Revenue")
        plot.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        assertEquals(
            "3 categories, Jan to Mar. Web: total 4040, highest Feb, 1450. " +
                "iOS: total 2730, highest Mar, 1010. Target 1300",
            plot.stateDescription(),
        )
    }

    @Test
    fun theSummaryCanBeReplaced() {
        rule.setMonarchContent {
            MonarchBarChart(
                title = "Revenue",
                categories = months,
                series = listOf(web),
                summary = "Rising all quarter",
            )
        }
        assertEquals("Rising all quarter", rule.onNodeWithContentDescription("Revenue").stateDescription())
    }

    @Test
    fun eachCategoryIsAnElementInCategoryOrder() {
        rule.setMonarchContent { Chart() }
        val jan = rule.onNodeWithContentDescription("Jan: Web 1200, iOS 800")
        val feb = rule.onNodeWithContentDescription("Feb: Web 1450, iOS 920")
        rule.onNodeWithContentDescription("Mar: Web 1390, iOS 1010").assertExists()
        jan.assertIsBefore(feb, LayoutDirection.Ltr)
    }

    @Test
    fun rightToLeftMirrorsTheCategories() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Chart() }
        val jan = rule.onNodeWithContentDescription("Jan: Web 1200, iOS 800")
        val feb = rule.onNodeWithContentDescription("Feb: Web 1450, iOS 920")
        jan.assertIsBefore(feb, LayoutDirection.Rtl)
        val plot = rule.onNodeWithContentDescription("Revenue").getUnclippedBoundsInRoot()
        val first = jan.getUnclippedBoundsInRoot()
        assertTrue(first.right <= plot.right && first.left >= plot.left, "Jan lies inside the plot: $first in $plot")
        assertTrue(first.left > (plot.left + plot.right) / 2, "Jan is on the right half in right to left")
    }

    @Test
    fun gapsAreReadAsNoValue() {
        rule.setMonarchContent { Chart(series = listOf(gaps)) }
        rule.onNodeWithContentDescription("Feb: Web No value").assertExists()
        rule.onNodeWithContentDescription("Mar: Web No value").assertExists()
        assertEquals(
            "3 categories, Jan to Mar. Web: total 1200, highest Jan, 1200",
            rule.onNodeWithContentDescription("Revenue").stateDescription(),
        )
    }

    @Test
    fun aStackReadsItsTotalAndTheTableHasATotalColumn() {
        rule.setMonarchContent { Chart(layout = MonarchBarChartLayout.Stacked) }
        rule.onNodeWithContentDescription("Jan: Web 1200, iOS 800, Total 2000").assertExists()
        rule.onNodeWithText("View as table").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Month, Web, iOS, Total").assertExists()
        rule.onNodeWithContentDescription("Feb, Web 1450, iOS 920, Total 2370").assertExists()
    }

    @Test
    fun theDetailsActionOfACategoryCallsOnSelect() {
        val selected = mutableListOf<Int>()
        rule.setMonarchContent { Chart(onSelect = { selected += it }) }
        val node = rule.onNodeWithContentDescription("Feb: Web 1450, iOS 920").fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].single()
        assertEquals("Details, Feb", action.label)
        rule.runOnIdle { action.action() }
        assertEquals(listOf(1), selected)
    }

    @Test
    fun withoutOnSelectCategoriesHaveNoAction() {
        rule.setMonarchContent { Chart() }
        val node = rule.onNodeWithContentDescription("Feb: Web 1450, iOS 920").fetchSemanticsNode()
        assertTrue(SemanticsActions.CustomActions !in node.config)
        assertTrue(SemanticsActions.OnClick !in node.config)
    }

    @Test
    fun aTapInspectsTheCategoryWithDetailsAndClear() {
        val selected = mutableListOf<Int>()
        rule.setMonarchContent { Chart(onSelect = { selected += it }) }
        rule.onNodeWithContentDescription("Feb: Web 1450, iOS 920").performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText("Feb: Web 1450, iOS 920")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNode(hasText("Details") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(1), selected)
        rule.onNode(hasText("Clear") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Feb: Web 1450, iOS 920").assertDoesNotExist()
    }

    @Test
    fun aTapInRightToLeftInspectsTheCategoryUnderTheFinger() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Chart() }
        rule.onNodeWithContentDescription("Jan: Web 1200, iOS 800").performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText("Jan: Web 1200, iOS 800").assertExists()
        rule.onNodeWithText("Details").assertDoesNotExist()
    }

    @Test
    fun theLegendHidesASeriesWhenTheChartKeepsItsOwnState() {
        rule.setMonarchContent { Chart() }
        rule.onNode(hasText("iOS") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Jan: Web 1200").assertExists()
        rule.onNode(hasText("iOS") and hasClickAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Hidden"),
        )
    }

    @Test
    fun aControlledLegendReportsTheChange() {
        var hidden: Set<String>? = null
        rule.setMonarchContent {
            MonarchBarChart(
                title = "Revenue",
                categories = months,
                series = listOf(web, ios),
                hiddenSeries = emptySet(),
                onHiddenSeriesChange = { hidden = it },
                formatValue = whole,
            )
        }
        rule.onNode(hasText("Web") and hasClickAction()).performClick()
        assertEquals(setOf("web"), hidden)
        rule.onNodeWithContentDescription("Jan: Web 1200, iOS 800").assertExists()
    }

    @Test
    fun aLegendThatCannotToggleListsOnlyTheDrawnSeries() {
        rule.setMonarchContent {
            MonarchBarChart(
                title = "Revenue",
                categories = months,
                series = listOf(web, ios),
                hiddenSeries = setOf("ios"),
                legendToggle = false,
                formatValue = whole,
            )
        }
        rule.onNodeWithText("Web").assertExists()
        rule.onNodeWithText("iOS").assertDoesNotExist()
        rule.onNode(hasText("Web") and hasClickAction()).assertDoesNotExist()
        rule.onNodeWithContentDescription("Jan: Web 1200").assertExists()
    }

    @Test
    fun theHighlightedCategoryIsSaid() {
        rule.setMonarchContent { Chart(highlighted = 2) }
        assertEquals("Highlighted", rule.onNodeWithContentDescription("Mar: Web 1390, iOS 1010").stateDescription())
        assertNull(rule.onNodeWithContentDescription("Jan: Web 1200, iOS 800").stateDescription())
    }

    @Test
    fun noCategoriesShowTheEmptyState() {
        rule.setMonarchContent { MonarchBarChart(title = "Revenue", categories = emptyList(), series = listOf(web)) }
        rule.onNodeWithText("No data yet").assertExists()
    }

    @Test
    fun valuesAreWrittenInTheLocaleByDefault() {
        rule.setMonarchContent { MonarchBarChart(title = "Revenue", categories = months, series = listOf(web)) }
        rule.onNodeWithContentDescription("Feb: Web 1,450").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchBarChart(
                title = "Umsatz",
                categories = months,
                series = listOf(web, ios),
                layout = MonarchBarChartLayout.Stacked,
                highlightedIndex = 0,
                formatValue = whole,
                onSelect = {},
                labels = MonarchBarChartLabels(
                    frame = MonarchChartFrameLabels(viewTable = "Als Tabelle"),
                    category = "Monat",
                    total = "Gesamt",
                    details = "Öffnen",
                    clearSelection = "Schließen",
                    highlighted = "Hervorgehoben",
                ),
            )
        }
        val jan = rule.onNodeWithContentDescription("Jan: Web 1200, iOS 800, Gesamt 2000")
        assertEquals("Hervorgehoben", jan.stateDescription())
        assertEquals("Öffnen, Jan", jan.fetchSemanticsNode().config[SemanticsActions.CustomActions].single().label)
        jan.performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText("Öffnen").assertExists()
        rule.onNodeWithText("Schließen").assertExists()
        rule.onNodeWithText("Als Tabelle").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Monat, Web, iOS, Gesamt").assertExists()
    }

    @Test
    fun theInspectionRowWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchBarChart(
                title = "Revenue",
                categories = months,
                series = listOf(web),
                height = 200.dp,
                formatValue = whole,
            )
        }
        rule.onNodeWithContentDescription("Feb: Web 1450").performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText("Feb: Web 1450").assertTextFullyVisible()
    }

    @Test
    fun theTicksAreRoundAndIncludeZero() {
        assertEquals(500.0, MonarchBarChartTicks.step(1450.0, 4))
        assertEquals(0.2, MonarchBarChartTicks.step(1.0, 5))
        assertEquals(1.0, MonarchBarChartTicks.step(0.0, 4))
        val axis = MonarchBarChartTicks.axis(listOf(1450.0, 800.0), 4, null)
        assertEquals(0.0..1500.0, axis.domain)
        assertEquals(listOf(0.0, 500.0, 1000.0, 1500.0), axis.ticks)
        val negative = MonarchBarChartTicks.axis(listOf(-40.0, 25.0), 4, null)
        assertEquals(-40.0..40.0, negative.domain)
        assertEquals(listOf(-40.0, -20.0, 0.0, 20.0, 40.0), negative.ticks)
        assertEquals(0.0..1.0, MonarchBarChartTicks.axis(emptyList(), 4, null).domain)
        assertEquals(0.0..2000.0, MonarchBarChartTicks.axis(listOf(5000.0), 4, 0.0..2000.0).domain)
        assertEquals(listOf(0.0, 0.1, 0.2, 0.3), MonarchBarChartTicks.ticks(0.0..0.3, 0.1))
        assertEquals(2, MonarchBarChartTicks.count(10f, 48f))
        assertEquals(6, MonarchBarChartTicks.count(1000f, 48f))
        assertEquals(4, MonarchBarChartTicks.count(200f, 48f))
    }

    @Test
    fun theScaleMapsValuesOntoPixels() {
        val up = MonarchBarChartScale(0.0..100.0, start = 200f, end = 0f)
        assertEquals(200f, up.map(0.0))
        assertEquals(100f, up.map(50.0))
        assertEquals(0f, up.map(100.0))
        assertEquals(10f, MonarchBarChartScale(1.0..1.0, 10f, 20f).map(5.0))
    }

    @Test
    fun stacksSumPositiveAndNegativeValuesApart() {
        val mixed = listOf(
            MonarchBarChartSeries("a", "A", listOf(10.0)),
            MonarchBarChartSeries("b", "B", listOf(-4.0)),
            MonarchBarChartSeries("c", "C", listOf(5.0)),
        )
        val stack = MonarchBarChartGeometry.stacks(mixed, listOf(0, 1, 2), 1, stacked = true).single()
        assertEquals(15.0, stack.positive)
        assertEquals(-4.0, stack.negative)
        assertEquals(11.0, stack.total)
        val (a, b, c) = stack.segments
        assertEquals(0.0 to 10.0, a.from to a.to)
        assertEquals(0.0 to -4.0, b.from to b.to)
        assertEquals(10.0 to 15.0, c.from to c.to)
        assertEquals(listOf(false, true, true), stack.segments.map { it.roundedEnd })
        assertEquals(listOf(15.0, -4.0), MonarchBarChartGeometry.extent(listOf(stack), stacked = true, emptyList()))
        val grouped = MonarchBarChartGeometry.stacks(mixed, listOf(0, 2), 1, stacked = false).single()
        assertEquals(listOf(0, 1), grouped.segments.map { it.slot })
        assertEquals(listOf(2), grouped.segments.drop(1).map { it.series })
        assertTrue(grouped.segments.all { it.roundedEnd && it.from == 0.0 })
        val gap = MonarchBarChartGeometry.stacks(listOf(gaps), listOf(0), 3, stacked = false)
        assertEquals(listOf(1, 0, 0), gap.map { it.segments.size })
    }

    @Test
    fun bandsAndBarsAreLaidOutAcross() {
        assertEquals(1, MonarchBarChartGeometry.indexAt(150f, bandStart = 50f, band = 100f, count = 3))
        assertNull(MonarchBarChartGeometry.indexAt(40f, 50f, 100f, 3))
        assertNull(MonarchBarChartGeometry.indexAt(350f, 50f, 100f, 3))
        assertNull(MonarchBarChartGeometry.indexAt(60f, 50f, 100f, 0))
        assertEquals(72f, MonarchBarChartGeometry.groupSize(100f, maxBar = 56f, bars = 2, stacked = false))
        assertEquals(56f, MonarchBarChartGeometry.groupSize(100f, maxBar = 56f, bars = 2, stacked = true))
        val (offset, thickness) =
            MonarchBarChartGeometry.cross(1, band = 100f, group = 72f, bars = 2, stacked = false, gap = 2f)
        assertEquals(51f, offset)
        assertEquals(34f, thickness)
        assertEquals(2, MonarchBarChartGeometry.labelStep(band = 30f, labelLength = 50f))
        assertEquals(1, MonarchBarChartGeometry.labelStep(band = 60f, labelLength = 50f))
        assertEquals(240.dp, MonarchBarChartTokens.height(MonarchBarChartOrientation.Vertical, 3))
        assertEquals(120.dp, MonarchBarChartTokens.height(MonarchBarChartOrientation.Horizontal, 2))
        assertEquals(224.dp, MonarchBarChartTokens.height(MonarchBarChartOrientation.Horizontal, 6))
    }

    @Test
    fun coloursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val parts = MonarchBarChartTokens.colors(colors)
            assertEquals(colors.borderSubtle, parts.grid, theme.id)
            assertEquals(colors.borderStrong, parts.zero, theme.id)
            assertEquals(colors.textMuted, parts.axisText, theme.id)
            assertEquals(colors.textSubtle, parts.reference, theme.id)
            assertEquals(colors.bgMuted, parts.band, theme.id)
            assertEquals(
                androidx.compose.ui.graphics.lerp(colors.data2, colors.bgSurface, MonarchBarChartTokens.BAR_SOFTEN),
                MonarchBarChartTokens.barColor(MonarchChartColor.Data2, false, colors),
                theme.id,
            )
            assertEquals(
                androidx.compose.ui.graphics.lerp(
                    colors.textSubtle,
                    colors.bgSurface,
                    MonarchBarChartTokens.BAR_SOFTEN,
                ),
                MonarchBarChartTokens.barColor(MonarchChartColor.Neutral, false, colors),
            )
            assertEquals(colors.textDisabled, MonarchBarChartTokens.barColor(MonarchChartColor.Data2, true, colors))
        }
        assertEquals(MonarchChartColor.Data1, MonarchBarChartTokens.slot(null, 0))
        assertEquals(MonarchChartColor.Neutral, MonarchBarChartTokens.slot(null, 5))
        assertEquals(MonarchChartColor.Data4, MonarchBarChartTokens.slot(MonarchChartColor.Data4, 0))
    }
}
