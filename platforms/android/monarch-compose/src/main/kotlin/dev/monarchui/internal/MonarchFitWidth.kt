package dev.monarchui.internal

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** The width of the Material 3 calendar picker: seven columns of 48dp and a 12dp margin on both sides. */
internal val MonarchCalendarPickerWidth: Dp = 360.dp

/**
 * Draws content that is built for [designWidth] in a narrower space by scaling it down evenly, so that nothing is
 * cut off. The Material 3 `DatePicker` is exactly 360dp wide and cannot shrink: in a 320dp window its last column
 * is clipped. The content keeps its own size when the space is as wide as [designWidth] or wider, and is never
 * scaled up.
 */
internal fun Modifier.monarchFitWidth(designWidth: Dp = MonarchCalendarPickerWidth): Modifier =
    layout { measurable, constraints ->
        val design = designWidth.roundToPx()
        val available = if (constraints.hasBoundedWidth) constraints.maxWidth else design
        val scale = (available.toFloat() / design).coerceIn(0f, 1f)
        val placeable = measurable.measure(Constraints(maxWidth = design, maxHeight = Constraints.Infinity))
        layout((placeable.width * scale).roundToInt(), (placeable.height * scale).roundToInt()) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
