package com.rookhq.monarch.components.typography.kbd

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchKbd, built from primitives and roles only. */
internal object MonarchKbdTokens {
    /** The smallest width and height of the cap at the default font size: 24dp. */
    val MinSize: Dp = MonarchTokens.Space.space6.dp
    val HorizontalPadding: Dp = MonarchTokens.Space.space1.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space0_5.dp

    /** The extra width of the bottom edge, which makes the cap read as a key: 1dp. */
    val BottomEdge: Dp = MonarchTokens.Space.px.dp

    /** Where the bottom edge stops before the rounded corners. */
    val BottomEdgeInset: Dp = MonarchTokens.Radius.xs.dp

    /** The colours of a key cap: subtle fill, muted text, default border. */
    fun colors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.bgSubtle, content = colors.textMuted, border = colors.borderDefault)

    /**
     * What TalkBack reads for [keys]: every key glyph in [names] is replaced
     * by its name, separated by spaces; every other character is kept as
     * written, so "Esc" and "Ctrl+K" are unchanged.
     */
    fun spokenKeys(keys: String, names: Map<Char, String>): String {
        val spoken = StringBuilder()
        keys.forEach { char ->
            val name = names[char]
            if (name == null) {
                spoken.append(char)
            } else {
                spoken.append(' ').append(name).append(' ')
            }
        }
        return spoken.toString().trim().replace(whiteSpace, " ")
    }

    private val whiteSpace = Regex("\\s+")
}

/** The name of every key glyph, from [labels] or the built-in strings. */
@Composable
internal fun kbdGlyphNames(labels: MonarchKbdLabels): Map<Char, String> = mapOf(
    '⌘' to monarchString(labels.command, R.string.monarch_kbd_command),
    '⇧' to monarchString(labels.shift, R.string.monarch_kbd_shift),
    '⌥' to monarchString(labels.option, R.string.monarch_kbd_option),
    '⌃' to monarchString(labels.control, R.string.monarch_kbd_control),
    '↩' to monarchString(labels.returnKey, R.string.monarch_kbd_return),
    '⎋' to monarchString(labels.escape, R.string.monarch_kbd_escape),
    '⌫' to monarchString(labels.delete, R.string.monarch_kbd_delete),
    '⇥' to monarchString(labels.tab, R.string.monarch_kbd_tab),
    '␣' to monarchString(labels.space, R.string.monarch_kbd_space),
    '⇪' to monarchString(labels.capsLock, R.string.monarch_kbd_caps_lock),
    '↑' to monarchString(labels.upArrow, R.string.monarch_kbd_up_arrow),
    '↓' to monarchString(labels.downArrow, R.string.monarch_kbd_down_arrow),
    '←' to monarchString(labels.leftArrow, R.string.monarch_kbd_left_arrow),
    '→' to monarchString(labels.rightArrow, R.string.monarch_kbd_right_arrow),
)
