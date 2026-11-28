package com.rookhq.monarch.components.charts

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameLabels
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor.Neutral
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChart
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartLabels
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartLegendContent
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartSegment
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartSlice
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartSlices
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartSort
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartTokens
import com.rookhq.monarch.components.charts.donutchart.MonarchDonutChartWords
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
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
import kotlin.test.assertTrue

private val assets = listOf(
    MonarchDonutChartSegment("tablet", "Tablets", 54.0),
    MonarchDonutChartSegment("laptop", "Laptops", 412.0),
    MonarchDonutChartSegment("monitor", "Monitors", 131.0),
    MonarchDonutChartSegment("phone", "Phones", 268.0),
)
private val many = (1..8).map { MonarchDonutChartSegment("s$it", "Team $it", (10 - it).toDouble()) }
private val whole: (Double) -> String = { it.roundToInt().toString() }
private val Diameter = 96.dp
private val words = MonarchDonutChartWords(
    entry = "%1\$s %2\$s (%3\$s)",
    namedValue = "%1\$s %2\$s",
    valueAndShare = "%1\$s · %2\$s",
    below = "<%1\$s",
    above = ">%1\$s",
    total = "Total",
)
private val percent: (Double) -> String = { "${(it * 100).roundToInt()}%" }

internal object MonarchDonutChartFixtures : ComponentFixtures {
    override val id = "donut-chart"
    override val fixtures = listOf(
        fixture("four segments with a center label") {
            MonarchDonutChart(title = "Assets by type", segments = assets, centerLabel = "assets", size = Diameter)
        },
        fixture("folded into Other, shares only") {
            MonarchDonutChart(
                title = "Owners",
                segments = many,
                maxSegments = 3,
                legendContent = MonarchDonutChartLegendContent.Percent,
                size = Diameter,
            )
        },
        fixture("given order, pinned neutral, long center value") {
            MonarchDonutChart(
                title = "Storage",
                segments = listOf(
                    MonarchDonutChartSegment("files", "Files", 820.0),
                    MonarchDonutChartSegment("free", "Free space on the shared drive", 640.0, Neutral),
                ),
                centerValue = "1,460 GB in total",
                centerLabel = "used and free",
                sort = MonarchDonutChartSort.None,
                legendContent = MonarchDonutChartLegendContent.Value,
                size = Diameter,
                thickness = 8.dp,
            )
        },
        fixture("no center, no legend") {
            MonarchDonutChart(
                title = "Assets",
                segments = assets,
                showCenterValue = false,
                showLegend = false,
                size = Diameter,
            )
        },
        fixture("one segment and a tiny one") {
            MonarchDonutChart(
                title = "Uptime",
                segments = listOf(
                    MonarchDonutChartSegment("up", "Up", 9999.0),
                    MonarchDonutChartSegment("down", "Down", 1.0),
                ),
                legendContent = MonarchDonutChartLegendContent.None,
                size = Diameter,
            )
        },
        fixture("loading") {
            MonarchDonutChart(
                title = "Assets",
                segments = emptyList(),
                status = MonarchChartFrameStatus.Loading,
                size = Diameter,
            )
        },
        fixture("empty: a total of zero") {
            MonarchDonutChart(
                title = "Assets",
                segments = listOf(MonarchDonutChartSegment("a", "A", 0.0)),
                emptyMessage = "Assets appear here once added.",
                size = Diameter,
            )
        },
        fixture("error with retry") {
            MonarchDonutChart(
                title = "Assets",
                segments = emptyList(),
                status = MonarchChartFrameStatus.Error,
                size = Diameter,
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDonutChartTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theDonutIsNamedByTheTitleAndReadsTheSummary() {
        rule.setMonarchContent { MonarchDonutChart(title = "Assets", segments = assets, formatValue = whole) }
        val donut = rule.onNodeWithContentDescription("Assets")
        donut.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        assertEquals(
            "Total 865. Laptops 412 (48%), Phones 268 (31%), Monitors 131 (15%), Tablets 54 (6%)",
            donut.stateDescription(),
        )
    }

    @Test
    fun theSummaryCanBeReplaced() {
        rule.setMonarchContent { MonarchDonutChart(title = "Assets", segments = assets, summary = "Mostly laptops") }
        assertEquals("Mostly laptops", rule.onNodeWithContentDescription("Assets").stateDescription())
    }

    @Test
    fun eachSliceIsAnElementAndTheCentreIsNotReadAgain() {
        rule.setMonarchContent { MonarchDonutChart(title = "Assets", segments = assets, formatValue = whole) }
        rule.onNodeWithContentDescription("Laptops 412 (48%)").assertExists()
        rule.onNodeWithContentDescription("Tablets 54 (6%)").assertExists()
        rule.onNodeWithText("865").assertDoesNotExist()
    }

    @Test
    fun theFirstSliceStartsClockwiseAndMirrorsInRightToLeft() {
        assertFirstSliceSide(LayoutDirection.Ltr) { first, center -> first > center }
    }

    @Test
    fun rightToLeftRunsTheSlicesAnticlockwise() {
        assertFirstSliceSide(LayoutDirection.Rtl) { first, center -> first < center }
    }

    private fun assertFirstSliceSide(direction: LayoutDirection, check: (Dp, Dp) -> Boolean) {
        rule.setMonarchContent(layoutDirection = direction) {
            MonarchDonutChart(title = "Assets", segments = assets, formatValue = whole)
        }
        val donut = rule.onNodeWithContentDescription("Assets").getUnclippedBoundsInRoot()
        val laptops = rule.onNodeWithContentDescription("Laptops 412 (48%)").getUnclippedBoundsInRoot()
        val center = (donut.left + donut.right) / 2
        assertTrue(check((laptops.left + laptops.right) / 2, center), "$direction: $laptops in $donut")
        assertTrue(laptops.left >= donut.left - 1.dp && laptops.right <= donut.right + 1.dp, "inside the donut")
    }

    @Test
    fun theLegendShowsValuesAndShares() {
        rule.setMonarchContent { MonarchDonutChart(title = "Assets", segments = assets, formatValue = whole) }
        rule.onNodeWithText("412 · 48%").assertExists()
        rule.onNodeWithText("Laptops").assertExists()
    }

    @Test
    fun theLegendCanShowOnlyTheShareOrNothing() {
        rule.setMonarchContent {
            MonarchDonutChart(
                title = "Assets",
                segments = assets,
                legendContent = MonarchDonutChartLegendContent.Percent,
                formatValue = whole,
            )
        }
        rule.onNodeWithText("48%").assertExists()
        rule.onNodeWithText("412 · 48%").assertDoesNotExist()
    }

    @Test
    fun smallSegmentsFoldIntoOtherAndTheTableListsEverySegment() {
        rule.setMonarchContent {
            MonarchDonutChart(title = "Owners", segments = many, maxSegments = 3, formatValue = whole)
        }
        rule.onNodeWithText("Other").assertExists()
        rule.onNodeWithText("Team 3").assertDoesNotExist()
        rule.onNodeWithContentDescription("Other 27 (61%)").assertExists()
        rule.onNodeWithText("View as table").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Segment, Value, Share").assertExists()
        rule.onNodeWithContentDescription("Team 8, Value 2, Share 5%").assertExists()
        rule.onNodeWithContentDescription("Total, Value 44, Share 100%").assertExists()
    }

    @Test
    fun aTotalOfZeroShowsTheEmptyState() {
        rule.setMonarchContent {
            MonarchDonutChart(
                title = "Assets",
                segments = listOf(
                    MonarchDonutChartSegment("a", "A", -5.0),
                    MonarchDonutChartSegment("b", "B", Double.NaN),
                ),
            )
        }
        rule.onNodeWithText("No data yet").assertExists()
        rule.onNodeWithContentDescription("Assets").assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchDonutChart(
                title = "Équipe",
                segments = many,
                maxSegments = 2,
                formatValue = whole,
                labels = MonarchDonutChartLabels(
                    frame = MonarchChartFrameLabels(viewTable = "Voir le tableau"),
                    other = "Autres",
                    segment = "Groupe",
                    value = "Valeur",
                    share = "Part",
                    total = "Somme",
                ),
            )
        }
        rule.onNodeWithText("Autres").assertExists()
        assertTrue(rule.onNodeWithContentDescription("Équipe").stateDescription().orEmpty().startsWith("Somme 44."))
        rule.onNodeWithText("Voir le tableau").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Groupe, Valeur, Part").assertExists()
        rule.onNodeWithContentDescription("Somme, Valeur 44, Part 100%").assertExists()
    }

    @Test
    fun percentDigitsAddDecimalsToShares() {
        rule.setMonarchContent {
            MonarchDonutChart(title = "Assets", segments = assets, percentDigits = 1, formatValue = whole)
        }
        rule.onNodeWithContentDescription("Laptops 412 (47.6%)").assertExists()
    }

    @Test
    fun sharesNeverRoundAwayASegment() {
        assertEquals("<1%", MonarchDonutChartSlices.share(1.0, 10000.0, 0, percent, words))
        assertEquals(">99%", MonarchDonutChartSlices.share(9999.0, 10000.0, 0, percent, words))
        assertEquals("100%", MonarchDonutChartSlices.share(5.0, 5.0, 0, percent, words))
        assertEquals("0%", MonarchDonutChartSlices.share(0.0, 5.0, 0, percent, words))
        assertEquals("0%", MonarchDonutChartSlices.share(1.0, 0.0, 0, percent, words))
        assertEquals("48%", MonarchDonutChartSlices.share(412.0, 865.0, 0, percent, words))
    }

    @Test
    fun segmentsAreCleanedSortedAndFolded() {
        val input = listOf(
            MonarchDonutChartSegment("a", "A", 1.0),
            MonarchDonutChartSegment("b", "B", 3.0),
            MonarchDonutChartSegment("c", "C", -2.0),
            MonarchDonutChartSegment("d", "D", 3.0, MonarchChartColor.Data5),
        )
        val sorted = MonarchDonutChartSlices.ordered(input, MonarchDonutChartSort.Descending)
        assertEquals(listOf("b", "d", "a", "c"), sorted.map { it.key })
        assertEquals(0.0, sorted.last().value)
        val given = MonarchDonutChartSlices.ordered(input, MonarchDonutChartSort.None)
        assertEquals(listOf("a", "b", "c", "d"), given.map { it.key })
        assertEquals(7.0, MonarchDonutChartSlices.total(sorted))
        val all = MonarchDonutChartSlices.slices(sorted, 6, "Other")
        assertEquals(listOf("b", "d", "a"), all.map { it.key })
        assertEquals(
            listOf(MonarchChartColor.Data1, MonarchChartColor.Data5, MonarchChartColor.Data3),
            all.map { it.slot },
        )
        val folded = MonarchDonutChartSlices.slices(sorted, 1, "Other")
        assertEquals(listOf("b", MonarchDonutChartSlices.OTHER_KEY), folded.map { it.key })
        assertEquals(4.0, folded.last().value)
        assertEquals(MonarchChartColor.Neutral, folded.last().slot)
    }

    @Test
    fun arcsFillTheTurnWithGaps() {
        val slices = listOf(
            MonarchDonutChartSlice("a", "A", 3.0, MonarchChartColor.Data1),
            MonarchDonutChartSlice("b", "B", 1.0, MonarchChartColor.Data2),
        )
        val (a, b) = MonarchDonutChartSlices.arcs(slices, gap = 4f, minSweep = 1f)
        assertEquals(2f, a.start)
        assertEquals(266f, a.sweep)
        assertEquals(272f, b.start)
        assertEquals(86f, b.sweep)
        val single = MonarchDonutChartSlices.arcs(slices.take(1), gap = 4f, minSweep = 1f).single()
        assertEquals(0f, single.start)
        assertEquals(360f, single.sweep)
        val tiny = MonarchDonutChartSlices.arcs(
            listOf(slices[0], MonarchDonutChartSlice("t", "T", 0.001, MonarchChartColor.Data2)),
            gap = 4f,
            minSweep = 1f,
        )
        assertTrue(tiny[1].sweep > 0f)
        assertTrue(MonarchDonutChartSlices.arcs(emptyList(), 4f, 1f).isEmpty())
        val (x, y) = MonarchDonutChartSlices.pointAt(90f, radius = 10f, center = 50f)
        assertTrue(abs(x - 60f) < 0.001f && abs(y - 50f) < 0.001f)
        val (topX, topY) = MonarchDonutChartSlices.pointAt(0f, radius = 10f, center = 50f)
        assertTrue(abs(topX - 50f) < 0.001f && abs(topY - 40f) < 0.001f)
    }

    @Test
    fun theRingKeepsItsThicknessWithinBounds() {
        assertEquals(160.dp, MonarchDonutChartTokens.DefaultSize)
        assertTrue(abs(MonarchDonutChartTokens.thickness(160.dp, null).value - 35.2f) < 0.01f)
        assertEquals(4.dp, MonarchDonutChartTokens.thickness(160.dp, 1.dp))
        assertEquals(80.dp, MonarchDonutChartTokens.thickness(160.dp, 200.dp))
        val legend: (MonarchDonutChartLegendContent) -> String? = {
            MonarchDonutChartSlices.legendValue(it, "412", "48%", words)
        }
        assertEquals("412 · 48%", legend(MonarchDonutChartLegendContent.Both))
        assertEquals("412", legend(MonarchDonutChartLegendContent.Value))
        assertEquals("48%", legend(MonarchDonutChartLegendContent.Percent))
        assertEquals(null, legend(MonarchDonutChartLegendContent.None))
    }

    @Test
    fun coloursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val parts = MonarchDonutChartTokens.colors(colors)
            assertEquals(colors.bgMuted, parts.track, theme.id)
            assertEquals(colors.textDefault, parts.centerValue, theme.id)
            assertEquals(colors.textMuted, parts.centerLabel, theme.id)
            assertEquals(colors.data1, MonarchDonutChartTokens.sliceColor(MonarchChartColor.Data1, colors), theme.id)
            assertEquals(colors.textSubtle, MonarchDonutChartTokens.sliceColor(Neutral, colors), theme.id)
        }
    }
}
