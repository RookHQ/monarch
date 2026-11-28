package dev.monarchui.components.intelligence.aicodeblock

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchAiCodeBlock. */
internal object MonarchAiCodeBlockTokens {
    /** The gap between the code block and the action row, and between the actions and the hint. */
    val SectionGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the buttons of the action row. */
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** The colour of the incomplete hint: muted text. */
    fun hintColor(colors: MonarchColors): Color = colors.textMuted

    /** Whether there is an action row: the app gave Apply, Insert or its own controls. */
    fun hasActions(onApply: Boolean, onInsert: Boolean, actions: Boolean): Boolean = onApply || onInsert || actions

    /** Whether Apply and Insert can be used: only when the code is complete. */
    fun actionsEnabled(incomplete: Boolean): Boolean = !incomplete

    /** Whether the hint shows: the code is incomplete and Apply or Insert is there to be disabled. */
    fun showsHint(incomplete: Boolean, onApply: Boolean, onInsert: Boolean): Boolean =
        incomplete && (onApply || onInsert)
}
