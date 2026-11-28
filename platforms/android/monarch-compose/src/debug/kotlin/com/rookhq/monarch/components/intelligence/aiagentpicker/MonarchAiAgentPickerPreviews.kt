@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aiagentpicker

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentState
import com.rookhq.monarch.components.intelligence.aiagentcard.MonarchAiAgentTool
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiAgentPickerPreview() {
    PreviewFrame {
        MonarchAiAgentPicker(
            agents = listOf(
                MonarchAiAgentPickerOption(
                    agent = MonarchAiAgentRef(name = "Research agent", id = "research", role = "Finds sources"),
                    description = "Reads documents in this space and answers with citations.",
                    capabilities = listOf("Summarizes", "Cites sources"),
                    tools = listOf(MonarchAiAgentTool("Search documents"), MonarchAiAgentTool("Read file")),
                ),
                MonarchAiAgentPickerOption(
                    agent = MonarchAiAgentRef(name = "Writing agent", id = "writer", role = "Drafts text"),
                    description = "Drafts replies in the tone you choose.",
                    state = MonarchAiAgentState.Working,
                ),
                MonarchAiAgentPickerOption(
                    agent = MonarchAiAgentRef(name = "Scheduling agent", id = "scheduler"),
                    description = "Not enabled in this space.",
                    enabled = false,
                ),
            ),
            selected = "research",
            onSelect = {},
        )
    }
}
