package dev.monarchui.components.charts

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.charts.chartframe.MonarchChartFrameStatus
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.components.charts.heatmap.HeatmapLayout
import dev.monarchui.components.charts.heatmap.HeatmapScale
import dev.monarchui.components.charts.heatmap.MonarchHeatmap
import dev.monarchui.components.charts.heatmap.MonarchHeatmapAxisItem
import dev.monarchui.components.charts.heatmap.MonarchHeatmapCell
import dev.monarchui.components.charts.heatmap.MonarchHeatmapLabels
import dev.monarchui.components.charts.heatmap.MonarchHeatmapTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val days = listOf(MonarchHeatmapAxisItem("Mon"), MonarchHeatmapAxisItem("Tue"))
private val hours = listOf(MonarchHeatmapAxisItem("9:00"), MonarchHeatmapAxisItem("10:00"))
private val tickets = listOf(
    MonarchHeatmapCell("Mon", "9:00", 12.0),
    MonarchHeatmapCell("Mon", "10:00", 3.0),
    MonarchHeatmapCell("Tue", "9:00", 7.0, note = "Launch"),
    MonarchHeatmapCell("Wed", "9:00", 99.0),
)

internal object MonarchHeatmapFixtures : ComponentFixtures {
    override val id = "heatmap"
    override val fixtures = listOf(
        fixture("default, with a gap") {
            MonarchHeatmap(title = "Tickets", rows = days, columns = hours, cells = tickets)
        },
        fixture("color, thresholds, values, selectable") {
            MonarchHeatmap(
                title = "Hours",
                rows = days,
                columns = hours,
                cells = tickets,
                color = MonarchChartColor.Data4,
                thresholds = listOf(2.0, 5.0, 10.0),
                showValues = true,
                onSelect = {},
            )
        },
        fixture("many columns, no legend") {
            MonarchHeatmap(
                title = "Tickets",
                rows = days,
                columns = (0 until 24).map { MonarchHeatmapAxisItem("$it:00") },
                cells = (0 until 24).map { MonarchHeatmapCell("Mon", "$it:00", it.toDouble()) },
                showLegend = false,
            )
        },
        fixture("long row labels") {
            MonarchHeatmap(
                title = "Hours",
                rows = listOf(MonarchHeatmapAxisItem("Platform and infrastructure"), MonarchHeatmapAxisItem("Design")),
                columns = hours,
                cells = listOf(MonarchHeatmapCell("Design", "9:00", 4.0)),
                showLegend = false,
            )
        },
        fixture("no values") {
            MonarchHeatmap(title = "Tickets", rows = days, columns = hours, cells = emptyList())
        },
        fixture("loading") {
            MonarchHeatmap(
                title = "Tickets",
                rows = days,
                columns = hours,
                cells = tickets,
                status = MonarchChartFrameStatus.Loading,
            )
        },
        fixture("error with retry") {
            MonarchHeatmap(
                title = "Tickets",
                rows = days,
                columns = hours,
                cells = tickets,
                status = MonarchChartFrameStatus.Error,
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchHeatmapTest {
    @get:Rule
    val rule = createComposeRule()

    /** A cell's element, not the details under the matrix that repeat its text. */
    private fun cell(text: String) =
        hasContentDescription(text) and SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionItemInfo)

    @Test
    fun theMatrixIsNamedWithTheSummaryAndEveryCellIsRead() {
        rule.setMonarchContent { MonarchHeatmap(title = "Tickets", rows = days, columns = hours, cells = tickets) }
        rule.onNodeWithContentDescription("2 rows by 2 columns. Highest: Mon, 9:00: 12").assertExists()
        rule.onNodeWithContentDescription("Mon, 10:00: 3").assertHasNoClickAction()
        rule.onNodeWithContentDescription("Tue, 9:00: 7, Launch").assertExists()
        rule.onNodeWithContentDescription("Tue, 10:00: No data")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        rule.onNodeWithContentDescription("Legend, No data, 0, Below 3, 3 to 6, 6 to 9, 9 or more").assertExists()
    }

    @Test
    fun aTapSelectsTheCellAndWritesItsDetails() {
        val chosen = mutableListOf<MonarchHeatmapCell>()
        rule.setMonarchContent {
            MonarchHeatmap(
                title = "Tickets",
                rows = days,
                columns = hours,
                cells = tickets,
                onSelect = { chosen += it },
            )
        }
        rule.onNodeWithContentDescription("Mon, 10:00: 3").performClick()
        rule.waitForIdle()
        rule.onNode(cell("Mon, 10:00: 3")).assertIsSelected()
        rule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite) and
                SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Mon, 10:00: 3")),
        ).assertExists()
        assertEquals(listOf("Mon" to "10:00"), chosen.map { it.row to it.column })
        assertEquals(3.0, chosen.single().value)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theSelectActionChoosesACellWithTalkBack() {
        val chosen = mutableListOf<MonarchHeatmapCell>()
        rule.setMonarchContent {
            MonarchHeatmap(
                title = "Tickets",
                rows = days,
                columns = hours,
                cells = tickets,
                onSelect = { chosen += it },
            )
        }
        rule.onNodeWithContentDescription("Tue, 10:00: No data").performCustomAccessibilityActionWithLabel("Select")
        rule.waitForIdle()
        assertEquals(listOf("Tue" to "10:00"), chosen.map { it.row to it.column })
        assertNull(chosen.single().value)
        rule.onNode(cell("Tue, 10:00: No data")).assertIsSelected()
    }

    @Test
    fun viewAsTableListsEveryValue() {
        rule.setMonarchContent { MonarchHeatmap(title = "Tickets", rows = days, columns = hours, cells = tickets) }
        rule.onNode(hasText("View as table") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Row, 9:00, 10:00").assertExists()
        rule.onNodeWithContentDescription("Mon, 9:00 12, 10:00 3").assertExists()
        rule.onNodeWithContentDescription("Tue, 9:00 7, Launch, 10:00 No value").assertExists()
    }

    @Test
    fun noValueShowsTheEmptyState() {
        rule.setMonarchContent {
            MonarchHeatmap(
                title = "Tickets",
                rows = days,
                columns = hours,
                cells = listOf(MonarchHeatmapCell("Mon", "9:00", Double.NaN)),
            )
        }
        rule.onNodeWithText("No data yet").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchHeatmap(
                title = "Tickets",
                rows = days,
                columns = hours,
                cells = tickets,
                formatValue = { "${it.toInt()} t" },
                labels = MonarchHeatmapLabels(
                    less = "Moins",
                    more = "Plus",
                    noData = "Aucune donnée",
                    rowCount = { "$it lignes" },
                    columnCount = { "$it colonnes" },
                    size = "%1\$s sur %2\$s",
                    highest = "Maximum : %1\$s",
                    cell = "%1\$s à %2\$s : %3\$s",
                    rangeBelow = "Moins de %1\$s",
                    range = "De %1\$s à %2\$s",
                    rangeAbove = "%1\$s et plus",
                ),
            )
        }
        rule.onNodeWithContentDescription("2 lignes sur 2 colonnes. Maximum : Mon à 9:00 : 12 t").assertExists()
        rule.onNodeWithContentDescription("Tue à 10:00 : Aucune donnée").assertExists()
        rule.onNodeWithText("Moins", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription(
            "Legend, Aucune donnée, 0 t, Moins de 3 t, De 3 t à 6 t, De 6 t à 9 t, 9 t et plus",
        ).assertExists()
    }

    @Test
    fun cellsRunFromTheStartEdgeRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchHeatmap(title = "Tickets", rows = days, columns = hours, cells = tickets)
        }
        rule.onNodeWithContentDescription("Mon, 9:00: 12")
            .assertIsBefore(rule.onNodeWithContentDescription("Mon, 10:00: 3"), LayoutDirection.Rtl)
    }

    @Test
    fun theLegendFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchHeatmap(title = "Tickets", rows = days, columns = hours, cells = tickets)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Less", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("More", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theScaleSplitsTheLargestValueIntoQuarters() {
        val scale = HeatmapScale.of(listOf(12.0, 3.0, null, Double.NaN), null)
        assertEquals(listOf(3.0, 6.0, 9.0), scale.thresholds)
        assertNull(scale.level(null))
        assertNull(scale.level(Double.POSITIVE_INFINITY))
        assertEquals(0, scale.level(-1.0))
        assertEquals(0, scale.level(0.0))
        assertEquals(1, scale.level(2.9))
        assertEquals(2, scale.level(3.0))
        assertEquals(3, scale.level(6.0))
        assertEquals(4, scale.level(12.0))
        assertEquals(listOf(2.0, 5.0, 10.0), HeatmapScale.of(listOf(1.0), listOf(2.0, 5.0, 10.0, 20.0)).thresholds)
        val allZero = HeatmapScale.of(listOf(0.0, -2.0), null)
        assertEquals(listOf(0.0, 0.0, 0.0), allZero.thresholds)
        assertEquals(1, allZero.level(5.0))
    }

    @Test
    fun theGeometryMirrorsRightToLeft() {
        assertEquals(36f, HeatmapLayout.step(available = 400f, columns = 4, minStep = 4f, maxStep = 36f))
        assertEquals(10f, HeatmapLayout.step(available = 240f, columns = 24, minStep = 4f, maxStep = 36f))
        assertEquals(4f, HeatmapLayout.step(available = 40f, columns = 24, minStep = 4f, maxStep = 36f))
        assertEquals(3, HeatmapLayout.labelEvery(step = 10f, labelWidth = 25f))
        assertEquals(1, HeatmapLayout.labelEvery(step = 36f, labelWidth = 20f))
        assertEquals(0f, HeatmapLayout.cellLeft(0, step = 10f, cell = 8f, columns = 3, rtl = false))
        assertEquals(22f, HeatmapLayout.cellLeft(0, step = 10f, cell = 8f, columns = 3, rtl = true))
        assertEquals(2f, HeatmapLayout.cellLeft(2, step = 10f, cell = 8f, columns = 3, rtl = true))
        assertEquals(0, HeatmapLayout.columnAt(5f, step = 10f, columns = 3, rtl = false))
        assertEquals(2, HeatmapLayout.columnAt(5f, step = 10f, columns = 3, rtl = true))
        assertEquals(0, HeatmapLayout.columnAt(29f, step = 10f, columns = 3, rtl = true))
        assertNull(HeatmapLayout.columnAt(31f, step = 10f, columns = 3, rtl = false))
    }

    @Test
    fun levelsMixTheDataColourIntoTheMutedBackgroundInEveryTheme() {
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            val slot = MonarchChartColor.Data2
            assertEquals(Color.Transparent, MonarchHeatmapTokens.levelColor(null, slot, colors))
            assertEquals(colors.bgMuted, MonarchHeatmapTokens.levelColor(0, slot, colors))
            assertEquals(lerp(colors.bgMuted, colors.data2, 0.5f), MonarchHeatmapTokens.levelColor(2, slot, colors))
            assertEquals(colors.data2, MonarchHeatmapTokens.levelColor(4, slot, colors))
            val levels = (0..4).map { MonarchHeatmapTokens.levelColor(it, slot, colors) }
            assertEquals(5, levels.toSet().size, "${theme.id}: the five levels must differ")
            assertEquals(colors.borderSubtle, MonarchHeatmapTokens.emptyOutline(colors))
            assertEquals(colors.borderDefault, MonarchHeatmapTokens.swatchOutline(colors))
            assertEquals(colors.textDefault, MonarchHeatmapTokens.selectedOutline(colors))
            assertEquals(colors.bgSurface, MonarchHeatmapTokens.chipColor(colors))
            assertEquals(colors.textMuted, MonarchHeatmapTokens.labelColor(colors))
        }
    }
}
