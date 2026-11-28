package dev.monarchui.components.forms.formactionsbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchFormActionsBar, built from primitives and roles only. */
internal object MonarchFormActionsBarTokens {
    val HorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space3.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /**
     * The colours of the bar.
     *
     * @property container the fill of the bar.
     * @property divider the line along its top edge.
     * @property status the text of the unsaved changes message.
     * @property error the text of an error.
     */
    data class Colors(val container: Color, val divider: Color, val status: Color, val error: Color)

    /** The colours of the bar, from the surface, border, muted text and danger roles. */
    fun colors(colors: MonarchColors): Colors = Colors(
        container = colors.bgSurface,
        divider = colors.borderSubtle,
        status = colors.textMuted,
        error = colors.dangerStrong,
    )

    /**
     * What the bar allows in a state.
     *
     * @property saveEnabled whether Save responds (while pending it shows progress instead).
     * @property discardEnabled whether Discard responds.
     * @property hidden whether the bar is not shown at all.
     */
    data class Availability(val saveEnabled: Boolean, val discardEnabled: Boolean, val hidden: Boolean)

    /**
     * Save and Discard respond only with unsaved changes; Discard not while a
     * save runs. With `hideWhenClean` the bar is not shown while there are no
     * changes, no error and nothing pending.
     */
    fun availability(dirty: Boolean, pending: Boolean, hasError: Boolean, hideWhenClean: Boolean): Availability =
        Availability(
            saveEnabled = dirty || pending,
            discardEnabled = dirty && !pending,
            hidden = hideWhenClean && !dirty && !pending && !hasError,
        )
}
