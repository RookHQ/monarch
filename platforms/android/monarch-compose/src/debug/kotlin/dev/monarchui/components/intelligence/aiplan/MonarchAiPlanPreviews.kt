@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aiplan

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStep
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStepState
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private val PreviewPlan = MonarchAiPlanPart(
    listOf(
        MonarchAiPlanStep(id = "1", label = "Find the three open invoices"),
        MonarchAiPlanStep(id = "2", label = "Draft a reminder for each customer"),
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchAiPlanPreview() {
    PreviewFrame {
        MonarchAiPlan(part = PreviewPlan, onApprove = {}, onReject = {})
        MonarchAiPlan(
            part = MonarchAiPlanPart(PreviewPlan.steps.mapIndexed { index, step ->
                step.copy(state = if (index == 0) MonarchAiPlanStepState.Done else MonarchAiPlanStepState.Active)
            }),
            decision = MonarchAiPlanDecision.Approved,
        )
    }
}
