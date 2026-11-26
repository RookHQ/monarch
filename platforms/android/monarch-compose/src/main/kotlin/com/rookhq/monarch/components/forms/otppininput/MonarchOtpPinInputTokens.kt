package com.rookhq.monarch.components.forms.otppininput

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchOtpPinInput, built from primitives and roles only. */
internal object MonarchOtpPinInputTokens {
    const val DEFAULT_LENGTH: Int = 6
    private const val DIGITS = 10

    val Gap: Dp = MonarchTokens.Space.space2.dp
    val PartGap: Dp = MonarchTokens.Space.space1_5.dp
    val BorderWidth: Dp = MonarchTokens.Space.px.dp
    val ActiveBorderWidth: Dp = MonarchTokens.Space.space0_5.dp
    val DotSize: Dp = MonarchTokens.Space.space2.dp
    val CaretWidth: Dp = MonarchTokens.Space.px.dp
    val CaretHeight: Dp = MonarchTokens.Space.space5.dp

    /** The width and height of a cell at [size]. The width shrinks when the row does not fit. */
    fun cellSize(size: MonarchOtpPinInputSize): Dp = when (size) {
        MonarchOtpPinInputSize.Md -> MonarchTokens.Space.space12.dp
        MonarchOtpPinInputSize.Lg -> MonarchTokens.Space.space16.dp
    }

    /** The full width of [length] cells with a wider gap after every [groupSize] cells. */
    fun rowWidth(length: Int, groupSize: Int?, size: MonarchOtpPinInputSize): Dp {
        val groups = if (groupSize != null && groupSize > 0) (length - 1) / groupSize else 0
        return cellSize(size) * length + Gap * (length - 1 + groups)
    }

    /** Whether a wider gap follows the cell at [index]. */
    fun groupEndsAfter(index: Int, length: Int, groupSize: Int?): Boolean =
        groupSize != null && groupSize > 0 && index < length - 1 && (index + 1) % groupSize == 0

    /** The accepted characters of [text], upper-cased in [mode] Alphanumeric, at most [length] of them. */
    fun clean(text: String, mode: MonarchOtpPinInputMode, length: Int): String = text.mapNotNull { ch ->
        when {
            Character.isDigit(ch) -> '0' + Character.digit(ch, DIGITS)
            mode == MonarchOtpPinInputMode.Alphanumeric && ch.uppercaseChar() in 'A'..'Z' -> ch.uppercaseChar()
            else -> null
        }
    }.joinToString("").take(length)

    /**
     * The colours of one cell. Disabled wins, then the error, then the cell
     * that takes the next character while the field has focus.
     */
    fun cell(filled: Boolean, active: Boolean, error: Boolean, enabled: Boolean, colors: MonarchColors) = when {
        !enabled -> MonarchSurfaceColors(colors.bgSubtle, colors.textDisabled, colors.borderDefault)
        error -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.dangerStrong)
        active -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.focusRing)
        filled -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.textSubtle)
        else -> MonarchSurfaceColors(colors.bgSurface, colors.textDefault, colors.borderInput)
    }
}
