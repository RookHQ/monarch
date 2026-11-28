package com.rookhq.monarch.components.workflow.stepper

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private val previewSteps = listOf(
    MonarchStepperStep(id = "plan", label = "Plan"),
    MonarchStepperStep(id = "team", label = "Team", description = "Invite people", optional = true),
    MonarchStepperStep(id = "billing", label = "Billing", error = true),
    MonarchStepperStep(id = "review", label = "Review"),
)

@MonarchPreviews
@Composable
internal fun MonarchStepperPreview() {
    PreviewFrame {
        MonarchStepper(steps = previewSteps.take(3), current = 1, compact = MonarchStepperCompactMode.Never)
        MonarchStepper(steps = previewSteps, current = 1, compact = MonarchStepperCompactMode.Always)
    }
}

@MonarchPreviews
@Composable
internal fun MonarchStepperVerticalPreview() {
    PreviewFrame {
        MonarchStepper(
            steps = previewSteps,
            current = 3,
            orientation = MonarchStepperOrientation.Vertical,
            onSelect = { _, _ -> },
        )
    }
}
