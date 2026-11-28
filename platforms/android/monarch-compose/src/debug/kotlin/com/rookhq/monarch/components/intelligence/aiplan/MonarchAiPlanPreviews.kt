@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence.aiplan

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStep
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPlanStepState
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
