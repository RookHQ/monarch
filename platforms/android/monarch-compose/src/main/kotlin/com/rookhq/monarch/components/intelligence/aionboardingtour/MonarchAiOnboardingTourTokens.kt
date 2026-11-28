package com.rookhq.monarch.components.intelligence.aionboardingtour

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiOnboardingTour. */
internal object MonarchAiOnboardingTourTokens {
    val Padding: Dp = MonarchTokens.Space.space4.dp
    val SectionGap: Dp = MonarchTokens.Space.space4.dp
    val TextGap: Dp = MonarchTokens.Space.space2.dp
    val EyebrowGap: Dp = MonarchTokens.Space.space1.dp
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** The height of a pip, and the width of one that is not the current step. */
    val PipSize: Dp = MonarchTokens.Space.space1_5.dp

    /** The space between pips. */
    val PipGap: Dp = MonarchTokens.Space.space1.dp

    /** The width of a pip: the current step's is longer. */
    fun pipWidth(active: Boolean): Dp = if (active) MonarchTokens.Space.space4.dp else PipSize

    /** The colour of a pip: the AI colour for the current step, a muted fill for the others. */
    fun pipColor(active: Boolean, colors: MonarchColors): Color = if (active) colors.aiSolid else colors.bgEmphasis

    /** The step shown: [step] kept inside the steps. */
    fun clamp(step: Int, count: Int): Int = step.coerceIn(0, maxOf(0, count - 1))

    /** Whether Back is offered: on every step but the first. */
    fun hasBack(step: Int): Boolean = step > 0

    /** Whether the step is the last, where the primary button finishes the tour. No steps counts as last. */
    fun isLast(step: Int, count: Int): Boolean = step >= count - 1

    /** Whether the stepper is shown: only for two or more steps. */
    fun showsStepper(count: Int): Boolean = count > 1

    /** The colour of the "Step 1 of 3" text. */
    fun stepColor(colors: MonarchColors): Color = colors.textMuted
}
