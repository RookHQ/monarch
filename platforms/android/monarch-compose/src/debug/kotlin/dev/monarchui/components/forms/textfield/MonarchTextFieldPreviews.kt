package dev.monarchui.components.forms.textfield

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchTextFieldPreview() {
    PreviewFrame {
        MonarchTextField(value = "", onValueChange = {}, label = "Email", helperText = "Used to sign in.")
        MonarchTextField(value = "ada@example.com", onValueChange = {}, label = "Email")
        MonarchTextField(
            value = "ada@",
            onValueChange = {},
            label = "Email",
            errorText = "Enter a complete email address.",
        )
        MonarchTextField(value = "ada@example.com", onValueChange = {}, label = "Email", enabled = false)
    }
}
