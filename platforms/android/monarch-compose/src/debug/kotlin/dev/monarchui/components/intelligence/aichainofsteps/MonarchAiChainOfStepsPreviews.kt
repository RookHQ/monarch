package dev.monarchui.components.intelligence.aichainofsteps

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStep
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStepState
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchAiChainOfStepsPreview() {
    PreviewFrame {
        MonarchAiChainOfSteps(
            steps = listOf(
                MonarchAiPlanStep("read", "Read the contract", MonarchAiPlanStepState.Done),
                MonarchAiPlanStep("find", "Find the renewal date", MonarchAiPlanStepState.Failed),
                MonarchAiPlanStep("skip", "Compare with last year", MonarchAiPlanStepState.Skipped),
                MonarchAiPlanStep("draft", "Draft the reply", MonarchAiPlanStepState.Todo),
            ),
            details = mapOf("find" to "The date is not in the file."),
        )
        MonarchAiChainOfSteps(
            steps = listOf(
                MonarchAiPlanStep("read", "Read the contract", MonarchAiPlanStepState.Done),
                MonarchAiPlanStep("draft", "Draft the reply", MonarchAiPlanStepState.Done),
            ),
        )
    }
}
