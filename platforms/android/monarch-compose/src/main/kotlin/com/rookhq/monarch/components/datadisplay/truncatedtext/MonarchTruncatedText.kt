package com.rookhq.monarch.components.datadisplay.truncatedtext

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Text cut after a number of lines, with a button that shows the rest. The
 * component keeps the expanded state itself and starts cut.
 *
 * The cut is visual: TalkBack reads the whole text whether or not it is cut.
 * The button appears only when the text is longer than [maxLines].
 *
 * @param text the text.
 * @param modifier the modifier of the text and its button.
 * @param maxLines how many lines are shown before the text is cut. Values below 1 count as 1.
 * @param expandable whether a button shows the rest when the text is cut. Turn it off only where the
 * whole text is one tap away, for example in a row that opens the record.
 * @param style the type role of the text.
 * @param showMoreLabel the label of the button while the text is cut. `null` uses the built-in string
 * `monarch_truncatedtext_show_more`.
 * @param showLessLabel the label of the button while the whole text is shown. `null` uses the built-in
 * string `monarch_truncatedtext_show_less`.
 */
@Composable
public fun MonarchTruncatedText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = MonarchTruncatedTextTokens.DEFAULT_MAX_LINES,
    expandable: Boolean = true,
    style: MonarchTextStyle = MonarchTextStyle.Body,
    showMoreLabel: String? = null,
    showLessLabel: String? = null,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    MonarchTruncatedText(
        text = text,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        maxLines = maxLines,
        expandable = expandable,
        style = style,
        showMoreLabel = showMoreLabel,
        showLessLabel = showLessLabel,
    )
}

/**
 * Text cut after a number of lines, with a button that shows the rest, whose
 * expanded state the app holds.
 *
 * A Material 3 `Text` with `maxLines` and an ellipsis; `onTextLayout` tells
 * whether the text was cut. The change of height is animated, and immediate
 * when the user has turned animations off.
 *
 * @param text the text.
 * @param expanded whether the whole text is shown.
 * @param onExpandedChange called with the new state when the user activates the button.
 * @param modifier the modifier of the text and its button.
 * @param maxLines how many lines are shown before the text is cut. Values below 1 count as 1.
 * @param expandable whether a button shows the rest when the text is cut.
 * @param style the type role of the text.
 * @param showMoreLabel the label of the button while the text is cut. `null` uses the built-in string
 * `monarch_truncatedtext_show_more`.
 * @param showLessLabel the label of the button while the whole text is shown. `null` uses the built-in
 * string `monarch_truncatedtext_show_less`.
 */
@Composable
public fun MonarchTruncatedText(
    text: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = MonarchTruncatedTextTokens.DEFAULT_MAX_LINES,
    expandable: Boolean = true,
    style: MonarchTextStyle = MonarchTextStyle.Body,
    showMoreLabel: String? = null,
    showLessLabel: String? = null,
) {
    val lines = maxLines.coerceAtLeast(1)
    // Whether the text did not fit in `lines`. Measured only while cut, because an expanded text always fits.
    var cut by remember(text, lines) { mutableStateOf(false) }
    val more = monarchString(showMoreLabel, R.string.monarch_truncatedtext_show_more)
    val less = monarchString(showLessLabel, R.string.monarch_truncatedtext_show_less)
    Column(modifier = modifier.animateContentSize(animationSpec = MonarchTheme.motion.spec())) {
        Text(
            text = text,
            style = MonarchTheme.typography.style(style),
            maxLines = if (expanded) Int.MAX_VALUE else lines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout -> if (!expanded) cut = layout.hasVisualOverflow },
        )
        if (expandable && (cut || expanded)) {
            MonarchButton(
                text = if (expanded) less else more,
                onClick = { onExpandedChange(!expanded) },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}
