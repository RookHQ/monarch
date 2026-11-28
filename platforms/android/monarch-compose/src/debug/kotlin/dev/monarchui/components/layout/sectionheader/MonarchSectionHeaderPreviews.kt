package dev.monarchui.components.layout.sectionheader

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSectionHeaderPreview() {
    PreviewFrame {
        MonarchSectionHeader(title = "Recent invoices", meta = "12 issued this month") {
            MonarchButton(text = "View all", onClick = {}, size = MonarchButtonSize.Sm)
        }
        MonarchSectionHeader(
            title = "Team members",
            description = "People who can sign in to this workspace.",
            size = MonarchSectionHeaderSize.Md,
        )
        MonarchSectionHeader(title = "Traffic sources", meta = "Last 7 days", busy = true)
    }
}
