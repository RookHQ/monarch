package com.rookhq.monarch.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The small upper-case section label of the web (`text-transform: uppercase`): drawn in capitals, while TalkBack and
 * tests read the text as it was given, in its own case, as a heading.
 */
@Composable
internal fun MonarchSmallCapsLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MonarchTheme.colors.textMuted,
) {
    MonarchText(
        text = text.uppercase(),
        modifier = modifier.clearAndSetSemantics {
            this.text = AnnotatedString(text)
            heading()
        },
        style = MonarchTextStyle.Caption,
        color = color,
    )
}
