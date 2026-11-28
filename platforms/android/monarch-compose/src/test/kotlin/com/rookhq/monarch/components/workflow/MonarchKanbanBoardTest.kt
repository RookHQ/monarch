package com.rookhq.monarch.components.workflow

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanBoard
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanBoardLabels
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanBoardText
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanBoardTokens
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanBoardMoves
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanBoardMoves.Location
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanBoardMoves.Slot
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanBoardTokens.Wip
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanColumn
import com.rookhq.monarch.components.workflow.kanbanboard.MonarchKanbanMove
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val HoldMillis = 1_000L

private fun board(
    todo: List<String> = listOf("Fix login", "Export CSV"),
    doing: List<String> = listOf("Billing page"),
    done: List<String> = emptyList(),
    doingLimit: Int? = 2,
): List<MonarchKanbanColumn<String>> = listOf(
    MonarchKanbanColumn("todo", "To do", todo),
    MonarchKanbanColumn("doing", "Doing", doing, wipLimit = doingLimit),
    MonarchKanbanColumn("done", "Done", done),
)

@Composable
private fun Board(
    columns: List<MonarchKanbanColumn<String>>,
    modifier: Modifier = Modifier,
    onMove: (suspend (MonarchKanbanMove<String>, List<MonarchKanbanColumn<String>>) -> Unit)? = { _, _ -> },
    canMove: ((String, String, String) -> Boolean)? = null,
    enforceWipLimits: Boolean = false,
    enabled: Boolean = true,
    locked: String? = null,
    labels: MonarchKanbanBoardLabels = MonarchKanbanBoardLabels(),
    menuIcon: (@Composable () -> Unit)? = null,
) {
    MonarchKanbanBoard(
        columns = columns,
        key = { it },
        itemLabel = { it },
        modifier = modifier,
        onMove = onMove,
        canMove = canMove,
        enforceWipLimits = enforceWipLimits,
        enabled = enabled,
        itemEnabled = { it != locked },
        labels = labels,
        menuIcon = menuIcon,
    ) { card, _ -> MonarchText(text = card) }
}

internal object MonarchKanbanBoardFixtures : ComponentFixtures {
    override val id = "kanban-board"
    override val fixtures = listOf(
        fixture("three columns with a WIP limit") { Board(board()) },
        fixture("over the limit") { Board(board(doing = listOf("Billing page", "Search", "Audit log"))) },
        fixture("an empty column first") { Board(board(todo = emptyList())) },
        fixture("a locked card") { Board(board(), locked = "Fix login") },
        fixture("read only") { Board(board(), onMove = null) },
        fixture("disabled") { Board(board(), enabled = false) },
        fixture("one column with a menu icon") {
            Board(listOf(MonarchKanbanColumn("todo", "To do", listOf("Fix login")))) {
                MonarchIcon(TestIcon, contentDescription = null)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchKanbanBoardTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchKanbanBoardTokens
    private val rules = MonarchKanbanBoardMoves
    private val keyOf: (String) -> String = { it }

    private fun announcement(): String? = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
    ).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()?.trim()

    private fun actionNames(handle: String): List<String> =
        rule.onNodeWithContentDescription(handle).fetchSemanticsNode()
            .config.getOrElse(SemanticsActions.CustomActions) { emptyList() }.map { it.label }

    private fun runAction(handle: String, name: String) {
        rule.onNodeWithContentDescription(handle).fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == name }.action()
        rule.waitForIdle()
    }

    /** A board whose columns follow each move, recording the moves. */
    private fun movable(
        start: List<MonarchKanbanColumn<String>>,
        moves: MutableList<MonarchKanbanMove<String>>,
        canMove: ((String, String, String) -> Boolean)? = null,
        enforceWipLimits: Boolean = false,
    ): () -> List<MonarchKanbanColumn<String>> {
        var columns by mutableStateOf(start)
        rule.setMonarchContent {
            Board(
                columns,
                modifier = Modifier.height(460.dp),
                onMove = { move, next ->
                    moves += move
                    columns = next
                },
                canMove = canMove,
                enforceWipLimits = enforceWipLimits,
            )
        }
        return { columns }
    }

    @Test
    fun columnsAreHeadingsWithTheirCountInWords() {
        rule.setMonarchContent { Board(board(), modifier = Modifier.height(460.dp)) }
        rule.onNode(isHeading() and hasText("To do")).assertExists()
        // The column header reads its count in words; there is no tab strip, the columns sit side by side.
        rule.onNodeWithContentDescription("2 cards", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("1 card, limit 2", useUnmergedTree = true).assertExists()
        val resources = RuntimeEnvironment.getApplication().resources
        assertEquals("1 / 2", MonarchKanbanBoardText(MonarchKanbanBoardLabels(), resources).countText(1, 2))
        assertEquals("3", MonarchKanbanBoardText(MonarchKanbanBoardLabels(), resources).countText(3, null))
        rule.onNodeWithText("Fix login").assertExists()
    }

    @Test
    fun theCountSaysWhenAColumnIsAtOrOverItsLimit() {
        rule.setMonarchContent { Board(board(doing = listOf("A", "B", "C")), modifier = Modifier.height(460.dp)) }
        rule.onNodeWithContentDescription("3 cards, limit 2, over the limit", useUnmergedTree = true).assertExists()
        assertEquals(Wip.Over, tokens.wip(3, 2))
        assertEquals(Wip.At, tokens.wip(2, 2))
        assertEquals(Wip.Under, tokens.wip(1, 2))
        assertEquals(Wip.None, tokens.wip(9, null))
    }

    @Test
    fun theMoveToMenuSendsTheCardToTheEndOfAColumn() {
        val moves = mutableListOf<MonarchKanbanMove<String>>()
        val columns = movable(board(), moves)
        rule.onNodeWithContentDescription("Move Fix login to another column")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNode(hasText("Move to Doing") and hasAnyAncestor(isPopup())).performClick()
        rule.waitForIdle()
        assertEquals(listOf(MonarchKanbanMove("Fix login", "Fix login", "todo", 0, "doing", 1)), moves)
        assertEquals(listOf("Billing page", "Fix login"), columns()[1].items)
        assertEquals("Fix login dropped in Doing, position 2 of 2.", announcement())
    }

    @Test
    fun aFullColumnRefusesCardsWhenLimitsAreEnforced() {
        rule.setMonarchContent {
            Board(board(doing = listOf("A", "B")), modifier = Modifier.height(460.dp), enforceWipLimits = true)
        }
        rule.onNodeWithContentDescription("Move Fix login to another column").performClick()
        rule.onNode(hasText("Move to Doing") and hasAnyAncestor(isPopup())).assertIsNotEnabled()
    }

    @Test
    fun handleActionsMoveWithinAndBetweenColumns() {
        val moves = mutableListOf<MonarchKanbanMove<String>>()
        val columns = movable(board(), moves, canMove = { _, to, _ -> to != "doing" })
        assertEquals(listOf("Move up", "Move down", "Move backward", "Move forward"), actionNames("Move Fix login"))
        rule.onNodeWithContentDescription("Move Fix login").assertTouchTargetAtLeast48dp(rule.density)
        runAction("Move Fix login", "Move down")
        assertEquals(MonarchKanbanMove("Fix login", "Fix login", "todo", 0, "todo", 1), moves.last())
        assertEquals("Fix login: To do, position 2 of 2.", announcement())
        runAction("Move Fix login", "Move forward")
        assertEquals(MonarchKanbanMove("Fix login", "Fix login", "todo", 1, "done", 0), moves.last())
        assertEquals(listOf("Fix login"), columns()[2].items)
        assertEquals(2, moves.size)
    }

    @Test
    fun aLongPressDragMovesTheCardOnceOnDrop() {
        val moves = mutableListOf<MonarchKanbanMove<String>>()
        val columns = movable(board(), moves)
        val first = rule.onNodeWithText("Fix login").fetchSemanticsNode().boundsInRoot
        val second = rule.onNodeWithText("Export CSV").fetchSemanticsNode().boundsInRoot
        val step = second.bottom - first.center.y
        rule.onNodeWithText("Fix login").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(HoldMillis)
        rule.waitForIdle()
        assertEquals("Picked up Fix login. To do, position 1 of 2.", announcement())
        rule.onNodeWithText("Export CSV").performTouchInput {
            moveBy(Offset(0f, step / 2))
            moveBy(Offset(0f, step / 2))
        }
        rule.waitForIdle()
        assertEquals(emptyList(), moves)
        rule.onNodeWithText("Export CSV").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(listOf(MonarchKanbanMove("Fix login", "Fix login", "todo", 0, "todo", 1)), moves)
        assertEquals(listOf("Export CSV", "Fix login"), columns()[0].items)
        assertEquals("Fix login dropped in To do, position 2 of 2.", announcement())
    }

    @Test
    fun aFailedSaveReturnsTheCardAndIsAnnounced() {
        val columns = board()
        rule.setMonarchContent {
            Board(columns, modifier = Modifier.height(460.dp), onMove = { _, _ -> throw IOException() })
        }
        runAction("Move Fix login", "Move down")
        assertEquals("Could not move Fix login. It is back in To do.", announcement())
        val firstTop = rule.onNodeWithText("Fix login").fetchSemanticsNode().boundsInRoot.top
        val secondTop = rule.onNodeWithText("Export CSV").fetchSemanticsNode().boundsInRoot.top
        assertEquals(true, firstTop < secondTop)
    }

    @Test
    fun lockedCardsDisabledBoardsAndReadOnlyBoardsDoNotMove() {
        var mode by mutableStateOf(0)
        rule.setMonarchContent {
            when (mode) {
                0 -> Board(board(), modifier = Modifier.height(460.dp), locked = "Fix login")
                1 -> Board(board(), modifier = Modifier.height(460.dp), enabled = false)
                else -> Board(board(), modifier = Modifier.height(460.dp), onMove = null)
            }
        }
        rule.onNodeWithContentDescription("Move Fix login").assertDoesNotExist()
        rule.onNodeWithContentDescription("Move Export CSV").assertExists()
        mode = 1
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Move Export CSV").assertDoesNotExist()
        mode = 2
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Move Export CSV to another column").assertDoesNotExist()
        rule.onNodeWithText("Export CSV").assertExists()
    }

    @Test
    fun movingFollowsTheBoardRules() {
        val columns = board()
        val moved = rules.move(columns, "Fix login", Location("done", 0), keyOf)
        assertEquals(listOf("Export CSV"), moved[0].items)
        assertEquals(listOf("Fix login"), moved[2].items)
        assertEquals(columns, rules.move(columns, "nope", Location("done", 0), keyOf))
        val open: (String) -> Boolean = { false }
        assertEquals(
            Location("doing", 1),
            rules.step(columns, "Export CSV", MonarchDragHandleDirection.Forward, keyOf, open),
        )
        assertNull(rules.step(columns, "Fix login", MonarchDragHandleDirection.Backward, keyOf, open))
        assertNull(rules.step(columns, "Fix login", MonarchDragHandleDirection.Up, keyOf, open))
        assertEquals(
            Location("done", 0),
            rules.step(columns, "Fix login", MonarchDragHandleDirection.Forward, keyOf) { it == "doing" },
        )
        val full = MonarchKanbanColumn("doing", "Doing", listOf("A", "B"), wipLimit = 2)
        assertEquals(true, rules.refuses(full, "C", "todo", null, enforceWipLimits = true))
        assertEquals(false, rules.refuses(full, "C", "todo", null, enforceWipLimits = false))
        assertEquals(false, rules.refuses(full, "A", "doing", { _, _, _ -> false }, enforceWipLimits = true))
        assertEquals(true, rules.refuses(full, "C", "todo", { _, _, _ -> false }, enforceWipLimits = false))
    }

    @Test
    fun dropPositionsAndPagesFollowTheFinger() {
        val slots = listOf(Slot(0, 50f), Slot(1, 150f))
        assertEquals(0, rules.indexAt(10f, slots, 2))
        assertEquals(1, rules.indexAt(100f, slots, 2))
        assertEquals(2, rules.indexAt(400f, slots, 2))
        assertEquals(3, rules.indexAt(0f, emptyList(), 3))
        assertEquals(-1, rules.edge(5f, 300f, 40f, rtl = false))
        assertEquals(1, rules.edge(5f, 300f, 40f, rtl = true))
        assertEquals(1, rules.edge(290f, 300f, 40f, rtl = false))
        assertEquals(0, rules.edge(150f, 300f, 40f, rtl = false))
        assertEquals(1, rules.columnsThatFit(320, 288, 12))
        assertEquals(3, rules.columnsThatFit(900, 288, 12))
        assertEquals(1, rules.columnsThatFit(100, 288, 12))
    }

    @Test
    fun coloursComeFromRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.dangerStrong, tokens.countColor(Wip.Over, colors))
            assertEquals(colors.warningStrong, tokens.countColor(Wip.At, colors))
            assertEquals(colors.textMuted, tokens.countColor(Wip.Under, colors))
            assertEquals(colors.dangerBorder, tokens.columnBorder(true, colors))
            assertEquals(colors.borderSubtle, tokens.columnBorder(false, colors))
            assertEquals(colors.accentBorder, tokens.cardBorder(true, colors))
            assertEquals(colors.bgSurface, tokens.card(colors))
            assertEquals(colors.bgSubtle, tokens.column(colors))
            assertEquals(colors.bgMuted, tokens.placeholder(colors))
            assertEquals(colors.accentSolid, tokens.insertion(colors))
        }
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Board(
                board(),
                modifier = Modifier.height(460.dp),
                labels = MonarchKanbanBoardLabels(
                    handle = { "Drag $it" },
                    move = "Shift",
                    count = { count, _ -> "$count tickets" },
                    countText = { count, limit -> "$count of ${limit ?: "any"}" },
                    empty = "Nothing",
                ),
            )
        }
        rule.onNodeWithContentDescription("Drag Fix login").assertExists()
        rule.onAllNodesWithText("Shift", useUnmergedTree = true).onFirst().assertExists()
        rule.onAllNodesWithContentDescription("2 tickets", useUnmergedTree = true).onFirst().assertExists()
        val custom = MonarchKanbanBoardLabels(countText = { count, limit -> "$count of ${limit ?: "any"}" })
        val resources = RuntimeEnvironment.getApplication().resources
        assertEquals("1 of 2", MonarchKanbanBoardText(custom, resources).countText(1, 2))
    }

    @Test
    fun rightToLeftPutsTheHandleAtTheStart() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            Board(board(), modifier = Modifier.height(460.dp))
        }
        rule.onNodeWithContentDescription("Move Fix login").assertIsBefore(
            rule.onNodeWithText("Fix login", useUnmergedTree = true),
            LayoutDirection.Rtl,
        )
    }

    @Test
    fun longTextIsNotCutAtTheLargestFontScale() {
        val long = "Review the onboarding flow with the research team"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Board(
                listOf(MonarchKanbanColumn("todo", "Waiting for review by finance", listOf(long))),
                modifier = Modifier.height(460.dp),
            )
        }
        rule.onNodeWithText(long, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNode(isHeading() and hasText("Waiting for review by finance"), useUnmergedTree = true)
            .assertTextFullyVisible()
    }
}
