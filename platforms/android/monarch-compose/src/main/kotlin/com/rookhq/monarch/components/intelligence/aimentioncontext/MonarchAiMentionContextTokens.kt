package com.rookhq.monarch.components.intelligence.aimentioncontext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The text colours of one result. */
internal data class MonarchAiMentionContextRowColors(val label: Color, val detail: Color)

/** Component tokens of MonarchAiMentionContext, from primitives and roles only. */
internal object MonarchAiMentionContextTokens {
    /** The padding of headings, messages and placeholders. */
    val Padding: Dp = MonarchTokens.Space.space4.dp

    /** The space inside a heading and between placeholder parts. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The top border of the strip. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The tallest the list grows before it scrolls: about four rows at the default font size. */
    val MaxListHeight: Dp = (MonarchTokens.Space.space16 * 7).dp

    /** The width of the text of a placeholder row. */
    val PlaceholderWidth: Dp = (MonarchTokens.Space.space16 * 2).dp

    /** The number of placeholder rows while searching. */
    const val PLACEHOLDER_ROWS: Int = 3

    /** The strip: the surface colour. */
    fun background(colors: MonarchColors): Color = colors.bgSurface

    /** The top border of the strip. */
    fun border(colors: MonarchColors): Color = colors.borderSubtle

    /** Headings, messages and token estimates: muted text. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted

    /** The fill of the active result: the subtle assistant tone. */
    fun activeFill(colors: MonarchColors): Color = colors.aiSubtle

    /** The fill of a kind's icon tile: the muted fill, or the subtle assistant tone on the active result. */
    fun tileFill(active: Boolean, colors: MonarchColors): Color = if (active) colors.aiSubtle else colors.bgMuted

    /** A result: default and muted text, or the disabled text colour when it cannot be chosen. */
    fun rowColors(enabled: Boolean, colors: MonarchColors): MonarchAiMentionContextRowColors =
        if (enabled) {
            MonarchAiMentionContextRowColors(colors.textDefault, colors.textMuted)
        } else {
            MonarchAiMentionContextRowColors(colors.textDisabled, colors.textDisabled)
        }
}
