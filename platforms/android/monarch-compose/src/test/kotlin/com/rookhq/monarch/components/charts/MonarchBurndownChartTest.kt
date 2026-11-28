package com.rookhq.monarch.components.charts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.rookhq.monarch.components.charts.burndownchart.MonarchBurndownChart
import com.rookhq.monarch.components.charts.burndownchart.MonarchBurndownChartData
import com.rookhq.monarch.components.charts.burndownchart.MonarchBurndownChartLabels
import com.rookhq.monarch.components.charts.burndownchart.MonarchBurndownChartLine
import com.rookhq.monarch.components.charts.burndownchart.MonarchBurndownChartPoint
import com.rookhq.monarch.components.charts.burndownchart.MonarchBurndownChartTokens
import com.rookhq.monarch.components.charts.burndownchart.MonarchBurndownChartTrend
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameLabels
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
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
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val sprint = listOf(
    MonarchBurndownChartPoint("Mon", remaining = 40.0, scope = 40.0),
    MonarchBurndownChartPoint("Tue", remaining = 38.0, scope = 40.0),
    MonarchBurndownChartPoint("Wed", remaining = 37.0, scope = 45.0),
    MonarchBurndownChartPoint("Thu", remaining = 31.0, scope = 43.0),
    MonarchBurndownChartPoint("Fri"),
    MonarchBurndownChartPoint("Sat"),
)
private val finished = listOf(30.0, 24.0, 18.0, 11.0, 3.0, 0.0).mapIndexed { i, left ->
    MonarchBurndownChartPoint("Day ${i + 1}", remaining = left)
}
private val burndownWhole: (Double) -> String = {
    BigDecimal(it).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
private const val BurndownMarch1 = 1_772_323_200_000L
private const val BurndownDay = 86_400_000L
private val BurndownPlotHeight = 120.dp
private const val SprintSummary =
    "Thu: 31 remaining, ideal 16, 15 behind ideal. Scope 43, 5 added and 2 removed since the start"

internal object MonarchBurndownChartFixtures : ComponentFixtures {
    override val id = "burndown-chart"
    override val fixtures = listOf(
        fixture("in progress, scope changes, today") {
            MonarchBurndownChart(
                title = "Sprint 14",
                points = sprint,
                description = "Two weeks",
                height = BurndownPlotHeight,
            )
        },
        fixture("finished, without scope") {
            MonarchBurndownChart(
                title = "Sprint 13",
                points = finished,
                description = "Closed on Friday",
                height = BurndownPlotHeight,
            )
        },
        fixture("dated days, inspected, with details") {
            MonarchBurndownChart(
                title = "Sprint 15",
                description = "One week",
                points = List(4) { MonarchBurndownChartPoint(BurndownMarch1 + it * BurndownDay, 20.0 - it * 6) },
                activeIndex = 2,
                onActiveIndexChange = {},
                tableToggle = false,
                height = BurndownPlotHeight,
                onSelect = {},
            )
        },
        fixture("not started, scope hidden") {
            MonarchBurndownChart(
                title = "Sprint 16",
                points = listOf(MonarchBurndownChartPoint("Mon", scope = 30.0), MonarchBurndownChartPoint("Tue")),
                showScope = false,
                showToday = false,
                height = BurndownPlotHeight,
            )
        },
        fixture("loading") {
            MonarchBurndownChart(
                title = "Sprint 14",
                points = sprint,
                status = MonarchChartFrameStatus.Loading,
                height = BurndownPlotHeight,
            )
        },
        fixture("empty") {
            MonarchBurndownChart(
                title = "Sprint 14",
                points = emptyList(),
                emptyMessage = "The burndown starts on the first day of the sprint.",
                height = BurndownPlotHeight,
            )
        },
        fixture("error with retry") {
            MonarchBurndownChart(
                title = "Sprint 14",
                points = sprint,
                status = MonarchChartFrameStatus.Error,
                height = BurndownPlotHeight,
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchBurndownChartTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun Chart(
        points: List<MonarchBurndownChartPoint> = sprint,
        onSelect: ((Int) -> Unit)? = null,
        labels: MonarchBurndownChartLabels = MonarchBurndownChartLabels(),
    ) {
        MonarchBurndownChart(
            title = "Sprint 14",
            points = points,
            height = 180.dp,
            labels = labels,
            formatValue = burndownWhole,
            onSelect = onSelect,
        )
    }

    @Test
    fun theSummaryIsTheDescriptionAndThePlotsState() {
        rule.setMonarchContent { Chart() }
        rule.onNodeWithText(SprintSummary).assertExists()
        val plot = rule.onNodeWithContentDescription("Sprint 14")
        plot.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        assertEquals(SprintSummary, plot.stateDescription())
    }

    @Test
    fun theAppsDescriptionAndSummaryReplaceTheGeneratedOne() {
        rule.setMonarchContent {
            MonarchBurndownChart(title = "Sprint 14", points = sprint, description = "Two weeks", summary = "Late")
        }
        rule.onNodeWithText("Two weeks").assertExists()
        rule.onNodeWithText(SprintSummary).assertDoesNotExist()
        assertEquals("Late", rule.onNodeWithContentDescription("Sprint 14").stateDescription())
    }

    @Test
    fun eachDayReadsItsValuesAndScopeChangeInOrder() {
        rule.setMonarchContent { Chart() }
        val mon = rule.onNodeWithContentDescription("Mon: Remaining 40, Ideal 40, Scope 40")
        val wed = rule.onNodeWithContentDescription("Wed: Remaining 37, Ideal 24, Scope 45, Scope change +5")
        rule.onNodeWithContentDescription("Thu: Remaining 31, Ideal 16, Scope 43, Scope change −2").assertExists()
        rule.onNodeWithContentDescription("Fri: Remaining No value, Ideal 8, Scope No value").assertExists()
        mon.assertIsBefore(wed, LayoutDirection.Ltr)
    }

    @Test
    fun rightToLeftMirrorsTheDays() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Chart() }
        val mon = rule.onNodeWithContentDescription("Mon: Remaining 40, Ideal 40, Scope 40")
        val tue = rule.onNodeWithContentDescription("Tue: Remaining 38, Ideal 32, Scope 40")
        mon.assertIsBefore(tue, LayoutDirection.Rtl)
        val plot = rule.onNodeWithContentDescription("Sprint 14").getUnclippedBoundsInRoot()
        assertTrue(mon.getUnclippedBoundsInRoot().left > (plot.left + plot.right) / 2, "Mon is on the right")
    }

    @Test
    fun aTapInspectsTheDayWithDetailsAndClear() {
        val selected = mutableListOf<Int>()
        rule.setMonarchContent { Chart(onSelect = { selected += it }) }
        val wed = "Wed: Remaining 37, Ideal 24, Scope 45, Scope change +5"
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
    fun aControlledInspectionFollowsTheApp() {
        var active by mutableStateOf<Int?>(1)
        val reported = mutableListOf<Int?>()
        rule.setMonarchContent {
            MonarchBurndownChart(
                title = "Sprint 13",
                points = finished,
                activeIndex = active,
                onActiveIndexChange = { reported += it },
                height = 180.dp,
                formatValue = burndownWhole,
            )
        }
        rule.onNodeWithText("Day 2: Remaining 24, Ideal 24").assertExists()
        active = 4
        rule.waitForIdle()
        rule.onNodeWithText("Day 5: Remaining 3, Ideal 6").assertExists()
        rule.onNode(hasText("Clear") and hasClickAction()).performClick()
        assertEquals(listOf<Int?>(null), reported)
    }

    @Test
    fun theLegendHidesALineAndKeepsOneVisible() {
        val hidden = mutableListOf<Set<MonarchBurndownChartLine>>()
        rule.setMonarchContent {
            MonarchBurndownChart(
                title = "Sprint 13",
                points = finished,
                onHiddenLinesChange = { hidden += it },
                height = 180.dp,
                formatValue = burndownWhole,
            )
        }
        rule.onNode(hasText("Ideal") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Day 1: Remaining 30").assertExists()
        assertEquals(setOf(MonarchBurndownChartLine.Ideal), hidden.last())
        rule.onNode(hasText("Remaining") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Day 1: Remaining 30").assertExists()
        rule.onNode(hasText("Scope") and hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun theTableHasEveryDayWithTheScopeChange() {
        rule.setMonarchContent { Chart() }
        rule.onNodeWithText("View as table").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Day, Remaining, Ideal, Scope, Scope change").assertExists()
        rule.onNodeWithContentDescription(
            "Wed, Remaining 37, Ideal 24, Scope 45, Scope change +5",
        ).assertExists()
        rule.onNodeWithContentDescription(
            "Fri, Remaining No value, Ideal 8, Scope No value, Scope change 0",
        ).assertExists()
    }

    @Test
    fun withoutScopeThereIsNoScopeLegendOrColumn() {
        rule.setMonarchContent { Chart(points = finished) }
        rule.onNode(hasText("Scope") and hasClickAction()).assertDoesNotExist()
        rule.onNodeWithText("Day 6: 0 remaining, ideal 0, on the ideal line").assertExists()
        rule.onNodeWithText("View as table").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Day, Remaining, Ideal").assertExists()
    }

    @Test
    fun datedDaysAreWrittenInUtc() {
        rule.setMonarchContent {
            val days = listOf(BurndownMarch1, BurndownMarch1 + BurndownDay)
            Chart(points = days.map { MonarchBurndownChartPoint(it, 10.0) })
        }
        rule.onNodeWithContentDescription("Mar 1: Remaining 10, Ideal 10").assertExists()
        rule.onNodeWithContentDescription("Mar 2: Remaining 10, Ideal 0").assertExists()
    }

    @Test
    fun noPointsShowTheEmptyState() {
        rule.setMonarchContent { MonarchBurndownChart(title = "Sprint 14", points = emptyList()) }
        rule.onNodeWithText("No data yet").assertExists()
        rule.onNodeWithContentDescription("Sprint 14").assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Chart(
                labels = MonarchBurndownChartLabels(
                    remaining = "Offen",
                    ideal = "Ideal",
                    scope = "Umfang",
                    scopeChange = "Änderung",
                    change = { amount, added -> if (added) "plus $amount" else "minus $amount" },
                    status = { day, left, ideal -> "$day: $left offen, Ideal $ideal" },
                    behind = { "$it im Rückstand" },
                    scopeSummary = { scope, added, removed -> "Umfang $scope, +$added, −$removed" },
                    frame = MonarchChartFrameLabels(viewTable = "Als Tabelle", noValue = "Kein Wert"),
                ),
            )
        }
        rule.onNodeWithText("Thu: 31 offen, Ideal 16, 15 im Rückstand. Umfang 43, +5, −2").assertExists()
        rule.onNodeWithContentDescription("Wed: Offen 37, Ideal 24, Umfang 45, Änderung plus 5").assertExists()
        rule.onNodeWithContentDescription("Fri: Offen Kein Wert, Ideal 8, Umfang Kein Wert").assertExists()
        rule.onNodeWithText("Als Tabelle").assertExists()
    }

    @Test
    fun theDescriptionWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { Chart() }
        rule.onNodeWithText(SprintSummary).assertTextFullyVisible()
    }

    @Test
    fun theFiguresFollowTheWeb() {
        val data = MonarchBurndownChartData.of(sprint, idealStart = null, todayIndex = null, showToday = true)
        assertEquals(listOf(40.0, 32.0, 24.0, 16.0, 8.0, 0.0), data.ideal)
        assertEquals(listOf(0.0, 0.0, 5.0, -2.0, 0.0, 0.0), data.changes)
        assertEquals(5.0, data.added)
        assertEquals(2.0, data.removed)
        assertEquals(3, data.lastActual)
        assertEquals(3, data.today)
        assertEquals(43.0, data.lastScope)
        assertEquals(MonarchBurndownChartTrend.Behind, data.trend)
        assertEquals(15.0, data.difference)
        assertNull(MonarchBurndownChartData.of(sprint, null, 9, true).today)
        assertNull(MonarchBurndownChartData.of(sprint, null, null, false).today)
        assertEquals(1, MonarchBurndownChartData.of(sprint, null, 1, true).today)
        assertNull(MonarchBurndownChartData.of(finished, null, null, true).today, "a finished sprint has no marker")
        assertEquals(listOf(60.0, 48.0), MonarchBurndownChartData.of(sprint, 60.0, null, true).ideal.take(2))
    }

    @Test
    fun aheadOnTrackAndNotStarted() {
        val ahead = listOf(MonarchBurndownChartPoint("a", 10.0), MonarchBurndownChartPoint("b", 2.0))
        val aheadData = MonarchBurndownChartData.of(ahead + MonarchBurndownChartPoint("c"), null, null, true)
        assertEquals(MonarchBurndownChartTrend.Ahead, aheadData.trend)
        val noise = listOf(MonarchBurndownChartPoint("a", 0.3), MonarchBurndownChartPoint("b", 0.1 + 0.2 - 0.3))
        assertEquals(MonarchBurndownChartTrend.OnTrack, MonarchBurndownChartData.of(noise, null, null, true).trend)
        val waiting = MonarchBurndownChartData.of(listOf(MonarchBurndownChartPoint("a", scope = 8.0)), null, null, true)
        assertNull(waiting.trend)
        assertEquals(-1, waiting.lastActual)
        assertEquals(listOf(8.0), waiting.ideal)
        val none = MonarchBurndownChartData.of(listOf(MonarchBurndownChartPoint("a")), null, null, true)
        assertEquals(listOf<Double?>(null), none.ideal)
    }

    @Test
    fun notStartedIsSaid() {
        rule.setMonarchContent {
            Chart(points = listOf(MonarchBurndownChartPoint("Mon", scope = 30.0), MonarchBurndownChartPoint("Tue")))
        }
        rule.onNodeWithText("Not started. Scope 30, 0 added and 0 removed since the start").assertExists()
    }

    @Test
    fun coloursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val parts = MonarchBurndownChartTokens.colors(colors)
            assertEquals(colors.data1, parts.remaining, theme.id)
            assertEquals(colors.textSubtle, parts.ideal, theme.id)
            assertEquals(colors.data2, parts.scope, theme.id)
            assertEquals(colors.textDefault, parts.changeText, theme.id)
            assertEquals(colors.borderStrong, parts.today, theme.id)
            assertEquals(colors.textMuted, parts.todayText, theme.id)
        }
    }
}
