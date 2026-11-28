package com.rookhq.monarch.internal.field

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchSurface

/**
 * The visible box of a small control (checkbox, radio, switch track, stepper marker): a fixed [size], the fill
 * and the 1px border of [shape].
 *
 * It does not reserve the touch target. Reserve it around the box and put the tap handler outside both, so
 * the whole 48dp square is tappable and the visible 16dp stays 16dp:
 *
 * ```
 * Box(
 *     Modifier
 *         .toggleable(value = checked, onValueChange = onChange, role = Role.Checkbox)
 *         .monarchMinimumTouchTarget()
 *         .monarchControlBox(MonarchControlMetrics.CheckSize, MonarchShapes.Default.xs, fill, border),
 * )
 * ```
 *
 * @param size the width and height of the visible box.
 * @param shape the outline.
 * @param container the fill.
 * @param border the 1px border colour, or null for none.
 */
internal fun Modifier.monarchControlBox(size: Dp, shape: Shape, container: Color, border: Color? = null): Modifier =
    this.size(size).monarchSurface(
        MonarchSurfaceColors(container = container, content = Color.Unspecified, border = border),
        shape,
    )
