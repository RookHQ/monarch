package com.rookhq.monarch.components.workflow.onboardingchecklist

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchOnboardingChecklist, built from primitives and roles only. */
internal object MonarchOnboardingChecklistTokens {
    val HeadingGap: Dp = MonarchTokens.Space.space1.dp
    val SectionGap: Dp = MonarchTokens.Space.space3.dp
    val StepGap: Dp = MonarchTokens.Space.space3.dp
    val StepPaddingHorizontal: Dp = MonarchTokens.Space.space2.dp
    val StepPaddingVertical: Dp = MonarchTokens.Space.space2_5.dp
    val StepActionGap: Dp = MonarchTokens.Space.space2.dp
    val CompletePaddingHorizontal: Dp = MonarchTokens.Space.space3.dp
    val CompletePaddingVertical: Dp = MonarchTokens.Space.space2.dp
    val CompleteGap: Dp = MonarchTokens.Space.space2.dp

    /** The smallest side of a step marker; it grows with the font size. */
    val MarkerSize: Dp = MonarchTokens.Space.space6.dp
    val MarkerBorder: Dp = MonarchTokens.Space.space0_5.dp

    /** The fraction of done steps; zero for an empty list. */
    fun fraction(done: Int, total: Int): Float = if (total <= 0) 0f else done.toFloat() / total

    /** Whether every step is done. An empty list is never complete. */
    fun complete(steps: List<MonarchOnboardingStep>): Boolean = steps.isNotEmpty() && steps.all { it.done }

    /** The index of the current step: the first that is not done, or -1. */
    fun current(steps: List<MonarchOnboardingStep>): Int = steps.indexOfFirst { !it.done }

    /** The fill, content and ring of a step marker: success when done, accent ring when current. */
    fun marker(done: Boolean, current: Boolean, colors: MonarchColors): MonarchSurfaceColors = when {
        done -> MonarchSurfaceColors(container = colors.successSolid, content = colors.successOnSolid, border = null)
        current -> MonarchSurfaceColors(
            container = Color.Transparent,
            content = colors.accentStrong,
            border = colors.accentStrong,
        )
        else -> MonarchSurfaceColors(
            container = Color.Transparent,
            content = colors.textMuted,
            border = colors.borderStrong,
        )
    }

    /** The fill of a step row: the current step stands out on the subtle fill. */
    fun stepBackground(current: Boolean, colors: MonarchColors): Color =
        if (current) colors.bgSubtle else Color.Transparent

    /** The colour of a step title: muted once done. */
    fun stepTitle(done: Boolean, colors: MonarchColors): Color = if (done) colors.textMuted else colors.textDefault

    /** The fill and text of the complete message. */
    fun completeColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.successSubtle, content = colors.successStrong, border = null)
}
