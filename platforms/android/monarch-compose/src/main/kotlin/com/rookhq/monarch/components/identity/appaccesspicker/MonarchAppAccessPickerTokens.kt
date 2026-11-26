package com.rookhq.monarch.components.identity.appaccesspicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The text colours of one app row. */
internal data class MonarchAppAccessRowColors(val name: Color, val description: Color)

/** Component tokens of MonarchAppAccessPicker and MonarchAppAccessSummary, from primitives and roles only. */
internal object MonarchAppAccessPickerTokens {
    /** The space between the parts of the picker. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The space between the checkbox, the logo and the text of a row. */
    val RowGap: Dp = MonarchTokens.Space.space3.dp

    /** The vertical padding of a row. */
    val RowPadding: Dp = MonarchTokens.Space.space1.dp

    /** The least height of a row: the touch target. */
    val RowMinHeight: Dp = MonarchTokens.Control.xl.dp

    /** The space between the name, its badge and the description. */
    val TextGap: Dp = MonarchTokens.Space.space1.dp

    /** The height of a placeholder row while loading. */
    val PlaceholderHeight: Dp = MonarchTokens.Control.lg.dp

    /** The number of placeholder rows while loading. */
    const val PLACEHOLDER_ROWS: Int = 3

    /** Name and description: default and muted text, or the disabled text colour. */
    fun rowColors(enabled: Boolean, colors: MonarchColors): MonarchAppAccessRowColors =
        if (enabled) {
            MonarchAppAccessRowColors(colors.textDefault, colors.textMuted)
        } else {
            MonarchAppAccessRowColors(colors.textDisabled, colors.textDisabled)
        }

    /** The count, the empty text, the help text and the "+N" of the summary: muted text. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted
}
