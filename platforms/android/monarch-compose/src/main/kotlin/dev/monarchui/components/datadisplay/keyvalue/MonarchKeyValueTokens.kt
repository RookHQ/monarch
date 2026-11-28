package dev.monarchui.components.datadisplay.keyvalue

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The colours of a pair and its separators. */
internal data class KeyValueColors(val label: Color, val value: Color, val dot: Color, val divider: Color)

/** Component tokens of MonarchKeyValue, built from primitives and roles only. */
internal object MonarchKeyValueTokens {
    /** The gap between the label and the value of a pair. */
    val PairGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between pairs and around a separator. */
    val ItemGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the lines of the stack layout. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The height of the divider separator at the default font size. */
    val DividerHeight: Dp = MonarchTokens.Space.space3.dp

    /** Shown for an empty value. Decorative: TalkBack reads "Not set". */
    const val EMPTY_MARK: String = "—"

    /** The middle dot between pairs. Decorative. */
    const val DOT: String = "·"

    /** The text style: caption for `Sm`, body for `Md`; the code face for a monospaced value. */
    fun textStyle(size: MonarchKeyValueSize, monospace: Boolean, typography: MonarchTypography): TextStyle {
        val base = if (size == MonarchKeyValueSize.Sm) typography.caption else typography.body
        return if (monospace) base.copy(fontFamily = typography.code.fontFamily) else base
    }

    /** The label is muted text, the value default text, the dot subtle text and the divider the default border. */
    fun colors(colors: MonarchColors): KeyValueColors = KeyValueColors(
        label = colors.textMuted,
        value = colors.textDefault,
        dot = colors.textSubtle,
        divider = colors.borderDefault,
    )
}
