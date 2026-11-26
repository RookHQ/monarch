package com.rookhq.monarch.components.layout.sectioncard

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchSectionCardPreview() {
    PreviewFrame {
        MonarchSectionCard(
            title = "Open tickets",
            meta = "12 open",
            actions = {
                MonarchButton(
                    text = "View all",
                    onClick = {},
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            },
            footer = { MonarchText(text = "Updated 5 min ago") },
        ) { MonarchText(text = "Printer offline on floor 3") }
        MonarchSectionCard(title = "Invoices", state = MonarchSectionCardState.Empty) {}
        MonarchSectionCard(title = "Usage", state = MonarchSectionCardState.Error, onRetry = {}) {}
        MonarchSectionCard(
            title = "Danger zone",
            tone = MonarchTone.Danger,
            icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        ) { MonarchButton(text = "Delete workspace", onClick = {}, variant = MonarchButtonVariant.Danger) }
        MonarchSectionCard(title = "Advanced", expanded = false, onExpandedChange = {}) {}
    }
}
