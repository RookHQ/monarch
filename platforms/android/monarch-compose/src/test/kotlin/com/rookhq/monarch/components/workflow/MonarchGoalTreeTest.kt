@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.workflow

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.workflow.goaltree.GoalTreeLine
import com.rookhq.monarch.components.workflow.goaltree.MonarchGoal
import com.rookhq.monarch.components.workflow.goaltree.MonarchGoalStatus
import com.rookhq.monarch.components.workflow.goaltree.MonarchGoalTree
import com.rookhq.monarch.components.workflow.goaltree.MonarchGoalTreeLabels
import com.rookhq.monarch.components.workflow.goaltree.MonarchGoalTreeTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val Objective = MonarchGoal(
    id = "o1",
    title = "Grow revenue",
    progress = 0.45f,
    status = MonarchGoalStatus.OnTrack,
    ownerName = "Ana Ruiz",
    meta = "weight 40%",
)
private val KeyResultA = MonarchGoal(
    id = "k1",
    parentId = "o1",
    title = "Raise renewals",
    progress = 0.6f,
    status = MonarchGoalStatus.OnTrack,
)
private val KeyResultB = MonarchGoal(
    id = "k2",
    parentId = "o1",
    title = "Cut churn",
    progress = 0.2f,
    status = MonarchGoalStatus.AtRisk,
)
private val Goals = listOf(Objective, KeyResultA, KeyResultB)

private val Chevron: @androidx.compose.runtime.Composable (
) -> Unit = { MonarchIcon(TestIcon, contentDescription = null) }

internal object MonarchGoalTreeFixtures : ComponentFixtures {
    override val id = "goal-tree"
    override val fixtures = listOf(
        fixture("objective with key results") { MonarchGoalTree(goals = Goals, expandIcon = Chevron) },
        fixture("closed, with add and actions") {
            MonarchGoalTree(
                goals = Goals,
                expandIcon = Chevron,
                expanded = emptySet(),
                onAddChild = {},
                actions = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("open with add button, loud children") {
            MonarchGoalTree(
                goals = listOf(Objective, KeyResultA),
                expandIcon = Chevron,
                quietChildren = false,
                onAddChild = {},
            )
        },
        fixture("every status, flat") {
            MonarchGoalTree(
                goals = MonarchGoalStatus.entries.map {
                    MonarchGoal(id = it.name, title = it.name, progress = 0.5f, status = it)
                }.take(2),
                expandIcon = Chevron,
            )
        },
        fixture("remaining statuses, aligned") {
            MonarchGoalTree(
                goals = MonarchGoalStatus.entries.drop(2).map {
                    MonarchGoal(id = it.name, title = it.name, progress = 1f, status = it, alignedTo = "Company goal")
                }.take(2),
                expandIcon = Chevron,
            )
        },
        fixture("empty") { MonarchGoalTree(goals = emptyList(), expandIcon = Chevron) },
        fixture("long title") {
            MonarchGoalTree(
                goals = listOf(
                    MonarchGoal(
                        id = "l",
                        title = "Make every invoice reconcile automatically before the month closes",
                        progress = Float.NaN,
                        status = MonarchGoalStatus.Done,
                    ),
                ),
                expandIcon = Chevron,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchGoalTreeTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun eachGoalIsOneElementWithItsFactsAndProgress() {
        rule.setMonarchContent { MonarchGoalTree(goals = Goals, expandIcon = Chevron) }
        rule.onNodeWithContentDescription("Grow revenue, On track, Ana Ruiz, weight 40%")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(0.45f, 0f..1f),
                ),
            )
            .also { assertEquals("45%", it.stateDescription()) }
        rule.onNodeWithContentDescription("Cut churn, At risk").assertExists()
    }

    @Test
    fun goalsWithChildrenStartOpenAndToggleWithANamedButton() {
        rule.setMonarchContent { MonarchGoalTree(goals = Goals, expandIcon = Chevron) }
        rule.onNodeWithContentDescription("Raise renewals, On track").assertExists()
        rule.onNodeWithContentDescription("Collapse Grow revenue")
            .assert(isButton)
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithContentDescription("Raise renewals, On track").assertDoesNotExist()
        rule.onNodeWithContentDescription("Grow revenue, On track, Ana Ruiz, weight 40%, 2 key results").assertExists()
        rule.onNodeWithContentDescription("Expand Grow revenue").performClick()
        rule.onNodeWithContentDescription("Raise renewals, On track").assertExists()
    }

    @Test
    fun aControlledTreeReportsTheNewSetAndFollowsIt() {
        var open by mutableStateOf(emptySet<String>())
        val reported = mutableListOf<Set<String>>()
        rule.setMonarchContent {
            MonarchGoalTree(
                goals = Goals,
                expandIcon = Chevron,
                expanded = open,
                onExpandedChange = { reported += it },
            )
        }
        rule.onNodeWithContentDescription("Expand Grow revenue").performClick()
        assertEquals(listOf(setOf("o1")), reported)
        rule.onNodeWithContentDescription("Raise renewals, On track").assertDoesNotExist()
        open = setOf("o1")
        rule.onNodeWithContentDescription("Raise renewals, On track").assertExists()
    }

    @Test
    fun theAddButtonIsNamedAndReceivesTheParent() {
        val added = mutableListOf<String>()
        rule.setMonarchContent { MonarchGoalTree(goals = Goals, expandIcon = Chevron, onAddChild = { added += it.id }) }
        rule.onNodeWithContentDescription("Add key result to Grow revenue")
            .assert(isButton)
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("o1"), added)
    }

    @Test
    fun aGoalWhoseOnlyChildIsTheAddButtonStillToggles() {
        rule.setMonarchContent {
            MonarchGoalTree(goals = listOf(Objective), expandIcon = Chevron, expanded = setOf("o1"), onAddChild = {})
        }
        rule.onNodeWithContentDescription("Collapse Grow revenue").assertExists()
        rule.onNodeWithText("Add key result", useUnmergedTree = true).assertExists()
    }

    @Test
    fun actionsAreSeparateElementsAtTheEnd() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchGoalTree(
                goals = listOf(Objective),
                expandIcon = Chevron,
                actions = { MonarchIcon(TestIcon, contentDescription = "More for ${it.title}") },
            )
        }
        rule.onNodeWithContentDescription("Grow revenue, On track, Ana Ruiz, weight 40%")
            .assertIsBefore(rule.onNodeWithContentDescription("More for Grow revenue"), LayoutDirection.Rtl)
    }

    @Test
    fun anEmptyTreeShowsTheEmptyState() {
        rule.setMonarchContent {
            Column {
                MonarchGoalTree(goals = emptyList(), expandIcon = Chevron)
                MonarchGoalTree(
                    goals = emptyList(),
                    expandIcon = Chevron,
                    labels = MonarchGoalTreeLabels(empty = "Sin objetivos"),
                )
            }
        }
        rule.onNodeWithText("No goals yet").assertExists()
        rule.onNodeWithText("Sin objetivos").assertExists()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchGoalTree(
                goals = Goals + MonarchGoal(
                    id = "x",
                    title = "Other",
                    progress = 0f,
                    status = MonarchGoalStatus.Done,
                    alignedTo = "Company",
                ),
                expandIcon = Chevron,
                expanded = setOf("o1"),
                onAddChild = {},
                labels = MonarchGoalTreeLabels(
                    status = { if (it == MonarchGoalStatus.Done) "Hecho" else it.name },
                    collapse = { "Cerrar $it" },
                    alignedTo = { "Alineado con $it" },
                    addChild = "Añadir",
                    addChildTo = { "Añadir a $it" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Cerrar Grow revenue").assertExists()
        rule.onNodeWithContentDescription("Añadir a Grow revenue").assertExists()
        rule.onNodeWithContentDescription("Other, Hecho, Alineado con Company").assertExists()
    }

    @Test
    fun theTitleIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchGoalTree(goals = listOf(Objective), expandIcon = Chevron)
        }
        rule.onNodeWithText("Grow revenue", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun progressIsClampedAndNotANumberIsZero() {
        val tokens = MonarchGoalTreeTokens
        assertEquals(0, tokens.percent(Float.NaN))
        assertEquals(0, tokens.percent(-1f))
        assertEquals(100, tokens.percent(1.4f))
        assertEquals(63, tokens.percent(0.625f))
        assertEquals(0f, tokens.fraction(Float.NEGATIVE_INFINITY))
        assertEquals(1f, tokens.fraction(Float.POSITIVE_INFINITY))
    }

    @Test
    fun statusesMapToTones() {
        assertEquals(
            listOf(MonarchTone.Neutral, MonarchTone.Info, MonarchTone.Warning, MonarchTone.Danger, MonarchTone.Success),
            MonarchGoalStatus.entries.map { MonarchGoalTreeTokens.tone(it) },
        )
        assertTrue(MonarchGoalTreeTokens.showsBadge(MonarchGoalStatus.OnTrack, 0, true))
        assertFalse(MonarchGoalTreeTokens.showsBadge(MonarchGoalStatus.NotStarted, 1, true))
        assertTrue(MonarchGoalTreeTokens.showsBadge(MonarchGoalStatus.OnTrack, 1, false))
        assertTrue(MonarchGoalTreeTokens.showsBadge(MonarchGoalStatus.Done, 2, true))
    }

    @Test
    fun missingSelfAndRepeatedParentsAndCyclesAreSafe() {
        val goals = listOf(
            MonarchGoal(id = "a", parentId = "missing", title = "A", progress = 0f, status = MonarchGoalStatus.OnTrack),
            MonarchGoal(id = "b", parentId = "b", title = "B", progress = 0f, status = MonarchGoalStatus.OnTrack),
            MonarchGoal(id = "c", parentId = "a", title = "C", progress = 0f, status = MonarchGoalStatus.OnTrack),
            MonarchGoal(id = "c", parentId = "b", title = "C again", progress = 0f, status = MonarchGoalStatus.OnTrack),
            MonarchGoal(id = "x", parentId = "y", title = "X", progress = 0f, status = MonarchGoalStatus.OnTrack),
            MonarchGoal(id = "y", parentId = "x", title = "Y", progress = 0f, status = MonarchGoalStatus.OnTrack),
        )
        val (roots, children) = MonarchGoalTreeTokens.structure(goals)
        assertEquals(listOf("a", "b"), roots.map { it.id })
        assertEquals(listOf("C"), children["a"]?.map { it.title })
        val lines = MonarchGoalTreeTokens.lines(goals, setOf("a", "b", "x", "y")) { _, _ -> false }
        assertEquals(listOf("a", "c", "b"), lines.filterIsInstance<GoalTreeLine.Goal>().map { it.goal.id })
        assertEquals(setOf("a", "x", "y"), MonarchGoalTreeTokens.initiallyExpanded(goals))
    }

    @Test
    fun linesListAddButtonsAfterTheChildrenOfOpenGoals() {
        val lines = MonarchGoalTreeTokens.lines(Goals, setOf("o1")) { _, depth -> depth == 0 }
        assertEquals(4, lines.size)
        assertEquals(GoalTreeLine.Add(Objective, 1), lines.last())
        val closed = MonarchGoalTreeTokens.lines(Goals, emptySet()) { _, depth -> depth == 0 }
        assertEquals(1, closed.size)
        assertEquals(2, (closed.single() as GoalTreeLine.Goal).childCount)
        assertEquals(setOf("o1"), MonarchGoalTreeTokens.toggled(emptySet(), "o1"))
        assertEquals(emptySet(), MonarchGoalTreeTokens.toggled(setOf("o1"), "o1"))
    }
}
