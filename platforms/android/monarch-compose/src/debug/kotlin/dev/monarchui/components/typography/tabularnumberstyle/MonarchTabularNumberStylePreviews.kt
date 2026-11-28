package dev.monarchui.components.typography.tabularnumberstyle

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchTabularNumberPreview() {
    PreviewFrame {
        Column(horizontalAlignment = Alignment.End) {
            MonarchTabularNumber(value = 1111.11)
            MonarchTabularNumber(value = null)
            MonarchTabularNumber(value = 9048.5)
        }
        MonarchTabularNumber(text = "12:04", style = MonarchTextStyle.Title)
    }
}
