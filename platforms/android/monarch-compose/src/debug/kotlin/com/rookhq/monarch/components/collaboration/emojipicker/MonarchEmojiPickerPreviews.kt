package com.rookhq.monarch.components.collaboration.emojipicker

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchEmojiPickerPreview() {
    PreviewFrame {
        MonarchEmojiPicker(onEmojiPicked = {})
        MonarchEmojiPicker(onEmojiPicked = {}, columns = 6, rows = 3.5f)
    }
}
