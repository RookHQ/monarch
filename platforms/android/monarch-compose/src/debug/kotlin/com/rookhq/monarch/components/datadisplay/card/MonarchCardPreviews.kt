package com.rookhq.monarch.components.datadisplay.card

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchCardPreview() {
    PreviewFrame {
        MonarchCard {
            MonarchText(text = "Quarterly report", style = MonarchTextStyle.Subheading)
            MonarchText(text = "Updated two hours ago", style = MonarchTextStyle.Caption)
        }
        MonarchCard(elevated = true) {
            MonarchText(text = "Elevated", style = MonarchTextStyle.Subheading)
        }
        MonarchCard(onClick = {}) {
            MonarchText(text = "Tap to open", style = MonarchTextStyle.Subheading)
        }
    }
}
