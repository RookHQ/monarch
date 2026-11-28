package com.rookhq.monarch.components.interaction.reorderablelist

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
internal fun MonarchReorderableListPreview() {
    PreviewFrame {
        MonarchReorderableList(
            items = listOf("Design review", "Budget", "Hiring plan"),
            key = { it },
            itemLabel = { it },
            onReorder = { _, _ -> },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            itemEnabled = { it != "Budget" },
        ) { item, _ -> Text(item) }
        MonarchReorderableList(
            items = listOf("Inbox", "Archive"),
            key = { it },
            itemLabel = { it },
            onReorder = { _, _ -> },
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            moveButtons = true,
        ) { item, _ -> Text(item) }
    }
}
