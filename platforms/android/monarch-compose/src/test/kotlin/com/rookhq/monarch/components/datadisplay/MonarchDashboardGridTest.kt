package com.rookhq.monarch.components.datadisplay

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datadisplay.dashboardgrid.MonarchDashboardGrid
import com.rookhq.monarch.components.datadisplay.dashboardgrid.MonarchDashboardGridLabels
import com.rookhq.monarch.components.datadisplay.dashboardgrid.MonarchDashboardGridSpan
import com.rookhq.monarch.components.datadisplay.dashboardgrid.MonarchDashboardGridTokens
import com.rookhq.monarch.components.datadisplay.widget.MonarchWidget
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Widgets = listOf("Revenue", "Team load", "Open tickets")

@Composable
private fun Dashboard(
    modifier: Modifier = Modifier,
    items: List<String> = Widgets,
    layout: List<String> = emptyList(),
    onLayoutChange: ((List<String>) -> Unit)? = {},
    editing: Boolean = false,
    columns: Int = 3,
    labels: MonarchDashboardGridLabels = MonarchDashboardGridLabels(),
) {
    MonarchDashboardGrid(
        items = items,
        key = { it },
        itemLabel = { it },
        modifier = modifier,
        layout = layout,
        onLayoutChange = onLayoutChange,
        span = { if (it == "Revenue") MonarchDashboardGridSpan.Full else MonarchDashboardGridSpan.One },
        columns = columns,
        editing = editing,
        labels = labels,
    ) { MonarchText(text = "$it content") }
}

internal object MonarchDashboardGridFixtures : ComponentFixtures {
    override val id = "dashboard-grid"
    override val fixtures = listOf(
        fixture("viewing") { Dashboard() },
        fixture("editing, first widget") { Dashboard(items = Widgets.take(1), editing = true) },
        fixture("editing, two widgets") { Dashboard(items = Widgets.take(2), editing = true) },
        fixture("stored layout") { Dashboard(layout = listOf("Open tickets", "gone", "Revenue")) },
        fixture("with a widget") {
            MonarchDashboardGrid(items = listOf("Revenue"), key = { it }, itemLabel = { it }) {
                MonarchWidget(title = it) { MonarchText(text = "12,400") }
            }
        },
        fixture("long label, editing") {
            Dashboard(items = listOf("Average first response time across every support queue"), editing = true)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDashboardGridTest {
    @get:Rule
    val rule = createComposeRule()

    private fun announcement(): String? = rule.onAllNodes(
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
    ).fetchSemanticsNodes().firstNotNullOfOrNull {
        it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
    }

    @Test
    fun theColumnsFollowTheWidthAndTheTextSize() {
        val tokens = MonarchDashboardGridTokens
        assertEquals(1, tokens.columnsAt(400.dp, 1f, 3))
        assertEquals(2, tokens.columnsAt(800.dp, 1f, 3))
        assertEquals(1, tokens.columnsAt(800.dp, 1f, 1))
        assertEquals(3, tokens.columnsAt(1200.dp, 1f, 3))
        assertEquals(4, tokens.columnsAt(1200.dp, 1f, 9))
        assertEquals(1, tokens.columnsAt(1200.dp, LargestFontScale, 3))
        assertEquals(1, tokens.spanOf(MonarchDashboardGridSpan.Full, 1))
        assertEquals(2, tokens.spanOf(MonarchDashboardGridSpan.columns(5), 2))
        assertEquals(1, tokens.spanOf(MonarchDashboardGridSpan.columns(0), 3))
    }

    @Test
    fun aStoredLayoutSkipsUnknownKeysAndAppendsNewWidgets() {
        val tokens = MonarchDashboardGridTokens
        assertEquals(listOf("c", "a", "b"), tokens.resolve(listOf("a", "b", "c"), listOf("c", "x", "a", "c")))
        assertEquals(listOf("a", "b"), tokens.resolve(listOf("a", "b"), emptyList()))
        assertEquals(listOf("b", "a", "c"), tokens.moved(listOf("a", "b", "c"), 0, 1))
    }

    @Test
    fun widgetsAreShownInTheStoredOrder() {
        rule.setMonarchContent { Dashboard(layout = listOf("Open tickets", "Revenue")) }
        rule.onNodeWithContentDescription("Dashboard").assertExists()
        val tickets = rule.onNodeWithText("Open tickets content").getUnclippedBoundsInRoot().top
        val revenue = rule.onNodeWithText("Revenue content").getUnclippedBoundsInRoot().top
        val load = rule.onNodeWithText("Team load content").getUnclippedBoundsInRoot().top
        assertTrue(tickets < revenue && revenue < load)
        rule.onNodeWithText("Move Revenue earlier").assertDoesNotExist()
    }

    @Test
    fun twoWidgetsShareARowOnAWideGridButAFullSpanTakesItAll() {
        rule.setMonarchContent {
            Dashboard(
                modifier = Modifier.requiredWidth(1100.dp),
                items = listOf("Team load", "Open tickets", "Revenue"),
            )
        }
        val load = rule.onNodeWithText("Team load content").getUnclippedBoundsInRoot()
        val tickets = rule.onNodeWithText("Open tickets content").getUnclippedBoundsInRoot()
        val revenue = rule.onNodeWithText("Revenue content").getUnclippedBoundsInRoot()
        assertEquals(load.top, tickets.top)
        assertTrue(revenue.top > load.top)
    }

    @Test
    fun moveButtonsReorderAnnounceAndStopAtTheEnds() {
        var layout by mutableStateOf(emptyList<String>())
        rule.setMonarchContent { Dashboard(layout = layout, onLayoutChange = { layout = it }, editing = true) }
        rule.onNodeWithContentDescription("Move Revenue earlier").assertIsNotEnabled()
        val later = rule.onNodeWithContentDescription("Move Revenue later")
        later.assertIsEnabled().assertTouchTargetAtLeast48dp(rule.density)
        later.performClick()
        assertEquals(listOf("Team load", "Revenue", "Open tickets"), layout)
        assertEquals("Revenue moved to position 2 of 3.", announcement())
    }

    @Test
    fun eachWidgetHasANamedHandleWithMoveActionsWhileEditing() {
        var layout by mutableStateOf(emptyList<String>())
        rule.setMonarchContent { Dashboard(layout = layout, onLayoutChange = { layout = it }, editing = true) }
        val handle = rule.onNodeWithContentDescription("Move Team load")
        handle.assertTouchTargetAtLeast48dp(rule.density)
        handle.fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == "Move up" }.action()
        rule.waitForIdle()
        assertEquals(listOf("Team load", "Revenue", "Open tickets"), layout)
    }

    @Test
    fun withoutOnLayoutChangeTheGridCannotBeRearranged() {
        rule.setMonarchContent { Dashboard(onLayoutChange = null, editing = true) }
        rule.onNodeWithContentDescription("Move Team load").assertDoesNotExist()
        rule.onNodeWithText("Team load content").assertExists()
    }

    @Test
    fun theArrangeBarReadsInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { Dashboard(items = Widgets.take(2), editing = true) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Move Revenue")
                .assertIsBefore(rule.onNodeWithText("Revenue content"), direction)
        }
    }

    @Test
    fun aLongLabelWrapsWholeAtTheLargestFontScale() {
        val long = "Average first response time across every support queue"
        rule.setMonarchContent(fontScale = LargestFontScale) { Dashboard(items = listOf(long), editing = true) }
        rule.onNodeWithText(long).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Dashboard(
                items = Widgets.take(2),
                editing = true,
                labels = MonarchDashboardGridLabels(
                    label = "Tableau de bord",
                    handle = { "Déplacer $it" },
                    earlier = "Avant",
                    later = "Après",
                    moveEarlier = { "Avancer $it" },
                    moveLater = { "Reculer $it" },
                    moved = { label, position, total -> "$label : $position sur $total" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Tableau de bord").assertExists()
        rule.onNodeWithContentDescription("Déplacer Revenue").assertExists()
        rule.onAllNodesWithText("Avant", useUnmergedTree = true).assertCountEquals(2)
        rule.onNodeWithContentDescription("Reculer Revenue").performClick()
        assertEquals("Revenue : 2 sur 2", announcement())
    }
}
