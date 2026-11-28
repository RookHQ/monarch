package dev.monarchui.components.workflow.onboardingchecklist

import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
