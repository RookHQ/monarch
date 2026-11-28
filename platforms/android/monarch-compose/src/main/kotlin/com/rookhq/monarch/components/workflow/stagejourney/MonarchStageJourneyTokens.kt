package com.rookhq.monarch.components.workflow.stagejourney

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Where a stage is relative to the current one. */
internal enum class StageJourneyStatus { Done, Current, Upcoming }

/** A run of consecutive stages with the same group, with each stage's index in the journey. */
internal data class StageJourneyGroup(val name: String?, val stages: List<IndexedValue<MonarchStageJourneyStage>>)

/** The moves split as the card shows them. */
internal data class StageJourneyMoves(
    val forward: List<MonarchStageJourneyTransition>,
    val exits: List<MonarchStageJourneyTransition>,
    val back: MonarchStageJourneyTransition?,
)

/**
 * The colours of a stage in the strip.
 *
 * @property container the fill.
 * @property content the text and the check mark.
 * @property border the outline.
 */
@Immutable
internal data class StageJourneyStageColors(val container: Color, val content: Color, val border: Color)

/** Component tokens and logic of MonarchStageJourney, built from primitives and roles only. */
internal object MonarchStageJourneyTokens {
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val SmallGap: Dp = MonarchTokens.Space.space1.dp
    val StageGap: Dp = MonarchTokens.Space.space1_5.dp
    val StageHorizontalPadding: Dp = MonarchTokens.Space.space3.dp
    val StageVerticalPadding: Dp = MonarchTokens.Space.space1_5.dp
    val CardPadding: Dp = MonarchTokens.Space.space4.dp
    val CheckSize: Dp = MonarchTokens.Space.space3.dp
    val DotSize: Dp = MonarchTokens.Space.space2_5.dp
    val LineWidth: Dp = MonarchTokens.Space.px.dp
    val HistoryIndent: Dp = MonarchTokens.Space.space6.dp
    val DotTop: Dp = MonarchTokens.Space.space1_5.dp

    /** The check mark on the web's 16-unit grid, `M3.5 8.5l3 3 6-7`. */
    val CheckPoints: List<Offset> = listOf(Offset(3.5f, 8.5f), Offset(6.5f, 11.5f), Offset(12.5f, 4.5f))
    const val CHECK_GRID: Float = 16f
    const val CHECK_STROKE: Float = 2f

    /** Consecutive stages with the same group, in order; a group name repeated later starts a new run. */
    fun groups(stages: List<MonarchStageJourneyStage>): List<StageJourneyGroup> {
        val runs = mutableListOf<StageJourneyGroup>()
        stages.withIndex().forEach { stage ->
            val last = runs.lastOrNull()
            if (last != null && last.name == stage.value.group) {
                runs[runs.lastIndex] = last.copy(stages = last.stages + stage)
            } else {
                runs += StageJourneyGroup(stage.value.group, listOf(stage))
            }
        }
        return runs
    }

    /** The status of the stage at [index]; with an unknown current stage ([current] < 0) every stage is upcoming. */
    fun status(index: Int, current: Int): StageJourneyStatus = when {
        current < 0 || index > current -> StageJourneyStatus.Upcoming
        index == current -> StageJourneyStatus.Current
        else -> StageJourneyStatus.Done
    }

    /** Forward moves, exits and the first back move. */
    fun moves(transitions: List<MonarchStageJourneyTransition>): StageJourneyMoves = StageJourneyMoves(
        forward = transitions.filter { it.kind == MonarchStageJourneyTransitionKind.Forward },
        exits = transitions.filter { it.kind == MonarchStageJourneyTransitionKind.Exit },
        back = transitions.firstOrNull { it.kind == MonarchStageJourneyTransitionKind.Back },
    )

    /**
     * A stage's colours: done stages on the accent subtle fill with the
     * accent border, the current stage solid accent (solid success when the
     * journey succeeded, the emphasis fill with a strong border when it
     * closed), upcoming stages outlined with muted text.
     */
    fun stageColors(
        colors: MonarchColors,
        status: StageJourneyStatus,
        terminal: MonarchStageJourneyTerminal?,
    ): StageJourneyStageColors = when (status) {
        StageJourneyStatus.Done ->
            StageJourneyStageColors(colors.accentSubtle, colors.accentStrong, colors.accentBorder)
        StageJourneyStatus.Upcoming ->
            StageJourneyStageColors(Color.Transparent, colors.textMuted, colors.borderDefault)
        StageJourneyStatus.Current -> when (terminal) {
            MonarchStageJourneyTerminal.Success ->
                StageJourneyStageColors(colors.successSolid, colors.successOnSolid, colors.successSolid)
            MonarchStageJourneyTerminal.Closed ->
                StageJourneyStageColors(colors.bgEmphasis, colors.textDefault, colors.borderStrong)
            null -> StageJourneyStageColors(colors.accentSolid, colors.accentOnSolid, colors.accentSolid)
        }
    }

    /** The weight of a stage's name: semibold for the current stage, medium for the others. */
    fun weight(status: StageJourneyStatus): Int =
        if (status == StageJourneyStatus.Current) MonarchTokens.FontWeight.semibold else MonarchTokens.FontWeight.medium

    /** The dot and the line of a history entry: the accent dot, the warning dot for a move back. */
    fun historyColors(colors: MonarchColors, backward: Boolean): Pair<Color, Color> =
        (if (backward) colors.warningSolid else colors.accentSolid) to colors.borderDefault

    /** The arrow between two stages, pointing in the reading direction. */
    fun arrow(direction: LayoutDirection): String = if (direction == LayoutDirection.Rtl) "←" else "→"

    /** The card's fill and border: success-tinted, subtle when closed, else the surface. */
    fun cardColors(colors: MonarchColors, terminal: MonarchStageJourneyTerminal?): Pair<Color, Color> =
        when (terminal) {
        MonarchStageJourneyTerminal.Success -> colors.successSubtle to colors.successBorder
        MonarchStageJourneyTerminal.Closed -> colors.bgSubtle to colors.borderDefault
        null -> colors.bgSurface to colors.borderSubtle
    }
}
