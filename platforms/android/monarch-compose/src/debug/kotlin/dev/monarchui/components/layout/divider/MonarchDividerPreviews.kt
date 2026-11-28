package dev.monarchui.components.layout.divider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchDividerPreview() {
    PreviewFrame {
        MonarchText(text = "Above")
        MonarchDivider()
        MonarchText(text = "Below")
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
        ) {
            MonarchText(text = "Start")
            MonarchDivider(orientation = MonarchDividerOrientation.Vertical)
            MonarchText(text = "End")
        }
    }
}
