package dev.monarchui.components.intelligence.aiagentavatar

import androidx.compose.runtime.Composable
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
