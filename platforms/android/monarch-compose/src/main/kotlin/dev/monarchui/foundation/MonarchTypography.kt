package dev.monarchui.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.monarchui.tokens.MonarchTokens

/**
 * One text style per Monarch type role.
 *
 * Sizes are in sp, so they follow the user's font size setting. Line height
 * and letter spacing are in em, so they stay proportional at every size.
 *
 * Create one with [monarchTypography] and pass it to `MonarchTheme`; read it
 * through `MonarchTheme.typography`.
 *
 * @property display style of [MonarchTextStyle.Display].
 * @property title style of [MonarchTextStyle.Title].
 * @property heading style of [MonarchTextStyle.Heading].
 * @property subheading style of [MonarchTextStyle.Subheading].
 * @property body style of [MonarchTextStyle.Body].
 * @property bodyStrong style of [MonarchTextStyle.BodyStrong].
 * @property label style of [MonarchTextStyle.Label].
 * @property caption style of [MonarchTextStyle.Caption].
 * @property code style of [MonarchTextStyle.Code].
 * @property codeSmall style of [MonarchTextStyle.CodeSmall]. By default [code] at the caption size.
 */
@Immutable
public class MonarchTypography(
    public val display: TextStyle,
    public val title: TextStyle,
    public val heading: TextStyle,
    public val subheading: TextStyle,
    public val body: TextStyle,
    public val bodyStrong: TextStyle,
    public val label: TextStyle,
    public val caption: TextStyle,
    public val code: TextStyle,
    public val codeSmall: TextStyle = code.copy(fontSize = MonarchTokens.FontSize.xs.sp),
) {
    /** The text style of [role]. */
    public fun style(role: MonarchTextStyle): TextStyle = when (role) {
        MonarchTextStyle.Display -> display
        MonarchTextStyle.Title -> title
        MonarchTextStyle.Heading -> heading
        MonarchTextStyle.Subheading -> subheading
        MonarchTextStyle.Body -> body
        MonarchTextStyle.BodyStrong -> bodyStrong
        MonarchTextStyle.Label -> label
        MonarchTextStyle.Caption -> caption
        MonarchTextStyle.Code -> code
        MonarchTextStyle.CodeSmall -> codeSmall
    }

    /** Holds the default typography. */
    public companion object {
        /** Monarch sizes, weights and tracking on the system typeface. */
        public val Default: MonarchTypography = monarchTypography()
    }
}

/**
 * Builds the Monarch type roles from the type tokens.
 *
 * @param fontFamily the typeface of every role except code. The default is the system typeface; Monarch
 * does not bundle a font.
 * @param codeFontFamily the typeface of [MonarchTextStyle.Code] and [MonarchTextStyle.CodeSmall].
 */
public fun monarchTypography(
    fontFamily: FontFamily = FontFamily.Default,
    codeFontFamily: FontFamily = FontFamily.Monospace,
): MonarchTypography {
    val size = MonarchTokens.FontSize
    val weight = MonarchTokens.FontWeight
    val leading = MonarchTokens.LineHeight
    val tracking = MonarchTokens.LetterSpacing
    fun style(fontSize: Float, fontWeight: Int, lineHeight: Float, letterSpacing: Float, family: FontFamily) =
        TextStyle(
            fontFamily = family,
            fontSize = fontSize.sp,
            fontWeight = FontWeight(fontWeight),
            lineHeight = lineHeight.em,
            letterSpacing = letterSpacing.em,
        )
    return MonarchTypography(
        display = style(size.xl4, weight.bold, leading.tight, tracking.tight, fontFamily),
        title = style(size.xl2, weight.semibold, leading.tight, tracking.snug, fontFamily),
        heading = style(size.xl, weight.semibold, leading.snug, tracking.snug, fontFamily),
        subheading = style(size.lg, weight.semibold, leading.snug, tracking.normal, fontFamily),
        body = style(size.base, weight.regular, leading.normal, tracking.normal, fontFamily),
        bodyStrong = style(size.base, weight.semibold, leading.normal, tracking.normal, fontFamily),
        label = style(size.sm, weight.medium, leading.snug, tracking.normal, fontFamily),
        caption = style(size.xs, weight.regular, leading.snug, tracking.normal, fontFamily),
        code = style(size.sm, weight.regular, leading.normal, tracking.normal, codeFontFamily),
        codeSmall = style(size.xs, weight.regular, leading.normal, tracking.normal, codeFontFamily),
    )
}
