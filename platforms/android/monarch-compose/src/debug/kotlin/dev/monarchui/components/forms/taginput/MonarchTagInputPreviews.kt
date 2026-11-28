package dev.monarchui.components.forms.taginput

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchTagInputPreview() {
    val close: @Composable () -> Unit = {
        MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm)
    }
    PreviewFrame {
        MonarchTagInput(
            tags = listOf("Research", "TypeScript"),
            onTagsChange = {},
            label = "Skills",
            removeIcon = close,
            placeholder = "Add a skill",
            helperText = "Separate skills with commas.",
        )
        MonarchTagInput(
            tags = listOf("a@b.c"),
            onTagsChange = {},
            label = "Recipients",
            removeIcon = close,
            errorText = "Enter an email address like name@example.com.",
        )
        MonarchTagInput(
            tags = listOf("Billing", "Priority"),
            onTagsChange = {},
            label = "Labels",
            removeIcon = close,
            readOnly = true,
        )
    }
}
