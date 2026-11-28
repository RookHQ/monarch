package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aichainofsteps.MonarchAiChainOfSteps
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStep
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStepState
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val StepLabels =
    listOf("Read the contract", "Find the renewal date", "Compare with last year", "Draft the reply")

/** The states of the steps after [progress] presses of "Next step". */
private fun stepsAt(progress: Int): List<MonarchAiPlanStep> = StepLabels.mapIndexed { index, label ->
    val state = when {
        index < progress -> MonarchAiPlanStepState.Done
        index == progress -> MonarchAiPlanStepState.Active
        else -> MonarchAiPlanStepState.Todo
    }
    MonarchAiPlanStep("step$index", label, state)
}

/** The catalogue page of AiChainOfSteps. */
object MonarchAiChainOfStepsSection : CataloguePage {
    override val title = "AiChainOfSteps"

    @Composable
    override fun Content() {
        var progress by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Live steps. They fold when every step has finished.")
        MonarchAiChainOfSteps(steps = stepsAt(progress))
        MonarchButton(
            text = if (progress >= StepLabels.size) "Start again" else "Next step",
            onClick = { progress = if (progress >= StepLabels.size) 0 else progress + 1 },
            size = MonarchButtonSize.Sm,
        )
        CatalogueCaption("A failed and a skipped step with a detail")
        MonarchAiChainOfSteps(
            steps = listOf(
                MonarchAiPlanStep("read", "Read the contract", MonarchAiPlanStepState.Done),
                MonarchAiPlanStep("find", "Find the renewal date", MonarchAiPlanStepState.Failed),
                MonarchAiPlanStep("compare", "Compare with last year", MonarchAiPlanStepState.Skipped),
            ),
            details = mapOf("find" to "The date is not in the file."),
            collapsible = false,
        )
    }
}
