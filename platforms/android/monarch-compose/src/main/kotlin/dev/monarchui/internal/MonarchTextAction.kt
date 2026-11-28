package dev.monarchui.internal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import dev.monarchui.theme.MonarchTheme

/**
 * The underlined link-coloured text action the web draws at the end of a row ("Open", "Manage source"): medium
 * weight, always underlined, as small as the row text, on a 48dp tap target.
 */
@Composable
internal fun MonarchTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MonarchTheme.typography.caption,
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = style.merge(
                TextStyle(
                    color = MonarchTheme.colors.textLink,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                ),
            ),
        )
    }
}
