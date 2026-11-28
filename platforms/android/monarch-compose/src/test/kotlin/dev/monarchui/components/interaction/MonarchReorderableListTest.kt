package dev.monarchui.components.interaction

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleDirection
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableList
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableListItemState
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableListLabels
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableListTokens
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableListTokens.Slot
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.MonarchDragAutoScroll
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Tasks = listOf("Design review", "Budget", "Hiring plan")

@Composable
private fun TaskList(
    modifier: Modifier = Modifier,
    items: List<String> = Tasks,
    onReorder: (Int, Int) -> Unit = { _, _ -> },
    enabled: Boolean = true,
    locked: String? = null,
    moveButtons: Boolean = false,
    labels: MonarchReorderableListLabels = MonarchReorderableListLabels(),
    states: MutableList<MonarchReorderableListItemState>? = null,
    state: LazyListState = rememberLazyListState(),
) {
    MonarchReorderableList(
        items = items,
        key = { it },
        itemLabel = { it },
        onReorder = onReorder,
        modifier = modifier,
        enabled = enabled,
        itemEnabled = { it != locked },
        moveButtons = moveButtons,
        state = state,
        labels = labels,
    ) { item, itemState ->
        states?.add(itemState)
        MonarchText(text = item)
    }
}

internal object MonarchReorderableListFixtures : ComponentFixtures {
    override val id = "reorderable-list"
    override val fixtures = listOf(
        fixture("three rows") { TaskList() },
        fixture("move buttons") { TaskList(moveButtons = true) },
        fixture("one locked row") { TaskList(locked = "Budget") },
        fixture("disabled") { TaskList(enabled = false, moveButtons = true) },
        fixture("long labels") {
            TaskList(items = listOf("Review the design of the onboarding flow with the research team", "Budget"))
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchReorderableListTest {
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

    private fun pickUp(text: String) {
        rule.onNodeWithText(text).performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(LONG_PRESS_MILLIS)
        rule.waitForIdle()
    }

    @Test
    fun movingAndTargetingFollowTheMiddleOfTheHeldRow() {
        assertEquals(listOf("b", "a", "c"), MonarchReorderableListTokens.moved(listOf("a", "b", "c"), 0, 1))
        assertEquals(listOf("b", "c", "a"), MonarchReorderableListTokens.moved(listOf("a", "b", "c"), 0, 9))
        assertEquals(listOf("a", "b"), MonarchReorderableListTokens.moved(listOf("a", "b"), 5, 0))
        val slots = listOf(Slot(0, 0, 100), Slot(1, 110, 100), Slot(2, 220, 100))
        assertNull(MonarchReorderableListTokens.target(slots[0], draggedTop = 40f, slots = slots))
        assertEquals(1, MonarchReorderableListTokens.target(slots[0], draggedTop = 80f, slots = slots))
        assertEquals(0, MonarchReorderableListTokens.target(slots[2], draggedTop = 0f, slots = slots))
    }

    @Test
    fun autoScrollStartsOnlyPastTheEdgeAndIsCapped() {
        // The list's auto-scroll is the shared one: top, bottom, viewport start, viewport end, direction.
        val auto = MonarchDragAutoScroll
        assertEquals(0f, auto.overscroll(10f, 90f, 0, 100, 5f))
        assertEquals(20f, auto.overscroll(40f, 120f, 0, 100, 5f))
        assertEquals(-5f, auto.overscroll(-5f, 75f, 0, 100, -1f))
        assertEquals(0f, auto.overscroll(-5f, 75f, 0, 100, 0f))
        assertEquals(16f, auto.step(40f, maxStep = 16f))
        assertEquals(-3f, auto.step(-3f, maxStep = 16f))
    }

    @Test
    fun everyRowHasANamedHandleWithMoveActions() {
        rule.setMonarchContent { TaskList() }
        assertEquals(listOf("Move up", "Move down"), actionNames("Reorder Budget"))
        rule.onNodeWithContentDescription("Reorder Budget").assertTouchTargetAtLeast48dp(rule.density)
        assertNull(rule.onNodeWithContentDescription("Reorder Budget").stateDescription())
    }

    @Test
    fun aMoveActionReordersAndAnnouncesTheNewPosition() {
        val items = mutableStateListOf(*Tasks.toTypedArray())
        val moves = mutableListOf<Pair<Int, Int>>()
        rule.setMonarchContent {
            TaskList(
                items = items,
                onReorder = { from, to ->
                    moves += from to to
                    items.add(to, items.removeAt(from))
                },
            )
        }
        runAction("Reorder Design review", "Move down")
        assertEquals(listOf(0 to 1), moves)
        assertEquals(listOf("Budget", "Design review", "Hiring plan"), items.toList())
        assertEquals("Design review moved to position 2 of 3.", announcement())
        runAction("Reorder Budget", "Move up")
        assertEquals(listOf(0 to 1), moves)
    }

    @Test
    fun aLongPressDragMovesTheRowOnceOnDrop() {
        val items = mutableStateListOf(*Tasks.toTypedArray())
        val moves = mutableListOf<Pair<Int, Int>>()
        val states = mutableListOf<MonarchReorderableListItemState>()
        rule.setMonarchContent {
            TaskList(
                items = items,
                onReorder = { from, to ->
                    moves += from to to
                    items.add(to, items.removeAt(from))
                },
                states = states,
            )
        }
        val rowStep = with(rule.density) { 70.dp.toPx() }
        pickUp("Design review")
        rule.onNodeWithText("Design review").performTouchInput {
            moveBy(Offset(0f, rowStep / 2))
            moveBy(Offset(0f, rowStep / 2))
        }
        rule.waitForIdle()
        assertTrue(states.any { it.dragged })
        assertEquals("Picked up Design review. Position 1 of 3.", announcement())
        assertEquals(emptyList(), moves)
        rule.onNodeWithText("Design review").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(listOf(0 to 1), moves)
        assertEquals(listOf("Budget", "Design review", "Hiring plan"), items.toList())
        assertEquals("Design review dropped at position 2 of 3.", announcement())
    }

    @Test
    fun aLockedRowAndADisabledListDoNotMove() {
        val moves = mutableListOf<Pair<Int, Int>>()
        rule.setMonarchContent { TaskList(onReorder = { from, to -> moves += from to to }, locked = "Design review") }
        assertEquals(emptyList(), actionNames("Reorder Design review"))
        rule.onNodeWithContentDescription("Reorder Design review").assertIsNotEnabled()
        pickUp("Design review")
        rule.onNodeWithText("Design review").performTouchInput {
            moveBy(Offset(0f, with(rule.density) { 140.dp.toPx() }))
            up()
        }
        rule.waitForIdle()
        assertEquals(emptyList(), moves)
        assertEquals(listOf("Move up", "Move down"), actionNames("Reorder Budget"))
    }

    @Test
    fun moveButtonsAreNamedAndStopAtTheEnds() {
        val moves = mutableListOf<Pair<Int, Int>>()
        rule.setMonarchContent { TaskList(onReorder = { from, to -> moves += from to to }, moveButtons = true) }
        rule.onNodeWithContentDescription("Move Design review up").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Move Hiring plan down").assertIsNotEnabled()
        val down = rule.onNodeWithContentDescription("Move Design review down")
        down.assertIsEnabled().assertTouchTargetAtLeast48dp(rule.density)
        down.performClick()
        assertEquals(listOf(0 to 1), moves)
        assertEquals("Design review moved to position 2 of 3.", announcement())
    }

    @Test
    fun aDisabledListOffersNoMoves() {
        rule.setMonarchContent { TaskList(enabled = false, moveButtons = true) }
        rule.onNodeWithContentDescription("Reorder Budget").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Move Budget up").assertIsNotEnabled()
        assertEquals(emptyList(), actionNames("Reorder Budget"))
    }

    @Test
    fun theListScrollsWhileARowIsHeldPastItsEdge() {
        val items = List(12) { "Task ${it + 1}" }
        val listState = LazyListState()
        rule.setMonarchContent { TaskList(items = items, modifier = Modifier.height(200.dp), state = listState) }
        pickUp("Task 1")
        rule.onNodeWithText("Task 1").performTouchInput { moveBy(Offset(0f, with(rule.density) { 150.dp.toPx() })) }
        rule.mainClock.advanceTimeBy(AUTO_SCROLL_MILLIS)
        assertTrue(listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)
        rule.onNodeWithText("Task 1").performTouchInput { up() }
    }

    @Test
    fun theHandleComesBeforeTheContentInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { TaskList() }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Reorder Budget")
                .assertIsBefore(rule.onNodeWithText("Budget"), direction)
        }
    }

    @Test
    fun aLongLabelWrapsWholeAtTheLargestFontScale() {
        val long = "Review the design of the onboarding flow with the research team"
        rule.setMonarchContent(fontScale = LargestFontScale) { TaskList(items = listOf(long), moveButtons = true) }
        rule.onNodeWithText(long).assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            TaskList(
                moveButtons = true,
                labels = MonarchReorderableListLabels(
                    handle = { "Déplacer $it" },
                    moveDown = { "Descendre $it" },
                    down = "Bas",
                    moved = { label, position, total -> "$label : $position sur $total" },
                    actions = mapOf(MonarchDragHandleDirection.Down to "Descendre"),
                ),
            )
        }
        assertEquals(listOf("Move up", "Descendre"), actionNames("Déplacer Budget"))
        // The move buttons are chevrons, as the web draws them, so the name carries the direction (no visible text).
        val moveDown = hasContentDescription("Descendre", substring = true)
        assertEquals(3, rule.onAllNodes(moveDown).fetchSemanticsNodes().size)
        rule.onNodeWithContentDescription("Descendre Budget").performClick()
        assertEquals("Budget : 3 sur 3", announcement())
    }

    @Test
    fun theHeldRowIsMarkedByItsBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentBorder, MonarchReorderableListTokens.border(dragged = true, colors = colors))
            assertEquals(colors.borderSubtle, MonarchReorderableListTokens.border(dragged = false, colors = colors))
            assertEquals(colors.bgSurface, MonarchReorderableListTokens.container(colors))
        }
    }

    private companion object {
        const val LONG_PRESS_MILLIS = 1_000L
        const val AUTO_SCROLL_MILLIS = 500L
    }
}
