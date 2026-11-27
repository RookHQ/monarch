package com.rookhq.monarch.components.intelligence.aiagentavatar

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiAgentAvatarPreview() {
    val agent = MonarchAiAgentRef(name = "Research agent", id = "research", codename = "RA")
    PreviewFrame {
        MonarchAiAgentState.entries.forEach { state ->
            MonarchAiAgentAvatar(agent = agent, state = state, showState = true)
        }
        MonarchAvatarSize.entries.forEach { size ->
            MonarchAiAgentAvatar(agent = agent, size = size, state = MonarchAiAgentState.Waiting)
        }
    }
}
