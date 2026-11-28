package com.rookhq.monarch.components.forms.radiocardgroup

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchRadioCardGroup, built from primitives and roles only. */
internal object MonarchRadioCardGroupTokens {
    const val MAX_COLUMNS: Int = 3

    /** Below this width the cards always stack in one column, as on the web under 40rem. */
    val SingleColumnBelow: Dp = MonarchTokens.Breakpoint.sm.dp

    /** From this font scale the cards always stack in one column. */
    const val SINGLE_COLUMN_FONT_SCALE: Float = 1.5f

    val Gap: Dp = MonarchTokens.Space.space3.dp
    val LabelGap: Dp = MonarchTokens.Space.space2.dp
    val ContentGap: Dp = MonarchTokens.Space.space3.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp
    val MetaGap: Dp = MonarchTokens.Space.space2.dp
    val ContentPadding: PaddingValues =
        PaddingValues(horizontal = MonarchTokens.Space.space4.dp, vertical = MonarchTokens.Space.space3.dp)
    val BorderWidth: Dp = MonarchTokens.Space.px.dp
    val SelectedBorderWidth: Dp = BorderWidth

    /** The gap between a tile's icon and its title. */
    val TileIconGap: Dp = MonarchTokens.Space.space2.dp

    /** The columns of tiles: [requested] on any width (a tile is narrow), one at the largest font scales. */
    fun tileColumns(requested: Int, fontScale: Float): Int =
        if (fontScale >= SINGLE_COLUMN_FONT_SCALE) 1 else requested.coerceIn(1, MAX_COLUMNS)

    /** The number of columns used for [requested] columns at [width] and [fontScale]. */
    fun columns(requested: Int, width: Dp, fontScale: Float): Int = when {
        width < SingleColumnBelow || fontScale >= SINGLE_COLUMN_FONT_SCALE -> 1
        else -> requested.coerceIn(1, MAX_COLUMNS)
    }

    /** The fill, text and border colours of a card. */
    fun card(
        selected: Boolean,
        enabled: Boolean,
        colors: MonarchColors,
        pressed: Boolean = false,
    ): MonarchSurfaceColors = when {
        !enabled -> MonarchSurfaceColors(colors.bgSubtle, colors.textDisabled, colors.borderSubtle)
        selected && pressed -> MonarchSurfaceColors(colors.accentSubtleHover, colors.textDefault, colors.accentStrong)
        selected -> MonarchSurfaceColors(colors.accentSubtle, colors.textDefault, colors.accentStrong)
        pressed -> MonarchSurfaceColors(colors.bgSubtle, colors.textDefault, colors.borderStrong)
        else -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderDefault)
    }

    /** The meta text (a price, a duration): 14sp regular with tabular figures, as the web's `__meta`. */
    fun metaStyle(typography: MonarchTypography): TextStyle =
        typography.label.copy(fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum")

    /** Centres the 16dp radio on the first line of the 14sp title. */
    val MarkTop: Dp = MonarchTokens.Space.px.dp * 2

    /** The colour of the description and the meta text. */
    fun muted(enabled: Boolean, colors: MonarchColors): Color = if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the icon. */
    fun icon(selected: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        selected -> colors.accentStrong
        else -> colors.textMuted
    }

    /** The border width: 1dp at rest and chosen; the chosen card is told apart by its accent edge and fill (rule 8). */
    fun borderWidth(selected: Boolean): Dp = if (selected) SelectedBorderWidth else BorderWidth
}
