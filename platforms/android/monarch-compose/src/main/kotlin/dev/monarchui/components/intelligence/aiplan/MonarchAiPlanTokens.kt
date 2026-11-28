package dev.monarchui.components.intelligence.aiplan

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStep
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiPlan. */
internal object MonarchAiPlanTokens {
    private const val VISIBLE_STEP_ROWS = 4

    /** The least width of a step's field before the controls wrap under it. */
    val FieldMinWidth: Dp = (MonarchTokens.Space.space20 * 2).dp

    /** The visible height of a step's field; its touch target is 48dp. */
    val FieldMinHeight: Dp = MonarchTokens.Space.space8.dp

    /** The border of a step's field. */
    val FieldBorder: Dp = MonarchTokens.Space.px.dp

    /** The padding inside a step's field. */
    val FieldPaddingHorizontal: Dp = MonarchTokens.Space.space2_5.dp
    val FieldPaddingVertical: Dp = MonarchTokens.Space.space1_5.dp

    /** The visible side of a Move up or Move down button; its touch target is 48dp. */
    val MoveSize: Dp = MonarchTokens.Space.space8.dp

    /** A weight small enough that only a wrapped line gives it room: it pushes the controls to the end. */
    const val PUSH_WEIGHT: Float = 0.001f

    /** The gap between the header, the steps and the footer. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the title and the description. */
    val HeaderGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the parts of a step row and between the footer buttons. */
    val RowGap: Dp = MonarchTokens.Space.space2.dp

    /** The tallest the list of steps grows before it scrolls, so it can sit inside a scrolling conversation. */
    val StepsMaxHeight: Dp = (MonarchTokens.Space.space24 * VISIBLE_STEP_ROWS).dp

    /** The colour of the title. */
    fun title(colors: MonarchColors): Color = colors.textDefault

    /** The colour of the description, the step numbers and the empty-plan note. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** Whether the plan is read-only: once decided, or when it may not be edited. */
    fun readOnly(decision: MonarchAiPlanDecision, editable: Boolean): Boolean =
        decision != MonarchAiPlanDecision.Pending || !editable

    /** Whether Approve and Reject show: while pending, when the app listens or leaves the decision to the view. */
    fun showsDecision(decision: MonarchAiPlanDecision, controlled: Boolean, handled: Boolean): Boolean =
        decision == MonarchAiPlanDecision.Pending && (handled || !controlled)

    /** The steps that can be approved: labels trimmed, empty steps left out. */
    fun cleaned(steps: List<MonarchAiPlanStep>): List<MonarchAiPlanStep> =
        steps.mapNotNull { step -> step.label.trim().takeIf { it.isNotEmpty() }?.let { step.copy(label = it) } }

    /** The steps the read-only list shows: the draft while pending, the cleaned steps once decided. */
    fun shownSteps(draft: List<MonarchAiPlanStep>, decision: MonarchAiPlanDecision): List<MonarchAiPlanStep> =
        if (decision == MonarchAiPlanDecision.Pending) draft else cleaned(draft)

    /** The name of a step in spoken names: its trimmed label, or [untitled] when it is empty. */
    fun name(step: MonarchAiPlanStep, untitled: String): String = step.label.trim().ifEmpty { untitled }

    /** The steps with the step at [from] moved to [to]. */
    fun moved(steps: List<MonarchAiPlanStep>, from: Int, to: Int): List<MonarchAiPlanStep> {
        if (from !in steps.indices || to !in steps.indices || from == to) return steps
        return steps.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** The steps with the label of the step [id] replaced. */
    fun relabelled(steps: List<MonarchAiPlanStep>, id: String, label: String): List<MonarchAiPlanStep> =
        steps.map { if (it.id == id) it.copy(label = label) else it }

    /** A step id for a new step that no step uses yet: "new-1", "new-2"… */
    fun newId(counter: Int, existing: List<MonarchAiPlanStep>): String {
        val ids = existing.mapTo(HashSet()) { it.id }
        var number = counter
        while ("new-$number" in ids) number++
        return "new-$number"
    }
}
