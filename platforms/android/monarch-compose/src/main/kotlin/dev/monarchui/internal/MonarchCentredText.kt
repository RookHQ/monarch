package dev.monarchui.internal

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/**
 * Text that is centred line by line when it wraps, in a parent that centres
 * it (an empty state, a dialog title, a button label).
 *
 * The lines are centred only once the text takes more than one line. A single
 * line measured with centre alignment can be laid out a pixel narrower than
 * its text at large font sizes, which cuts off the last glyph; the parent
 * centres a single line anyway.
 */
@Composable
internal fun MonarchCentredText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    var wraps by remember(text) { mutableStateOf(false) }
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = if (wraps) TextAlign.Center else TextAlign.Unspecified,
        onTextLayout = { wraps = it.lineCount > 1 },
    )
}
