package com.rookhq.monarch.components.files.columnsblock

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
