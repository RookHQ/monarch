package com.rookhq.monarch.internal.choice

import androidx.compose.animation.core.Spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** Measures shared by the checkbox, radio and switch rows, from the web's `choice.css`. */
internal object MonarchChoiceTokens {
    /** The gap between the control and its text: 10dp. */
    val Gap: Dp = MonarchTokens.Space.space2_5.dp

    /** The gap between the label and its description: 2dp. */
    val DescriptionGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The least vertical padding of a row, so a two-line label does not touch the row's edge. */
    val MinVerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The focus ring sits one gap and half a stroke outside the control, like `monarchFocusRing`. */
    const val RING_OUTSET_IN_STROKES: Float = 1.5f

    /** A toggle (thumb, dot, check) settles with a small overshoot, about 150ms. */
    const val TOGGLE_DAMPING: Float = 0.72f

    /** An indicator that moves between places (the segment pill) never overshoots: about 200ms. */
    const val INDICATOR_DAMPING: Float = Spring.DampingRatioNoBouncy
    const val INDICATOR_STIFFNESS: Float = 700f
}
