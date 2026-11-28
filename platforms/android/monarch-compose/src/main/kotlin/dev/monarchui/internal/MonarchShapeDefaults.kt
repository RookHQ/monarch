package dev.monarchui.internal

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchShapes
import dev.monarchui.tokens.MonarchTokens

/**
 * The shapes components share, by what they are used for, so that all
 * controls, all containers and all marks have the same corners.
 */
internal object MonarchShapeDefaults {
    /** Buttons, fields and other controls: 8dp corners. */
    val Control: CornerBasedShape = MonarchShapes.Default.md

    /** Cards, banners and other containers: 12dp corners. */
    val Container: CornerBasedShape = MonarchShapes.Default.lg

    /** Badges, chips, avatars and other small marks: fully rounded. */
    val Mark: CornerBasedShape = MonarchShapes.Default.full

    /** Focus ring of a row or a list item: 6dp corners. */
    val Row: CornerBasedShape = MonarchShapes.Default.sm

    /** The width of every border and divider: 1dp. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp
}
