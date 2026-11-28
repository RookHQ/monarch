package dev.monarchui.components.forms.ratingpicker

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Component tokens of MonarchRatingPicker, built from primitives and roles only. */
internal object MonarchRatingPickerTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val StarGap: Dp = MonarchTokens.Space.space0.dp
    val LabelGap: Dp = MonarchTokens.Space.space2.dp
    val TileTextGap: Dp = MonarchTokens.Space.space0_5.dp
    val TileMinSize: Dp = MonarchTokens.Space.space12.dp
    val BorderWidth: Dp = MonarchTokens.Space.px.dp
    val SelectedBorderWidth: Dp = MonarchTokens.Space.space0_5.dp
    val StarBorderWidth: Dp = MonarchTokens.Space.px.dp * 2

    fun tilePadding(size: MonarchRatingPickerSize): PaddingValues = when (size) {
        MonarchRatingPickerSize.Sm ->
            PaddingValues(horizontal = MonarchTokens.Space.space2.dp, vertical = MonarchTokens.Space.space1.dp)
        MonarchRatingPickerSize.Md ->
            PaddingValues(horizontal = MonarchTokens.Space.space3.dp, vertical = MonarchTokens.Space.space2.dp)
    }

    fun starSize(size: MonarchRatingPickerSize): Dp = when (size) {
        MonarchRatingPickerSize.Sm -> MonarchTokens.Space.space5.dp
        MonarchRatingPickerSize.Md -> MonarchTokens.Space.space6.dp
    }

    /** The colours of a scale tile. Disabled wins over selected. */
    fun tile(selected: Boolean, enabled: Boolean, colors: MonarchColors): MonarchSurfaceColors = when {
        !enabled && selected -> MonarchSurfaceColors(colors.bgSubtle, colors.textDisabled, colors.borderStrong)
        !enabled -> MonarchSurfaceColors(colors.bgSubtle, colors.textDisabled, colors.borderSubtle)
        selected -> MonarchSurfaceColors(colors.accentSubtle, colors.textDefault, colors.accentBorder)
        else -> MonarchSurfaceColors(colors.bgSurface, colors.textMuted, colors.borderDefault)
    }

    /** The border width of a tile: heavier when selected, so the choice is not colour alone. */
    fun tileBorderWidth(selected: Boolean): Dp = if (selected) SelectedBorderWidth else BorderWidth

    /** The colour of a star: filled stars in the strong warning colour, empty ones in the strong border colour. */
    fun star(filled: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        filled -> colors.warningStrong
        else -> colors.borderStrong
    }

    private const val POINTS = 5
    private const val INNER_RATIO = 0.4f

    /** A five-pointed star, drawn as a shape because Monarch bundles no icons. */
    val StarShape: Shape = GenericShape { size, _ ->
        val cx = size.width / 2
        val cy = size.height / 2
        val outer = minOf(size.width, size.height) / 2
        val inner = outer * INNER_RATIO
        repeat(POINTS * 2) { i ->
            val radius = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / POINTS
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}
