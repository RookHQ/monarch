package com.rookhq.monarch.components.charts

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegend
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendItem
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendMarker
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartLegendTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val series = listOf(
    MonarchChartLegendItem("revenue", "Revenue", value = "$48,200"),
    MonarchChartLegendItem("costs", "Costs", value = "$31,050"),
    MonarchChartLegendItem("target", "Target", color = MonarchChartColor.Neutral, dashed = true),
)

internal object MonarchChartLegendFixtures : ComponentFixtures {
    override val id = "chart-legend"
    override val fixtures = MonarchChartLegendMarker.entries.map { marker ->
        fixture("plain, ${marker.name} markers") { MonarchChartLegend(items = series, marker = marker) }
    } + listOf(
        fixture("plain, vertical with values") {
            MonarchChartLegend(items = series, orientation = Orientation.Vertical)
        },
        fixture("plain, disabled") { MonarchChartLegend(items = series, enabled = false) },
        fixture("toggles, all shown") {
            MonarchChartLegend(items = series, hiddenSeries = emptySet(), onHiddenSeriesChange = {})
        },
        fixture("toggles, one hidden with Show all") {
            MonarchChartLegend(
                items = series,
                hiddenSeries = setOf("costs"),
                onHiddenSeriesChange = {},
                marker = MonarchChartLegendMarker.Square,
            )
        },
        fixture("toggles, vertical") {
            MonarchChartLegend(
                items = series.take(2),
                hiddenSeries = emptySet(),
                onHiddenSeriesChange = {},
                orientation = Orientation.Vertical,
            )
        },
        fixture("toggles, disabled") {
            MonarchChartLegend(
                items = series.take(2),
                hiddenSeries = setOf("costs"),
                onHiddenSeriesChange = {},
                enabled = false,
            )
        },
        fixture("seven series and long labels") {
            MonarchChartLegend(
                items = (1..7).map { MonarchChartLegendItem("s$it", "Series number $it with a long name") },
                modifier = Modifier.width(280.dp),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchChartLegendTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aPlainEntryIsOneElementWithItsLabelAndValue() {
        rule.setMonarchContent { MonarchChartLegend(items = series) }
        rule.onNode(hasText("Revenue") and hasText("$48,200")).assertExists()
        rule.onNodeWithContentDescription("Legend").assertExists()
        rule.onNode(hasText("Revenue") and hasClickAction()).assertDoesNotExist()
    }

    @Test
    fun aToggleHidesItsSeriesAndSaysSo() {
        var hidden by mutableStateOf(emptySet<String>())
        rule.setMonarchContent {
            MonarchChartLegend(items = series, hiddenSeries = hidden, onHiddenSeriesChange = { hidden = it })
        }
        val revenue = rule.onNode(hasText("Revenue") and hasClickAction())
        revenue.assertIsSelected()
        assertEquals("Shown", revenue.stateDescription())
        revenue.performClick()
        rule.waitForIdle()
        assertEquals(setOf("revenue"), hidden)
        revenue.assertIsNotSelected()
        assertEquals("Hidden", revenue.stateDescription())
    }

    @Test
    fun theLastShownSeriesCannotBeHiddenWhenOneMustStay() {
        val changes = mutableListOf<Set<String>>()
        rule.setMonarchContent {
            MonarchChartLegend(
                items = series,
                hiddenSeries = setOf("revenue", "costs"),
                onHiddenSeriesChange = { changes += it },
            )
        }
        rule.onNode(hasText("Target") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertTrue(changes.isEmpty())
    }

    @Test
    fun showAllShowsTheLegendsSeriesAndKeepsOtherKeys() {
        var hidden by mutableStateOf(setOf("costs", "other-chart"))
        rule.setMonarchContent {
            MonarchChartLegend(items = series, hiddenSeries = hidden, onHiddenSeriesChange = { hidden = it })
        }
        rule.onNodeWithText("Show all").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(setOf("other-chart"), hidden)
        rule.onNodeWithText("Show all").assertDoesNotExist()
    }

    @Test
    fun aDisabledLegendIgnoresTaps() {
        var changes = 0
        rule.setMonarchContent {
            MonarchChartLegend(
                items = series,
                hiddenSeries = emptySet(),
                onHiddenSeriesChange = { changes++ },
                enabled = false,
            )
        }
        rule.onNode(hasText("Costs") and hasClickAction()).assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchChartLegend(
                items = series,
                hiddenSeries = setOf("costs"),
                onHiddenSeriesChange = {},
                legendLabel = "Légende",
                showAllLabel = "Tout afficher",
                shownLabel = "Affiché",
                hiddenLabel = "Masqué",
            )
        }
        rule.onNodeWithContentDescription("Légende").assertExists()
        rule.onNodeWithText("Tout afficher").assertExists()
        assertEquals("Affiché", rule.onNode(hasText("Revenue") and hasClickAction()).stateDescription())
        assertEquals("Masqué", rule.onNode(hasText("Costs") and hasClickAction()).stateDescription())
    }

    @Test
    fun togglesHave48dpTargetsAndFollowTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchChartLegend(items = series.take(2), hiddenSeries = emptySet(), onHiddenSeriesChange = {})
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val revenue = rule.onNode(hasText("Revenue") and hasClickAction())
            revenue.assertTouchTargetAtLeast48dp(rule.density)
            revenue.assertIsBefore(rule.onNode(hasText("Costs") and hasClickAction()), direction)
        }
    }

    @Test
    fun labelsAreNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchChartLegend(items = listOf(MonarchChartLegendItem("a", "Returning customers this quarter")))
        }
        rule.onNodeWithText("Returning customers this quarter", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun entriesTakeTheDataSlotsInOrderThenNeutral() {
        assertEquals(MonarchChartColor.Data1, MonarchChartLegendTokens.slot(null, 0))
        assertEquals(MonarchChartColor.Data5, MonarchChartLegendTokens.slot(null, 4))
        assertEquals(MonarchChartColor.Neutral, MonarchChartLegendTokens.slot(null, 5))
        assertEquals(MonarchChartColor.Data3, MonarchChartLegendTokens.slot(MonarchChartColor.Data3, 0))
    }

    @Test
    fun theToggleRulesKeepOneVisibleAndOtherKeys() {
        val keys = listOf("a", "b")
        assertEquals(setOf("a"), MonarchChartLegendTokens.toggle(emptySet(), "a", keys, keepOneVisible = true))
        assertEquals(setOf("a"), MonarchChartLegendTokens.toggle(setOf("a"), "b", keys, keepOneVisible = true))
        assertEquals(setOf("a", "b"), MonarchChartLegendTokens.toggle(setOf("a"), "b", keys, keepOneVisible = false))
        assertEquals(setOf("x"), MonarchChartLegendTokens.showAll(setOf("a", "x"), keys))
        assertFalse(MonarchChartLegendTokens.showsShowAll(setOf("x"), keys, showAllButton = true))
        assertTrue(MonarchChartLegendTokens.showsShowAll(setOf("a"), keys, showAllButton = true))
        assertFalse(MonarchChartLegendTokens.showsShowAll(setOf("a"), keys, showAllButton = false))
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.data1, MonarchChartLegendTokens.color(MonarchChartColor.Data1, colors), theme.id)
            assertEquals(colors.data2, MonarchChartLegendTokens.color(MonarchChartColor.Data2, colors), theme.id)
            assertEquals(colors.data3, MonarchChartLegendTokens.color(MonarchChartColor.Data3, colors), theme.id)
            assertEquals(colors.data4, MonarchChartLegendTokens.color(MonarchChartColor.Data4, colors), theme.id)
            assertEquals(colors.data5, MonarchChartLegendTokens.color(MonarchChartColor.Data5, colors), theme.id)
            assertEquals(colors.textSubtle, MonarchChartLegendTokens.color(MonarchChartColor.Neutral, colors), theme.id)
            val slot = MonarchChartColor.Data2
            assertEquals(colors.data2, MonarchChartLegendTokens.markerColor(slot, true, true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchChartLegendTokens.markerColor(slot, false, true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchChartLegendTokens.markerColor(slot, true, false, colors), theme.id)
            assertEquals(colors.textDefault, MonarchChartLegendTokens.labelColor(true, true, colors), theme.id)
            assertEquals(colors.textMuted, MonarchChartLegendTokens.labelColor(false, true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchChartLegendTokens.labelColor(true, false, colors), theme.id)
        }
    }
}
