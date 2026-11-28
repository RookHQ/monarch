package dev.monarchui.components.files.tableblock

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchTableBlockPreview() {
    PreviewFrame {
        MonarchTableBlock(
            data = MonarchTableBlockData.of(
                listOf(
                    listOf("Plan", "Seats", "Price"),
                    listOf("Team", "10", "12"),
                    listOf("Business", "Unlimited", "30"),
                ),
            ),
            caption = "Plans",
        )
        MonarchTableBlock(data = MonarchTableBlockData.empty(rowCount = 2, columnCount = 2), onDataChange = {})
    }
}
