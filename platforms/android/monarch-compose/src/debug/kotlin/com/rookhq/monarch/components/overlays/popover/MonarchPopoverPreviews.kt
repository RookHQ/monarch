package com.rookhq.monarch.components.overlays.popover

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
