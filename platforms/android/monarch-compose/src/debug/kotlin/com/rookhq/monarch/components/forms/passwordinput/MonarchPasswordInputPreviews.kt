package com.rookhq.monarch.components.forms.passwordinput

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchPasswordInputPreview() {
    PreviewFrame {
        MonarchPasswordInput(value = "", onValueChange = {}, label = "Password", helperText = "At least 8 characters.")
        MonarchPasswordInput(value = "correct horse", onValueChange = {}, label = "Password", revealed = true)
        MonarchPasswordInput(
            value = "secret",
            onValueChange = {},
            label = "New password",
            purpose = MonarchPasswordInputPurpose.New,
            errorText = "Use at least 8 characters.",
        )
        MonarchPasswordInput(value = "secret", onValueChange = {}, label = "Password", enabled = false)
    }
}
