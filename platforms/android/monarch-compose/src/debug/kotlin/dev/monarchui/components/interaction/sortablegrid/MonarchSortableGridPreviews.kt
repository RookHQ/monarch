package dev.monarchui.components.interaction.sortablegrid

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

/** Lazy lists need a bounded height inside the scrolling preview column. */
private const val PreviewHeightSteps = 3

@MonarchPreviews
@Composable
internal fun MonarchSortableGridPreview() {
    PreviewFrame {
        MonarchSortableGrid(
            items = listOf("Revenue", "Signups", "Churn", "Tickets"),
            key = { it },
            itemLabel = { it },
            onReorder = { _, _ -> },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            columns = 2,
            itemEnabled = { it != "Churn" },
        ) { item, _ -> Text(item) }
    }
}
