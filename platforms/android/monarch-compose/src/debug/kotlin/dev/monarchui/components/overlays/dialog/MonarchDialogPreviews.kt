package dev.monarchui.components.overlays.dialog

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
