package com.rookhq.monarch.components.forms.inlineedittext

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
