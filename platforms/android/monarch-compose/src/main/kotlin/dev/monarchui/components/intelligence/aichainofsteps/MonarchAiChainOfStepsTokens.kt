package dev.monarchui.components.intelligence.aichainofsteps

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPlanStepState
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The texts of the announcements, resolved by the caller so the logic stays pure. */
internal class MonarchAiChainOfStepsAnnouncementTexts(
    val started: (label: String, position: Int, total: Int) -> String,
    val failedStep: (label: String) -> String,
    val finished: (count: Int, failed: Int) -> String,
)

/** Component tokens and the pure logic of MonarchAiChainOfSteps. */
internal object MonarchAiChainOfStepsTokens {
    /** The gap between steps. */
    val StepGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the mark and the text of a step. */
    val MarkGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the label, the state and the detail of a step. */
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The size of the mark column. */
    val MarkSize: Dp = MonarchTokens.Space.space4.dp

    /** The size of the drawn mark inside the column. */
    val DotSize: Dp = MonarchTokens.Space.space3.dp

    /**
     * The centre dot of the still running mark under reduced motion: a ring with a dot, as the web's active
     * glyph (radius 2.75 in a 6.25 ring).
     */
    val RunningDotSize: Dp = MonarchTokens.Space.space1_5.dp

    /** The stroke of the running indicator and the outline of the open marks. */
    val StrokeWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** How the running step is marked: a spinner, or under reduced motion the web's still ring with a centre dot. */
    fun runningMark(reducedMotion: Boolean): AiChainRunningMark =
        if (reducedMotion) AiChainRunningMark.RingWithDot else AiChainRunningMark.Spinner

    /** Whether the drawn mark is filled; open marks are todo and skipped, so done is not colour alone. */
    fun filledMark(state: MonarchAiPlanStepState): Boolean =
        state != MonarchAiPlanStepState.Todo && state != MonarchAiPlanStepState.Skipped

    /** The colour of the mark of a state. */
    fun markColor(state: MonarchAiPlanStepState, colors: MonarchColors): Color = when (state) {
        MonarchAiPlanStepState.Todo, MonarchAiPlanStepState.Skipped -> colors.textSubtle
        MonarchAiPlanStepState.Active -> colors.aiStrong
        MonarchAiPlanStepState.Done -> colors.successStrong
        MonarchAiPlanStepState.Failed -> colors.dangerStrong
    }

    /** The colour of the label of a state. */
    fun labelColor(state: MonarchAiPlanStepState, colors: MonarchColors): Color = when (state) {
        MonarchAiPlanStepState.Todo, MonarchAiPlanStepState.Skipped -> colors.textMuted
        MonarchAiPlanStepState.Active, MonarchAiPlanStepState.Done, MonarchAiPlanStepState.Failed -> colors.textDefault
    }

    /** Whether the state is written under the label: only failed and skipped, which a mark alone would not say. */
    fun showsStateText(state: MonarchAiPlanStepState): Boolean =
        state == MonarchAiPlanStepState.Failed || state == MonarchAiPlanStepState.Skipped

    /** The colour of the written state. */
    fun stateColor(state: MonarchAiPlanStepState, colors: MonarchColors): Color =
        if (state == MonarchAiPlanStepState.Failed) colors.dangerStrong else colors.textMuted

    /** The built-in name of a state. */
    fun stateName(state: MonarchAiPlanStepState): Int = when (state) {
        MonarchAiPlanStepState.Todo -> R.string.monarch_aichainofsteps_state_todo
        MonarchAiPlanStepState.Active -> R.string.monarch_aichainofsteps_state_active
        MonarchAiPlanStepState.Done -> R.string.monarch_aichainofsteps_state_done
        MonarchAiPlanStepState.Skipped -> R.string.monarch_aichainofsteps_state_skipped
        MonarchAiPlanStepState.Failed -> R.string.monarch_aichainofsteps_state_failed
    }
}

/** The mark of the running step. */
internal enum class AiChainRunningMark {
    /** An indeterminate Material 3 progress ring. */
    Spinner,

    /** A still ring with a centre dot, the web's active glyph, under reduced motion. */
    RingWithDot,
}
