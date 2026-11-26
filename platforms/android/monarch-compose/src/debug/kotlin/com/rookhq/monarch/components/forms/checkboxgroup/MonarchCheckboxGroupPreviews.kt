package com.rookhq.monarch.components.forms.checkboxgroup

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val previewOptions = listOf(
    MonarchCheckboxGroupOption("email", "Email", description = "A summary every morning."),
    MonarchCheckboxGroupOption("push", "Push notifications"),
    MonarchCheckboxGroupOption("sms", "Text message", enabled = false),
)

@MonarchPreviews
@Composable
internal fun MonarchCheckboxGroupPreview() {
    PreviewFrame {
        MonarchCheckboxGroup(
            options = previewOptions,
            value = listOf("email"),
            onValueChange = {},
            selectAll = true,
            label = "Notify me by",
            helperText = "Pick at least one.",
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchCheckboxGroupStatesPreview() {
    PreviewFrame {
        MonarchCheckboxGroup(
            options = previewOptions,
            value = emptyList(),
            onValueChange = {},
            orientation = MonarchCheckboxGroupOrientation.Horizontal,
            required = true,
            label = "Channels",
            errorText = "Choose at least one channel.",
        )
        MonarchCheckboxGroup(options = previewOptions, value = listOf("push"), onValueChange = {}, enabled = false)
    }
}
