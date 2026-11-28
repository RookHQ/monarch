package com.rookhq.monarch.components.collaboration.postcard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** The text colours of a post card. */
internal data class MonarchPostCardTextColors(val title: Color, val meta: Color)

/** Component tokens and logic of MonarchPostCard, built from primitives and roles only. */
internal object MonarchPostCardTokens {
    /** The space between the author line, the title, the excerpt and the footer. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The space between the parts of the author line and of the footer. */
    val InlineGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The excerpt: the body role at 14, under a title that is bigger than it. */
    fun excerptStyle(typography: MonarchTypography): TextStyle =
        typography.body.copy(fontSize = MonarchTokens.FontSize.sm.sp)

    /** A cut at a space is kept only past this share of the limit, as on the web. */
    private const val WORD_CUT_SHARE: Double = 0.6

    /**
     * Shortens [text] to at most [max] characters at a word boundary and adds
     * an ellipsis, as the web's truncateExcerpt does. Text that fits is
     * returned trimmed and unchanged; a single long word is cut at the limit.
     */
    fun truncateExcerpt(text: String, max: Int): String {
        val plain = text.trim()
        if (plain.length <= max) return plain
        val cut = plain.substring(0, max.coerceAtLeast(0))
        val space = cut.lastIndexOf(' ')
        val kept = if (space > max * WORD_CUT_SHARE) cut.substring(0, space) else cut
        return kept.trimEnd() + "…"
    }

    /** "3 hours ago" in the user's locale, by the shared Monarch rule (the web rule). */
    fun relativeTime(time: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(time, now, locale)

    /** The title in the default text colour; the author line, excerpt and counts in the muted text colour. */
    fun textColors(colors: MonarchColors): MonarchPostCardTextColors =
        MonarchPostCardTextColors(title = colors.textDefault, meta = colors.textMuted)
}
