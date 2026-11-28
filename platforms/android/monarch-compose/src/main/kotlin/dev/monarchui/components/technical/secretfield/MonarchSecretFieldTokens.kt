package dev.monarchui.components.technical.secretfield

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens of MonarchSecretField, built from primitives and roles only. */
internal object MonarchSecretFieldTokens {
    /** Space at the start and end of the text of the reveal toggle. */
    val TogglePadding: Dp = MonarchTokens.Space.space2.dp

    /** The space after the copy button, before the edge of the field. */
    val EndPadding: Dp = MonarchTokens.Space.space1.dp

    /** The colour of the text of the reveal toggle. */
    fun toggleColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.accentStrong else colors.textDisabled

    /** Whether reveal and copy have something to act on: a value the user can see or copy. */
    fun actionsEnabled(enabled: Boolean, value: String): Boolean = enabled && value.isNotEmpty()

    /** Whether the "saved" placeholder and description apply: a saved secret and nothing typed yet. */
    fun showsSaved(saved: Boolean, value: String): Boolean = saved && value.isEmpty()
}
