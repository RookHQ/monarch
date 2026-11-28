@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.workflow

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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.components.workflow.backlogplanner.MonarchBacklogBucket
import dev.monarchui.components.workflow.backlogplanner.MonarchBacklogMove
import dev.monarchui.components.workflow.backlogplanner.MonarchBacklogPlanner
import dev.monarchui.components.workflow.backlogplanner.MonarchBacklogPlannerLabels
import dev.monarchui.components.workflow.backlogplanner.MonarchBacklogPlannerTokens
import dev.monarchui.components.workflow.backlogplanner.MonarchBacklogPlannerMoves
import dev.monarchui.components.workflow.backlogplanner.MonarchBacklogPlannerMoves.Location
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val LongPressMillis = 1_000L

private data class Issue(val key: String, val title: String, val points: Int? = null)

private val Login = Issue("ABC-1", "Fix login", 3)
private val Export = Issue("ABC-2", "Export CSV", 1)
private val Search = Issue("ABC-3", "Search", null)
private val Billing = Issue("ABC-4", "Billing page", 5)

private fun plan(
    sprint: List<Issue> = listOf(Login, Export),
    backlog: List<Issue> = listOf(Search),
    capacity: Int? = 5,
    extra: List<MonarchBacklogBucket<Issue>> = emptyList(),
): List<MonarchBacklogBucket<Issue>> = listOf(
    MonarchBacklogBucket("sprint", "Sprint 14", sprint, subtitle = "Nov 10 – Nov 21", capacity = capacity),
) + extra + MonarchBacklogBucket("backlog", "Backlog", backlog)

@Composable
private fun Planner(
    buckets: List<MonarchBacklogBucket<Issue>>,
    modifier: Modifier = Modifier,
    onMove: (suspend (MonarchBacklogMove<Issue>, List<MonarchBacklogBucket<Issue>>) -> Unit)? = { _, _ -> },
    canMove: ((Issue, String, String) -> Boolean)? = null,
    moveMenu: Boolean = true,
    collapsed: Set<String>? = null,
    onCollapsedChange: ((Set<String>) -> Unit)? = null,
    points: Boolean = true,
    labels: MonarchBacklogPlannerLabels = MonarchBacklogPlannerLabels(),
    menuIcon: (@Composable () -> Unit)? = null,
) {
    MonarchBacklogPlanner(
        buckets = buckets,
        key = { it.key },
        itemLabel = { it.key },
        modifier = modifier,
        itemPoints = if (points) ({ it.points }) else null,
        onMove = onMove,
        canMove = canMove,
        moveMenu = moveMenu,
        collapsed = collapsed,
        onCollapsedChange = onCollapsedChange,
        labels = labels,
        menuIcon = menuIcon,
    ) { issue, _ -> MonarchText(text = issue.title) }
}

internal object MonarchBacklogPlannerFixtures : ComponentFixtures {
    override val id = "backlog-planner"
    override val fixtures = listOf(
        fixture("a sprint with capacity and the backlog") { Planner(plan()) },
        fixture("over capacity") { Planner(plan(sprint = listOf(Login, Billing), backlog = emptyList())) },
        fixture("a closed bucket and an empty one") {
            Planner(plan(sprint = emptyList()), collapsed = setOf("backlog"))
        },
        fixture("read only, without points") { Planner(plan(), onMove = null, points = false) },
        fixture("without the menu") { Planner(plan(backlog = emptyList()), moveMenu = false) },
        fixture("with a menu icon") {
            Planner(plan(backlog = emptyList())) { MonarchIcon(TestIcon, contentDescription = null) }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchBacklogPlannerTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchBacklogPlannerTokens
    private val rules = MonarchBacklogPlannerMoves
    private val keyOf: (Issue) -> String = { it.key }

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

    /** A planner whose buckets follow each move, recording the moves. */
    private fun movable(
        start: List<MonarchBacklogBucket<Issue>>,
        moves: MutableList<MonarchBacklogMove<Issue>>,
        canMove: ((Issue, String, String) -> Boolean)? = null,
        collapsed: Set<String>? = null,
    ): () -> List<MonarchBacklogBucket<Issue>> {
        var buckets by mutableStateOf(start)
        rule.setMonarchContent {
            Planner(
                buckets,
                modifier = Modifier.height(460.dp),
                onMove = { move, next ->
                    moves += move
                    buckets = next
                },
                canMove = canMove,
                collapsed = collapsed,
            )
        }
        return { buckets }
    }

    @Test
    fun bucketsAreHeadingsThatOpenAndClose() {
        var reported: Set<String>? = null
        rule.setMonarchContent { Planner(plan(), onCollapsedChange = { reported = it }) }
        val sprint = rule.onNode(isHeading() and hasText("Sprint 14"))
        sprint.assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Expanded", sprint.stateDescription())
        rule.onNodeWithText("Fix login").assertExists()
        sprint.performClick()
        assertEquals(setOf("sprint"), reported)
        assertEquals("Collapsed", sprint.stateDescription())
        rule.onNodeWithText("Fix login").assertDoesNotExist()
        rule.onNodeWithText("2 items · 4 points").assertExists()
    }

    @Test
    fun headersSumPointsAgainstCapacity() {
        rule.setMonarchContent { Planner(plan(sprint = listOf(Login, Billing, Search), backlog = emptyList())) }
        rule.onNodeWithText("3 items · 8 points · 1 unestimated").assertExists()
        rule.onNodeWithText("Planned work is over capacity by 3 points.").assertExists()
        rule.onNodeWithContentDescription("8 of 5 points").assertExists()
        rule.onNodeWithText("Nothing here yet.").assertExists()
    }

    @Test
    fun rowsShowPointsAndSayWhenThereIsNoEstimate() {
        rule.setMonarchContent { Planner(plan()) }
        rule.onNodeWithContentDescription("3 pt", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Not estimated", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theMoveToMenuSendsTheItemToTheEndOfABucket() {
        val moves = mutableListOf<MonarchBacklogMove<Issue>>()
        val buckets = movable(plan(), moves)
        rule.onNodeWithContentDescription("Move ABC-1 to another bucket")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNode(hasText("Move to Backlog") and hasAnyAncestor(isPopup())).performClick()
        rule.waitForIdle()
        assertEquals(listOf(MonarchBacklogMove(Login, "ABC-1", "sprint", 0, "backlog", 1)), moves)
        assertEquals(listOf(Search, Login), buckets().last().items)
        assertEquals("ABC-1 dropped in Backlog, position 2 of 2.", announcement())
    }

    @Test
    fun refusedBucketsAreDisabledInTheMenu() {
        rule.setMonarchContent { Planner(plan(), canMove = { _, to, _ -> to != "backlog" }) }
        rule.onNodeWithContentDescription("Move ABC-1 to another bucket").performClick()
        rule.onNode(hasText("Move to Backlog") and hasAnyAncestor(isPopup())).assertIsNotEnabled()
    }

    @Test
    fun handleActionsStepAcrossBucketsAndSkipClosedOnes() {
        val moves = mutableListOf<MonarchBacklogMove<Issue>>()
        val release = MonarchBacklogBucket<Issue>("release", "Release", listOf(Billing))
        val buckets = movable(plan(extra = listOf(release)), moves, collapsed = setOf("release"))
        assertEquals(listOf("Move up", "Move down"), actionNames("Move ABC-2"))
        rule.onNodeWithContentDescription("Move ABC-2").assertTouchTargetAtLeast48dp(rule.density)
        runAction("Move ABC-2", "Move down")
        assertEquals(MonarchBacklogMove(Export, "ABC-2", "sprint", 1, "backlog", 0), moves.single())
        assertEquals(listOf(Export, Search), buckets().last().items)
        assertEquals("ABC-2 moved to Backlog, position 1 of 2.", announcement())
        runAction("Move ABC-1", "Move up")
        assertEquals(1, moves.size)
    }

    @Test
    fun aLongPressDragMovesTheItemOnceOnDrop() {
        val moves = mutableListOf<MonarchBacklogMove<Issue>>()
        val buckets = movable(plan(), moves)
        val first = rule.onNodeWithText("Fix login").fetchSemanticsNode().boundsInRoot
        val second = rule.onNodeWithText("Export CSV").fetchSemanticsNode().boundsInRoot
        val step = second.center.y - first.center.y
        rule.onNodeWithText("Fix login").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(LongPressMillis)
        rule.waitForIdle()
        assertEquals("Picked up ABC-1. Sprint 14, position 1 of 2.", announcement())
        rule.onNodeWithText("Fix login").performTouchInput {
            moveBy(Offset(0f, step / 2))
            moveBy(Offset(0f, step / 2))
        }
        rule.waitForIdle()
        assertEquals(emptyList(), moves)
        rule.onNodeWithText("Fix login").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(listOf(MonarchBacklogMove(Login, "ABC-1", "sprint", 0, "sprint", 1)), moves)
        assertEquals(listOf(Export, Login), buckets().first().items)
        assertEquals("ABC-1 dropped in Sprint 14, position 2 of 2.", announcement())
    }

    @Test
    fun aFailedSaveReturnsTheItemAndIsAnnounced() {
        var buckets by mutableStateOf(plan())
        rule.setMonarchContent {
            Planner(buckets, onMove = { _, _ -> throw IOException() })
        }
        runAction("Move ABC-2", "Move down")
        assertEquals("ABC-2 could not be moved. It is back where it was.", announcement())
        rule.onNodeWithContentDescription("Move ABC-2").assertExists()
        assertEquals(listOf(Login, Export), buckets.first().items)
    }

    @Test
    fun withoutOnMoveNothingCanBeMoved() {
        rule.setMonarchContent { Planner(plan(), onMove = null) }
        rule.onNodeWithContentDescription("Move ABC-1").assertDoesNotExist()
        rule.onNodeWithContentDescription("Move ABC-1 to another bucket").assertDoesNotExist()
        rule.onNodeWithText("Fix login").assertExists()
    }

    @Test
    fun movingFollowsTheBucketRules() {
        val buckets = plan()
        val moved = rules.move(buckets, "ABC-1", Location("backlog", 0), keyOf)
        assertEquals(listOf(Export), moved.first().items)
        assertEquals(listOf(Login, Search), moved.last().items)
        assertEquals(buckets, rules.move(buckets, "nope", Location("backlog", 0), keyOf))
        assertEquals(buckets, rules.move(buckets, "ABC-1", Location("nope", 0), keyOf))
        val clamped = rules.move(buckets, "ABC-1", Location("sprint", 9), keyOf)
        assertEquals(listOf(Export, Login), clamped.first().items)
        val open: (String) -> Boolean = { false }
        assertEquals(Location("sprint", 1), rules.step(buckets, "ABC-1", 1, keyOf, open))
        assertEquals(Location("backlog", 0), rules.step(buckets, "ABC-2", 1, keyOf, open))
        assertEquals(Location("sprint", 2), rules.step(buckets, "ABC-3", -1, keyOf, open))
        assertNull(rules.step(buckets, "ABC-1", -1, keyOf, open))
        assertNull(rules.step(buckets, "ABC-2", 1, keyOf) { it == "backlog" })
    }

    @Test
    fun dragTargetsFollowTheEntryUnderTheRow() {
        val buckets = plan()
        val open: (String) -> Boolean = { false }
        assertEquals(
            listOf("h:sprint", "i:ABC-1", "i:ABC-2", "h:backlog", "i:ABC-3"),
            rules.entries(buckets, emptySet(), keyOf),
        )
        assertEquals(listOf("h:sprint", "h:backlog", "i:ABC-3"), rules.entries(buckets, setOf("sprint"), keyOf))
        assertEquals(Location("sprint", 1), rules.target("i:ABC-2", buckets, "ABC-1", keyOf, open))
        assertEquals(Location("backlog", 0), rules.target("h:backlog", buckets, "ABC-2", keyOf, open))
        assertEquals(Location("sprint", 2), rules.target("h:backlog", buckets, "ABC-3", keyOf, open))
        assertNull(rules.target("h:sprint", buckets, "ABC-1", keyOf, open))
        assertNull(rules.target("h:backlog", buckets, "ABC-2", keyOf) { it == "backlog" })
        assertEquals(Location("sprint", 0), rules.target("e:sprint", buckets, "ABC-3", keyOf, open))
        assertNull(rules.target("i:ABC-1", buckets, "ABC-1", keyOf, open))
    }

    @Test
    fun pointsAndCapacity() {
        val pointsOf: (Issue) -> Int? = { it.points }
        assertEquals(4, rules.points(listOf(Login, Export, Search), pointsOf))
        assertNull(rules.points(listOf(Login), null))
        assertEquals(1, rules.unestimated(listOf(Login, Search), pointsOf))
        assertEquals(0.5f, tokens.fill(2, 4))
        assertEquals(1f, tokens.fill(9, 4))
        assertEquals(1f, tokens.fill(1, 0))
    }

    @Test
    fun coloursComeFromRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgSurface, tokens.container(colors))
            assertEquals(colors.accentBorder, tokens.border(true, colors))
            assertEquals(colors.borderSubtle, tokens.border(false, colors))
            assertEquals(colors.textMuted, tokens.meta(colors))
            assertEquals(colors.dangerStrong, tokens.overCapacity(colors))
        }
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Planner(
                plan(),
                labels = MonarchBacklogPlannerLabels(
                    summary = { count, _, _ -> "$count tickets" },
                    handle = { "Drag $it" },
                    move = "Shift",
                    expanded = "Open",
                ),
            )
        }
        rule.onNodeWithText("2 tickets").assertExists()
        rule.onNodeWithContentDescription("Drag ABC-1").assertExists()
        rule.onAllNodesWithText("Shift", useUnmergedTree = true).onFirst().assertExists()
        assertEquals("Open", rule.onNode(isHeading() and hasText("Sprint 14")).stateDescription())
    }

    @Test
    fun rightToLeftPutsTheHandleAtTheStart() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Planner(plan()) }
        rule.onNodeWithContentDescription("Move ABC-1").assertIsBefore(
            rule.onNodeWithText("Fix login", useUnmergedTree = true),
            LayoutDirection.Rtl,
        )
        rule.onNodeWithContentDescription("Move ABC-1").assertIsEnabled()
    }

    @Test
    fun longTitlesAreNotCutAtTheLargestFontScale() {
        val long = MonarchBacklogBucket(
            "sprint",
            "Sprint 14: payments, onboarding and the new billing page",
            listOf(Login),
            subtitle = "From Monday, November 10 to Friday, November 21",
        )
        rule.setMonarchContent(fontScale = LargestFontScale) { Planner(listOf(long)) }
        rule.onNodeWithText(long.title, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText(long.subtitle!!, useUnmergedTree = true).assertTextFullyVisible()
        assertTrue(rule.onNodeWithText("Fix login", useUnmergedTree = true).fetchSemanticsNode().size.height > 0)
    }
}
