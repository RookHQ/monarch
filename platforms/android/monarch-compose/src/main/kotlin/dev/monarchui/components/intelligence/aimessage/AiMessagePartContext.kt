package dev.monarchui.components.intelligence.aimessage

import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiEditProposalPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiFilePart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aieditproposal.MonarchAiEditHunk
import dev.monarchui.components.intelligence.aigenerativeui.MonarchAiUiAction
import dev.monarchui.components.intelligence.aigenerativeui.MonarchAiUiRegistry

/** What the parts of one MonarchAiMessage need from it: callbacks, registries and labels. */
internal class AiMessagePartContext(
    val announces: Boolean,
    val lastText: Int,
    val replyDone: Boolean,
    val sources: List<MonarchAiSourcePart>,
    val sourceNumbers: Map<String, Int>,
    val labels: MonarchAiMessageLabels,
    val onRetry: (() -> Unit)? = null,
    val onResume: (() -> Unit)? = null,
    val onForgetMemory: ((id: String) -> Unit)? = null,
    val onEditMemory: ((id: String, text: String) -> Unit)? = null,
    val onApproveTool: ((toolCallId: String, input: MonarchAiJSON?) -> Unit)? = null,
    val onDenyTool: ((toolCallId: String, reason: String?) -> Unit)? = null,
    val onAlwaysAllowTool: ((toolName: String) -> Unit)? = null,
    val onOpenArtifact: ((MonarchAiArtifactPart) -> Unit)? = null,
    val onOpenFile: ((MonarchAiFilePart) -> Unit)? = null,
    val onOpenSource: ((MonarchAiSourcePart) -> Unit)? = null,
    val onOpenLink: ((url: String) -> Unit)? = null,
    val uiRegistry: MonarchAiUiRegistry? = null,
    val onUiAction: ((MonarchAiUiAction) -> Unit)? = null,
    val editHunks: ((MonarchAiEditProposalPart) -> List<MonarchAiEditHunk>?)? = null,
) {
    /** The index of the first source part: the source list is shown there, once. */
    var firstSource: Int = -1
        private set

    /** Records where the first source part is in [parts]. */
    fun place(parts: List<MonarchAiPart>): AiMessagePartContext = apply {
        firstSource = parts.indexOfFirst { it is MonarchAiSourcePart }
    }
}
