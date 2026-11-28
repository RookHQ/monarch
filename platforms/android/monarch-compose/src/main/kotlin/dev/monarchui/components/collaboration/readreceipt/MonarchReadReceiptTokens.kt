package dev.monarchui.components.collaboration.readreceipt

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and logic of MonarchReadReceipt, built from primitives and roles only. */
internal object MonarchReadReceiptTokens {
    /** The space between the mark or the avatars and the text. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The smallest number of avatars shown. */
    const val MIN_AVATARS: Int = 1

    /** The text and the mark: the muted text colour, in every theme. */
    fun contentColor(colors: MonarchColors): Color = colors.textMuted

    /** How many avatars are shown: at least one, and never a "+1" tile instead of the face it hides. */
    fun avatarLimit(maxAvatars: Int): Int = maxAvatars.coerceAtLeast(MIN_AVATARS)

    /**
     * Joins [names] in the English way of the web: "Ana", "Ana and Ben",
     * "Ana, Ben and Chloé". [separator] goes between all but the last two,
     * [lastPair] joins the last two.
     */
    fun joinNames(names: List<String>, separator: String, lastPair: (String, String) -> String): String =
        when (names.size) {
            0 -> ""
            1 -> names.single()
            else -> lastPair(names.dropLast(1).joinToString(separator), names.last())
        }
}
