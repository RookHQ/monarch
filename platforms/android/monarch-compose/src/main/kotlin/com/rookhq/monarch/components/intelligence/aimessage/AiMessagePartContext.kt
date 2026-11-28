package com.rookhq.monarch.components.intelligence.aimessage

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiArtifactPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiEditProposalPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiFilePart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiSourcePart
import com.rookhq.monarch.components.intelligence.aieditproposal.MonarchAiEditHunk
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiUiAction
import com.rookhq.monarch.components.intelligence.aigenerativeui.MonarchAiUiRegistry

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
