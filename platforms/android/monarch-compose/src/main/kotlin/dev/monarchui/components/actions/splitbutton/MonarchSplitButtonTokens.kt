package dev.monarchui.components.actions.splitbutton

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and logic of MonarchSplitButton. Colours are the ones of MonarchButton. */
internal object MonarchSplitButtonTokens {
    /** The gap that joins the main button and the arrow, as in the Material 3 split button. */
    val Gap: Dp = MonarchTokens.Space.space0_5.dp

    /** Whether the arrow opens the menu: not while the main action is off or busy, or the menu is off. */
    fun arrowEnabled(enabled: Boolean, loading: Boolean, menuEnabled: Boolean): Boolean =
        enabled && !loading && menuEnabled
}
