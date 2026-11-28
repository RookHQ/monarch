package dev.monarchui.components.typography.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.isHeading
import dev.monarchui.theme.MonarchTheme

/**
 * Text in one of the Monarch type roles.
 *
 * A thin layer over Material 3 `Text`: it selects the style of [style] from
 * `MonarchTheme.typography` and marks heading roles as headings for TalkBack.
 * The size follows the user's font size setting.
 *
 * @param text the text to show.
 * @param modifier the modifier of the text.
 * @param style the type role.
 * @param color the text colour. Unspecified uses the content colour of the surrounding surface.
 * @param textAlign the alignment inside the available width; `null` follows the layout direction.
 * @param overflow what happens to text that does not fit.
 * @param softWrap whether the text wraps at soft line breaks.
 * @param maxLines the maximum number of lines.
 * @param minLines the minimum height in lines.
 * @param heading whether TalkBack announces the text as a heading. By default the display, title,
 * heading and subheading roles are headings.
 */
@Composable
public fun MonarchText(
    text: String,
    modifier: Modifier = Modifier,
    style: MonarchTextStyle = MonarchTextStyle.Body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    heading: Boolean = style.isHeading,
) {
    Text(
        text = text,
        modifier = if (heading) modifier.semantics { heading() } else modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        style = MonarchTheme.typography.style(style),
    )
}

/**
 * Text with inline styling, links or annotations in one of the Monarch type roles.
 *
 * @param text the annotated text to show.
 * @param modifier the modifier of the text.
 * @param style the type role.
 * @param color the text colour. Unspecified uses the content colour of the surrounding surface.
 * @param textAlign the alignment inside the available width; `null` follows the layout direction.
 * @param overflow what happens to text that does not fit.
 * @param softWrap whether the text wraps at soft line breaks.
 * @param maxLines the maximum number of lines.
 * @param minLines the minimum height in lines.
 * @param heading whether TalkBack announces the text as a heading.
 */
@Composable
public fun MonarchText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: MonarchTextStyle = MonarchTextStyle.Body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    heading: Boolean = style.isHeading,
) {
    Text(
        text = text,
        modifier = if (heading) modifier.semantics { heading() } else modifier,
        color = color,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        style = MonarchTheme.typography.style(style),
    )
}
