package com.rookhq.monarch.components.overlays.responsivedialog

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchResponsiveDialogPreview() {
    PreviewFrame {
        MonarchResponsiveDialog(
            onDismissRequest = {},
            title = "Rename project",
            description = "The new name is shown to everyone in the workspace.",
            actions = {
                MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
                MonarchButton(text = "Rename", onClick = {}, variant = MonarchButtonVariant.Primary)
            },
        ) {
            MonarchText(text = "Monarch for Android")
        }
    }
}
