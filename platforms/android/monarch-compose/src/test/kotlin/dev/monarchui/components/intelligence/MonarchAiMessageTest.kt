@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiBranchPosition
import dev.monarchui.components.intelligence.aiconversation.MonarchAiCitationPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiErrorPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiFilePart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiFinishReason
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMemoryAction
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMemoryPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPartState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStep
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStepState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiReasoningPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiUiPart
import dev.monarchui.components.intelligence.aimessage.MonarchAiMessage
import dev.monarchui.components.intelligence.aimessage.MonarchAiMessageLabels
import dev.monarchui.components.intelligence.aimessage.MonarchAiMessageTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.MonarchTestFrame
import dev.monarchui.testing.TestIcon
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
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** ICU puts a narrow no-break space before AM and PM in en-US. */
private fun clock(text: String) = text.replace(" AM", " AM").replace(" PM", " PM")

/** 2026-11-05T09:05:00Z. */
private const val Created = "2026-11-05T09:05:00.000Z"

private val Source =
    MonarchAiSourcePart("s1", "Billing handbook", url = "https://example.com", kind = MonarchAiSourceKind.Doc)
private val OtherSource = MonarchAiSourcePart("s2", "Invoice API", kind = MonarchAiSourceKind.Web)

private fun message(
    role: MonarchAiRole,
    vararg parts: MonarchAiPart,
    finishReason: MonarchAiFinishReason? = null,
    queued: Boolean = false,
    branch: MonarchAiBranchPosition? = null,
    agent: MonarchAiAgentRef? = null,
) = MonarchAiConversationMessage(
    id = "m1",
    role = role,
    parts = parts.toList(),
    createdAt = Created,
    agent = agent,
    branch = branch,
    finishReason = finishReason,
    queued = queued,
)

private fun answer(text: String = "The invoice is due on 4 November.") =
    message(MonarchAiRole.Assistant, MonarchAiTextPart(text), agent = MonarchAiAgentRef("Ledger", role = "Billing"))

internal object MonarchAiMessageFixtures : ComponentFixtures {
    override val id = "ai-message"
    override val fixtures = listOf(
        fixture("assistant answer with actions") {
            MonarchAiMessage(
                message = answer(),
                timeZone = "UTC",
                onRegenerate = {},
                onFeedback = {},
                actionIcon = { _, _ -> MonarchIcon(TestIcon, contentDescription = null) },
                agentIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        },
        fixture("the person's bubble") {
            MonarchAiMessage(
                message = message(MonarchAiRole.User, MonarchAiTextPart("When is it due?")),
                onEdit = {},
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        },
        fixture("queued, with cancel") {
            MonarchAiMessage(
                message = message(MonarchAiRole.User, MonarchAiTextPart("Send it later"), queued = true),
                onCancelQueued = {},
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        },
        fixture("editing") {
            MonarchAiMessage(
                message = message(MonarchAiRole.User, MonarchAiTextPart("When is it due?")),
                editing = true,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        },
        fixture("reasoning and a running tool") {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiReasoningPart("Look up the invoice.", durationMs = 3_000),
                    MonarchAiToolCallPart("t1", "find_invoice", state = MonarchAiToolCallState.Running),
                    MonarchAiTextPart("Looking", state = MonarchAiPartState.Streaming),
                ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        },
        fixture("sources and a citation") {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiTextPart("It is due on 4 November."),
                    MonarchAiCitationPart("c1", "s1"),
                    Source,
                    OtherSource,
                ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        },
        fixture("an error with retry and resume") {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiErrorPart("network", "socket closed", retryable = true, resumable = true),
                ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                onRetry = {},
                onResume = {},
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        },
        fixture("cut short, with a version switcher") {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiTextPart("The first three steps are"),
                    finishReason = MonarchAiFinishReason.Length,
                    branch = MonarchAiBranchPosition(1, 3),
                ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                onBranch = {},
                previousVersionIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                nextVersionIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                agentIcon = null,
                actionIcon = null,
            )
        },
        fixture("plan, memory and file") {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiPlanPart(listOf(MonarchAiPlanStep("p1", "Find the invoice", MonarchAiPlanStepState.Done))),
                    MonarchAiMemoryPart("mem1", MonarchAiMemoryAction.Write, "Prefers PDF invoices"),
                    MonarchAiFilePart("invoice-0042.pdf", "application/pdf", size = 120_000),
                ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                onForgetMemory = {},
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        },
        fixture("a tool call waiting for approval") {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiToolCallPart("t2", "send_invoice", state = MonarchAiToolCallState.PendingApproval),
                ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                onApproveTool = { _, _ -> },
                onDenyTool = { _, _ -> },
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        },
        fixture("long text") {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.User,
                    MonarchAiTextPart(
                        "Could you check every invoice sent to Northwind Traders in October " +
                            "and tell me which are overdue?",
                    ),
                ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        },
    )
}

/** An assertive live region with something to say: an announcement being made. */
private val AssertiveAnnouncement =
    SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive) and
        SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiMessageTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun anAnswerReadsItsRoleThenShowsTheAgentAndTime() {
        rule.setMonarchContent { MonarchAiMessage(
            message = answer(),
            timeZone = "UTC",
            is24Hour = false,
            agentIcon = null,
            actionIcon = null,
            previousVersionIcon = null,
            nextVersionIcon = null,
        ) }
        rule.onNodeWithContentDescription("Assistant said:").assertExists()
        rule.onNodeWithText("Ledger").assertExists()
        rule.onNodeWithText("Billing").assertExists()
        rule.onNodeWithText(clock("9:05 AM")).assertExists()
        rule.onNodeWithText("The invoice is due on 4 November.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun withoutAnAgentTheNameIsAssistant() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(MonarchAiRole.Assistant, MonarchAiTextPart("Hi")),
                showTimestamp = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Assistant").assertExists()
    }

    @Test
    fun thePersonsMessageOffersEditButNotRegenerateOrThumbs() {
        var edits = 0
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(MonarchAiRole.User, MonarchAiTextPart("When is it due?")),
                onEdit = { edits++ },
                onRegenerate = { error("not for the person's message") },
                onFeedback = { error("not for the person's message") },
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithContentDescription("You said:").assertExists()
        rule.onNodeWithText("Regenerate").assertDoesNotExist()
        rule.onNodeWithText("Good response").assertDoesNotExist()
        rule.onNodeWithText("Edit").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, edits)
    }

    @Test
    fun copyIsOfferedAndCallsBackWithTheText() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent { MonarchAiMessage(
            message = answer(),
            onCopy = { copied += it },
            agentIcon = null,
            actionIcon = null,
            previousVersionIcon = null,
            nextVersionIcon = null,
        ) }
        rule.onNodeWithContentDescription("Copy").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("The invoice is due on 4 November."), copied)
    }

    @Test
    fun copyCanBeTurnedOffAndActionsHiddenWhileStreaming() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(MonarchAiRole.Assistant, MonarchAiTextPart("Look", MonarchAiPartState.Streaming)),
                onRegenerate = {},
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Regenerate").assertDoesNotExist()
        rule.onNodeWithContentDescription("Copy").assertDoesNotExist()
    }

    @Test
    fun copyableFalseHidesCopy() {
        rule.setMonarchContent { MonarchAiMessage(
            message = answer(),
            copyable = false,
            onRegenerate = {},
            agentIcon = null,
            actionIcon = null,
            previousVersionIcon = null,
            nextVersionIcon = null,
        ) }
        rule.onNodeWithContentDescription("Copy").assertDoesNotExist()
        rule.onNodeWithText("Regenerate").assertExists()
    }

    @Test
    fun partsAppearInOrderAndSourcesShowOnce() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiTextPart("First paragraph."),
                    Source,
                    MonarchAiTextPart("Second paragraph."),
                    OtherSource,
                ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        val first = rule.onNodeWithText("First paragraph.", useUnmergedTree = true).fetchSemanticsNode()
        val second = rule.onNodeWithText("Second paragraph.", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(first.boundsInRoot.top < second.boundsInRoot.top)
        rule.onAllNodesWithText("Sources").assertCountEquals(1)
    }

    @Test
    fun aCitationIsNumberedByItsSource() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(MonarchAiRole.Assistant, MonarchAiCitationPart("c1", "s2"), Source, OtherSource),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Source 2: Invoice API").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun aRetryableErrorRetriesAndAResumableOneResumes() {
        var retries = 0
        var resumes = 0
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiErrorPart("network", "socket closed", retryable = true, resumable = true),
                ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                onRetry = { retries++ },
                onResume = { resumes++ },
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Connection lost", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").performScrollTo().performClick()
        rule.onNodeWithText("Resume").performScrollTo().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retries)
        assertEquals(1, resumes)
    }

    @Test
    fun anErrorIsAnnouncedOnceAndNotAgainWhenTheMessageComesBack() {
        val restoration = StateRestorationTester(rule)
        var speaking by mutableStateOf(false)
        restoration.setContent {
            MonarchTestFrame(MonarchTestEnvironment()) {
                MonarchAiMessage(
                    message = message(MonarchAiRole.Assistant, MonarchAiErrorPart("network", "x", retryable = false)),
                    speaking = speaking,
                    showActions = false,
                    agentIcon = null,
                    actionIcon = null,
                    previousVersionIcon = null,
                    nextVersionIcon = null,
                )
            }
        }
        rule.waitForIdle()
        rule.onAllNodes(AssertiveAnnouncement, useUnmergedTree = true).assertCountEquals(1)
        // A recomposition keeps the one announcement; it is not made again.
        speaking = true
        rule.waitForIdle()
        rule.onAllNodes(AssertiveAnnouncement, useUnmergedTree = true).assertCountEquals(1)
        // The message leaves the composition and comes back with its saved state, as in a scrolled thread.
        restoration.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        rule.onAllNodes(AssertiveAnnouncement, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun aNewErrorOnTheSameMessageIsAnnounced() {
        var part by mutableStateOf(MonarchAiErrorPart("network", "x", retryable = false))
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(MonarchAiRole.Assistant, part),
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.waitForIdle()
        part = MonarchAiErrorPart("rate_limit", "y", retryable = false)
        rule.waitForIdle()
        rule.onAllNodes(AssertiveAnnouncement, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun anErrorThatIsNotRetryableHasNoTryAgain() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(MonarchAiRole.Assistant, MonarchAiErrorPart("unknown", "x", retryable = false)),
                onRetry = { error("not retryable") },
                onResume = { error("not resumable") },
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Try again").assertDoesNotExist()
        rule.onNodeWithText("Resume").assertDoesNotExist()
    }

    @Test
    fun aToolWaitingForApprovalIsApprovedById() {
        val approved = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiToolCallPart("t2", "send_invoice", state = MonarchAiToolCallState.PendingApproval),
                ),
                modifier = Modifier.verticalScroll(rememberScrollState()),
                onApproveTool = { id, _ -> approved += id },
                onDenyTool = { _, _ -> },
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Approve").performScrollTo().performClick()
        assertEquals(listOf("t2"), approved)
    }

    @Test
    fun withoutApprovalCallbacksTheToolCallIsShownAsIs() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiToolCallPart("t2", "send_invoice", state = MonarchAiToolCallState.PendingApproval),
                ),
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Approve").assertDoesNotExist()
    }

    @Test
    fun aCutShortReplyShowsTheNoticeOnceFinished() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiTextPart("The first three steps are"),
                    finishReason = MonarchAiFinishReason.Length,
                ),
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("This answer was cut short because it reached the length limit.").assertExists()
    }

    @Test
    fun aQueuedMessageShowsTheNoteAndCancelsWithoutActions() {
        var cancelled = 0
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(MonarchAiRole.User, MonarchAiTextPart("Later"), queued = true),
                onCancelQueued = { cancelled++ },
                onEdit = {},
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Queued. It will send when you are back online.").assertExists()
        rule.onNodeWithText("Edit").assertDoesNotExist()
        rule.onNodeWithText("Cancel").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, cancelled)
    }

    @Test
    fun editingShowsTheFieldAndSavesTheText() {
        val saved = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(MonarchAiRole.User, MonarchAiTextPart("When is it due?")),
                editing = true,
                onEditSubmit = { saved += it },
                onEditCancel = {},
                labels = MonarchAiMessageLabels(editSave = "Save"),
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNode(hasSetTextAction()).performTextReplacement("When is invoice 42 due?")
        rule.onNodeWithText("Save").performClick()
        assertEquals(listOf("When is invoice 42 due?"), saved)
    }

    @Test
    fun theVersionSwitcherNeedsItsIconsAndCallsBack() {
        val versions = mutableListOf<Int>()
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiTextPart("Hi"),
                    branch = MonarchAiBranchPosition(0, 2),
                ),
                onBranch = { versions += it },
                previousVersionIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                nextVersionIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                agentIcon = null,
                actionIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Next version").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(1), versions)
    }

    @Test
    fun withoutIconsTheVersionSwitcherIsLeftOut() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiTextPart("Hi"),
                    branch = MonarchAiBranchPosition(0, 2),
                ),
                onBranch = {},
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Next version").assertDoesNotExist()
    }

    @Test
    fun renderPartReplacesTheDefault() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiTextPart("Default"),
                    MonarchAiFilePart("a.pdf", "application/pdf"),
                ),
                renderPart = { part, _ -> if (part is MonarchAiFilePart) ({ Text("Custom ${part.name}") }) else null },
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Custom a.pdf").assertExists()
        rule.onNodeWithText("Default", useUnmergedTree = true).assertExists()
    }

    @Test
    fun artifactAndUiPartsNeedTheAppsCallback() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiUiPart("Chart", MonarchAiJSON.Null),
                ),
                showActions = false,
                showTimestamp = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Chart", substring = true, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theNameComesBeforeTheTimeRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiMessage(
                message = answer(),
                timeZone = "UTC",
                is24Hour = true,
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Ledger").assertIsBefore(rule.onNodeWithText("09:05"), LayoutDirection.Rtl)
    }

    @Test
    fun theTextIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiMessage(
                message = answer(),
                timeZone = "UTC",
                showActions = false,
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithText("Ledger").assertTextFullyVisible()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAiMessage(
                message = message(
                    MonarchAiRole.Assistant,
                    MonarchAiTextPart("Hi"),
                    finishReason = MonarchAiFinishReason.Refusal,
                ),
                showActions = false,
                labels = MonarchAiMessageLabels(
                    assistantSaid = "Antwort:",
                    assistant = "Helfer",
                    finishNotice = { if (it == MonarchAiFinishReason.Refusal) "Abgelehnt." else null },
                ),
                agentIcon = null,
                actionIcon = null,
                previousVersionIcon = null,
                nextVersionIcon = null,
            )
        }
        rule.onNodeWithContentDescription("Antwort:").assertExists()
        rule.onNodeWithText("Helfer").assertExists()
        rule.onNodeWithText("Abgelehnt.").assertExists()
    }

    @Test
    fun theReplyIsDoneOnlyWithoutStreamingTextOrRunningTools() {
        val done = message(MonarchAiRole.Assistant, MonarchAiTextPart("a"), finishReason = MonarchAiFinishReason.Length)
        val streaming = message(MonarchAiRole.Assistant, MonarchAiTextPart("a", MonarchAiPartState.Streaming))
        val running = message(
            MonarchAiRole.Assistant,
            MonarchAiTextPart("a"),
            MonarchAiToolCallPart("t", "x", state = MonarchAiToolCallState.AwaitingResult),
            finishReason = MonarchAiFinishReason.Length,
        )
        assertTrue(MonarchAiMessageTokens.replyDone(done))
        assertFalse(MonarchAiMessageTokens.replyDone(streaming))
        assertFalse(MonarchAiMessageTokens.replyDone(running))
        assertEquals(MonarchAiFinishReason.Length, MonarchAiMessageTokens.finishNotice(done))
        assertNull(MonarchAiMessageTokens.finishNotice(running))
        assertNull(
            MonarchAiMessageTokens.finishNotice(
                message(MonarchAiRole.Assistant, MonarchAiTextPart("a"), finishReason = MonarchAiFinishReason.Stop),
            ),
        )
        val parts = listOf(Source, MonarchAiTextPart("a"), MonarchAiTextPart("b"))
        assertEquals(2, MonarchAiMessageTokens.lastTextIndex(parts))
        assertEquals(-1, MonarchAiMessageTokens.lastTextIndex(listOf(Source)))
        assertTrue(MonarchAiMessageTokens.queued(message(MonarchAiRole.User, queued = true)))
        assertFalse(MonarchAiMessageTokens.queued(message(MonarchAiRole.Assistant, queued = true)))
    }

    @Test
    fun theTimeFollowsTheZoneAndTheClock() {
        val millis = 1_793_869_500_000L // 2026-11-05T09:05:00Z
        assertEquals(clock("9:05 AM"), MonarchAiMessageTokens.time(millis, Locale.US, "UTC", is24Hour = false))
        assertEquals("10:05", MonarchAiMessageTokens.time(millis, Locale.UK, "Europe/Paris", is24Hour = true))
    }

    @Test
    fun colorsComeFromRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val bubble = MonarchAiMessageTokens.bubble(colors)
            assertEquals(colors.bgMuted, bubble.container)
            assertEquals(colors.textDefault, bubble.content)
            assertEquals(colors.textMuted, MonarchAiMessageTokens.metaColor(colors))
            assertEquals(colors.warningStrong, MonarchAiMessageTokens.noticeColor(colors))
        }
    }
}
