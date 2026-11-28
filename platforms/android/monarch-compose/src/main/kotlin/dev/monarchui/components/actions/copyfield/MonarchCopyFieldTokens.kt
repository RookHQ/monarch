package dev.monarchui.components.actions.copyfield

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchCopyField. */
internal object MonarchCopyFieldTokens {
    /** The space between the field and the copy button beside it. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The most of the row the button may take: a long label or a large font wraps its text instead. */
    const val BUTTON_MAX_SHARE: Float = 0.5f

    /** The text style of the value: the code style for keys and hostnames, else the body style. */
    fun textStyle(monospace: Boolean, typography: MonarchTypography): TextStyle =
        if (monospace) typography.code else typography.body
}
