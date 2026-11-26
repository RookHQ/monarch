package com.rookhq.monarch.components.interaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleAxis
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection
import com.rookhq.monarch.components.interaction.sortablegrid.MonarchSortableGrid
import com.rookhq.monarch.components.interaction.sortablegrid.MonarchSortableGridItemState
import com.rookhq.monarch.components.interaction.sortablegrid.MonarchSortableGridLabels
import com.rookhq.monarch.components.interaction.sortablegrid.MonarchSortableGridTokens
import com.rookhq.monarch.components.interaction.sortablegrid.MonarchSortableGridTokens.Slot
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Widgets = listOf("Revenue", "Signups", "Churn", "Tickets")

@Composable
private fun WidgetGrid(
    items: List<String> = Widgets,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    columns: Int? = 2,
    minItemWidth: Dp = 140.dp,
    enabled: Boolean = true,
    locked: String? = null,
    labels: MonarchSortableGridLabels = MonarchSortableGridLabels(),
    states: MutableList<MonarchSortableGridItemState>? = null,
) {
    MonarchSortableGrid(
        items = items,
        key = { it },
        itemLabel = { it },
        onReorder = onReorder,
        columns = columns,
        minItemWidth = minItemWidth,
        enabled = enabled,
        itemEnabled = { it != locked },
        labels = labels,
    ) { item, state ->
        states?.add(state)
        MonarchText(text = item)
    }
}

internal object MonarchSortableGridFixtures : ComponentFixtures {
    override val id = "sortable-grid"
    override val fixtures = listOf(
        fixture("two columns") { WidgetGrid() },
        fixture("adaptive columns") { WidgetGrid(columns = null) },
        fixture("one column") { WidgetGrid(columns = 1, items = Widgets.take(3)) },
        fixture("one locked tile") { WidgetGrid(locked = "Churn") },
        fixture("disabled") { WidgetGrid(enabled = false) },
        fixture("long labels") {
            WidgetGrid(items = listOf("Monthly recurring revenue by plan", "Net revenue retention"))
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSortableGridTest {
    @get:Rule
    val rule = createComposeRule()

    private fun announcement(): String? = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
    ).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()

    private fun actionNames(handle: String): List<String> =
        rule.onNodeWithContentDescription(handle).fetchSemanticsNode()
            .config.getOrElse(SemanticsActions.CustomActions) { emptyList() }.map { it.label }

    private fun runAction(handle: String, name: String) {
        rule.onNodeWithContentDescription(handle).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == name }.action()
        rule.waitForIdle()
    }

    private fun recorder(items: MutableList<String>, moves: MutableList<Pair<Int, Int>>): (Int, Int) -> Unit =
        { from, to ->
            moves += from to to
            items.add(to, items.removeAt(from))
        }

    private fun pickUp(text: String) {
        rule.onNodeWithText(text).performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(LONG_PRESS_MILLIS)
        rule.waitForIdle()
    }

    private fun dragTile(label: String, by: Offset) {
        pickUp(label)
        rule.onNodeWithText(label).performTouchInput {
            moveBy(by / 2f)
            moveBy(by / 2f)
            up()
        }
        rule.waitForIdle()
    }

    @Test
    fun aStepMovesAcrossOrDownAndStopsAtTheEnds() {
        val tokens = MonarchSortableGridTokens
        assertEquals(1, tokens.step(0, MonarchDragHandleDirection.Forward, columns = 2, count = 4))
        assertEquals(2, tokens.step(0, MonarchDragHandleDirection.Down, columns = 2, count = 4))
        assertEquals(1, tokens.step(3, MonarchDragHandleDirection.Up, columns = 2, count = 4))
        assertEquals(2, tokens.step(3, MonarchDragHandleDirection.Backward, columns = 2, count = 4))
        assertNull(tokens.step(0, MonarchDragHandleDirection.Up, columns = 2, count = 4))
        assertNull(tokens.step(3, MonarchDragHandleDirection.Forward, columns = 2, count = 4))
        assertNull(tokens.step(2, MonarchDragHandleDirection.Down, columns = 2, count = 4))
        assertEquals(MonarchDragHandleAxis.Vertical, tokens.axis(1))
        assertEquals(MonarchDragHandleAxis.Both, tokens.axis(3))
    }

    @Test
    fun theTargetIsTheTileUnderTheCentre() {
        val slots = listOf(Slot(0, 0, 0, 100, 80), Slot(1, 110, 0, 100, 80), Slot(2, 0, 90, 100, 80))
        assertNull(MonarchSortableGridTokens.target(slots[0], Offset(30f, 0f), slots))
        assertEquals(1, MonarchSortableGridTokens.target(slots[0], Offset(80f, 0f), slots))
        assertEquals(2, MonarchSortableGridTokens.target(slots[0], Offset(0f, 70f), slots))
        assertEquals(listOf("b", "a", "c"), MonarchSortableGridTokens.moved(listOf("a", "b", "c"), 0, 1))
    }

    @Test
    fun theHandleOffersEveryDirectionTheColumnsAllow() {
        rule.setMonarchContent { WidgetGrid() }
        assertEquals(listOf("Move up", "Move down", "Move backward", "Move forward"), actionNames("Reorder Revenue"))
        rule.onNodeWithContentDescription("Reorder Revenue").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun oneColumnOffersOnlyUpAndDown() {
        rule.setMonarchContent { WidgetGrid(columns = 1) }
        assertEquals(listOf("Move up", "Move down"), actionNames("Reorder Revenue"))
    }

    @Test
    fun adaptiveColumnsBecomeFewerAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) { WidgetGrid(columns = null) }
        assertEquals(listOf("Move up", "Move down"), actionNames("Reorder Revenue"))
        rule.onNodeWithText("Revenue").assertTextFullyVisible()
    }

    @Test
    fun actionsMoveAcrossAndDownAndAnnounce() {
        val items = mutableStateListOf(*Widgets.toTypedArray())
        val moves = mutableListOf<Pair<Int, Int>>()
        rule.setMonarchContent { WidgetGrid(items = items, onReorder = recorder(items, moves)) }
        runAction("Reorder Revenue", "Move forward")
        assertEquals("Revenue moved to position 2 of 4.", announcement())
        runAction("Reorder Revenue", "Move down")
        assertEquals(listOf(0 to 1, 1 to 3), moves)
        assertEquals(listOf("Signups", "Churn", "Tickets", "Revenue"), items.toList())
        runAction("Reorder Revenue", "Move forward")
        assertEquals(2, moves.size)
    }

    @Test
    fun aLongPressDragMovesTheTileAcross() {
        val items = mutableStateListOf(*Widgets.toTypedArray())
        val moves = mutableListOf<Pair<Int, Int>>()
        rule.setMonarchContent { WidgetGrid(items = items, onReorder = recorder(items, moves)) }
        val width = rule.onNodeWithContentDescription("Reorder Signups").getUnclippedBoundsInRoot().left -
            rule.onNodeWithContentDescription("Reorder Revenue").getUnclippedBoundsInRoot().left
        dragTile("Revenue", Offset(with(rule.density) { width.toPx() }, 0f))
        assertEquals(listOf(0 to 1), moves)
        assertEquals("Revenue dropped at position 2 of 4.", announcement())
    }

    @Test
    fun aLongPressDragMovesTheTileDown() {
        val items = mutableStateListOf(*Widgets.toTypedArray())
        val moves = mutableListOf<Pair<Int, Int>>()
        rule.setMonarchContent { WidgetGrid(items = items, onReorder = recorder(items, moves)) }
        val height = rule.onNodeWithContentDescription("Reorder Churn").getUnclippedBoundsInRoot().top -
            rule.onNodeWithContentDescription("Reorder Revenue").getUnclippedBoundsInRoot().top
        dragTile("Revenue", Offset(0f, with(rule.density) { height.toPx() }))
        assertEquals(listOf(0 to 2), moves)
    }

    @Test
    fun aDragFollowsTheFingerRightToLeft() {
        val items = mutableStateListOf(*Widgets.toTypedArray())
        val moves = mutableListOf<Pair<Int, Int>>()
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            WidgetGrid(items = items, onReorder = recorder(items, moves))
        }
        val revenue = rule.onNodeWithContentDescription("Reorder Revenue").getUnclippedBoundsInRoot()
        val signups = rule.onNodeWithContentDescription("Reorder Signups").getUnclippedBoundsInRoot()
        assertTrue(signups.left < revenue.left)
        dragTile("Revenue", Offset(with(rule.density) { (signups.left - revenue.left).toPx() }, 0f))
        assertEquals(listOf(0 to 1), moves)
    }

    @Test
    fun theTileBeingDraggedKnowsIt() {
        val states = mutableListOf<MonarchSortableGridItemState>()
        rule.setMonarchContent { WidgetGrid(states = states) }
        pickUp("Revenue")
        rule.onNodeWithText("Revenue").performTouchInput { moveBy(Offset(1f, 1f)) }
        rule.waitForIdle()
        assertTrue(states.any { it.dragged && it.index == 0 })
        assertEquals("Picked up Revenue. Position 1 of 4.", announcement())
        rule.onNodeWithText("Revenue").performTouchInput { up() }
    }

    @Test
    fun aLockedTileHasNoActionsAndDoesNotMove() {
        val moves = mutableListOf<Pair<Int, Int>>()
        rule.setMonarchContent { WidgetGrid(onReorder = { from, to -> moves += from to to }, locked = "Revenue") }
        rule.onNodeWithContentDescription("Reorder Revenue").assertIsNotEnabled()
        assertEquals(emptyList(), actionNames("Reorder Revenue"))
        dragTile("Revenue", Offset(with(rule.density) { 160.dp.toPx() }, 0f))
        assertEquals(emptyList(), moves)
    }

    @Test
    fun tilesFollowTheReadingOrderInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { WidgetGrid() }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Revenue").assertIsBefore(rule.onNodeWithText("Signups"), direction)
            rule.onNodeWithContentDescription("Reorder Revenue")
                .assertIsBefore(rule.onNodeWithText("Revenue"), direction)
        }
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            WidgetGrid(
                labels = MonarchSortableGridLabels(
                    handle = { "Déplacer $it" },
                    moved = { label, position, total -> "$label : $position sur $total" },
                    actions = mapOf(MonarchDragHandleDirection.Forward to "Suivant"),
                ),
            )
        }
        assertEquals(listOf("Move up", "Move down", "Move backward", "Suivant"), actionNames("Déplacer Revenue"))
        runAction("Déplacer Revenue", "Suivant")
        assertEquals("Revenue : 2 sur 4", announcement())
    }

    @Test
    fun theHeldTileIsMarkedByItsBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentBorder, MonarchSortableGridTokens.border(dragged = true, colors = colors))
            assertEquals(colors.borderSubtle, MonarchSortableGridTokens.border(dragged = false, colors = colors))
            assertEquals(colors.bgSurface, MonarchSortableGridTokens.container(colors))
        }
    }

    private companion object {
        const val LONG_PRESS_MILLIS = 1_000L
    }
}
