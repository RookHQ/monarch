package dev.monarchui.components.charts

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.charts.barchart.MonarchBarChartLabels
import dev.monarchui.components.charts.barchart.MonarchBarChartLayout
import dev.monarchui.components.charts.barchart.MonarchBarChartOrientation
import dev.monarchui.components.charts.barchart.MonarchBarChartReferenceLine
import dev.monarchui.components.charts.barchart.MonarchBarChartSeries
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.stackedbarchart.MonarchStackedBarChart
import dev.monarchui.components.charts.stackedbarchart.MonarchStackedBarChartLabels
import dev.monarchui.components.charts.stackedbarchart.MonarchStackedBarChartOffset
import dev.monarchui.components.charts.stackedbarchart.MonarchStackedBarChartShares
import dev.monarchui.components.charts.stackedbarchart.MonarchStackedBarChartTokens
import dev.monarchui.components.charts.stackedbarchart.MonarchStackedBarChartWords
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

private val quarter = listOf("Jan", "Feb", "Mar")
private val resolved = MonarchBarChartSeries("resolved", "Resolved", listOf(120.0, 132.0, 101.0))
private val pending = MonarchBarChartSeries("pending", "Pending", listOf(22.0, 18.0, 30.0))
private val escalated = MonarchBarChartSeries("escalated", "Escalated", listOf(8.0, 6.0, 12.0))
private val tickets = listOf(resolved, pending, escalated)
private val count: (Double) -> String = { it.roundToInt().toString() }
private val StackHeight = 96.dp

internal object MonarchStackedBarChartFixtures : ComponentFixtures {
    override val id = "stacked-bar-chart"
    override val fixtures = listOf(
        fixture("stacked values with totals and a target") {
            MonarchStackedBarChart(
                title = "Tickets by status",
                categories = quarter,
                series = tickets,
                referenceLines = listOf(MonarchBarChartReferenceLine(150.0, "Target")),
                showValueLabels = true,
                height = StackHeight,
            )
        },
        fixture("shares of 100%, vertical, drill-in") {
            MonarchStackedBarChart(
                title = "Tickets by status",
                categories = quarter,
                series = tickets,
                offset = MonarchStackedBarChartOffset.Expand,
                showValueLabels = true,
                height = StackHeight,
                onSelect = {},
            )
        },
        fixture("shares of 100%, horizontal, one hidden") {
            MonarchStackedBarChart(
                title = "Tickets by team",
                categories = listOf("Support", "Sales"),
                series = tickets,
                orientation = MonarchBarChartOrientation.Horizontal,
                offset = MonarchStackedBarChartOffset.Expand,
                hiddenSeries = setOf("escalated"),
                height = StackHeight,
            )
        },
        fixture("grouped, highlighted, offset ignored") {
            MonarchStackedBarChart(
                title = "Tickets by status",
                categories = quarter,
                series = tickets,
                layout = MonarchBarChartLayout.Grouped,
                offset = MonarchStackedBarChartOffset.Expand,
                highlightedIndex = 1,
                height = StackHeight,
            )
        },
        fixture("gaps, negative values and a zero total") {
            MonarchStackedBarChart(
                title = "Net change",
                categories = quarter,
                series = listOf(
                    MonarchBarChartSeries("a", "A", listOf(-4.0, null, 0.0)),
                    MonarchBarChartSeries("b", "B", listOf(6.0, 3.0, 0.0)),
                ),
                offset = MonarchStackedBarChartOffset.Expand,
                height = StackHeight,
            )
        },
        fixture("loading") {
            MonarchStackedBarChart(
                title = "Tickets",
                categories = quarter,
                series = tickets,
                status = MonarchChartFrameStatus.Loading,
                height = StackHeight,
            )
        },
        fixture("empty") {
            MonarchStackedBarChart(title = "Tickets", categories = quarter, series = emptyList(), height = StackHeight)
        },
        fixture("error with retry") {
            MonarchStackedBarChart(
                title = "Tickets",
                categories = quarter,
                series = tickets,
                status = MonarchChartFrameStatus.Error,
                height = StackHeight,
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchStackedBarChartTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun Chart(
        offset: MonarchStackedBarChartOffset = MonarchStackedBarChartOffset.Normal,
        layout: MonarchBarChartLayout = MonarchBarChartLayout.Stacked,
        onSelect: ((Int) -> Unit)? = null,
        labels: MonarchStackedBarChartLabels = MonarchStackedBarChartLabels(),
    ) {
        MonarchStackedBarChart(
            title = "Tickets",
            categories = quarter,
            series = tickets,
            categoryTitle = "Month",
            layout = layout,
            offset = offset,
            referenceLines = listOf(MonarchBarChartReferenceLine(150.0, "Target")),
            height = 160.dp,
            labels = labels,
            formatValue = count,
            onSelect = onSelect,
        )
    }

    @Test
    fun theNormalOffsetStacksTheValuesWithATotal() {
        rule.setMonarchContent { Chart() }
        rule.onNodeWithContentDescription("Jan: Resolved 120, Pending 22, Escalated 8, Total 150").assertExists()
        val summary = rule.onNodeWithContentDescription("Tickets").stateDescription().orEmpty()
        assertEquals(true, summary.endsWith("Target 150"), summary)
    }

    @Test
    fun theExpandOffsetReadsSharesOfOneHundredPercent() {
        rule.setMonarchContent { Chart(offset = MonarchStackedBarChartOffset.Expand) }
        rule.onNodeWithContentDescription("Jan: Resolved 80%, Pending 14.7%, Escalated 5.3%, Total 100%")
            .assertExists()
        rule.onNodeWithContentDescription("Feb: Resolved 84.6%, Pending 11.5%, Escalated 3.8%, Total 100%")
            .assertExists()
    }

    @Test
    fun theExpandSummaryGivesAverageAndLargestShares() {
        rule.setMonarchContent { Chart(offset = MonarchStackedBarChartOffset.Expand) }
        assertEquals(
            "3 categories, Jan to Mar. Resolved: average share 78.4%, largest Feb, 84.6%. " +
                "Pending: average share 15.7%, largest Mar, 21%. Escalated: average share 5.9%, largest Mar, 8.4%",
            rule.onNodeWithContentDescription("Tickets").stateDescription(),
        )
    }

    @Test
    fun hidingAPartLeavesTheOtherSharesAsTheyWere() {
        rule.setMonarchContent { Chart(offset = MonarchStackedBarChartOffset.Expand) }
        rule.onNode(hasText("Escalated") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Jan: Resolved 80%, Pending 14.7%, Total 94.7%").assertExists()
        val summary = rule.onNodeWithContentDescription("Tickets").stateDescription().orEmpty()
        assertEquals(false, "Escalated" in summary, summary)
    }

    @Test
    fun aControlledHiddenSetIsReported() {
        var reported: Set<String>? = null
        rule.setMonarchContent {
            MonarchStackedBarChart(
                title = "Tickets",
                categories = quarter,
                series = tickets,
                offset = MonarchStackedBarChartOffset.Expand,
                hiddenSeries = emptySet(),
                onHiddenSeriesChange = { reported = it },
            )
        }
        rule.onNode(hasText("Pending") and hasClickAction()).performClick()
        assertEquals(setOf("pending"), reported)
        rule.onNodeWithContentDescription("Jan: Resolved 80%, Pending 14.7%, Escalated 5.3%, Total 100%")
            .assertExists()
    }

    @Test
    fun groupedBarsIgnoreTheOffset() {
        rule.setMonarchContent {
            Chart(offset = MonarchStackedBarChartOffset.Expand, layout = MonarchBarChartLayout.Grouped)
        }
        rule.onNodeWithContentDescription("Jan: Resolved 120, Pending 22, Escalated 8").assertExists()
    }

    @Test
    fun theTableListsSharesWithTheExpandOffset() {
        rule.setMonarchContent { Chart(offset = MonarchStackedBarChartOffset.Expand) }
        rule.onNodeWithText("View as table").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Month, Resolved, Pending, Escalated, Total").assertExists()
        rule.onNodeWithContentDescription("Jan, Resolved 80%, Pending 14.7%, Escalated 5.3%, Total 100%")
            .assertExists()
    }

    @Test
    fun drillInGivesTheCategoryIndexWithTheExpandOffset() {
        val selected = mutableListOf<Int>()
        rule.setMonarchContent { Chart(offset = MonarchStackedBarChartOffset.Expand, onSelect = { selected += it }) }
        val node = rule.onNodeWithContentDescription("Mar: Resolved 70.6%, Pending 21%, Escalated 8.4%, Total 100%")
            .fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].single()
        assertEquals("Details, Mar", action.label)
        rule.runOnIdle { action.action() }
        assertEquals(listOf(2), selected)
    }

    @Test
    fun rightToLeftKeepsTheCategoryOrder() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            Chart(offset = MonarchStackedBarChartOffset.Expand)
        }
        val jan = rule.onNodeWithContentDescription("Jan: Resolved 80%, Pending 14.7%, Escalated 5.3%, Total 100%")
        val feb = rule.onNodeWithContentDescription("Feb: Resolved 84.6%, Pending 11.5%, Escalated 3.8%, Total 100%")
        jan.assertIsBefore(feb, LayoutDirection.Rtl)
    }

    @Test
    fun theTitleIsFullyVisibleAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchStackedBarChart(
                title = "Tickets by status this quarter",
                categories = quarter,
                series = tickets,
                offset = MonarchStackedBarChartOffset.Expand,
                height = 120.dp,
            )
        }
        rule.onNodeWithText("Tickets by status this quarter").assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Chart(
                offset = MonarchStackedBarChartOffset.Expand,
                labels = MonarchStackedBarChartLabels(
                    bar = MonarchBarChartLabels(total = "Gesamt"),
                    shareSeries = "%1\$s: Schnitt %2\$s, Spitze %3\$s %4\$s",
                ),
            )
        }
        rule.onNodeWithContentDescription("Jan: Resolved 80%, Pending 14.7%, Escalated 5.3%, Gesamt 100%")
            .assertExists()
        val summary = rule.onNodeWithContentDescription("Tickets").stateDescription().orEmpty()
        assertEquals(true, "Resolved: Schnitt 78.4%, Spitze Feb 84.6%" in summary, summary)
    }

    @Test
    fun sharesCountNegativeValuesAsZeroAndKeepGaps() {
        val shares = MonarchStackedBarChartShares.shares(
            listOf(
                MonarchBarChartSeries("a", "A", listOf(-4.0, null, 0.0, Double.NaN)),
                MonarchBarChartSeries("b", "B", listOf(6.0, 3.0, 0.0, 2.0)),
            ),
            categoryCount = 4,
        )
        assertEquals(listOf(0.0, null, 0.0, null), shares[0].values)
        assertEquals(listOf(1.0, 1.0, 0.0, 1.0), shares[1].values)
        assertEquals(listOf("a", "b"), shares.map { it.key })
        assertEquals(0.0..1.0, MonarchStackedBarChartTokens.ShareRange)
    }

    @Test
    fun theShareSummaryIsPure() {
        val percent: (Double) -> String = { String.format(Locale.US, "%.0f%%", it * 100) }
        val words = MonarchStackedBarChartWords(
            count = "2 categories",
            range = "%1\$s to %2\$s",
            shareSeries = "%1\$s: average share %2\$s, largest %3\$s, %4\$s",
            categoryValues = "%1\$s: %2\$s",
            noData = "No data",
        )
        val shares = listOf(
            MonarchBarChartSeries("a", "A", listOf(0.25, 0.75)),
            MonarchBarChartSeries("b", "B", listOf(null, null)),
        )
        assertEquals(
            "2 categories, Q1 to Q2. A: average share 50%, largest Q2, 75%. B: No data",
            MonarchStackedBarChartShares.summary(listOf("Q1", "Q2"), shares, listOf(0, 1), percent, words),
        )
        assertEquals("No data", MonarchStackedBarChartShares.summary(emptyList(), shares, listOf(0), percent, words))
    }
}
