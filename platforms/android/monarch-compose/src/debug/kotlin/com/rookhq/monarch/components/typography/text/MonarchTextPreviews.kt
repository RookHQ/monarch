package com.rookhq.monarch.components.typography.text

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchTextPreview() {
    PreviewFrame {
        MonarchTextStyle.entries.forEach { role ->
            MonarchText(text = role.name, style = role)
        }
    }
}
