package com.rookhq.monarch.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/**
 * Draws the fill and, when the colours have one, the 1dp border of a surface.
 *
 * For a custom component that is not built on the Material 3 `Surface`.
 * Content colour is not set here: provide `LocalContentColor` with
 * `colors.content` around the content.
 *
 * @param colors the colours of the current state, for example from [MonarchTonePalette.surface].
 * @param shape the outline; one of [MonarchShapeDefaults] or `MonarchTheme.shapes`.
 */
internal fun Modifier.monarchSurface(colors: MonarchSurfaceColors, shape: Shape): Modifier {
    val filled = background(colors.container, shape)
    return if (colors.border != null) {
        filled.border(MonarchShapeDefaults.BorderWidth, colors.border, shape)
    } else {
        filled
    }
}
