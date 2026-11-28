package dev.monarchui.components.files.columnsblock

import androidx.compose.runtime.Composable
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchColumnsBlockPreview() {
    PreviewFrame {
        MonarchColumnsBlock(ratio = 0.35f) {
            column { MonarchText(text = "Before the launch", style = MonarchTextStyle.BodyStrong) }
            column { MonarchText(text = "Sign-ups doubled after the launch, mostly from the new referral page.") }
        }
        MonarchColumnsBlock {
            column { MonarchText(text = "Plan") }
            column { MonarchText(text = "Build") }
            column { MonarchText(text = "Ship") }
        }
    }
}
