package dev.monarchui.components.collaboration.emojipicker

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchEmojiPickerPreview() {
    PreviewFrame {
        MonarchEmojiPicker(onEmojiPicked = {})
        MonarchEmojiPicker(onEmojiPicked = {}, columns = 6, rows = 3.5f)
    }
}
