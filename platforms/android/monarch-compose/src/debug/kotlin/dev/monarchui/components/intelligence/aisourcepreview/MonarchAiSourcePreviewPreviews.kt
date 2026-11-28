package dev.monarchui.components.intelligence.aisourcepreview

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

/** The picker and the preview scroll themselves, so the scrolling preview frame must bound them. */
private val PreviewHeight = 400.dp

@MonarchPreviews
@Composable
internal fun MonarchAiSourcePreviewPreview() {
    PreviewFrame {
        MonarchAiSourcePreview(
            modifier = Modifier.heightIn(max = PreviewHeight),
            source = MonarchAiSourcePart("s1", "Q3 revenue report", "https://example.com/q3", MonarchAiSourceKind.Doc),
            quote = "revenue grew 12%",
            content = "Summary of the quarter.\n\nRevenue grew 12% over the quarter, led by Payments.",
            onClose = {},
        )
        MonarchAiSourcePreview(
            modifier = Modifier.heightIn(max = PreviewHeight),
            source = MonarchAiSourcePart("s2", "Acme Corp", kind = MonarchAiSourceKind.Record, product = "CRM"),
        )
    }
}
