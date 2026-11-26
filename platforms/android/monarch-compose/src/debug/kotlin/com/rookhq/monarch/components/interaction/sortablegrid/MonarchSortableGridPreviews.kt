package com.rookhq.monarch.components.interaction.sortablegrid

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

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
