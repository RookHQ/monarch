package dev.monarchui.components.forms.inlineedittext

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchInlineEditTextPreview() {
    PreviewFrame {
        MonarchInlineEditText(
            value = "Quarterly plan",
            onSave = {},
            label = "Title",
            editIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
        )
        MonarchInlineEditText(value = "", onSave = {}, label = "Notes", placeholder = "Add notes")
        MonarchInlineEditText(value = "Ada", onSave = {}, label = "Name", startEditing = true)
        MonarchInlineEditText(value = "Locked", onSave = {}, label = "Owner", enabled = false)
    }
}
