package com.rookhq.monarch.components.typography.inlinecodestyle

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchInlineCodeStylePreview() {
    PreviewFrame {
        MonarchText(text = MonarchInlineCodeStyle.markdown("Run `./gradlew build` before you push."))
        MonarchText(
            text = MonarchInlineCodeStyle.markdown("Exit code `127`", MonarchInlineCodeVariant.Plain),
            style = MonarchTextStyle.Caption,
        )
    }
}
