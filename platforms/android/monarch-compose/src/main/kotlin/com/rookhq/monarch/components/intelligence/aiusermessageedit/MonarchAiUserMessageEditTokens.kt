package com.rookhq.monarch.components.intelligence.aiusermessageedit

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchAiUserMessageEdit, built from primitives only. */
internal object MonarchAiUserMessageEditTokens {
    /** The gap between the field and the buttons, and between the buttons. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The fewest lines of the field: it starts two lines high, as the web's 64px minimum. */
    const val MIN_LINES: Int = 2

    /** The lines of the field before it scrolls, at least [MIN_LINES]. */
    fun maxLines(requested: Int): Int = maxOf(MIN_LINES, requested)

    /** The text to resend: [text] trimmed, or `null` when it is empty or the same as the trimmed [original]. */
    fun submission(text: String, original: String): String? {
        val next = text.trim()
        return next.takeIf { it.isNotEmpty() && it != original.trim() }
    }
}
