package com.rookhq.monarch.theme

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.RippleConfiguration

/**
 * The ripple of every Material 3 surface inside `MonarchTheme`: the default text colour at Monarch's state
 * opacities (PREMIUM.md rule 10: a state is one surface step, subtle), in place of Material's default
 * ripple colour and alphas.
 */
internal fun monarchRippleConfiguration(colors: MonarchColors): RippleConfiguration =
    RippleConfiguration(color = colors.textDefault, rippleAlpha = MonarchRippleAlpha)

/** Pressed and dragged a little stronger than focused, hovered the lightest. */
internal val MonarchRippleAlpha: RippleAlpha = RippleAlpha(
    draggedAlpha = 0.12f,
    focusedAlpha = 0.10f,
    hoveredAlpha = 0.06f,
    pressedAlpha = 0.10f,
)
