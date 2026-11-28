package dev.monarchui.components.forms.autosaveindicator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val PreviewSavedAt = 0L

@MonarchPreviews
@Composable
internal fun MonarchAutosaveIndicatorPreview() {
    PreviewFrame {
        val scope = rememberCoroutineScope()
        val saved = remember { MonarchAutosaveState("Draft", scope, onSave = {}, initialSavedAt = PreviewSavedAt) }
        val unsaved = remember {
            MonarchAutosaveState("Draft", scope, onSave = {}).apply {
                enabled = false
                update("Draft, edited")
            }
        }
        MonarchAutosaveIndicator(state = saved)
        MonarchAutosaveIndicator(state = unsaved)
    }
}
