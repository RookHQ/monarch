package dev.monarchui.components.intelligence.aiconversation

import androidx.compose.runtime.Composable
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

/** The conversation draws nothing; the preview shows a stored conversation read from the web's JSON. */
@MonarchPreviews
@Composable
internal fun MonarchAiConversationPreview() {
    val messages = MonarchAiConversationMessage.listFromJson(
        """
        [{"id":"u1","role":"user","createdAt":"2025-03-10T12:00:00.000Z",
          "parts":[{"type":"text","text":"What changed since yesterday?","state":"done"}]},
         {"id":"a1","role":"assistant","createdAt":"2025-03-10T12:00:02.000Z",
          "parts":[{"type":"text","text":"Two invoices were paid.","state":"done"}]}]
        """.trimIndent(),
    )
    PreviewFrame {
        messages.forEach { MonarchText(text = "${it.role.name}: ${it.text}") }
    }
}
