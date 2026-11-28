@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStep
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStepState
import com.rookhq.monarch.components.intelligence.aiplan.MonarchAiPlan
import com.rookhq.monarch.components.intelligence.aiplan.MonarchAiPlanDecision
import com.rookhq.monarch.components.intelligence.aiplan.MonarchAiPlanLabels
import com.rookhq.monarch.components.intelligence.aiplan.MonarchAiPlanTokens
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
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private val Draft = MonarchAiPlanStep(id = "a", label = "Draft the reply")
private val Send = MonarchAiPlanStep(id = "b", label = "Send it ")
private val Blank = MonarchAiPlanStep(id = "c", label = "  ")
private val Plan = MonarchAiPlanPart(listOf(Draft, Send))
private val Brief = MonarchAiPlanLabels(description = "Review.", editHint = "Edit first.")

internal object MonarchAiPlanFixtures : ComponentFixtures {
    override val id = "ai-plan"
    override val fixtures = listOf(
        // The first cell has no earlier cell for the screenshot gate to compare with, so it has no text field.
        fixture("read-only, pending") { MonarchAiPlan(part = Plan, editable = false, labels = Brief) },
        fixture("approved") {
            MonarchAiPlan(
                part = MonarchAiPlanPart(listOf(Draft.copy(state = MonarchAiPlanStepState.Active), Send)),
                decision = MonarchAiPlanDecision.Approved,
            )
        },
        fixture("rejected") { MonarchAiPlan(part = Plan, decision = MonarchAiPlanDecision.Rejected) },
        fixture("editable, one step") {
            MonarchAiPlan(part = MonarchAiPlanPart(listOf(Draft)), labels = Brief)
        },
        fixture("editable with a remove icon") {
            MonarchAiPlan(
                part = MonarchAiPlanPart(listOf(Send)),
                removeIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                labels = Brief,
            )
        },
        fixture("empty plan") { MonarchAiPlan(part = MonarchAiPlanPart(listOf(Blank)), labels = Brief) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiPlanTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchAiPlanTokens

    @Test
    fun approvingHandsOverTheCleanedStepsAndShowsTheLiveSteps() {
        var approved: List<MonarchAiPlanStep>? = null
        rule.setMonarchContent {
            Box(Modifier.verticalScroll(rememberScrollState())) {
                MonarchAiPlan(part = MonarchAiPlanPart(listOf(Draft, Send, Blank)), onApprove = { approved = it })
            }
        }
        rule.onNodeWithText("Proposed plan").assertExists()
        rule.onNodeWithText("Approve plan").performScrollTo().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(Draft, Send.copy(label = "Send it")), approved)
        rule.onNodeWithText("Plan approved. The assistant is working through it.").assertExists()
        rule.onNodeWithText("Approve plan").assertDoesNotExist()
        rule.onNodeWithText("Add step").assertDoesNotExist()
        rule.onNodeWithContentDescription("Plan approved. The assistant is working through it.").assertExists()
    }

    @Test
    fun rejectingSaysSoAndRemovesTheButtons() {
        var rejected = 0
        rule.setMonarchContent { MonarchAiPlan(part = Plan, onReject = { rejected++ }) }
        rule.onNodeWithText("Reject").performClick()
        assertEquals(1, rejected)
        rule.onNodeWithText("Plan rejected. The assistant will not take these steps.").assertExists()
        rule.onNodeWithText("Reject").assertDoesNotExist()
    }

    @Test
    fun anEmptyPlanCannotBeApproved() {
        rule.setMonarchContent { MonarchAiPlan(part = MonarchAiPlanPart(listOf(Blank)), onApprove = {}) }
        rule.onNodeWithText("Approve plan").assertIsNotEnabled()
        rule.onNodeWithText("Add at least one step to approve the plan.").assertExists()
        rule.onNodeWithContentDescription("Remove step 1").assertExists()
    }

    @Test
    fun removingAStepReportsTheDraftAndIsAnnounced() {
        val changes = mutableListOf<List<MonarchAiPlanStep>>()
        rule.setMonarchContent { MonarchAiPlan(part = Plan, onChange = { changes += it }) }
        rule.onNodeWithContentDescription("Remove Send it").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(listOf(Draft)), changes)
        rule.onNodeWithContentDescription("Removed Send it").assertExists()
        rule.onNodeWithContentDescription("Remove Send it").assertDoesNotExist()
    }

    @Test
    fun addingAStepAppendsAnEmptyStep() {
        val changes = mutableListOf<List<MonarchAiPlanStep>>()
        rule.setMonarchContent { MonarchAiPlan(part = Plan, onChange = { changes += it }) }
        rule.onNodeWithText("Add step").performClick()
        assertEquals(listOf(Draft, Send, MonarchAiPlanStep(id = "new-1", label = "")), changes.single())
    }

    @Test
    fun theHandleMovesAStepWithTalkBackActions() {
        val changes = mutableListOf<List<MonarchAiPlanStep>>()
        rule.setMonarchContent { MonarchAiPlan(part = Plan, onChange = { changes += it }) }
        rule.onNodeWithContentDescription("Reorder Draft the reply").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first { it.label == "Move down" }.action()
        rule.waitForIdle()
        assertEquals(listOf(listOf(Send, Draft)), changes)
    }

    @Test
    fun theMoveButtonsReorderTheSteps() {
        val changes = mutableListOf<List<MonarchAiPlanStep>>()
        rule.setMonarchContent { MonarchAiPlan(part = Plan, onChange = { changes += it }) }
        rule.onNodeWithContentDescription("Move Draft the reply down").performClick()
        assertEquals(listOf(listOf(Send, Draft)), changes)
        rule.onNodeWithContentDescription("Move Send it up").assertIsNotEnabled()
    }

    @Test
    fun aReadOnlyPlanStillDecides() {
        rule.setMonarchContent { MonarchAiPlan(part = Plan, editable = false) }
        rule.onNodeWithText("Add step").assertDoesNotExist()
        rule.onNodeWithText("The assistant will take these steps after you approve. Nothing runs until then.")
            .assertExists()
        rule.onNodeWithText("Approve plan").assertIsEnabled()
    }

    @Test
    fun aDecisionKeptByTheAppWithoutCallbacksShowsNoButtons() {
        rule.setMonarchContent { MonarchAiPlan(part = Plan, decision = MonarchAiPlanDecision.Pending) }
        rule.onNodeWithText("Approve plan").assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiPlan(
                part = Plan,
                onApprove = {},
                labels = MonarchAiPlanLabels(title = "Plan propuesto", approve = "Aprobar", remove = { "Quitar $it" }),
            )
        }
        rule.onNodeWithText("Plan propuesto").assertExists()
        rule.onNodeWithText("Aprobar").assertExists()
        rule.onNodeWithContentDescription("Quitar Draft the reply").assertExists()
    }

    @Test
    fun rejectComesBeforeApproveRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { MonarchAiPlan(part = Plan, editable = false) }
        rule.onNodeWithText("Reject").assertIsBefore(rule.onNodeWithText("Approve plan"), LayoutDirection.Rtl)
    }

    @Test
    fun theDescriptionWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiPlan(part = MonarchAiPlanPart(listOf(Draft)), labels = Brief)
        }
        rule.onNodeWithText("Review. Edit first.").assertTextFullyVisible()
    }

    @Test
    fun theDraftLogic() {
        assertEquals(listOf(Draft, Send.copy(label = "Send it")), tokens.cleaned(listOf(Draft, Blank, Send)))
        assertEquals(listOf(Send, Draft), tokens.moved(listOf(Draft, Send), 0, 1))
        assertEquals(listOf(Draft, Send), tokens.moved(listOf(Draft, Send), 0, 5))
        assertEquals(listOf(Draft.copy(label = "x"), Send), tokens.relabelled(listOf(Draft, Send), "a", "x"))
        assertEquals("new-2", tokens.newId(1, listOf(MonarchAiPlanStep(id = "new-1", label = ""))))
        assertEquals("step 3", tokens.name(Blank, "step 3"))
        assertEquals(true, tokens.readOnly(MonarchAiPlanDecision.Approved, editable = true))
        assertEquals(true, tokens.readOnly(MonarchAiPlanDecision.Pending, editable = false))
        assertEquals(false, tokens.showsDecision(MonarchAiPlanDecision.Pending, controlled = true, handled = false))
        assertEquals(true, tokens.showsDecision(MonarchAiPlanDecision.Pending, controlled = false, handled = false))
        assertEquals(listOf(Draft), tokens.shownSteps(listOf(Draft, Blank), MonarchAiPlanDecision.Approved))
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textDefault, tokens.title(c), theme.id)
            assertEquals(c.textMuted, tokens.muted(c), theme.id)
        }
    }
}
