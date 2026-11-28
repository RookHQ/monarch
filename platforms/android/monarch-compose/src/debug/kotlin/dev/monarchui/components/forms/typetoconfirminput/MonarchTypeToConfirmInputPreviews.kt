package dev.monarchui.components.forms.typetoconfirminput

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchTypeToConfirmInputPreview() {
    PreviewFrame {
        MonarchTypeToConfirmInput(
            phrase = "acme-site",
            value = "",
            onValueChange = {},
            helperText = "This deletes the site.",
        )
        MonarchTypeToConfirmInput(phrase = "acme-site", value = "acme", onValueChange = {}, showMismatch = true)
        MonarchTypeToConfirmInput(phrase = "acme-site", value = "acme-site", onValueChange = {})
        MonarchTypeToConfirmInput(phrase = "acme-site", value = "", onValueChange = {}, enabled = false)
    }
}
