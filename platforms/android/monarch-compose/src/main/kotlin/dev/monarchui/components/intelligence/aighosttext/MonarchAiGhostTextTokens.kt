package dev.monarchui.components.intelligence.aighosttext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchAiGhostText, built from primitives and roles only. */
internal object MonarchAiGhostTextTokens {
    /** The room kept under the field for the accept button: a full touch target. */
    val AcceptRowHeight: Dp = MonarchTokens.Space.space12.dp

    /** The margin around the suggestion that still counts as a tap on it. */
    val TapSlop: Dp = MonarchTokens.Space.space1.dp

    /** The colour of the suggestion after the caret. */
    fun suggestionColor(colors: MonarchColors): Color = colors.textMuted

    /**
     * Whether the field is where a suggestion belongs: one caret, with only white space after it. A
     * suggestion in the middle of the text would cover what follows.
     */
    fun caretAtEnd(text: CharSequence, selection: TextRange): Boolean =
        selection.collapsed && text.subSequence(selection.start, text.length).isBlank()
}
