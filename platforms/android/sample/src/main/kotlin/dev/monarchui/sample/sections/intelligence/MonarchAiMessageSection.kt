@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiBranchPosition
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.intelligence.aiconversation.MonarchAiErrorPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiFinishReason
import dev.monarchui.components.intelligence.aiconversation.MonarchAiMessageFeedback
import dev.monarchui.components.intelligence.aiconversation.MonarchAiReasoningPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiRole
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallState
import dev.monarchui.components.intelligence.aimessage.MonarchAiMessage
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sample.aiActionIcon
import dev.monarchui.sampleicons.SampleIcons

private const val Created = "2026-11-05T09:05:00.000Z"
private val Ledger = MonarchAiAgentRef("Ledger", role = "Billing")
private val Answers = listOf(
    "Invoice 42 is due on **4 November**. I added a reminder for 1 November.",
    "Invoice 42 is due on 4 November, in 12 days.",
)

/** The catalogue page of AiMessageView: the person's question and the assistant's answer with every part. */
object MonarchAiMessageSection : CataloguePage {
    override val title = "AiMessageView"

    @Composable
    override fun Content() {
        var editing by rememberSaveable { mutableStateOf(false) }
        var question by rememberSaveable { mutableStateOf("When is invoice 42 due?") }
        var feedback by rememberSaveable { mutableStateOf<MonarchAiMessageFeedback?>(null) }
        var version by rememberSaveable { mutableIntStateOf(0) }
        var approved by rememberSaveable { mutableStateOf(false) }
        var last by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("The person's message")
        MonarchAiMessage(
            message = MonarchAiConversationMessage(
                id = "u1",
                role = MonarchAiRole.User,
                parts = listOf(MonarchAiTextPart(question)),
                createdAt = Created,
            ),
            editing = editing,
            onEdit = { editing = true },
            onEditSubmit = {
                question = it
                editing = false
            },
            onEditCancel = { editing = false },
            actionIcon = { action, _ -> MonarchIcon(aiActionIcon(action), contentDescription = null) },
        )
        CatalogueCaption("An answer with reasoning, a tool call, sources and versions")
        MonarchAiMessage(
            message = MonarchAiConversationMessage(
                id = "a1",
                role = MonarchAiRole.Assistant,
                parts = listOf(
                    MonarchAiReasoningPart("Find invoice 42 and read its terms.", durationMs = 3_000),
                    MonarchAiToolCallPart(
                        "t1",
                        "send_reminder",
                        state = if (approved) {
                            MonarchAiToolCallState.Succeeded
                        } else {
                            MonarchAiToolCallState.PendingApproval
                        },
                    ),
                    MonarchAiTextPart(Answers[version]),
                    MonarchAiSourcePart("s1", "Billing handbook", kind = MonarchAiSourceKind.Doc),
                    MonarchAiSourcePart("s2", "Invoice 42", kind = MonarchAiSourceKind.Record),
                ),
                createdAt = Created,
                agent = Ledger,
                feedback = feedback,
                branch = MonarchAiBranchPosition(version, Answers.size),
            ),
            onRegenerate = { last = "Regenerate" },
            onFeedback = { feedback = it },
            onBranch = { version = it },
            onApproveTool = { _, _ -> approved = true },
            onDenyTool = { _, _ -> last = "Denied" },
            actionIcon = { action, _ -> MonarchIcon(aiActionIcon(action), contentDescription = null) },
            previousVersionIcon = { MonarchIcon(SampleIcons.ChevronLeft, contentDescription = null) },
            nextVersionIcon = { MonarchIcon(SampleIcons.ChevronRight, contentDescription = null) },
        )
        CatalogueCaption("Cut short, and an error that can be resumed")
        MonarchAiMessage(
            message = MonarchAiConversationMessage(
                id = "a2",
                role = MonarchAiRole.Assistant,
                parts = listOf(
                    MonarchAiTextPart("The overdue invoices are 17, 23 and"),
                    MonarchAiErrorPart("network", "The stream closed.", retryable = true, resumable = true),
                ),
                createdAt = Created,
                agent = Ledger,
                finishReason = MonarchAiFinishReason.Length,
            ),
            onRetry = { last = "Try again" },
            onResume = { last = "Resume" },
            showActions = false,
        )
        if (last.isNotEmpty()) MonarchText(text = "Last action: $last")
    }
}
