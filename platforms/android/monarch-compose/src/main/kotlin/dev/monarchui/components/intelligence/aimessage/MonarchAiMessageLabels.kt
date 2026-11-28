package dev.monarchui.components.intelligence.aimessage

import androidx.compose.runtime.Immutable
import dev.monarchui.components.files.attachmentchip.MonarchAttachmentChipLabels
import dev.monarchui.components.intelligence.aicitation.MonarchAiCitationLabels
import dev.monarchui.components.intelligence.aichainofsteps.MonarchAiChainOfStepsLabels
import dev.monarchui.components.intelligence.aiconversation.MonarchAiFinishReason
import dev.monarchui.components.intelligence.aierrormessage.MonarchAiErrorMessageLabels
import dev.monarchui.components.intelligence.aimemorychip.MonarchAiMemoryChipLabels
import dev.monarchui.components.intelligence.aimessageactions.MonarchAiMessageActionsLabels
import dev.monarchui.components.intelligence.aireasoning.MonarchAiReasoningLabels
import dev.monarchui.components.intelligence.aisourcelist.MonarchAiSourceListLabels
import dev.monarchui.components.intelligence.aistreamingtext.MonarchAiStreamingTextLabels
import dev.monarchui.components.intelligence.aitoolapproval.MonarchAiToolApprovalLabels
import dev.monarchui.components.intelligence.aitoolcall.MonarchAiToolCallLabels

/**
 * Replacements for the built-in strings of a `MonarchAiMessage` and of the components it shows. Every `null`
 * keeps the built-in string `monarch_aimessage_<key>`; the nested labels go to the component of each part.
 *
 * @property assistantSaid read by TalkBack before an assistant message, "Assistant said:".
 * @property youSaid read by TalkBack before the person's own message, "You said:".
 * @property systemSaid read by TalkBack before a system or tool message, "System message:".
 * @property assistant the name in the header when the message has no agent, "Assistant".
 * @property finishNotice the notice under a reply that did not finish normally, by its finish reason. Return
 * `null` to keep the built-in notice of `Length`, `ContentFilter` or `Refusal`.
 * @property queued shown on a queued message, "Queued. It will send when you are back online.".
 * @property cancelQueued the button that removes a queued message, "Cancel".
 * @property resume the button on an error that can be resumed, "Resume".
 * @property editLabel the TalkBack label of the edit field; `null` keeps that of `MonarchAiUserMessageEdit`.
 * @property editSave the save button of the edit field; `null` keeps that of `MonarchAiUserMessageEdit`.
 * @property editCancel the cancel button of the edit field; `null` keeps that of `MonarchAiUserMessageEdit`.
 * @property versions the TalkBack name of the version switcher; `null` keeps that of `MonarchAiMessageBranch`.
 * @property previousVersion the previous-version button; `null` keeps that of `MonarchAiMessageBranch`.
 * @property nextVersion the next-version button; `null` keeps that of `MonarchAiMessageBranch`.
 * @property text the labels of each text part's `MonarchAiStreamingText`.
 * @property reasoning the labels of each reasoning part's `MonarchAiReasoning`.
 * @property toolCall the labels of each tool call's `MonarchAiToolCall`.
 * @property toolApproval the labels of a tool call waiting for approval, a `MonarchAiToolApproval`.
 * @property sources the labels of the `MonarchAiSourceList`.
 * @property citation the labels of each citation's `MonarchAiCitation`.
 * @property error the labels of each error part's `MonarchAiErrorMessage`.
 * @property memory the labels of each memory part's `MonarchAiMemoryChip`.
 * @property file the labels of each file part's `MonarchAttachmentChip`.
 * @property plan the labels of each plan part's `MonarchAiChainOfSteps`.
 * @property actions the labels of the `MonarchAiMessageActions` under the message.
 */
@Immutable
public class MonarchAiMessageLabels(
    public val assistantSaid: String? = null,
    public val youSaid: String? = null,
    public val systemSaid: String? = null,
    public val assistant: String? = null,
    public val finishNotice: ((MonarchAiFinishReason) -> String?)? = null,
    public val queued: String? = null,
    public val cancelQueued: String? = null,
    public val resume: String? = null,
    public val editLabel: String? = null,
    public val editSave: String? = null,
    public val editCancel: String? = null,
    public val versions: String? = null,
    public val previousVersion: String? = null,
    public val nextVersion: String? = null,
    public val text: MonarchAiStreamingTextLabels = MonarchAiStreamingTextLabels(),
    public val reasoning: MonarchAiReasoningLabels = MonarchAiReasoningLabels(),
    public val toolCall: MonarchAiToolCallLabels = MonarchAiToolCallLabels(),
    public val toolApproval: MonarchAiToolApprovalLabels = MonarchAiToolApprovalLabels(),
    public val sources: MonarchAiSourceListLabels = MonarchAiSourceListLabels(),
    public val citation: MonarchAiCitationLabels = MonarchAiCitationLabels(),
    public val error: MonarchAiErrorMessageLabels = MonarchAiErrorMessageLabels(),
    public val memory: MonarchAiMemoryChipLabels = MonarchAiMemoryChipLabels(),
    public val file: MonarchAttachmentChipLabels = MonarchAttachmentChipLabels(),
    public val plan: MonarchAiChainOfStepsLabels = MonarchAiChainOfStepsLabels(),
    public val actions: MonarchAiMessageActionsLabels = MonarchAiMessageActionsLabels(),
)
