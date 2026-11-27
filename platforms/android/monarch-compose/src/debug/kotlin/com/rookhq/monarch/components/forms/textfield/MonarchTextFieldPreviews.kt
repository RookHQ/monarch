package com.rookhq.monarch.components.forms.textfield

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
