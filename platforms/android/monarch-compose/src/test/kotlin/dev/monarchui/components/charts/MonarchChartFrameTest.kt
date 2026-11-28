package dev.monarchui.components.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.charts.chartframe.MonarchChartFrame
import dev.monarchui.components.charts.chartframe.MonarchChartFrameLabels
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartframe.MonarchChartFrameTokens
import dev.monarchui.components.charts.chartframe.MonarchChartTable
import dev.monarchui.components.charts.chartframe.MonarchChartTableSeries
import dev.monarchui.components.charts.chartlegend.MonarchChartLegendItem
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val signups = MonarchChartTable(
    categoryTitle = "Week",
    categories = listOf("W1", "W2", "W3"),
    series = listOf(
        MonarchChartTableSeries("Signups", listOf(32.0, null, 52.0)),
        MonarchChartTableSeries("Revenue", listOf(1200.0, 900.0, 1500.0), listOf("$1,200", "$900", "$1,500")),
    ),
)
private val legend = listOf(
    MonarchChartLegendItem("signups", "Signups"),
    MonarchChartLegendItem("revenue", "Revenue"),
)
private val ChartHeight = 96.dp

internal object MonarchChartFrameFixtures : ComponentFixtures {
    override val id = "chart-frame"
    override val fixtures = listOf(
        fixture("ready, with legend, table and footer") {
            MonarchChartFrame(
                title = "Signups",
                description = "Last three weeks",
                table = signups,
                legend = legend,
                footer = "Source: billing",
                height = ChartHeight,
            ) { Box(Modifier.fillMaxSize()) }
        },
        fixture("ready, toggle legend and actions") {
            MonarchChartFrame(
                title = "Revenue",
                legend = legend,
                hiddenSeries = setOf("revenue"),
                onHiddenSeriesChange = {},
                height = ChartHeight,
                actions = { MonarchButton(text = "Week", onClick = {}, size = MonarchButtonSize.Sm) },
            ) { Box(Modifier.fillMaxSize()) }
        },
        fixture("loading") {
            MonarchChartFrame(title = "Signups", status = MonarchChartFrameStatus.Loading, height = ChartHeight) {}
        },
        fixture("empty") {
            MonarchChartFrame(
                title = "Signups",
                status = MonarchChartFrameStatus.Empty,
                emptyMessage = "Signups appear here after the first week.",
                height = ChartHeight,
            ) {}
        },
        fixture("error with retry") {
            MonarchChartFrame(
                title = "Signups",
                status = MonarchChartFrameStatus.Error,
                onRetry = {},
                height = ChartHeight,
            ) {}
        },
        fixture("error without retry") {
            MonarchChartFrame(title = "Signups", status = MonarchChartFrameStatus.Error, height = ChartHeight) {}
        },
        fixture("long title") {
            MonarchChartFrame(
                title = "Monthly recurring revenue by region and plan, compared with the same month last year",
                height = ChartHeight,
            ) { Box(Modifier.fillMaxSize()) }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchChartFrameTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndTheChartKeepsItsHeight() {
        rule.setMonarchContent {
            MonarchChartFrame(title = "Signups", description = "Last three weeks", height = 120.dp) {
                Box(Modifier.fillMaxSize().testTag("chart"))
            }
        }
        rule.onNode(hasText("Signups") and isHeading()).assertExists()
        rule.onNodeWithText("Last three weeks").assertExists()
        rule.onNodeWithTag("chart").assertHeightIsEqualTo(120.dp)
        rule.onNodeWithText("View as table").assertDoesNotExist()
    }

    @Test
    fun viewAsTableShowsTheDataReadRowByRow() {
        rule.setMonarchContent {
            MonarchChartFrame(title = "Signups", table = signups, legend = legend, height = ChartHeight) {
                Text("the chart")
            }
        }
        rule.onNode(hasText("View as table") and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        rule.onNodeWithText("the chart").assertDoesNotExist()
        rule.onNodeWithContentDescription("Week, Signups, Revenue").assert(isHeading())
        rule.onNodeWithContentDescription("W1, Signups 32, Revenue $1,200").assertExists()
        rule.onNodeWithContentDescription("W2, Signups No value, Revenue $900").assertExists()
        rule.onNodeWithContentDescription("Legend").assertDoesNotExist()
        rule.onNodeWithText("View as chart").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("the chart").assertExists()
        rule.onNodeWithContentDescription("Legend").assertExists()
    }

    @Test
    fun loadingIsASkeletonNamedForTalkBack() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchChartFrame(title = "Signups", status = MonarchChartFrameStatus.Loading, table = signups) {
                Text("the chart")
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading chart")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        rule.onNodeWithText("the chart").assertDoesNotExist()
        rule.onNodeWithText("View as table").assertDoesNotExist()
    }

    @Test
    fun emptyShowsTheEmptyState() {
        rule.setMonarchContent {
            MonarchChartFrame(
                title = "Signups",
                status = MonarchChartFrameStatus.Empty,
                emptyMessage = "Signups appear here after the first week.",
            ) {}
        }
        rule.onNodeWithText("No data yet").assertExists()
        rule.onNodeWithText("Signups appear here after the first week.").assertExists()
    }

    @Test
    fun anErrorOffersTryAgainWhenItCanRetry() {
        var retries = 0
        rule.setMonarchContent {
            MonarchChartFrame(title = "Signups", status = MonarchChartFrameStatus.Error, onRetry = { retries++ }) {}
        }
        rule.onNodeWithText("This chart didn't load.", substring = true).assertExists()
        rule.onNode(hasText("Try again") and hasClickAction()).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun anErrorWithoutRetryIsAPoliteLiveRegion() {
        rule.setMonarchContent {
            MonarchChartFrame(title = "Signups", status = MonarchChartFrameStatus.Error) {}
        }
        rule.onNodeWithText("This chart didn't load.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("Try again").assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchChartFrame(
                title = "Inscriptions",
                table = signups,
                legend = legend,
                labels = MonarchChartFrameLabels(
                    viewTable = "Voir le tableau",
                    viewChart = "Voir le graphique",
                    legend = "Légende",
                    noValue = "Aucune valeur",
                ),
            ) {}
        }
        rule.onNodeWithContentDescription("Légende").assertExists()
        rule.onNodeWithText("Voir le tableau").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Voir le graphique").assertExists()
        rule.onNodeWithContentDescription("W2, Signups Aucune valeur, Revenue $900").assertExists()
    }

    @Test
    fun theTitleWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchChartFrame(title = "Signups by week and region", height = ChartHeight) {}
        }
        rule.onNodeWithText("Signups by week and region", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesDecideWhatIsShown() {
        val ready = MonarchChartFrameStatus.Ready
        assertTrue(MonarchChartFrameTokens.showsLegend(ready, showTable = false, legendCount = 2))
        assertFalse(MonarchChartFrameTokens.showsLegend(ready, showTable = true, legendCount = 2))
        assertFalse(MonarchChartFrameTokens.showsLegend(MonarchChartFrameStatus.Loading, false, 2))
        assertTrue(MonarchChartFrameTokens.showsTableToggle(ready, signups, tableToggle = true))
        assertFalse(MonarchChartFrameTokens.showsTableToggle(ready, signups, tableToggle = false))
        assertFalse(MonarchChartFrameTokens.showsTableToggle(ready, null, tableToggle = true))
        assertFalse(MonarchChartFrameTokens.showsTableToggle(MonarchChartFrameStatus.Error, signups, true))
        val format: (Double) -> String = { it.toInt().toString() }
        assertEquals("32", MonarchChartFrameTokens.cellText(signups.series[0], 0, format))
        assertEquals("—", MonarchChartFrameTokens.cellText(signups.series[0], 1, format))
        assertEquals("$1,200", MonarchChartFrameTokens.cellText(signups.series[1], 0, format))
        assertEquals("No value", MonarchChartFrameTokens.spokenCell(signups.series[0], 1, "No value", format))
        assertEquals(240.dp, MonarchChartFrameTokens.DefaultHeight)
    }
}
