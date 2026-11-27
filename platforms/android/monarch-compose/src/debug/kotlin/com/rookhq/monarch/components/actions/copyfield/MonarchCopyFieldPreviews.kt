package com.rookhq.monarch.components.actions.copyfield

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
