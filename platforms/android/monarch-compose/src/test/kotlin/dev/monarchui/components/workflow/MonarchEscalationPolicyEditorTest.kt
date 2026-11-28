package dev.monarchui.components.workflow

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.entitypicker.MonarchEntityPickerEntity
import dev.monarchui.components.workflow.escalationpolicyeditor.MonarchEscalationLevel as Level
import dev.monarchui.components.workflow.escalationpolicyeditor.MonarchEscalationLevelProblem as Problem
import dev.monarchui.components.workflow.escalationpolicyeditor.MonarchEscalationPolicyEditor
import dev.monarchui.components.workflow.escalationpolicyeditor.MonarchEscalationPolicyEditorLabels
import dev.monarchui.components.workflow.escalationpolicyeditor.MonarchEscalationPolicyEditorTokens as Tokens
import dev.monarchui.components.workflow.escalationpolicyeditor.MonarchEscalationTiming
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private val Targets = listOf(
    MonarchEntityPickerEntity("oncall", "On-call rota", description = "Rota"),
    MonarchEntityPickerEntity("lead", "Team lead", description = "Person"),
    MonarchEntityPickerEntity("slack", "#incidents", description = "Channel"),
)
private val One = listOf(Level("l1", 15, listOf("oncall")))
private val Two = One + Level("l2", 45, listOf("lead", "slack"))

internal object MonarchEscalationPolicyEditorFixtures : ComponentFixtures {
    override val id = "escalation-policy-editor"
    override val fixtures = listOf(
        fixture("one level at the minimum") {
            MonarchEscalationPolicyEditor(value = One, onValueChange = {}, targets = Targets)
        },
        fixture("no targets, with the problem") {
            MonarchEscalationPolicyEditor(
                value = listOf(Level("l1", 15)),
                onValueChange = {},
                targets = Targets,
                showErrors = true,
            )
        },
        fixture("out of order, at the limit") {
            MonarchEscalationPolicyEditor(
                value = listOf(Level("l1", 30, listOf("oncall")), Level("l2", 15, listOf("lead"))),
                onValueChange = {},
                targets = Targets,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                maxLevels = 2,
                showErrors = true,
            )
        },
        fixture("nothing to notify") {
            MonarchEscalationPolicyEditor(value = emptyList(), onValueChange = {}, targets = emptyList(), minLevels = 0)
        },
        fixture("disabled") {
            MonarchEscalationPolicyEditor(value = One, onValueChange = {}, targets = Targets, enabled = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchEscalationPolicyEditorTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        start: List<Level>,
        onChange: (List<Level>) -> Unit = {},
        timing: MonarchEscalationTiming = MonarchEscalationTiming.Cumulative,
        showErrors: Boolean = false,
    ) {
        rule.setMonarchContent {
            var value by remember { mutableStateOf(start) }
            MonarchEscalationPolicyEditor(
                value = value,
                onValueChange = {
                    value = it
                    onChange(it)
                },
                targets = Targets,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                timing = timing,
                showErrors = showErrors,
            )
        }
    }

    @Test
    fun levelsAreHeadingsWithASummary() {
        show(Two)
        rule.onNode(hasText("Level 2") and isHeading()).assertExists()
        rule.onNodeWithText("After 15m, notify On-call rota").assertExists()
        rule.onNodeWithText("After 45m, notify Team lead, #incidents").performScrollTo()
        rule.onNodeWithContentDescription("Remove level 1").assertIsEnabled()
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun aLevelWithoutTargetsSaysNobodyYet() {
        show(listOf(Level("l1", 15)))
        rule.onNodeWithText("After 15m, notify nobody yet").assertExists()
    }

    @Test
    fun removeIsDisabledAtTheMinimumAndRemovesOtherwise() {
        var result: List<Level> = emptyList()
        show(Two, { result = it })
        rule.onNodeWithContentDescription("Remove level 1").performClick()
        assertEquals(listOf("l2"), result.map { it.id })
        rule.onNodeWithContentDescription("Remove level 1").assertIsNotEnabled()
    }

    @Test
    fun addStartsAfterTheLastLevelWithCumulativeTiming() {
        var result: List<Level> = emptyList()
        show(One, { result = it })
        rule.onNodeWithText("Add level").performScrollTo().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(Level("level-1", 30), result.last())
    }

    @Test
    fun addUsesTheStepWithIntervalTiming() {
        var result: List<Level> = emptyList()
        show(Two, { result = it }, timing = MonarchEscalationTiming.Interval)
        rule.onNodeWithText("Add level").performScrollTo().performClick()
        assertEquals(15, result.last().afterMinutes)
    }

    @Test
    fun theTimeIsTypedInThePlainField() {
        var result: List<Level> = emptyList()
        show(One, { result = it })
        val field = rule.onNode(hasSetTextAction())
        field.performTextClearance()
        field.performTextInput("20m")
        field.performImeAction()
        assertEquals(20, result.single().afterMinutes)
    }

    @Test
    fun aLevelWithoutTargetsShowsTheProblemOnThePicker() {
        show(listOf(Level("l1", 15)), showErrors = true)
        rule.onNodeWithText("Choose at least one target", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun anEarlierLevelShowsTheOrderProblemOnTheTime() {
        show(listOf(Level("l1", 30, listOf("oncall")), Level("l2", 15, listOf("lead"))), showErrors = true)
        rule.onNodeWithText("This level must come later", substring = true, useUnmergedTree = true)
            .performScrollTo()
    }

    @Test
    fun aMissingTargetListShowsAWarning() {
        rule.setMonarchContent {
            MonarchEscalationPolicyEditor(value = emptyList(), onValueChange = {}, targets = emptyList())
        }
        rule.onNodeWithText("There is nothing to notify yet", substring = true, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun theMaximumReplacesTheAddButtonWithANote() {
        rule.setMonarchContent {
            MonarchEscalationPolicyEditor(value = One, onValueChange = {}, targets = Targets, maxLevels = 1)
        }
        rule.onNodeWithText("Add level").assertDoesNotExist()
        rule.onNodeWithText("A policy can have up to 1 level.").assertExists()
    }

    @Test
    fun disabledEditorCannotChange() {
        rule.setMonarchContent {
            MonarchEscalationPolicyEditor(value = Two, onValueChange = { error("changed") }, targets = Targets,
                enabled = false, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onNodeWithContentDescription("Remove level 1").assertIsNotEnabled()
        rule.onNodeWithText("Add level").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun theRemoveButtonIsAtTheEndRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchEscalationPolicyEditor(value = One, onValueChange = {}, targets = Targets)
        }
        rule.onNodeWithText("Level 1").assertIsBefore(
            rule.onNodeWithContentDescription("Remove level 1"),
            LayoutDirection.Rtl,
        )
    }

    @Test
    fun theSummaryIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchEscalationPolicyEditor(value = One, onValueChange = {}, targets = Targets)
        }
        rule.onNodeWithText("After 15m, notify On-call rota").assertTextFullyVisible()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchEscalationPolicyEditor(
                value = One,
                onValueChange = {},
                targets = Targets,
                labels = MonarchEscalationPolicyEditorLabels(
                    level = { "Tier $it" },
                    summary = { d, names -> "$names after $d" },
                    add = "Add tier",
                ),
            )
        }
        rule.onNodeWithText("Tier 1").assertExists()
        rule.onNodeWithText("On-call rota after 15m").assertExists()
        rule.onNodeWithText("Add tier").assertExists()
    }

    @Test
    fun validateFollowsTheWebRules() {
        assertEquals(emptyMap(), Level.validate(Two))
        assertEquals(mapOf("x" to Problem.Targets), Level.validate(listOf(Level("x", 15))))
        val equal = listOf(Level("a", 15, listOf("t")), Level("b", 15, listOf("t")))
        assertEquals(mapOf("b" to Problem.Order), Level.validate(equal))
        assertEquals(emptyMap(), Level.validate(equal, MonarchEscalationTiming.Interval))
    }

    @Test
    fun tokensKeepTargetOrderAndFormatDurations() {
        val order = Targets.map { it.id }
        assertEquals(listOf("oncall", "slack", "x"), Tokens.ordered(listOf("x", "slack", "oncall"), order))
        assertEquals("1d 2h", Tokens.duration(1560))
        assertEquals("level-2", Tokens.freshId(listOf("level-1")))
        assertEquals(60, Tokens.nextMinutes(Two, MonarchEscalationTiming.Cumulative, 15))
    }

    @Test
    fun anInvalidLevelTakesTheDangerOutlineInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.borderSubtle, Tokens.cardColors(colors, false).border)
            assertEquals(colors.dangerBorder, Tokens.cardColors(colors, true).border)
            assertEquals(colors.textMuted, Tokens.noteColor(colors))
        }
    }
}
