package com.rookhq.monarch.components.technical.secretfield

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSecretFieldPreview() {
    PreviewFrame {
        MonarchSecretField(value = "sk_live_51Hx", label = "API key", onValueChange = {}, copyable = true)
        MonarchSecretField(value = "sk_live_51Hx", label = "API key", onValueChange = {}, revealed = true)
        MonarchSecretField(value = "", label = "Signing secret", onValueChange = {}, saved = true)
        MonarchSecretField(
            value = "abc",
            label = "Bot token",
            onValueChange = {},
            errorText = "Tokens start with xoxb-.",
        )
        MonarchSecretField(value = "sk_live_51Hx", label = "API key", onValueChange = {}, enabled = false)
    }
}
