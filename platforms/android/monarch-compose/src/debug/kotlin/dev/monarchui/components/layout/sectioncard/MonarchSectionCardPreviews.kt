package dev.monarchui.components.layout.sectioncard

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
