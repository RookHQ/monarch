package dev.monarchui.components.typography.text

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchTextPreview() {
    PreviewFrame {
        MonarchTextStyle.entries.forEach { role ->
            MonarchText(text = role.name, style = role)
        }
    }
}
