package com.rookhq.monarch.components.layout.divider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

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
