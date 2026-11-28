package dev.monarchui.components.intelligence.aiconversation

/**
 * The steps the assistant intends to take.
 *
 * @property steps the steps, in order.
 */
public data class MonarchAiPlanPart(public val steps: List<MonarchAiPlanStep>) : MonarchAiPart {
    override val type: String get() = "plan"
}
