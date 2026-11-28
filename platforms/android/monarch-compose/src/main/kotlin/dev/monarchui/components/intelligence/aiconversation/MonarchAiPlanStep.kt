package dev.monarchui.components.intelligence.aiconversation

/**
 * One step of a plan.
 *
 * @property id the id of the step.
 * @property label what the step does.
 * @property state where the step is.
 */
public data class MonarchAiPlanStep(
    public val id: String,
    public val label: String,
    public val state: MonarchAiPlanStepState = MonarchAiPlanStepState.Todo,
)
