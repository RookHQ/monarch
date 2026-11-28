package dev.monarchui.components.overlays.bottomsheet

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchShapes
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/**
 * Component tokens of MonarchBottomSheet: the raised surface with 16dp top
 * corners, no shadow, a 36 by 4dp handle, and the
 * 20dp padding and type of the Monarch dialog.
 */
internal object MonarchBottomSheetTokens {
    /** The padding at the sides and under the content. */
    val Padding: Dp = MonarchTokens.Space.space5.dp

    /** Between the title and the description. */
    val HeaderGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The side padding of the header over full-width content: the padding of a list row. */
    val ListInset: Dp = MonarchTokens.Space.space4.dp

    /** Between the header and the content. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The visible handle. */
    val HandleWidth: Dp = MonarchTokens.Control.md.dp
    val HandleHeight: Dp = MonarchTokens.Space.space1.dp

    /** The top corners of the sheet; the bottom ones meet the screen edge. */
    fun shape(shapes: MonarchShapes): CornerBasedShape =
        shapes.xl.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize)

    /**
     * The sheet: the raised background (a lighter step than the canvas in a
     * dark theme) with the default text. No border: the sheet meets the
     * screen edges and the scrim separates it.
     */
    fun colors(colors: MonarchColors): MonarchSurfaceColors = MonarchSurfaceColors(
        container = colors.bgRaised,
        content = colors.textDefault,
        border = null,
    )

    /** The handle: the subtle text role, visible on the raised surface in the light and the dark themes. */
    fun handleColor(colors: MonarchColors): Color = colors.textSubtle
}
