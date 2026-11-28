package dev.monarchui.components.feedback.celebration

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchCelebrationPreview() {
    PreviewFrame {
        MonarchCelebration(
            title = "Workspace ready",
            message = "Invite your team to start working together.",
            announces = false,
            icon = { MonarchIcon(PreviewIcon, contentDescription = null) },
        ) {
            MonarchButton(text = "Invite people", onClick = {}, variant = MonarchButtonVariant.Primary)
        }
        MonarchCelebration(
            title = "Domain verified",
            tone = MonarchTone.Info,
            decoration = MonarchCelebrationDecoration.None,
            announces = false,
            icon = { MonarchIcon(PreviewIcon, contentDescription = null) },
        )
    }
}
