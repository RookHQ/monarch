package dev.monarchui.components.typography.kbd

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/**
 * A keyboard key or shortcut drawn as a key cap, such as "Ctrl K" or "⌘K".
 *
 * Built by Monarch: Material 3 has no equivalent. A shortcut hint only helps
 * someone who has a keyboard, so by default the cap is drawn only while a
 * hardware keyboard is attached (`Configuration.keyboard` is
 * `KEYBOARD_QWERTY` and the keyboard is not hidden) and takes no room
 * otherwise. TalkBack reads the cap as one element whose name spells the key
 * glyphs out: "⇧⌘P" is read as "Shift Command P".
 *
 * @param keys the key or shortcut as it is shown, such as "Esc", "Ctrl+K" or "⇧⌘P".
 * @param modifier the modifier of the key cap.
 * @param alwaysVisible draws the cap without a hardware keyboard too, for example in documentation of
 * shortcuts.
 * @param labels replacements for the names TalkBack reads for the key glyphs.
 */
@Composable
public fun MonarchKbd(
    keys: String,
    modifier: Modifier = Modifier,
    alwaysVisible: Boolean = false,
    labels: MonarchKbdLabels = MonarchKbdLabels(),
) {
    if (!alwaysVisible && !LocalConfiguration.current.hasHardwareKeyboard()) return
    val tokens = MonarchKbdTokens
    val colors = tokens.colors(MonarchTheme.colors)
    val names = kbdGlyphNames(labels)
    val spoken = tokens.spokenKeys(keys, names)
    val shape = MonarchTheme.shapes.xs
    val fontScale = LocalDensity.current.fontScale
    Box(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = spoken }
            .background(colors.container, shape)
            .border(MonarchShapeDefaults.BorderWidth, colors.border ?: colors.container, shape)
            .drawBehind {
                // The thicker bottom edge of a key cap, inside the outline.
                val y = size.height - tokens.BottomEdge.toPx() * 2
                drawLine(
                    color = colors.border ?: colors.container,
                    start = Offset(tokens.BottomEdgeInset.toPx(), y),
                    end = Offset(size.width - tokens.BottomEdgeInset.toPx(), y),
                    strokeWidth = tokens.BottomEdge.toPx(),
                )
            }
            .widthIn(min = tokens.MinSize * fontScale)
            .heightIn(min = tokens.MinSize * fontScale)
            .padding(
                start = tokens.HorizontalPadding,
                end = tokens.HorizontalPadding,
                top = tokens.VerticalPadding,
                bottom = tokens.VerticalPadding + tokens.BottomEdge,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = keys, style = MonarchTheme.typography.code, color = colors.content)
    }
}

/** Whether a hardware keyboard with letter keys is attached and not hidden. */
private fun Configuration.hasHardwareKeyboard(): Boolean =
    keyboard == Configuration.KEYBOARD_QWERTY && hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
