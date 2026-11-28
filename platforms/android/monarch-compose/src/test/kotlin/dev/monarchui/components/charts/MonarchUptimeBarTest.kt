package dev.monarchui.components.charts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBar
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBarDetail
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBarLabels
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBarSegment
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBarSize
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBarStatus
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBarText
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBarThresholds
import dev.monarchui.components.charts.uptimebar.MonarchUptimeBarTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val week = listOf(
    MonarchUptimeBarSegment("1", "Mar 1", uptime = 100.0),
    MonarchUptimeBarSegment(
        "2",
        "Mar 2",
        uptime = 99.42,
        details = listOf(MonarchUptimeBarDetail("Checks", "1,440")),
        note = "Slow responses",
    ),
    MonarchUptimeBarSegment("3", "Mar 3"),
    MonarchUptimeBarSegment("4", "Mar 4", uptime = 96.4, note = "Database failover"),
    MonarchUptimeBarSegment("5", "Mar 5", status = MonarchUptimeBarStatus.Maintenance),
)
private val month = (1..30).map { day ->
    MonarchUptimeBarSegment(
        "2026-03-$day",
        "Mar $day",
        uptime = when (day) {
            12 -> 99.42
            24 -> 96.4
            else -> 100.0
        },
    )
}

internal object MonarchUptimeBarFixtures : ComponentFixtures {
    override val id = "uptime-bar"
    override val fixtures = listOf(
        fixture("thirty days with labels and the overall line") {
            MonarchUptimeBar(
                segments = month,
                label = "Search API",
                description = "Last 30 days",
                startLabel = "30 days ago",
                endLabel = "Today",
            )
        },
        fixture("every state, small, with the legend") {
            MonarchUptimeBar(segments = week, size = MonarchUptimeBarSize.Sm, showLegend = true)
        },
        fixture("inspected, with Open") {
            MonarchUptimeBar(
                segments = week,
                label = "Website",
                activeIndex = 1,
                onActiveIndexChange = {},
                showOverallUptime = false,
                onSegmentSelect = {},
            )
        },
        fixture("ninety thin days") {
            MonarchUptimeBar(segments = (1..90).map { MonarchUptimeBarSegment("$it", "Day $it", uptime = 100.0) })
        },
        fixture("long label and description") {
            MonarchUptimeBar(
                segments = week,
                label = "Payments gateway for every region and every currency",
                description = "The last five days, measured every minute from three locations",
            )
        },
        fixture("no periods") {
            MonarchUptimeBar(segments = emptyList(), label = "New service")
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchUptimeBarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theBarIsNamedByTheLabelAndReadsTheSummary() {
        rule.setMonarchContent { MonarchUptimeBar(segments = week, label = "Website") }
        val bar = rule.onNodeWithContentDescription("Website")
        bar.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        assertEquals(
            "Periods: 5. Operational 1, Degraded 1, Down 1, Maintenance 1, No data 1. Uptime 98.61%",
            bar.stateDescription(),
        )
    }

    @Test
    fun withoutALabelTheBarIsCalledUptime() {
        rule.setMonarchContent { MonarchUptimeBar(segments = month) }
        assertEquals(
            "Periods: 30. Operational 28, Degraded 1, Down 1. Uptime 99.86%",
            rule.onNodeWithContentDescription("Uptime").stateDescription(),
        )
    }

    @Test
    fun theSummaryCanBeReplacedOrWritten() {
        rule.setMonarchContent {
            MonarchUptimeBar(
                segments = week,
                label = "Website",
                labels = MonarchUptimeBarLabels(
                    summary = { count, counts, overall -> "$count, ${counts.size}, $overall" },
                ),
            )
        }
        assertEquals("5, 5, 98.61%", rule.onNodeWithContentDescription("Website").stateDescription())
    }

    @Test
    fun eachPeriodIsAnElementInOrderWithItsDetails() {
        rule.setMonarchContent { MonarchUptimeBar(segments = week, label = "Website") }
        val first = rule.onNodeWithContentDescription("Mar 1: Operational, Uptime 100.00%")
        val second = rule.onNodeWithContentDescription("Mar 2: Degraded, Uptime 99.42%, Checks 1,440, Slow responses")
        rule.onNodeWithContentDescription("Mar 3: No data").assertExists()
        rule.onNodeWithContentDescription("Mar 4: Down, Uptime 96.40%, Database failover").assertExists()
        rule.onNodeWithContentDescription("Mar 5: Maintenance").assertExists()
        first.assertIsBefore(second, LayoutDirection.Ltr)
        val bounds = second.getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom - bounds.top >= 48.dp, "the touch area is at least 48dp high: $bounds")
        assertTrue(SemanticsActions.CustomActions !in second.fetchSemanticsNode().config)
    }

    @Test
    fun rightToLeftPutsTheOldestPeriodAtTheRight() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { MonarchUptimeBar(segments = week) }
        val first = rule.onNodeWithContentDescription("Mar 1: Operational, Uptime 100.00%")
        first.assertIsBefore(rule.onNodeWithContentDescription("Mar 3: No data"), LayoutDirection.Rtl)
        val bar = rule.onNodeWithContentDescription("Uptime").getUnclippedBoundsInRoot()
        assertTrue(first.getUnclippedBoundsInRoot().left > (bar.left + bar.right) / 2)
    }

    @Test
    fun aTapOpensTheDetailsOfThePeriodWithOpen() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchUptimeBar(segments = week, onSegmentSelect = { opened += it.id })
        }
        rule.onNodeWithContentDescription("Mar 4: Down, Uptime 96.40%, Database failover")
            .performTouchInput { click(center) }
        rule.waitForIdle()
        rule.onNodeWithText("Mar 4").assertExists()
        rule.onNodeWithText("Database failover").assertExists()
        rule.onNode(hasText("Open") and hasClickAction()).performClick()
        assertEquals(listOf("4"), opened)
    }

    @Test
    fun theOpenActionOfAPeriodCallsOnSegmentSelect() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent { MonarchUptimeBar(segments = week, onSegmentSelect = { opened += it.id }) }
        val node = rule.onNodeWithContentDescription("Mar 3: No data").fetchSemanticsNode()
        val action = node.config[SemanticsActions.CustomActions].single()
        assertEquals("Open, Mar 3", action.label)
        rule.runOnIdle { action.action() }
        assertEquals(listOf("3"), opened)
    }

    @Test
    fun aControlledInspectionReportsTheTappedPeriod() {
        var active by mutableStateOf<Int?>(null)
        val reported = mutableListOf<Int?>()
        rule.setMonarchContent {
            MonarchUptimeBar(segments = week, activeIndex = active, onActiveIndexChange = { reported += it })
        }
        rule.onNodeWithContentDescription("Mar 5: Maintenance").performTouchInput { click(center) }
        rule.waitForIdle()
        assertEquals(listOf<Int?>(4), reported)
        active = 4
        rule.waitForIdle()
        rule.onNodeWithText("Mar 5").assertExists()
    }

    @Test
    fun theLegendNamesEveryStateAndTheNamesCanBeReplaced() {
        rule.setMonarchContent {
            MonarchUptimeBar(
                segments = week,
                showLegend = true,
                labels = MonarchUptimeBarLabels(
                    statusNames = mapOf(MonarchUptimeBarStatus.Down to "Ausfall"),
                    legend = "Legende",
                    uptime = "Verfügbarkeit",
                    periods = "Zeiträume",
                ),
            )
        }
        val legend = rule.onNodeWithContentDescription("Legende")
        legend.assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
        rule.onNodeWithText("Ausfall").assertExists()
        rule.onNodeWithText("Partial outage").assertExists()
        assertEquals(
            "Zeiträume: 5. Operational 1, Degraded 1, Ausfall 1, Maintenance 1, No data 1. Verfügbarkeit 98.61%",
            rule.onNodeWithContentDescription("Verfügbarkeit").stateDescription(),
        )
    }

    @Test
    fun theDescriptionWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchUptimeBar(
                segments = week,
                label = "Payments",
                description = "The last five days, measured every minute from three locations",
            )
        }
        rule.onNodeWithText("The last five days, measured every minute from three locations").assertTextFullyVisible()
    }

    @Test
    fun aStatusWinsOverTheUptimeAndThresholdsDecideTheRest() {
        val thresholds = MonarchUptimeBarThresholds()
        fun status(segment: MonarchUptimeBarSegment) = MonarchUptimeBarText.status(segment, thresholds)
        assertEquals(MonarchUptimeBarStatus.Operational, status(MonarchUptimeBarSegment("a", "A", uptime = 99.9)))
        assertEquals(MonarchUptimeBarStatus.Degraded, status(MonarchUptimeBarSegment("a", "A", uptime = 99.0)))
        assertEquals(MonarchUptimeBarStatus.Down, status(MonarchUptimeBarSegment("a", "A", uptime = 98.99)))
        assertEquals(MonarchUptimeBarStatus.NoData, status(MonarchUptimeBarSegment("a", "A", uptime = Double.NaN)))
        assertEquals(
            MonarchUptimeBarStatus.Partial,
            status(MonarchUptimeBarSegment("a", "A", MonarchUptimeBarStatus.Partial, uptime = 100.0)),
        )
        val strict = MonarchUptimeBarThresholds(operational = 100.0, degraded = 99.99)
        assertEquals(
            MonarchUptimeBarStatus.Down,
            MonarchUptimeBarText.status(MonarchUptimeBarSegment("a", "A", uptime = 99.9), strict),
        )
    }

    @Test
    fun theOverallUptimeAveragesOnlyMeasuredPeriods() {
        assertEquals(98.60666666666667, MonarchUptimeBarText.overall(week, null)!!, 1e-9)
        assertEquals(99.5, MonarchUptimeBarText.overall(week, 99.5))
        assertNull(MonarchUptimeBarText.overall(listOf(MonarchUptimeBarSegment("a", "A")), null))
        val statuses = week.map { MonarchUptimeBarText.status(it, MonarchUptimeBarThresholds()) }
        assertEquals(5, MonarchUptimeBarText.counts(statuses).size)
        val empty = List(2) { MonarchUptimeBarStatus.NoData }
        assertEquals(mapOf(MonarchUptimeBarStatus.NoData to 2), MonarchUptimeBarText.counts(empty))
    }

    @Test
    fun aTapFindsTheSegmentUnderTheFingerAndThinSegmentsShareSmallerGaps() {
        assertEquals(0, MonarchUptimeBarText.indexAt(0f, 100f, 4))
        assertEquals(1, MonarchUptimeBarText.indexAt(30f, 100f, 4))
        assertEquals(3, MonarchUptimeBarText.indexAt(100f, 100f, 4))
        assertNull(MonarchUptimeBarText.indexAt(120f, 100f, 4))
        assertNull(MonarchUptimeBarText.indexAt(10f, 100f, 0))
        val wide = MonarchUptimeBarText.spans(4, 106f, 2f, 1f, 4f)
        assertEquals(listOf(0f to 25f, 27f to 25f, 54f to 25f, 81f to 25f), wide)
        val thin = MonarchUptimeBarText.spans(10, 39f, 2f, 1f, 4f)
        assertEquals(3f, thin[0].second)
        assertEquals(4f, thin[1].first)
        assertTrue(MonarchUptimeBarText.spans(0, 100f, 2f, 1f, 4f).isEmpty())
    }

    @Test
    fun coloursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            fun fill(status: MonarchUptimeBarStatus) = MonarchUptimeBarTokens.statusColors(status, colors)
            assertEquals(
                androidx.compose.ui.graphics.lerp(
                    colors.successSolid, colors.bgSurface, MonarchUptimeBarTokens.OPERATIONAL_SOFTEN,
                ),
                fill(MonarchUptimeBarStatus.Operational).fill,
                theme.id,
            )
            assertEquals(colors.warningSolid, fill(MonarchUptimeBarStatus.Degraded).fill, theme.id)
            assertEquals(
                androidx.compose.ui.graphics.lerp(
                    colors.warningSolid, colors.dangerSolid, MonarchUptimeBarTokens.PARTIAL_STEP,
                ),
                fill(MonarchUptimeBarStatus.Partial).fill,
                theme.id,
            )
            assertEquals(colors.dangerSolid, fill(MonarchUptimeBarStatus.Down).fill, theme.id)
            assertEquals(colors.infoSolid, fill(MonarchUptimeBarStatus.Maintenance).fill, theme.id)
            assertEquals(colors.bgMuted, fill(MonarchUptimeBarStatus.NoData).fill, theme.id)
            assertEquals(colors.borderDefault, fill(MonarchUptimeBarStatus.NoData).outline, theme.id)
            assertNull(fill(MonarchUptimeBarStatus.Down).outline)
            assertEquals(colors.textDefault to colors.bgSurface, MonarchUptimeBarTokens.ring(colors), theme.id)
            assertEquals(colors.textMuted, MonarchUptimeBarTokens.mutedText(colors), theme.id)
        }
    }
}
