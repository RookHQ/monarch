package dev.monarchui.components.intelligence.aisourcelist

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiSourceListPreview() {
    PreviewFrame {
        MonarchAiSourceList(
            items = listOf(
                MonarchAiSourceListItem(
                    MonarchAiSourcePart("s1", "Q3 report", "https://www.example.com/q3", MonarchAiSourceKind.Doc),
                    snippet = "Revenue grew 12% over the quarter.",
                ),
                MonarchAiSourceListItem(
                    MonarchAiSourcePart("s2", "Acme Corp", kind = MonarchAiSourceKind.Record, product = "CRM"),
                ),
                MonarchAiSourceListItem(
                    MonarchAiSourcePart("s3", "Pricing page", "https://example.com/pricing", MonarchAiSourceKind.Web),
                    used = false,
                ),
            ),
            onOpen = {},
        )
    }
}
