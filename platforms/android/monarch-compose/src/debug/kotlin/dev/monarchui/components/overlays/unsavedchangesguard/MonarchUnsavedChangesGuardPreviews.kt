package dev.monarchui.components.overlays.unsavedchangesguard

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchUnsavedChangesGuardPreview() {
    PreviewFrame {
        MonarchUnsavedChangesGuard(hasChanges = true, open = true, onOpenChange = {}, onLeave = {}, onSave = {})
    }
}
