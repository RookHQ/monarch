package com.rookhq.monarch.components.workflow.onboardingchecklist

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchOnboardingChecklistPreview() {
    PreviewFrame {
        MonarchOnboardingChecklist(
            title = "Get started",
            steps = listOf(
                MonarchOnboardingStep(id = "profile", title = "Complete your profile", done = true),
                MonarchOnboardingStep(id = "invite", title = "Invite your team", description = "Work together."),
                MonarchOnboardingStep(id = "calendar", title = "Connect a calendar"),
            ),
            description = "A few steps to set up your workspace.",
            onDismiss = {},
            doneIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null, size = MonarchIconSize.Sm) },
        ) { step ->
            MonarchButton(
                text = if (step.id == "invite") "Invite" else "Connect",
                onClick = {},
                size = MonarchButtonSize.Sm,
            )
        }
        MonarchOnboardingChecklist(
            title = "Set up billing",
            steps = listOf(MonarchOnboardingStep(id = "card", title = "Add a card", done = true)),
            collapsible = false,
        )
    }
}
