package dev.monarchui.components.overlays.popover

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchPopoverPreview() {
    PreviewFrame {
        Box {
            MonarchButton(text = "Filter", onClick = {})
            MonarchPopover(expanded = true, onDismissRequest = {}, label = "Filter invoices") {
                MonarchText(text = "Filter invoices", style = MonarchTextStyle.Subheading, heading = true)
                MonarchText(text = "Show only the invoices that are not paid yet.")
                MonarchButton(text = "Done", onClick = {}, variant = MonarchButtonVariant.Primary)
            }
        }
    }
}
