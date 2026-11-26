package com.rookhq.monarch.components.intelligence.aitoolapproval

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallRisk

/** What the controls of MonarchAiToolApproval need from the app. */
internal class AiToolApprovalConfig(
    val risk: MonarchAiToolCallRisk,
    val editable: Boolean,
    val onAlwaysAllow: ((String) -> Unit)?,
    val onApprove: (MonarchAiJSON?) -> Unit,
    val onDeny: (String?) -> Unit,
)
