package com.rookhq.monarch.internal.field

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.rookhq.monarch.theme.MonarchColors

/**
 * The state of a field, for choosing its colours. Disabled wins over invalid, invalid over focused, focused
 * over hovered, so a disabled field never shows a hover and an invalid field keeps its red border in focus.
 */
@Immutable
internal data class MonarchFieldState(
    val enabled: Boolean = true,
    val invalid: Boolean = false,
    val success: Boolean = false,
    val focused: Boolean = false,
    val hovered: Boolean = false,
)

/** The colours of a field in one state. [ring] is the focus ring outside the border, or null. */
@Immutable
internal data class MonarchFieldColors(
    val container: Color,
    val border: Color,
    val text: Color,
    val placeholder: Color,
    val icon: Color,
    val affix: Color,
    val ring: Color?,
)

/**
 * The colours of the field: a 1px `borderInput` border; hover `textSubtle`; focus a 2px `focusRing` ring at a 2dp
 * offset around the unchanged border (the web turns the border `focusRing` and draws the ring flush; native keeps
 * one clean ring, PREMIUM.md rule 10); invalid `dangerStrong`; disabled a `bgSubtle` fill, `textDisabled` text and a
 * `borderDefault` hairline. Every colour is an opaque theme role: a disabled field is drawn in its own quiet
 * colours, never by lowering the alpha of the enabled ones.
 */
internal fun MonarchColors.fieldColors(state: MonarchFieldState): MonarchFieldColors {
    val border = when {
        !state.enabled -> borderDefault
        state.invalid -> dangerStrong
        state.success -> successStrong
        // Focus is carried by the ring alone, 2dp outside: a second coloured line inside it would read as a
        // double border, and as the invalid state where focusRing and dangerStrong are the same red.
        state.focused -> borderInput
        state.hovered -> textSubtle
        else -> borderInput
    }
    return MonarchFieldColors(
        container = if (state.enabled) bgSurface else bgSubtle,
        border = border,
        text = if (state.enabled) textDefault else textDisabled,
        placeholder = if (state.enabled) textSubtle else textDisabled,
        icon = if (state.enabled) textMuted else textDisabled,
        affix = if (state.enabled) textMuted else textDisabled,
        ring = if (state.enabled && state.focused) focusRing else null,
    )
}
