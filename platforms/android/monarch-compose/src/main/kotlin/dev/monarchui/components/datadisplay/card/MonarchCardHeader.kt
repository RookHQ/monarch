package dev.monarchui.components.datadisplay.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * The title and description at the top of a [MonarchCard], with an optional element at the end.
 *
 * The title is 16sp semibold and a heading for TalkBack; the description is 14sp in the muted colour.
 *
 * @param title the title of the card.
 * @param modifier the modifier of the header.
 * @param description a line under the title. `null` for none.
 * @param trailing an element at the end of the header, for example a `MonarchIconButton` or a badge.
 */
@Composable
public fun MonarchCardHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MonarchTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchCardTokens.HeaderGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MonarchCardTokens.TitleGap)) {
            Text(
                text = title,
                style = MonarchTheme.typography.subheading.copy(fontSize = MonarchTokens.FontSize.base.sp),
                color = colors.textDefault,
                modifier = Modifier.semantics { heading() },
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                    color = colors.textMuted,
                )
            }
        }
        trailing?.invoke()
    }
}
