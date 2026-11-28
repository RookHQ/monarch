package dev.monarchui.components.collaboration.slashmenu

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The colours of an entry of the slash menu. */
internal data class MonarchSlashMenuRowColors(val label: Color, val description: Color, val icon: Color)

/** Component tokens and logic of MonarchSlashMenu, built from primitives and roles only. */
internal object MonarchSlashMenuTokens {
    /** The padding of headings and the empty text. */
    val Padding: Dp = MonarchTokens.Space.space4.dp

    /** The space above and below a heading. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The top border of the strip. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The tallest the list grows before it scrolls: about eight rows at the default font size. */
    val MaxListHeight: Dp = (MonarchTokens.Space.space16 * 9).dp

    /** The strip: the surface colour. */
    fun background(colors: MonarchColors): Color = colors.bgSurface

    /** The top border of the strip. */
    fun border(colors: MonarchColors): Color = colors.borderSubtle

    /** Headings and the empty text in the muted text colour. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted

    /** The fill of the active entry of the assistant's menu: the subtle assistant tone. */
    fun activeFill(colors: MonarchColors): Color = colors.aiSubtle

    /** An entry: default label, muted description and icon; the disabled text colour when it cannot be chosen. */
    fun rowColors(enabled: Boolean, colors: MonarchColors, active: Boolean = false): MonarchSlashMenuRowColors =
        if (enabled) {
            val icon = if (active) colors.aiStrong else colors.textMuted
            MonarchSlashMenuRowColors(colors.textDefault, colors.textMuted, icon)
        } else {
            MonarchSlashMenuRowColors(colors.textDisabled, colors.textDisabled, colors.textDisabled)
        }
}
