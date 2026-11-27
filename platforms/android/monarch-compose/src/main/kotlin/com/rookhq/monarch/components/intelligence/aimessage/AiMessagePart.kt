package com.rookhq.monarch.components.intelligence.aimessage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.files.attachmentchip.MonarchAttachmentChip
import com.rookhq.monarch.components.intelligence.aiartifactcard.MonarchAiArtifactCard
import com.rookhq.monarch.components.intelligence.aichainofsteps.MonarchAiChainOfSteps
import com.rookhq.monarch.components.intelligence.aicitation.MonarchAiCitation
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiArtifactPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiCitationPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiEditProposalPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiErrorPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiFilePart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryAction
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMemoryPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPartState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiReasoningPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourcePart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTextPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallState
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiUiPart
import com.rookhq.monarch.components.intelligence.aieditproposal.MonarchAiEditProposal
import com.rookhq.monarch.components.intelligence.aierrormessage.MonarchAiErrorKind
import com.rookhq.monarch.components.intelligence.aierrormessage.MonarchAiErrorMessage
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiGenerativeUi
import com.rookhq.monarch.components.intelligence.aimemorychip.MonarchAiMemoryChip
import com.rookhq.monarch.components.intelligence.aireasoning.LocalMonarchAiReasoningQuiet
import com.rookhq.monarch.components.intelligence.aireasoning.MonarchAiReasoning
import com.rookhq.monarch.components.intelligence.aisourcelist.MonarchAiSourceList
import com.rookhq.monarch.components.intelligence.aisourcelist.MonarchAiSourceListItem
import com.rookhq.monarch.components.intelligence.aistreamingtext.MonarchAiStreamingText
import com.rookhq.monarch.components.intelligence.aitoolapproval.MonarchAiToolApproval
import com.rookhq.monarch.components.intelligence.aitoolcall.MonarchAiToolCall
import com.rookhq.monarch.internal.monarchFormatBytes
import com.rookhq.monarch.internal.monarchString

/** The default rendering of one part of a MonarchAiMessage, with the Monarch component for its type. */
@OptIn(ExperimentalMonarchApi::class)
@Composable
internal fun AiMessagePart(part: MonarchAiPart, index: Int, context: AiMessagePartContext) {
    val labels = context.labels
    when (part) {
        is MonarchAiTextPart -> MonarchAiStreamingText(
            text = part.text,
            streaming = part.state == MonarchAiPartState.Streaming,
            announces = context.announces,
            announceComplete = index == context.lastText && context.replyDone,
            onOpenLink = context.onOpenLink,
            labels = labels.text,
        )
        is MonarchAiReasoningPart -> CompositionLocalProvider(LocalMonarchAiReasoningQuiet provides true) {
            MonarchAiReasoning(part = part, labels = labels.reasoning)
        }
        is MonarchAiToolCallPart -> AiMessageToolCall(part, context)
        is MonarchAiSourcePart -> if (index == context.firstSource) AiMessageSources(context)
        is MonarchAiCitationPart -> AiMessageCitation(part, context)
        is MonarchAiFilePart -> MonarchAttachmentChip(
            name = part.name,
            sizeBytes = part.size,
            sizeText = part.size?.let { monarchFormatBytes(it).replace(" KB", " kB") },
            contentType = part.mediaType,
            onOpen = context.onOpenFile?.let { open -> { open(part) } },
            labels = labels.file,
        )
        is MonarchAiErrorPart -> AiMessageError(part, context)
        else -> AiMessageWorkPart(part, context)
    }
}

/** Memories, plans, artifacts, generated UI and edit proposals: the parts that show the assistant's work. */
@OptIn(ExperimentalMonarchApi::class)
@Composable
private fun AiMessageWorkPart(part: MonarchAiPart, context: AiMessagePartContext) {
    when (part) {
        is MonarchAiMemoryPart -> {
            // Only a memory the reply just saved can still be changed from the message.
            val saved = part.action == MonarchAiMemoryAction.Write
            MonarchAiMemoryChip(
                memory = part,
                labels = context.labels.memory,
                onEdit = context.onEditMemory.takeIf { saved },
                onForget = context.onForgetMemory.takeIf { saved },
            )
        }
        is MonarchAiPlanPart -> MonarchAiChainOfSteps(
            steps = part.steps,
            collapsible = false,
            announces = false,
            labels = context.labels.plan,
        )
        is MonarchAiArtifactPart -> context.onOpenArtifact?.let { open ->
            MonarchAiArtifactCard(part = part, onOpen = open)
        }
        is MonarchAiUiPart -> context.uiRegistry?.let { registry ->
            MonarchAiGenerativeUi(part = part, registry = registry, onAction = context.onUiAction)
        }
        is MonarchAiEditProposalPart -> context.editHunks?.invoke(part)?.let { hunks ->
            MonarchAiEditProposal(part = part, hunks = hunks)
        }
        else -> Unit
    }
}

@Composable
private fun AiMessageSources(context: AiMessagePartContext) {
    MonarchAiSourceList(
        items = context.sources.map { MonarchAiSourceListItem(it) },
        labels = context.labels.sources,
        onOpen = context.onOpenSource?.let { open -> { item -> open(item.source) } },
        plain = true,
    )
}

@OptIn(ExperimentalMonarchApi::class)
@Composable
private fun AiMessageToolCall(part: MonarchAiToolCallPart, context: AiMessagePartContext) {
    val approve = context.onApproveTool
    val deny = context.onDenyTool
    if (part.state == MonarchAiToolCallState.PendingApproval && (approve != null || deny != null)) {
        MonarchAiToolApproval(
            part = part,
            onApprove = { input -> approve?.invoke(part.id, input) },
            onDeny = { reason -> deny?.invoke(part.id, reason) },
            onAlwaysAllow = context.onAlwaysAllowTool,
            labels = context.labels.toolApproval,
        )
    } else {
        MonarchAiToolCall(
            part = part,
            toolLabels = mapOf(part.name to part.name),
            labels = context.labels.toolCall,
            compact = true,
        )
    }
}

@Composable
private fun AiMessageCitation(part: MonarchAiCitationPart, context: AiMessagePartContext) {
    val number = context.sourceNumbers[part.sourceId] ?: return
    val source = context.sources.firstOrNull { it.id == part.sourceId } ?: return
    MonarchAiCitation(
        index = number,
        source = source,
        quote = part.quote,
        onOpen = context.onOpenSource,
        labels = context.labels.citation,
        bare = true,
    )
}

/**
 * An error part. It is announced once per error, when it first appears, as on the web: not again when the message
 * comes back into a scrolled list, after a configuration change or when [AiMessagePartContext.announces] turns on
 * later. The "already shown" mark is saved, so it outlives the card leaving the composition.
 */
@Composable
private fun AiMessageError(part: MonarchAiErrorPart, context: AiMessagePartContext) {
    val resume = context.onResume.takeIf { part.resumable }
    val resumeLabel = monarchString(context.labels.resume, R.string.monarch_aimessage_resume)
    var shown by rememberSaveable(part.code, part.message) { mutableStateOf(false) }
    val announce = remember(part.code, part.message) { context.announces && !shown }
    SideEffect { shown = true }
    MonarchAiErrorMessage(
        kind = MonarchAiErrorKind.fromCode(part.code),
        details = part.message,
        retryAfterMillis = part.retryAfterMs,
        announce = announce,
        labels = context.labels.error,
        onRetry = context.onRetry.takeIf { part.retryable },
        actions = resume?.let { onResume ->
            { MonarchButton(text = resumeLabel, onClick = onResume, size = MonarchButtonSize.Sm) }
        },
    )
}
