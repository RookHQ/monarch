package dev.monarchui.components.typography.inlinecodestyle

import androidx.compose.runtime.Composable
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
