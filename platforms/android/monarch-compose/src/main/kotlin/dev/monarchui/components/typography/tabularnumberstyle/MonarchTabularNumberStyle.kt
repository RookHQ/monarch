package dev.monarchui.components.typography.tabularnumberstyle

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle

/**
 * Fixed-width (tabular) digits, so that columns of figures line up and
 * values that change in place do not jitter.
 *
 * The OpenType feature `tnum` as a Compose text style. Use [applyTo] for a
 * whole text, [spanStyle] for a run inside an `AnnotatedString`, or
 * `MonarchTabularNumber` to format and show a number.
 */
public object MonarchTabularNumberStyle {
    /** The OpenType feature setting of tabular digits. */
    public const val FONT_FEATURE_SETTINGS: String = "tnum"

    /**
     * [style] with tabular digits. Other font features of [style] are
     * replaced, because Compose holds them in one setting.
     *
     * @param style the text style, for example `MonarchTheme.typography.body`.
     */
    public fun applyTo(style: TextStyle): TextStyle = style.copy(fontFeatureSettings = FONT_FEATURE_SETTINGS)

    /** A span style with tabular digits, for a number inside other text. */
    public fun spanStyle(): SpanStyle = SpanStyle(fontFeatureSettings = FONT_FEATURE_SETTINGS)
}
