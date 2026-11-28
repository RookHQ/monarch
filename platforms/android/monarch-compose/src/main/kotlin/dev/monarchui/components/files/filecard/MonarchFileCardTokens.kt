package dev.monarchui.components.files.filecard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and rules of MonarchFileCard, built from primitives and roles only. */
internal object MonarchFileCardTokens {
    val Padding: Dp = MonarchTokens.Space.space3.dp
    val Gap: Dp = MonarchTokens.Space.space1.dp
    val RowGap: Dp = MonarchTokens.Space.space3.dp
    val CornerInset: Dp = MonarchTokens.Space.space1.dp
    val RowThumbnail: Dp = MonarchTokens.Space.space10.dp
    val RowMinHeight: Dp = MonarchTokens.Space.space12.dp
    val BorderWidth: Dp = MonarchTokens.Space.px.dp
    val SelectedBorderWidth: Dp = MonarchTokens.Space.px.dp * 2

    /** The width to height ratio of the preview of a card. */
    const val PREVIEW_RATIO: Float = 4f / 3f

    /** Lines of the name: two in a card, one in a row. */
    fun nameLines(layout: MonarchFileCardLayout): Int = if (layout == MonarchFileCardLayout.Card) 2 else 1

    /**
     * The fill, text and border of the card. Disabled wins, then selected,
     * then pressed. Selection is also a ticked checkbox and a thicker border.
     */
    fun colors(selected: Boolean, enabled: Boolean, pressed: Boolean, colors: MonarchColors): MonarchSurfaceColors =
        when {
            !enabled -> MonarchSurfaceColors(colors.bgSubtle, colors.textDisabled, colors.borderSubtle)
            selected -> MonarchSurfaceColors(colors.accentSubtle, colors.textDefault, colors.accentBorder)
            pressed -> MonarchSurfaceColors(colors.bgSubtle, colors.textDefault, colors.borderDefault)
            else -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderSubtle)
        }

    /** A selected card has a thicker border, so selection is not shown by colour alone. */
    fun borderWidth(selected: Boolean, enabled: Boolean): Dp =
        if (selected && enabled) SelectedBorderWidth else BorderWidth

    /** The meta line is muted, and takes the disabled role when disabled. */
    fun metaColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The fill behind the type icon when there is no picture. */
    fun placeholder(colors: MonarchColors): Color = colors.bgSubtle

    /** The fill behind the checkbox on a card, so it stays visible over a picture. */
    fun checkboxBackdrop(colors: MonarchColors): Color = colors.bgSurface

    /** The meta line: the type, the size and the app's text, in that order, without the parts that are missing. */
    fun meta(type: String, size: String?, meta: String?, separator: String): String =
        listOfNotNull(type, size, meta?.takeIf { it.isNotBlank() }).joinToString(separator)
}
