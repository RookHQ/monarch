package com.rookhq.monarch.components.workflow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.entitypicker.MonarchEntityPickerEntity
import com.rookhq.monarch.components.workflow.approvalchaineditor.MonarchApprovalApproverType
import com.rookhq.monarch.components.workflow.approvalchaineditor.MonarchApprovalChainEditor
import com.rookhq.monarch.components.workflow.approvalchaineditor.MonarchApprovalChainEditorLabels
import com.rookhq.monarch.components.workflow.approvalchaineditor.MonarchApprovalChainEditorTokens
import com.rookhq.monarch.components.workflow.approvalchaineditor.MonarchApprovalChainStep as Step
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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

private val Finance = MonarchEntityPickerEntity("finance", "Finance team")
private val Legal = MonarchEntityPickerEntity("legal", "Legal team")
private val Types = listOf(
    MonarchApprovalApproverType("manager", "Reporting manager"),
    MonarchApprovalApproverType("role", "Anyone with a role", targets = listOf(Finance, Legal), targetLabel = "Role"),
)
private val Two = listOf(Step("a", "manager"), Step("b", "role", target = "finance"))

internal object MonarchApprovalChainEditorFixtures : ComponentFixtures {
    override val id = "approval-chain-editor"
    override val fixtures = listOf(
        fixture("one step at the minimum") {
            MonarchApprovalChainEditor(value = listOf(Step("a", "manager")), onValueChange = {}, approverTypes = Types)
        },
        fixture("target missing, with the error") {
            MonarchApprovalChainEditor(
                value = listOf(Step("a", "role")),
                onValueChange = {},
                approverTypes = Types,
                showLabels = false,
                showErrors = true,
            )
        },
        fixture("two steps without labels, at the limit") {
            MonarchApprovalChainEditor(
                value = Two,
                onValueChange = {},
                approverTypes = Types,
                description = "Each step must approve before the next is asked.",
                showLabels = false,
                maxSteps = 2,
            )
        },
        fixture("disabled") {
            MonarchApprovalChainEditor(
                value = Two,
                onValueChange = {},
                approverTypes = Types,
                showLabels = false,
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchApprovalChainEditorTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(start: List<Step>, onChange: (List<Step>) -> Unit = {}, showLabels: Boolean = false) {
        rule.setMonarchContent {
            var value by remember { mutableStateOf(start) }
            MonarchApprovalChainEditor(
                value = value,
                onValueChange = {
                    value = it
                    onChange(it)
                },
                approverTypes = Types,
                showLabels = showLabels,
            )
        }
    }

    @Test
    fun stepsAreHeadingsWithNamedRemoveButtons() {
        show(Two)
        rule.onNode(hasText("Steps, in order") and isHeading()).assertExists()
        rule.onNode(hasText("Step 2") and isHeading()).assertExists()
        rule.onNodeWithContentDescription("Remove step 1").assertIsEnabled()
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun removeIsDisabledAtTheMinimum() {
        show(listOf(Step("a", "manager")))
        rule.onNodeWithContentDescription("Remove step 1").assertIsNotEnabled()
    }

    @Test
    fun addAppendsTheFirstKindWithAFreshId() {
        var result: List<Step> = emptyList()
        show(listOf(Step("step-1", "manager")), { result = it })
        rule.onNodeWithText("Add step").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(Step("step-1", "manager"), Step("step-2", "manager")), result)
    }

    @Test
    fun theMaximumReplacesTheAddButtonWithANote() {
        rule.setMonarchContent {
            MonarchApprovalChainEditor(value = Two, onValueChange = {}, approverTypes = Types, maxSteps = 2,
                showLabels = false)
        }
        rule.onNodeWithText("Add step").assertDoesNotExist()
        rule.onNodeWithText("A chain can have up to 2 steps.").assertExists()
    }

    @Test
    fun moveButtonsReorderAndAnnounce() {
        var result: List<Step> = emptyList()
        show(Two, { result = it })
        rule.onNodeWithContentDescription("Move Step 1, Reporting manager down").performClick()
        assertEquals(listOf("b", "a"), result.map { it.id })
        rule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite) and
                SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
        ).assertExists()
    }

    @Test
    fun theHandleOffersMoveActionsToTalkBack() {
        show(Two)
        val actions = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
            .fetchSemanticsNodes()
            .flatMap { node -> node.config[SemanticsActions.CustomActions].map { it.label } }
        assertEquals(true, "Move down" in actions)
    }

    @Test
    fun aKindWithTargetsShowsThePickerAndTheErrorAfterASaveAttempt() {
        rule.setMonarchContent {
            MonarchApprovalChainEditor(
                value = listOf(Step("a", "role")),
                onValueChange = {},
                approverTypes = Types,
                showLabels = false,
                showErrors = true,
            )
        }
        rule.onNodeWithText("Choose who decides this step.", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun changingTheKindDropsATargetItDoesNotOffer() {
        var result: List<Step> = emptyList()
        show(listOf(Step("a", "role", target = "finance")), { result = it })
        rule.onNodeWithText("Anyone with a role").performClick()
        rule.onNodeWithText("Reporting manager").performClick()
        assertEquals(listOf(Step("a", "manager")), result)
    }

    @Test
    fun typingALabelStoresItAndClearingRemovesIt() {
        var result: List<Step> = emptyList()
        show(listOf(Step("a", "manager")), { result = it }, showLabels = true)
        rule.onNodeWithContentDescription("Label (optional)").performTextInput("Finance check")
        assertEquals("Finance check", result.single().label)
    }

    @Test
    fun disabledEditorCannotChange() {
        rule.setMonarchContent {
            MonarchApprovalChainEditor(value = Two, onValueChange = { error("changed") }, approverTypes = Types,
                showLabels = false, enabled = false)
        }
        rule.onNodeWithText("Add step").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove step 1").assertIsNotEnabled()
    }

    @Test
    fun theRemoveButtonIsAtTheEndRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchApprovalChainEditor(value = Two, onValueChange = {}, approverTypes = Types, showLabels = false)
        }
        rule.onNodeWithText("Step 1").assertIsBefore(
            rule.onNodeWithContentDescription("Remove step 1"),
            LayoutDirection.Rtl,
        )
    }

    @Test
    fun theHeadingIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchApprovalChainEditor(
                value = listOf(Step("a", "manager")),
                onValueChange = {},
                approverTypes = Types,
                showLabels = false,
                labels = MonarchApprovalChainEditorLabels(title = "Who approves, in order"),
            )
        }
        rule.onNodeWithText("Who approves, in order").assertTextFullyVisible()
    }

    @Test
    fun validateAndSummarizeFollowTheWeb() {
        assertEquals(setOf("x"), Step.validate(listOf(Step("x", "role"), Step("y", "manager")), Types))
        assertEquals(emptySet(), Step.validate(Two, Types))
        assertEquals(
            "Reporting manager → Finance check (Anyone with a role: Finance team)",
            Step.summarize(listOf(Step("a", "manager"), Step("b", "role", "finance", "Finance check")), Types),
        )
        assertEquals("gone", Step.summarize(listOf(Step("a", "gone")), Types))
        assertEquals("step-2", MonarchApprovalChainEditorTokens.freshId(listOf("step-1")))
        assertEquals(listOf("b", "a"), MonarchApprovalChainEditorTokens.moved(Two, 0, 1).map { it.id })
    }

    @Test
    fun theNoteUsesTheMutedTextInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchApprovalChainEditorTokens.noteColor(colors))
        }
    }
}
