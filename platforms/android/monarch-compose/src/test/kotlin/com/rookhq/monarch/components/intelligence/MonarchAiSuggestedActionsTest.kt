@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallRisk
import com.rookhq.monarch.components.intelligence.aisuggestedactions.MonarchAiSuggestedAction
import com.rookhq.monarch.components.intelligence.aisuggestedactions.MonarchAiSuggestedActionStatus
import com.rookhq.monarch.components.intelligence.aisuggestedactions.MonarchAiSuggestedActions
import com.rookhq.monarch.components.intelligence.aisuggestedactions.MonarchAiSuggestedActionsLabels
import com.rookhq.monarch.components.intelligence.aisuggestedactions.MonarchAiSuggestedActionsOrigin
import com.rookhq.monarch.components.intelligence.aisuggestedactions.MonarchAiSuggestedActionsTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Reminder = MonarchAiSuggestedAction(
    id = "reminder",
    label = "Send the renewal reminder",
    description = "The contract ends in 14 days.",
)
private val Task = MonarchAiSuggestedAction(
    id = "task",
    label = "Create a follow-up task",
    risk = MonarchAiToolCallRisk.Medium,
)
private val Email = MonarchAiSuggestedAction(
    id = "email",
    label = "Email the account team",
    requiresApproval = true,
    consequences = listOf("3 people will get an email"),
    confirmLabel = "Send email",
)
private val Close = MonarchAiSuggestedAction(
    id = "close",
    label = "Close 4 stale tickets",
    risk = MonarchAiToolCallRisk.Destructive,
    consequences = listOf("4 tickets will be closed"),
)

internal object MonarchAiSuggestedActionsFixtures : ComponentFixtures {
    override val id = "ai-suggested-actions"
    override val fixtures = listOf(
        fixture("ready with dismiss") {
            MonarchAiSuggestedActions(actions = listOf(Reminder, Task), onRun = {}, onDismiss = {})
        },
        fixture("needs approval") {
            MonarchAiSuggestedActions(actions = listOf(Email), onRun = {})
        },
        fixture("destructive") {
            MonarchAiSuggestedActions(actions = listOf(Close), onRun = {})
        },
        fixture("running") {
            MonarchAiSuggestedActions(
                actions = listOf(Reminder.copy(status = MonarchAiSuggestedActionStatus.Running)),
                onRun = {},
            )
        },
        fixture("done") {
            MonarchAiSuggestedActions(
                actions = listOf(Task.copy(status = MonarchAiSuggestedActionStatus.Done)),
                onRun = {},
            )
        },
        fixture("failed") {
            MonarchAiSuggestedActions(
                actions = listOf(Email.copy(status = MonarchAiSuggestedActionStatus.Failed)),
                onRun = {},
            )
        },
        fixture("rule-based, empty") {
            MonarchAiSuggestedActions(actions = emptyList(), onRun = {}, origin = MonarchAiSuggestedActionsOrigin.Rule)
        },
        fixture("icons and a long label") {
            MonarchAiSuggestedActions(
                actions = listOf(
                    MonarchAiSuggestedAction(
                        id = "long",
                        label = "Move every open opportunity of this account to the renewal pipeline before Friday",
                        risk = MonarchAiToolCallRisk.High,
                    ),
                ),
                onRun = {},
                onDismiss = {},
                originIcon = { Icon(TestIcon, contentDescription = null) },
                dismissIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiSuggestedActionsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theHeadingAndTheOriginAreShown() {
        rule.setMonarchContent {
            MonarchAiSuggestedActions(actions = listOf(Reminder), onRun = {})
        }
        rule.onNode(isHeading()).assertTextEquals("Suggested actions")
        rule.onNodeWithText("Suggested by AI").assertExists()
    }

    @Test
    fun runRunsAndShowsDoneWithAnAnnouncement() {
        val ran = mutableListOf<String>()
        rule.setMonarchContent { MonarchAiSuggestedActions(actions = listOf(Reminder), onRun = { ran += it }) }
        rule.onNodeWithContentDescription("Run Send the renewal reminder")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(listOf("reminder"), ran)
        rule.onNodeWithText("Done").assertExists()
        rule.onAllNodesWithText("Run").assertCountEquals(0)
        val region = SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion) and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)
        val done = listOf("“Send the renewal reminder” done.")
        rule.onNode(region).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, done))
    }

    @Test
    fun whileRunningTheButtonSaysRunningAndAFailureSaysToTryAgain() {
        val gate = CompletableDeferred<Unit>()
        rule.setMonarchContent {
            MonarchAiSuggestedActions(
                actions = listOf(Reminder),
                onRun = {
                    gate.await()
                    error("offline")
                },
            )
        }
        rule.onNodeWithContentDescription("Run Send the renewal reminder").performClick()
        rule.waitForIdle()
        val run = rule.onNodeWithContentDescription("Run Send the renewal reminder")
        assertEquals("Running…", run.stateDescription())
        gate.complete(Unit)
        rule.waitForIdle()
        rule.onNodeWithText("That didn’t work. Try again.").assertExists()
        assertNull(rule.onNodeWithContentDescription("Run Send the renewal reminder").stateDescription())
    }

    @Test
    fun anActionThatNeedsApprovalRunsOnlyAfterConfirming() {
        val ran = mutableListOf<String>()
        rule.setMonarchContent { MonarchAiSuggestedActions(actions = listOf(Email), onRun = { ran += it }) }
        rule.onNodeWithText("Needs approval").assertExists()
        rule.onNodeWithContentDescription("Run Email the account team").performClick()
        rule.onNodeWithText("Run “Email the account team”?").assertExists()
        rule.onNodeWithText("• 3 people will get an email").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertTrue(ran.isEmpty())
        rule.onNodeWithContentDescription("Run Email the account team").performClick()
        rule.onNodeWithText("Send email").performClick()
        rule.waitForIdle()
        assertEquals(listOf("email"), ran)
        rule.onAllNodesWithText("Run “Email the account team”?").assertCountEquals(0)
        rule.onNodeWithText("Done").assertExists()
    }

    @Test
    fun aFailureInsideTheDialogKeepsItOpen() {
        rule.setMonarchContent { MonarchAiSuggestedActions(actions = listOf(Close), onRun = { error("offline") }) }
        rule.onNodeWithText("Destructive").assertExists()
        rule.onNodeWithContentDescription("Run Close 4 stale tickets").performClick()
        rule.onNodeWithText("Run action").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Run “Close 4 stale tickets”?").assertExists()
        rule.onAllNodesWithText("That didn’t work. Try again.").assertCountEquals(2)
    }

    @Test
    fun dismissCallsBackWithTheId() {
        val dismissed = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiSuggestedActions(actions = listOf(Reminder, Task), onRun = {}, onDismiss = { dismissed += it })
        }
        rule.onNodeWithContentDescription("Dismiss “Create a follow-up task”")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("task"), dismissed)
    }

    @Test
    fun aKeyboardDismissMovesFocusToTheNextAction() {
        var actions by mutableStateOf(listOf(Reminder, Task))
        rule.setMonarchContent {
            // Buttons take focus from a keyboard, as with a hardware keyboard or Switch Access.
            val inputMode = LocalInputModeManager.current
            LaunchedEffect(Unit) { inputMode.requestInputMode(InputMode.Keyboard) }
            MonarchAiSuggestedActions(
                actions = actions,
                onRun = {},
                onDismiss = { id -> actions = actions.filterNot { it.id == id } },
            )
        }
        rule.waitForIdle()
        val dismiss = rule.onNodeWithContentDescription("Dismiss “Send the renewal reminder”")
        dismiss.requestFocus()
        rule.waitForIdle()
        dismiss.assertIsFocused()
        // Activate without a touch, as a keyboard or switch access does, so focus stays where it is.
        dismiss.performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals(listOf("task"), actions.map { it.id })
        rule.onNodeWithContentDescription("Run Create a follow-up task").assertIsFocused()
    }

    @Test
    fun theRuleOriginTitleAndEmptyTextCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiSuggestedActions(
                actions = emptyList(),
                onRun = {},
                origin = MonarchAiSuggestedActionsOrigin.Rule,
                labels = MonarchAiSuggestedActionsLabels(title = "Next steps", empty = "Nothing to do"),
            )
        }
        rule.onNodeWithText("Next steps").assertExists()
        rule.onNodeWithText("Rule-based").assertExists()
        rule.onNodeWithText("Nothing to do").assertExists()
    }

    @Test
    fun theDefaultStringsAreBuiltIn() {
        rule.setMonarchContent { MonarchAiSuggestedActions(actions = emptyList(), onRun = {}) }
        rule.onNodeWithText("No suggestions right now.").assertExists()
    }

    @Test
    fun theButtonsCanBeRenamed() {
        rule.setMonarchContent {
            MonarchAiSuggestedActions(
                actions = listOf(Reminder),
                onRun = {},
                onDismiss = {},
                labels = MonarchAiSuggestedActionsLabels(
                    run = "Ejecutar",
                    runAction = { "Ejecutar $it" },
                    dismiss = { "Descartar $it" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Ejecutar Send the renewal reminder").assertExists()
        rule.onNodeWithContentDescription("Descartar Send the renewal reminder").assertExists()
    }

    @Test
    fun theActionTextIsOneElement() {
        rule.setMonarchContent { MonarchAiSuggestedActions(actions = listOf(Task), onRun = {}) }
        val node = rule.onNodeWithText("Create a follow-up task").fetchSemanticsNode()
        val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        assertEquals(listOf("Create a follow-up task", "Medium risk"), texts)
    }

    @Test
    fun runComesBeforeDismissInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchAiSuggestedActions(actions = listOf(Reminder), onRun = {}, onDismiss = {})
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Run Send the renewal reminder")
                .assertIsBefore(rule.onNodeWithContentDescription("Dismiss “Send the renewal reminder”"), direction)
        }
    }

    @Test
    fun longLabelsWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiSuggestedActions(
                actions = listOf(MonarchAiSuggestedAction("a", "Move every open opportunity to the renewal pipeline")),
                onRun = {},
            )
        }
        rule.onNodeWithText("Move every open opportunity to the renewal pipeline", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theRulesHold() {
        val t = MonarchAiSuggestedActionsTokens
        assertTrue(Close.needsApproval)
        assertTrue(Email.needsApproval)
        assertFalse(Task.needsApproval)
        assertEquals(MonarchAiSuggestedActionStatus.Ready, t.status(Reminder, null))
        assertEquals(MonarchAiSuggestedActionStatus.Done, t.status(Reminder, MonarchAiSuggestedActionStatus.Done))
        val controlled = Reminder.copy(status = MonarchAiSuggestedActionStatus.Running)
        assertEquals(MonarchAiSuggestedActionStatus.Running, t.status(controlled, MonarchAiSuggestedActionStatus.Done))
        assertNull(t.riskTone(MonarchAiToolCallRisk.Low))
        assertEquals(MonarchTone.Warning, t.riskTone(MonarchAiToolCallRisk.Medium))
        assertEquals(MonarchTone.Warning, t.riskTone(MonarchAiToolCallRisk.High))
        assertEquals(MonarchTone.Danger, t.riskTone(MonarchAiToolCallRisk.Destructive))
        assertNull(t.riskResource(MonarchAiToolCallRisk.Low))
        assertEquals("task", t.focusAfterDismiss(listOf(Task, Email), 0))
        assertEquals("task", t.focusAfterDismiss(listOf(Task), 1))
        assertNull(t.focusAfterDismiss(emptyList(), 0))
    }

    @Test
    fun theColoursComeFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchAiSuggestedActionsTokens
            val ai = t.origin(MonarchAiSuggestedActionsOrigin.Ai, colors)
            assertEquals(colors.aiSubtle, ai.container, theme.id)
            assertEquals(colors.aiStrong, ai.content, theme.id)
            assertEquals(colors.textMuted, t.origin(MonarchAiSuggestedActionsOrigin.Rule, colors).content, theme.id)
            assertEquals(colors.textMuted, t.labelColor(done = true, colors), theme.id)
            assertEquals(colors.textDefault, t.labelColor(done = false, colors), theme.id)
            assertEquals(colors.successStrong, t.doneColor(colors), theme.id)
            assertEquals(colors.dangerStrong, t.failedColor(colors), theme.id)
        }
    }
}
