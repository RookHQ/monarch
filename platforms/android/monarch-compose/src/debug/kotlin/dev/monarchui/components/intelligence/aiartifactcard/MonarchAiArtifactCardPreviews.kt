package dev.monarchui.components.intelligence.aiartifactcard

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactKind
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiArtifactCardPreview() {
    PreviewFrame {
        MonarchAiArtifactCard(title = "Q3 summary", kind = MonarchAiArtifactKind.Document, version = 3, onClick = {})
        MonarchAiArtifactCard(
            title = "Revenue by region",
            kind = MonarchAiArtifactKind.Chart,
            version = 1,
            onClick = {},
            description = "Updated just now",
            active = true,
        )
        MonarchAiArtifactCard(
            title = "Import script",
            kind = MonarchAiArtifactKind.Code,
            version = 2,
            onClick = {},
            enabled = false,
        )
    }
}
