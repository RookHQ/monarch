package dev.monarchui.components.intelligence.aitableanswer

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiTableAnswerPreview() {
    PreviewFrame {
        MonarchAiTableAnswer(
            rows = listOf(
                mapOf("Region" to MonarchAiJSON.Text("North"), "Revenue" to MonarchAiJSON.Number(1200.0)),
                mapOf("Region" to MonarchAiJSON.Text("South"), "Revenue" to MonarchAiJSON.Number(950.5)),
            ),
            title = "Revenue by region",
            onOpenAsArtifact = {},
        )
        MonarchAiTableAnswer(rows = emptyList(), title = "Waiting for rows", streaming = true)
        MonarchAiTableAnswer(rows = emptyList())
    }
}
