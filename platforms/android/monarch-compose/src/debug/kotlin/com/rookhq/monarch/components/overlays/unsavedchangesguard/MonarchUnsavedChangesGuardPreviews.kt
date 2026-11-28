package com.rookhq.monarch.components.overlays.unsavedchangesguard

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchUnsavedChangesGuardPreview() {
    PreviewFrame {
        MonarchUnsavedChangesGuard(hasChanges = true, open = true, onOpenChange = {}, onLeave = {}, onSave = {})
    }
}
