package dev.monarchui.components.typography.tabularnumberstyle

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.R
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.text.NumberFormat

/**
 * A number in tabular (fixed-width) digits, formatted in the user's locale,
 * or a dash when there is no number.
 *
 * A Material 3 `Text` in a Monarch type role with
 * [MonarchTabularNumberStyle] applied. The number is formatted with
 * [format], by default the number format of the locale of the current
 * configuration. A missing value (`null`, NaN or infinite) is shown as an
 * em dash and read by TalkBack as "No value".
 *
 * @param value the number, or `null` when there is none.
 * @param modifier the modifier of the text.
 * @param style the type role.
 * @param format how the number is written, for example `NumberFormat.getPercentInstance(locale)` or a
 * currency instance. `null` uses `NumberFormat.getNumberInstance` in the configuration's locale.
 * @param color the text colour. Unspecified uses the content colour of the surrounding surface.
 * @param textAlign the alignment inside the available width. Align columns of figures to the end.
 * @param emptyText replaces the dash shown for a missing value (`monarch_tabularnumber_empty`).
 * @param emptyLabel replaces what TalkBack reads for a missing value, "No value"
 * (`monarch_tabularnumber_empty_label`).
 */
@Composable
public fun MonarchTabularNumber(
    value: Number?,
    modifier: Modifier = Modifier,
    style: MonarchTextStyle = MonarchTextStyle.Body,
    format: NumberFormat? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    emptyText: String? = null,
    emptyLabel: String? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val numberFormat = format ?: remember(locale) { NumberFormat.getNumberInstance(locale) }
    val formatted = MonarchTabularNumberTokens.format(value, numberFormat)
    if (formatted != null) {
        MonarchTabularNumber(text = formatted, modifier = modifier, style = style, color = color, textAlign = textAlign)
    } else {
        val label = monarchString(emptyLabel, R.string.monarch_tabularnumber_empty_label)
        MonarchTabularNumber(
            text = monarchString(emptyText, R.string.monarch_tabularnumber_empty),
            // TalkBack reads the content description in place of the dash.
            modifier = modifier.semantics { contentDescription = label },
            style = style,
            color = color,
            textAlign = textAlign,
        )
    }
}

/**
 * Text that is already formatted, such as a time "12:04" or a total with its
 * unit, in tabular digits.
 *
 * @param text the formatted text.
 * @param modifier the modifier of the text.
 * @param style the type role.
 * @param color the text colour. Unspecified uses the content colour of the surrounding surface.
 * @param textAlign the alignment inside the available width.
 */
@Composable
public fun MonarchTabularNumber(
    text: String,
    modifier: Modifier = Modifier,
    style: MonarchTextStyle = MonarchTextStyle.Body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = textAlign,
        style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.style(style)),
    )
}
