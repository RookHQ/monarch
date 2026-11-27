@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallRisk
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallState
import com.rookhq.monarch.components.intelligence.aitoolapproval.AiToolApprovalDecision
import com.rookhq.monarch.components.intelligence.aitoolapproval.AiToolApprovalEdit
import com.rookhq.monarch.components.intelligence.aitoolapproval.AiToolApprovalOutcome
import com.rookhq.monarch.components.intelligence.aitoolapproval.MonarchAiToolApproval
import com.rookhq.monarch.components.intelligence.aitoolapproval.MonarchAiToolApprovalFormat
import com.rookhq.monarch.components.intelligence.aitoolapproval.MonarchAiToolApprovalLabels
import com.rookhq.monarch.components.intelligence.aitoolapproval.MonarchAiToolApprovalTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val EmailInput = MonarchAiJSON.Object(
    mapOf(
        "to" to MonarchAiJSON.Array(
            listOf(MonarchAiJSON.Text("ada@example.com"), MonarchAiJSON.Text("ben@example.com")),
        ),
        "sendAt" to MonarchAiJSON.Null,
        "retries" to MonarchAiJSON.Number(3.0),
    ),
)

private fun call(
    id: String = "call-1",
    name: String = "send_email",
    input: MonarchAiJSON = EmailInput,
    state: MonarchAiToolCallState = MonarchAiToolCallState.PendingApproval,
    risk: MonarchAiToolCallRisk? = null,
) = MonarchAiToolCallPart(id = id, name = name, input = input, state = state, risk = risk)

internal object MonarchAiToolApprovalFixtures : ComponentFixtures {
    override val id = "ai-tool-approval"
    override val fixtures = listOf(
        // The Robolectric window is 470dp tall: each fixture stays short enough at font scale 2.0.
        fixture("pending, medium risk") {
            MonarchAiToolApproval(
                part = call(input = MonarchAiJSON.Null),
                onApprove = {},
                onDeny = {},
                editable = false,
            )
        },
        fixture("pending, destructive") {
            MonarchAiToolApproval(
                part = call(name = "delete_records", input = MonarchAiJSON.Object(emptyMap())),
                onApprove = {},
                onDeny = {},
                risk = MonarchAiToolCallRisk.Destructive,
                editable = false,
            )
        },
        fixture("approved, with arguments") {
            MonarchAiToolApproval(part = call(state = MonarchAiToolCallState.Succeeded), onApprove = {}, onDeny = {})
        },
        fixture("cancelled, raw input, low risk") {
            MonarchAiToolApproval(
                part = call(input = MonarchAiJSON.Text("rm -rf build"), state = MonarchAiToolCallState.Cancelled),
                onApprove = {},
                onDeny = {},
                risk = MonarchAiToolCallRisk.Low,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiToolApprovalTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theRiskSetsTheToneTheButtonAndAlwaysAllow() {
        val t = MonarchAiToolApprovalTokens
        assertEquals(MonarchTone.Neutral, t.tone(MonarchAiToolCallRisk.Low))
        assertEquals(MonarchTone.Warning, t.tone(MonarchAiToolCallRisk.Medium))
        assertEquals(MonarchTone.Warning, t.tone(MonarchAiToolCallRisk.High))
        assertEquals(MonarchTone.Danger, t.tone(MonarchAiToolCallRisk.Destructive))
        assertEquals(MonarchButtonVariant.Danger, t.approveVariant(MonarchAiToolCallRisk.Destructive))
        assertEquals(MonarchButtonVariant.Primary, t.approveVariant(MonarchAiToolCallRisk.High))
        assertTrue(t.offersAlwaysAllow(hasCallback = true, risk = MonarchAiToolCallRisk.High))
        assertFalse(t.offersAlwaysAllow(hasCallback = true, risk = MonarchAiToolCallRisk.Destructive))
        assertFalse(t.offersAlwaysAllow(hasCallback = false, risk = MonarchAiToolCallRisk.Low))
        assertNull(t.consequenceText(MonarchAiToolCallRisk.High))
    }

    @Test
    fun theOutcomeFollowsTheDecisionThenThePart() {
        val t = MonarchAiToolApprovalTokens
        assertNull(t.outcome(MonarchAiToolCallState.PendingApproval, null))
        assertEquals(
            AiToolApprovalOutcome.Denied,
            t.outcome(MonarchAiToolCallState.PendingApproval, AiToolApprovalDecision.Denied),
        )
        assertEquals(AiToolApprovalOutcome.Denied, t.outcome(MonarchAiToolCallState.Denied, null))
        assertEquals(AiToolApprovalOutcome.Cancelled, t.outcome(MonarchAiToolCallState.Cancelled, null))
        assertEquals(AiToolApprovalOutcome.Approved, t.outcome(MonarchAiToolCallState.Running, null))
    }

    @Test
    fun argumentsAreLabelledAndReadable() {
        val f = MonarchAiToolApprovalFormat
        assertEquals(
            listOf("Recipients" to "ada@example.com, ben@example.com", "Send at" to "—", "Retries" to "3"),
            f.rows(EmailInput, mapOf("to" to "Recipients")),
        )
        assertNull(f.rows(MonarchAiJSON.Text("x"), emptyMap()))
        assertEquals("true", f.readable(MonarchAiJSON.Bool(true)))
        assertEquals("{}", f.editableText(MonarchAiJSON.Null))
        assertNull(f.reason("   "))
        assertEquals("Not now", f.reason(" Not now "))
    }

    @Test
    fun editedJsonIsParsedAndInvalidJsonIsRefused() {
        val f = MonarchAiToolApprovalFormat
        val original = f.editableText(EmailInput)
        assertEquals(AiToolApprovalEdit.Unchanged, f.edit(editing = false, text = "{", original = original))
        assertEquals(AiToolApprovalEdit.Unchanged, f.edit(editing = true, text = "$original\n", original = original))
        assertEquals(AiToolApprovalEdit.Invalid, f.edit(editing = true, text = "{", original = original))
        assertEquals(
            AiToolApprovalEdit.Edited(MonarchAiJSON.Object(mapOf("retries" to MonarchAiJSON.Number(1.0)))),
            f.edit(editing = true, text = "{\"retries\": 1}", original = original),
        )
    }

    @Test
    fun approveSendsNoInputWhenNothingWasEditedAndShowsTheOutcome() {
        val approved = mutableListOf<MonarchAiJSON?>()
        rule.setMonarchContent {
            MonarchAiToolApproval(part = call(), onApprove = { approved += it }, onDeny = {})
        }
        rule.onNode(hasText("Send email") and isHeading()).assertExists()
        rule.onNodeWithText("Medium risk").assertExists()
        rule.onNodeWithText("Approve").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(listOf<MonarchAiJSON?>(null), approved)
        rule.onNodeWithText("Approved")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("Approve").assertDoesNotExist()
    }

    @Test
    fun editedArgumentsAreSentAndInvalidJsonKeepsTheCardOpen() {
        val approved = mutableListOf<MonarchAiJSON?>()
        rule.setMonarchContent {
            MonarchAiToolApproval(part = call(), onApprove = { approved += it }, onDeny = {})
        }
        rule.onNodeWithText("Edit arguments").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextReplacement("{\"to\": ")
        rule.onNodeWithText("Approve").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Fix the JSON before approving.", useUnmergedTree = true).assertExists()
        assertEquals(emptyList(), approved)
        rule.onNode(hasSetTextAction()).performTextReplacement("{\"to\": \"ada@example.com\"}")
        rule.onNodeWithText("Approve").performClick()
        rule.waitForIdle()
        val edited = MonarchAiJSON.Object(mapOf("to" to MonarchAiJSON.Text("ada@example.com")))
        assertEquals(listOf<MonarchAiJSON?>(edited), approved)
    }

    @Test
    fun discardEditsRestoresTheArguments() {
        rule.setMonarchContent {
            MonarchAiToolApproval(part = call(), onApprove = {}, onDeny = {})
        }
        rule.onNodeWithText("Edit arguments").performClick()
        rule.onNode(hasSetTextAction()).performTextReplacement("{}")
        rule.onNodeWithText("Discard edits").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
        rule.onNodeWithText("Edit arguments").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction() and hasText("ada@example.com", substring = true)).assertExists()
    }

    @Test
    fun denySendsTheTrimmedReason() {
        val reasons = mutableListOf<String?>()
        rule.setMonarchContent {
            MonarchAiToolApproval(part = call(), onApprove = {}, onDeny = { reasons += it })
        }
        rule.onNodeWithText("Add a reason").performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextInput("  Wrong people  ")
        rule.onNodeWithText("Deny").performClick()
        rule.waitForIdle()
        assertEquals(listOf<String?>("Wrong people"), reasons)
        rule.onNodeWithText("Denied").assertExists()
    }

    @Test
    fun alwaysAllowIsCalledBeforeApprove() {
        val calls = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiToolApproval(
                part = call(),
                onApprove = { calls += "approve" },
                onDeny = {},
                onAlwaysAllow = { calls += "always $it" },
            )
        }
        rule.onNodeWithText("Always allow this tool").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Approve").performClick()
        rule.waitForIdle()
        assertEquals(listOf("always send_email", "approve"), calls)
    }

    @Test
    fun aDestructiveCallSaysSoAndNeverOffersAlwaysAllow() {
        rule.setMonarchContent {
            MonarchAiToolApproval(
                part = call(risk = MonarchAiToolCallRisk.Destructive),
                onApprove = {},
                onDeny = {},
                onAlwaysAllow = {},
            )
        }
        rule.onNodeWithText("Can’t be undone").assertExists()
        rule.onNodeWithText("This can’t be undone once it runs.").assertExists()
        rule.onNodeWithText("Always allow this tool").assertDoesNotExist()
    }

    @Test
    fun aNewCallResetsTheCardAndTheCardIsAnnounced() {
        var part by mutableStateOf(call())
        rule.setMonarchContent {
            MonarchAiToolApproval(part = part, onApprove = {}, onDeny = {}, describeTool = { "Email 2 people" })
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Approval needed: Email 2 people")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("Deny").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Denied").assertExists()
        part = call(id = "call-2")
        rule.waitForIdle()
        rule.onNodeWithText("Deny").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiToolApproval(
                part = call(input = MonarchAiJSON.Null),
                onApprove = {},
                onDeny = {},
                toolLabels = mapOf("send_email" to "Send the email"),
                labels = MonarchAiToolApprovalLabels(approve = "Allow", noArguments = "Nothing to send."),
            )
        }
        rule.onNodeWithText("Send the email").assertExists()
        rule.onNodeWithText("Allow").assertExists()
        rule.onNodeWithText("Nothing to send.").assertExists()
    }

    @Test
    fun theTitleWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiToolApproval(part = call(input = MonarchAiJSON.Null), onApprove = {}, onDeny = {})
        }
        rule.onNodeWithText("Send email").assertTextFullyVisible()
    }

    @Test
    fun theColoursComeFromTheTheme() {
        val t = MonarchAiToolApprovalTokens
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.dangerStrong, t.consequenceColor(MonarchAiToolCallRisk.Destructive, c), theme.id)
            assertEquals(c.warningStrong, t.consequenceColor(MonarchAiToolCallRisk.High, c), theme.id)
            assertEquals(c.successStrong, t.outcomeColor(AiToolApprovalOutcome.Approved, c), theme.id)
            assertEquals(c.dangerStrong, t.outcomeColor(AiToolApprovalOutcome.Denied, c), theme.id)
            assertEquals(c.textMuted, t.outcomeColor(AiToolApprovalOutcome.Cancelled, c), theme.id)
        }
    }
}
