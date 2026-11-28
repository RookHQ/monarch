package dev.monarchui.components.actions.copybutton

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
