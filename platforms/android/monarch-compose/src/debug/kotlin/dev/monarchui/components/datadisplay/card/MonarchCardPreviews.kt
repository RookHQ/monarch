package dev.monarchui.components.datadisplay.card

import androidx.compose.runtime.Composable
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
