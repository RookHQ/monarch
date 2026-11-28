package dev.monarchui.components.workflow

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.workflow.actioneditor.MonarchActionEditor
import dev.monarchui.components.workflow.actioneditor.MonarchActionEditorLabels
import dev.monarchui.components.workflow.actioneditor.MonarchActionEditorTokens
import dev.monarchui.components.workflow.actioneditor.MonarchActionParameter
import dev.monarchui.components.workflow.actioneditor.MonarchActionParameterKind as Pt
import dev.monarchui.components.workflow.actioneditor.MonarchActionProblem as Problem
import dev.monarchui.components.workflow.actioneditor.MonarchActionType
import dev.monarchui.components.workflow.actioneditor.MonarchActionValue as Pv
import dev.monarchui.components.workflow.actioneditor.MonarchRuleAction
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val Close = MonarchActionType(key = "close", label = "Close ticket", description = "Closes without a reply.")
private val Priority = MonarchActionType(
    key = "priority",
    label = "Set priority",
    group = "Fields",
    parameters = listOf(
        MonarchActionParameter(
            key = "level",
            label = "Priority",
            kind = Pt.Select,
            options = listOf(MonarchSelectOption("high", "High"), MonarchSelectOption("low", "Low")),
            required = true,
        ),
    ),
)
private val Notify = MonarchActionType(
    key = "notify",
    label = "Notify",
    parameters = listOf(
        MonarchActionParameter(key = "urgent", label = "Mark as urgent", kind = Pt.Boolean),
        MonarchActionParameter(
            key = "message",
            label = "Message",
            kind = Pt.TextArea,
            required = true,
            visibleWhen = { (it["urgent"] as? Pv.Flag)?.checked == true },
        ),
    ),
)
private val Wait = MonarchActionType(
    key = "wait",
    label = "Wait",
    parameters = listOf(
        MonarchActionParameter(key = "hours", label = "Hours", kind = Pt.Number, min = 1.0, max = 72.0),
    ),
    defaults = mapOf("hours" to Pv.Number(4.0)),
    validate = { if ((it["hours"] as? Pv.Number)?.parsed == 13.0) "Avoid 13 hours." else null },
)
private val Tag = MonarchActionType(
    key = "tag",
    label = "Add tags",
    parameters = listOf(MonarchActionParameter(key = "tags", label = "Tags", kind = Pt.Tags)),
)
private val Sms =
    MonarchActionType(key = "sms", label = "Send SMS", enabled = false, disabledReason = "Connect a provider first.")
private val Types = listOf(Close, Priority, Notify, Wait, Tag, Sms)

internal object MonarchActionEditorFixtures : ComponentFixtures {
    override val id = "action-editor"
    override val fixtures = listOf(
        fixture("empty") { MonarchActionEditor(value = emptyList(), onValueChange = {}, actionTypes = Types) },
        fixture("no parameters, at the limit") {
            MonarchActionEditor(
                value = listOf(MonarchRuleAction("a", "close")),
                onValueChange = {},
                actionTypes = Types,
                maxActions = 1,
            )
        },
        fixture("required option with an error") {
            MonarchActionEditor(
                value = listOf(MonarchRuleAction("a", "priority")),
                onValueChange = {},
                actionTypes = Types,
                maxActions = 1,
                showErrors = true,
            )
        },
        fixture("checkbox") {
            MonarchActionEditor(
                value = listOf(MonarchRuleAction("a", "notify")),
                onValueChange = {},
                actionTypes = Types,
                maxActions = 1,
            )
        },
        fixture("number with the type's own problem") {
            MonarchActionEditor(
                value = listOf(MonarchRuleAction("a", "wait", mapOf("hours" to Pv.Number("13")))),
                onValueChange = {},
                actionTypes = Types,
                maxActions = 1,
                showErrors = true,
            )
        },
        fixture("tags") {
            MonarchActionEditor(
                value = listOf(MonarchRuleAction("a", "tag", mapOf("tags" to Pv.Values(listOf("vip"))))),
                onValueChange = {},
                actionTypes = Types,
                maxActions = 1,
            )
        },
        fixture("unavailable type, disabled") {
            MonarchActionEditor(
                value = listOf(MonarchRuleAction("a", "sms")),
                onValueChange = {},
                actionTypes = Types,
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchActionEditorTest {
    @get:Rule
    val rule = createComposeRule()

    private val two = listOf(MonarchRuleAction("a", "close"), MonarchRuleAction("b", "priority"))

    private fun show(
        value: List<MonarchRuleAction>,
        onChange: (List<MonarchRuleAction>) -> Unit = {},
        showErrors: Boolean = false,
    ) {
        rule.setMonarchContent {
            MonarchActionEditor(
                value = value,
                onValueChange = onChange,
                actionTypes = Types,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                showErrors = showErrors,
            )
        }
    }

    @Test
    fun addStartsTheFirstEnabledTypeWithItsDefaults() {
        var result: List<MonarchRuleAction>? = null
        rule.setMonarchContent {
            MonarchActionEditor(
                value = emptyList(),
                onValueChange = { result = it },
                actionTypes = Types,
                defaultType = "wait",
            )
        }
        rule.onNodeWithText("No actions yet.").assertExists()
        rule.onNodeWithText("Add action").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(MonarchRuleAction("action-1", "wait", mapOf("hours" to Pv.Number("4")))), result)
    }

    @Test
    fun actionsAreHeadingsWithNamedButtons() {
        show(two)
        rule.onNode(hasText("Action 2") and isHeading()).assertExists()
        rule.onNodeWithContentDescription("Move up: action 1").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Move down: action 1").assertIsEnabled()
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Move down: action 2").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove action 1").performScrollTo()
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun movingReordersAndAnnouncesThePosition() {
        rule.setMonarchContent {
            var actions by remember { mutableStateOf(two) }
            MonarchActionEditor(
                value = actions,
                onValueChange = { actions = it },
                actionTypes = Types,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
        rule.onNodeWithContentDescription("Move down: action 1").performClick()
        rule.onNodeWithContentDescription("Close ticket moved to position 2 of 2.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun anUnorderedEditorHasNoMoveButtons() {
        rule.setMonarchContent {
            MonarchActionEditor(value = two, onValueChange = {}, actionTypes = Types, ordered = false,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onNodeWithContentDescription("Move down: action 1").assertDoesNotExist()
    }

    @Test
    fun removeDropsTheAction() {
        var result: List<MonarchRuleAction>? = null
        show(two, { result = it })
        rule.onNodeWithContentDescription("Remove action 1").performClick()
        assertEquals(listOf("b"), result!!.map { it.id })
    }

    @Test
    fun aCheckboxShowsTheParameterItControls() {
        rule.setMonarchContent {
            var actions by remember { mutableStateOf(listOf(MonarchRuleAction("a", "notify"))) }
            MonarchActionEditor(value = actions, onValueChange = { actions = it }, actionTypes = Types,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        // The parameter's field is named by its label, which sits above the box.
        // A required field is named "Message, required" for TalkBack, so the name is matched by its start.
        rule.onNode(hasContentDescription("Message", substring = true)).assertDoesNotExist()
        rule.onNodeWithText("Mark as urgent").performClick().assertIsOn()
        rule.onNode(hasContentDescription("Message", substring = true)).assertExists()
    }

    @Test
    fun typingANumberStoresTheText() {
        var result: List<MonarchRuleAction>? = null
        show(listOf(MonarchRuleAction("a", "wait")), { result = it })
        rule.onNodeWithContentDescription("Hours").performTextInput("12")
        assertEquals(Pv.Number("12"), result!!.single().parameters["hours"])
    }

    @Test
    fun showErrorsNamesTheMissingParameterAndTheTypesProblem() {
        show(listOf(MonarchRuleAction("a", "priority")), showErrors = true)
        rule.onNodeWithText("Fill in “Priority”.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theTypesOwnCheckShowsUnderTheInputs() {
        show(listOf(MonarchRuleAction("a", "wait", mapOf("hours" to Pv.Number("13")))), showErrors = true)
        rule.onNodeWithText("Avoid 13 hours.", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun anUnavailableTypeShowsAWarning() {
        show(listOf(MonarchRuleAction("a", "sms")))
        val warning = "This action cannot run right now. Connect a provider first."
        rule.onNodeWithText(warning, substring = true, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun disabledEditorCannotChange() {
        rule.setMonarchContent {
            MonarchActionEditor(value = two, onValueChange = { error("changed") }, actionTypes = Types, enabled = false,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onNodeWithText("Add action").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Remove action 1").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Move down: action 1").assertIsNotEnabled()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchActionEditor(
                value = emptyList(),
                onValueChange = {},
                actionTypes = Types,
                labels = MonarchActionEditorLabels(add = "Add step", empty = "Nothing happens yet."),
            )
        }
        rule.onNodeWithText("Add step").assertExists()
        rule.onNodeWithText("Nothing happens yet.").assertExists()
    }

    @Test
    fun theMoveButtonsMirrorRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchActionEditor(value = two, onValueChange = {}, actionTypes = Types,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
        rule.onNodeWithContentDescription("Move up: action 1").assertIsBefore(
            rule.onNodeWithContentDescription("Move down: action 1"),
            LayoutDirection.Rtl,
        )
    }

    @Test
    fun theHeadingIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchActionEditor(
                value = listOf(MonarchRuleAction("a", "close")),
                onValueChange = {},
                actionTypes = Types,
                modifier = Modifier.verticalScroll(rememberScrollState()),
                labels = MonarchActionEditorLabels(action = { "Step $it of the escalation for new tickets" }),
            )
        }
        rule.onNodeWithText("Step 1 of the escalation for new tickets").assertTextFullyVisible()
    }

    @Test
    fun problemsFollowTheWebRules() {
        assertEquals(listOf(Problem.Missing("level")), MonarchRuleAction("a", "priority").problems(Types))
        assertEquals(emptyList(), MonarchRuleAction("a", "notify").problems(Types))
        val urgent = mapOf("urgent" to Pv.Flag(true))
        assertEquals(listOf(Problem.Missing("message")), MonarchRuleAction("a", "notify", urgent).problems(Types))
        val outside = MonarchRuleAction("a", "wait", mapOf("hours" to Pv.Number("100")))
        assertEquals(listOf(Problem.OutOfRange("hours")), outside.problems(Types))
        val thirteen = MonarchRuleAction("a", "wait", mapOf("hours" to Pv.Number("13")))
        assertEquals(listOf(Problem.Invalid("Avoid 13 hours.")), thirteen.problems(Types))
        assertEquals(emptyList(), MonarchRuleAction("a", "gone").problems(Types))
        val list = listOf(MonarchRuleAction("p", "priority"), MonarchRuleAction("c", "close"))
        assertEquals(setOf("p"), MonarchRuleAction.problems(list, Types).keys)
    }

    @Test
    fun typeOptionsListUngroupedFirstAndKeepTheCurrentDisabledType() {
        val options = MonarchActionEditorTokens.typeOptions(Types, "sms") { "$it (not available)" }
        assertEquals("priority", options.last().value)
        val sms = options.first { it.value == "sms" }
        assertTrue(sms.enabled)
        assertEquals("Connect a provider first.", sms.description)
        val forClose = MonarchActionEditorTokens.typeOptions(Types, "close") { it }
        assertEquals(false, forClose.first { it.value == "sms" }.enabled)
        val forGone = MonarchActionEditorTokens.typeOptions(Types, "gone") { "$it (not available)" }
        assertEquals("gone (not available)", forGone.first().label)
        assertEquals(listOf("b", "a"), MonarchActionEditorTokens.moved(two, 0, 1).map { it.id })
        assertEquals("action-2", MonarchActionEditorTokens.freshId(listOf("action-1")))
        assertEquals(Close, MonarchActionEditorTokens.startType(Types, null))
    }

    @Test
    fun anIncompleteActionTakesTheDangerBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.borderSubtle, MonarchActionEditorTokens.cardColors(colors, false).border)
            assertEquals(colors.dangerBorder, MonarchActionEditorTokens.cardColors(colors, true).border)
        }
    }
}
