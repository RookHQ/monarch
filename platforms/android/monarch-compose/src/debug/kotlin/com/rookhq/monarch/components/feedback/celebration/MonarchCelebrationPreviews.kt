package com.rookhq.monarch.components.feedback.celebration

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

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
