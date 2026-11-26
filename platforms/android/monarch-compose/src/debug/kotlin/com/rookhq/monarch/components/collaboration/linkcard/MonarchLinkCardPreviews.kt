package com.rookhq.monarch.components.collaboration.linkcard

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchLinkCardPreview() {
    PreviewFrame {
        MonarchLinkCard(
            title = "Release notes for March",
            url = "https://www.example.com/blog/march",
            description = "Faster search, shared views and a new API.",
            hasImage = true,
            onDismiss = {},
            dismissIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchLinkCard(
            title = "Product tour",
            url = "https://example.com/tour",
            hasImage = true,
            video = true,
            size = MonarchLinkCardSize.Sm,
        )
        MonarchLinkCard(title = "Internal page", url = "intranet://wiki/home", onDismiss = {})
    }
}
