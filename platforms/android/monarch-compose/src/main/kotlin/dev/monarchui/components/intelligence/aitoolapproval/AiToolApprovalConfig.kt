package dev.monarchui.components.intelligence.aitoolapproval

import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallRisk

/** What the controls of MonarchAiToolApproval need from the app. */
internal class AiToolApprovalConfig(
    val risk: MonarchAiToolCallRisk,
    val editable: Boolean,
    val onAlwaysAllow: ((String) -> Unit)?,
    val onApprove: (MonarchAiJSON?) -> Unit,
    val onDeny: (String?) -> Unit,
)
