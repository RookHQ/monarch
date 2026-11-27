@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aiconversationheader

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiAgentRef
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
