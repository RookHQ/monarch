package com.rookhq.monarch.components.billing.money

import androidx.compose.ui.graphics.Color
import com.rookhq.monarch.theme.MonarchColors

/** Component tokens of MonarchMoney: the colour of the amount. */
internal object MonarchMoneyTokens {
    /**
     * The text colour: the strong danger role for a toned negative amount, the
     * muted role for a missing one, otherwise [color] (unspecified keeps the
     * surrounding content colour).
     */
    fun textColor(negativeToned: Boolean, empty: Boolean, color: Color, colors: MonarchColors): Color = when {
        negativeToned -> colors.dangerStrong
        empty -> colors.textMuted
        else -> color
    }
}
