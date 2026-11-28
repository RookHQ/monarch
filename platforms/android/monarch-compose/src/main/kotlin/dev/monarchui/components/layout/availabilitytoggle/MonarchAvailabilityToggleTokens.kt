package dev.monarchui.components.layout.availabilitytoggle

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchAvailabilityToggle, built from primitives and roles only. */
internal object MonarchAvailabilityToggleTokens {
    val MinHeight: Dp = MonarchTouchTarget.Minimum
    val VerticalPadding: Dp = MonarchTokens.Space.space2.dp
    val Gap: Dp = MonarchTokens.Space.space3.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp
    val MarkGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The diameter of the mark at the default font size. It grows with the font size setting. */
    val MarkSize: Dp = MonarchTokens.Space.space2.dp

    /** The width of the ring drawn while away. */
    val MarkStroke: Dp = MonarchTokens.Space.space0_5.dp

    /** The diameter of the progress ring shown while a change is saved. */
    val SpinnerSize: Dp = MonarchTokens.Space.space4.dp
    val SpinnerStroke: Dp = MonarchTokens.Space.space0_5.dp

    /** The share of the still ring drawn instead of the spinning one when animations are off. */
    const val STILL_SPINNER_FRACTION: Float = 0.75f

    /** The colour of the mark: the solid success colour while available, the solid warning colour while away. */
    fun markColor(available: Boolean, colors: MonarchColors): Color =
        if (available) colors.successSolid else colors.warningSolid

    /** Whether the mark is a filled dot (available) or a ring (away), so the state is not shown by colour alone. */
    fun markFilled(available: Boolean): Boolean = available

    /** The colour of the status word. */
    fun statusColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of the hint. */
    fun hintColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** Whether the switch can be toggled: not while disabled and not while a change is saved. */
    fun interactive(enabled: Boolean, pending: Boolean): Boolean = enabled && !pending
}
