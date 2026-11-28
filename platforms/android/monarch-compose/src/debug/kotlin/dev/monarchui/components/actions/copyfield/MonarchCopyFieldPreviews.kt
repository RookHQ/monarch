package dev.monarchui.components.actions.copyfield

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchCopyFieldPreview() {
    PreviewFrame {
        MonarchCopyField(value = "https://example.com/hooks/1042", label = "Webhook URL")
        MonarchCopyField(
            value = "sk_live_example_1234",
            label = "API key",
            sensitive = true,
            copyIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchCopyField(value = "v=spf1 include:_spf.example.com ~all", label = "SPF record", multiline = true)
        MonarchCopyField(value = "https://example.com/feed.ics", label = "Calendar feed", enabled = false)
    }
}
