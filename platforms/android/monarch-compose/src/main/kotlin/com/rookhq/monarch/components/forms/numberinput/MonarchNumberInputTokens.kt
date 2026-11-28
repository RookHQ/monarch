package com.rookhq.monarch.components.forms.numberinput

import androidx.compose.ui.graphics.Color
import com.rookhq.monarch.theme.MonarchColors

/** Component tokens of MonarchNumberInput, built from roles only. */
internal object MonarchNumberInputTokens {
    /** The fewest fraction digits shown and kept, unless the step has more. */
    const val MIN_FRACTION_DIGITS: Int = 3

    /** The colour of the − and + signs: muted like every icon in a field, disabled at the bounds. */
    fun stepper(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled
}
