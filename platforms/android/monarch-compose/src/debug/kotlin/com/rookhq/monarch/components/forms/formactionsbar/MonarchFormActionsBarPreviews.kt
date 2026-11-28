package com.rookhq.monarch.components.forms.formactionsbar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchFormActionsBarPreview() {
    PreviewFrame {
        MonarchFormActionsBar(dirty = true, onSave = {}, onDiscard = {}, windowInsets = WindowInsets(0))
        MonarchFormActionsBar(dirty = true, onSave = {}, pending = true, windowInsets = WindowInsets(0))
        MonarchFormActionsBar(
            dirty = true,
            onSave = {},
            onDiscard = {},
            error = "Your changes could not be saved. Try again.",
            windowInsets = WindowInsets(0),
        )
    }
}
