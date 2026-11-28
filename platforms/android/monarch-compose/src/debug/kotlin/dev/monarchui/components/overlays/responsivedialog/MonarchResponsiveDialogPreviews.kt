package dev.monarchui.components.overlays.responsivedialog

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

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
