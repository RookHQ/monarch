package com.rookhq.monarch.components.actions.copybutton

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchCopyButtonPreview() {
    PreviewFrame {
        MonarchCopyButton(value = "https://example.com/hooks/1042", target = "webhook URL")
        MonarchCopyButton(
            value = "https://example.com/hooks/1042",
            variant = MonarchButtonVariant.Primary,
            leadingIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchCopyButton(value = "sk_live_1234", enabled = false)
        MonarchCopyButton(
            value = "sk_live_1234",
            icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
            sensitive = true,
            target = "API key",
        )
    }
}
