package com.rookhq.monarch.components.intelligence.aitoolresultcard

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.datadisplay.keyvalue.MonarchKeyValueItem
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
