@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aiconversationheader

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiConversationHeaderPreview() {
    PreviewFrame {
        MonarchAiConversationHeader(
            title = "Q3 revenue summary",
            agent = MonarchAiAgentRef(name = "Dossier"),
            onRename = {},
            model = { Text("Fast model") },
        )
        MonarchAiConversationHeader(title = "", naming = true, agent = MonarchAiAgentRef(name = "Dossier"))
        MonarchAiConversationHeader(title = "")
    }
}
