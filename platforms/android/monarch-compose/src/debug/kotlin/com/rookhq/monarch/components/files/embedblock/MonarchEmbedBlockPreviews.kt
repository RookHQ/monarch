package com.rookhq.monarch.components.files.embedblock

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchEmbedBlockPreview() {
    PreviewFrame {
        MonarchEmbedBlock(url = "https://youtu.be/dQw4w9WgXcQ", caption = "Product tour")
        MonarchEmbedBlock(url = "https://example.com/changelog")
        MonarchEmbedBlock(url = "javascript:alert(1)")
    }
}
