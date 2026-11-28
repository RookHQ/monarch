package dev.monarchui.components.overlays.menu

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.theme.MonarchTheme

/**
 * The name of a group of rows in a [MonarchMenu], such as "Sort by": 12 in
 * the subtle text colour, aligned with the text of the rows. TalkBack reads it
 * as a heading.
 *
 * @param text the name of the group.
 * @param modifier the modifier of the label.
 */
@Composable
public fun MonarchMenuLabel(text: String, modifier: Modifier = Modifier) {
    val tokens = MonarchMenuTokens
    Text(
        text = text,
        modifier = modifier
            .semantics { heading() }
            .padding(
                start = tokens.Inset + tokens.RowPadding,
                end = tokens.Inset + tokens.RowPadding,
                top = tokens.LabelTop,
                bottom = tokens.LabelBottom,
            ),
        style = MonarchTheme.typography.caption,
        color = MonarchTheme.colors.textSubtle,
    )
}
