package dev.monarchui.components.intelligence.aitoolresultcard

import androidx.compose.runtime.Composable
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueItem
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiToolResultCardPreview() {
    PreviewFrame {
        MonarchAiToolResultCard(
            title = "Acme Ltd",
            kind = MonarchAiToolResultKind.RecordCreated,
            description = "Added to Accounts",
            meta = listOf(MonarchKeyValueItem("Owner", "Ana Flores"), MonarchKeyValueItem("Region", "EMEA")),
            onOpen = {},
            onUndo = {},
        )
        MonarchAiToolResultCard(title = "Quarterly review", kind = MonarchAiToolResultKind.EventBooked)
    }
}
