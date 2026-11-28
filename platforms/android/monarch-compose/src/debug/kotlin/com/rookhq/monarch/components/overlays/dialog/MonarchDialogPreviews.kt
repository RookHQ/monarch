package com.rookhq.monarch.components.overlays.dialog

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchDialogPreview() {
    PreviewFrame {
        MonarchDialog(
            onDismissRequest = {},
            title = "Delete project?",
            description = "Atlas and its 214 tasks are deleted for everyone in the workspace.",
            actions = {
                MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
                MonarchButton(text = "Delete project", onClick = {}, variant = MonarchButtonVariant.Danger)
            },
        )
    }
}
