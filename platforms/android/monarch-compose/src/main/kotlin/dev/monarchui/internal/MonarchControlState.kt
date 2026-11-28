package dev.monarchui.internal

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue

/**
 * The interaction state of a control, for choosing colours in a pure function.
 *
 * @property enabled whether the user can act on the control.
 * @property pressed whether a press is in progress.
 * @property focused whether the control has keyboard focus.
 * @property hovered whether a pointer is over the control.
 */
@Immutable
internal data class MonarchControlState(
    val enabled: Boolean = true,
    val pressed: Boolean = false,
    val focused: Boolean = false,
    val hovered: Boolean = false,
) {
    /**
     * Picks the value for this state. Disabled wins over pressed, and pressed
     * over hovered, so a disabled control never shows a press.
     *
     * Use it with colour roles that exist for the state (`textDisabled`,
     * `bgMuted`, `accentSolidActive`), never with an opacity.
     */
    fun <T> pick(resting: T, disabled: T, pressed: T = resting, hovered: T = resting): T = when {
        !enabled -> disabled
        this.pressed -> pressed
        this.hovered -> hovered
        else -> resting
    }
}

/**
 * Collects the state of a control from its interaction source.
 *
 * Material 3 components draw their own pressed state (the ripple); use this
 * for a custom control that changes colour instead.
 */
@Composable
internal fun rememberMonarchControlState(interactionSource: InteractionSource, enabled: Boolean): MonarchControlState {
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    return MonarchControlState(enabled = enabled, pressed = pressed, focused = focused, hovered = hovered)
}
