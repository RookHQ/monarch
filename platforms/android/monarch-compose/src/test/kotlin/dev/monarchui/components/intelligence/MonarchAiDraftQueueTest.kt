package dev.monarchui.components.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.components.intelligence.aidraftqueue.MonarchAiDraft
import dev.monarchui.components.intelligence.aidraftqueue.MonarchAiDraftQueue
import dev.monarchui.components.intelligence.aidraftqueue.MonarchAiDraftQueueLabels
import dev.monarchui.components.intelligence.aidraftqueue.MonarchAiDraftQueueTokens as Tokens
import dev.monarchui.components.intelligence.aidraftqueue.MonarchAiDraftQueueTokens.Action
import dev.monarchui.components.intelligence.aidraftqueue.MonarchAiDraftStatus
import dev.monarchui.foundation.MonarchTone
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue


private val reply = MonarchAiDraft("1", "Reply to Alex", "The refund was issued today.")
private val note = MonarchAiDraft("2", "Note for Sam", "Moved the call to Friday.")

private fun queue(
    drafts: List<MonarchAiDraft>,
    title: String? = null,
    onSend: ((String) -> Unit)? = null,
): @Composable () -> Unit = {
    MonarchAiDraftQueue(drafts, {}, { _, _ -> }, {}, {}, title = title, onSend = onSend)
}

internal object MonarchAiDraftQueueFixtures : ComponentFixtures {
    override val id = "ai-draft-queue"
    override val fixtures = listOf(
        fixture("needs review", queue(listOf(reply))),
        fixture("approved with send and context") {
            MonarchAiDraftQueue(
                drafts = listOf(
                    MonarchAiDraft("1", "Reply", "Refund issued.", "Alex: charged", MonarchAiDraftStatus.Approved),
                ),
                onApprove = {},
                onEdit = { _, _ -> },
                onRegenerate = {},
                onSkip = {},
                onSend = {},
            )
        },
        fixture("regenerating", queue(listOf(reply.copy(status = MonarchAiDraftStatus.Regenerating)))),
        fixture(
            "sent and skipped",
            queue(
                listOf(
                    reply.copy(status = MonarchAiDraftStatus.Sent),
                    note.copy(status = MonarchAiDraftStatus.Skipped),
                ),
            ),
        ),
        fixture("empty with title", queue(emptyList(), title = "Replies to review")),
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiDraftQueueTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachDraftShowsItsTitleAsAHeadingItsStatusAndItsBodyAfterAiDraft() {
        rule.setMonarchContent(content = queue(listOf(reply), title = "Replies to review"))
        rule.onNodeWithText("Replies to review").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Reply to Alex").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Needs review").assertExists()
        rule.onNodeWithContentDescription("AI draft, The refund was issued today.").assertExists()
        rule.onNodeWithText("0 of 1 reviewed").assertExists()
        rule.onNode(hasContentDescription("Replies to review")).assertExists()
    }

    @Test
    fun approvingCallsTheAppAndMovesFocusToTheNextDraftThatNeedsReview() {
        var approved: String? = null
        rule.setMonarchContent {
            MonarchAiDraftQueue(listOf(reply, note), { approved = it }, { _, _ -> }, {}, {})
        }
        rule.onAllNodesWithText("Approve")[0].assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals("1", approved)
        rule.onNodeWithText("Note for Sam").assertIsFocused()
    }

    @Test
    fun skipAndRegenerateCallTheApp() {
        val calls = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiDraftQueue(listOf(reply), {}, { _, _ -> }, { calls += "regenerate $it" }, { calls += "skip $it" })
        }
        rule.onNodeWithText("Regenerate").performClick()
        rule.onNodeWithText("Skip").performClick()
        assertEquals(listOf("regenerate 1", "skip 1"), calls)
    }

    @Test
    fun editingSavesTheTrimmedChangedTextAndCancelKeepsTheDraft() {
        val edits = mutableListOf<Pair<String, String>>()
        rule.setMonarchContent { MonarchAiDraftQueue(listOf(reply), {}, { id, body -> edits += id to body }, {}, {}) }
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithContentDescription("Edit draft: Reply to Alex").assertIsFocused().performTextReplacement("  ")
        rule.onNodeWithText("Save").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Edit draft: Reply to Alex")
            .performTextReplacement("  The refund is on its way. ")
        rule.onNodeWithText("Save").performClick()
        assertEquals(listOf("1" to "The refund is on its way."), edits)
        rule.onNodeWithText("Edit").performClick()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithContentDescription("Edit draft: Reply to Alex").assertDoesNotExist()
        assertEquals(1, edits.size)
    }

    @Test
    fun aRegeneratingDraftShowsWritingAndOffersNoActions() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(content = queue(listOf(reply.copy(status = MonarchAiDraftStatus.Regenerating))))
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("AI is still writing…").assertExists()
        rule.onNodeWithText("Writing").assertExists()
        rule.onNodeWithText("Approve").assertDoesNotExist()
    }

    @Test
    fun sendIsOfferedOnApprovedDraftsOnlyWhenTheQueueCanSend() {
        var sent: String? = null
        val approved = reply.copy(status = MonarchAiDraftStatus.Approved)
        rule.setMonarchContent {
            MonarchAiDraftQueue(listOf(approved), {}, { _, _ -> }, {}, {}, onSend = { sent = it })
        }
        rule.onNodeWithText("Send").performClick()
        assertEquals("1", sent)
        assertEquals(listOf(Action.Edit), Tokens.actions(MonarchAiDraftStatus.Approved, canSend = false))
        assertTrue(Tokens.actions(MonarchAiDraftStatus.Sent, canSend = true).isEmpty())
    }

    @Test
    fun statusChangesAreAnnouncedThroughAPoliteLiveRegion() {
        val drafts = mutableStateOf(listOf(reply, note))
        rule.setMonarchContent { MonarchAiDraftQueue(drafts.value, {}, { _, _ -> }, {}, {}) }
        rule.onNode(hasContentDescription("Approved: Reply to Alex")).assertDoesNotExist()
        drafts.value = listOf(reply.copy(status = MonarchAiDraftStatus.Approved), note)
        rule.waitForIdle()
        rule.onNode(hasContentDescription("Approved: Reply to Alex"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        drafts.value = listOf(reply, note)
        rule.waitForIdle()
        rule.onNode(hasContentDescription("New draft ready: Reply to Alex")).assertExists()
    }

    @Test
    fun withNoDraftsTheEmptyStateIsShown() {
        rule.setMonarchContent(content = queue(emptyList()))
        rule.onNodeWithText("No drafts to review").assertExists()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiDraftQueue(
                listOf(reply),
                {},
                { _, _ -> },
                {},
                {},
                labels = MonarchAiDraftQueueLabels(
                    approve = "Valider",
                    status = { "État ${it.name}" },
                    progress = { done, total -> "$done/$total" },
                ),
            )
        }
        rule.onNodeWithText("Valider").assertExists()
        rule.onNodeWithText("État Draft").assertExists()
        rule.onNodeWithText("0/1").assertExists()
    }

    @Test
    fun actionsFollowTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl, content = queue(listOf(reply)))
        rule.onNodeWithText("Approve").assertIsBefore(rule.onNodeWithText("Edit"), LayoutDirection.Rtl)
    }

    @Test
    fun theBodyWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale, content = queue(listOf(reply)))
        rule.onNodeWithText("The refund was issued today.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun reviewLogicFollowsTheWebAndIos() {
        val skipped = note.copy(status = MonarchAiDraftStatus.Skipped)
        val third = MonarchAiDraft("3", "Third", "Body")
        assertEquals("3", Tokens.next("1", listOf(reply, skipped, third)))
        assertEquals("1", Tokens.next("3", listOf(reply, skipped, third)))
        assertNull(Tokens.next("1", listOf(reply, skipped)))
        assertEquals("New", Tokens.edited("  New ", "Old"))
        assertNull(Tokens.edited(" Old ", "Old"))
        assertNull(Tokens.edited("   ", "Old"))
        assertEquals(0.5f, Tokens.fraction(listOf(reply, skipped)))
        val before = mapOf("2" to MonarchAiDraftStatus.Draft)
        assertEquals(listOf(skipped), Tokens.changed(before, listOf(reply, skipped)))
        assertEquals(MonarchButtonVariant.Primary, Action.Approve.variant)
        assertTrue(Action.Skip.advances)
        assertFalse(Action.Edit.advances)
        assertEquals(MonarchTone.Success, Tokens.tone(MonarchAiDraftStatus.Sent))
        assertEquals(MonarchTone.Ai, Tokens.tone(MonarchAiDraftStatus.Regenerating))
        assertEquals(MonarchTone.Ai, Tokens.tone(MonarchAiDraftStatus.Draft))
    }

    @Test
    fun aDraftCanBeMadeFromAnAssistantMessage() {
        val message = MonarchAiConversationMessage(
            id = "m1",
            role = MonarchAiRole.Assistant,
            parts = listOf(MonarchAiTextPart("Hello Alex")),
            createdAt = "2026-11-14T10:00:00Z",
        )
        val draft = MonarchAiDraft(message, title = "Reply to Alex")
        assertEquals("m1", draft.id)
        assertEquals("Hello Alex", draft.body)
        assertEquals(MonarchAiDraftStatus.Draft, draft.status)
    }

    @Test
    fun aSkippedBodyIsMutedAndTheContextSitsOnTheSubtleFillInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, Tokens.bodyColor(MonarchAiDraftStatus.Skipped, colors), theme.id)
            assertEquals(colors.textDefault, Tokens.bodyColor(MonarchAiDraftStatus.Draft, colors), theme.id)
            assertEquals(colors.bgSubtle, Tokens.contextFill(colors), theme.id)
        }
    }
}
