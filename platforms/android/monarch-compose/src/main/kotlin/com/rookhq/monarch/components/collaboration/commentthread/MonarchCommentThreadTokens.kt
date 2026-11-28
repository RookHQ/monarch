package com.rookhq.monarch.components.collaboration.commentthread

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and pure logic of MonarchCommentThread, built from primitives and roles only. */
internal object MonarchCommentThreadTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val Padding: Dp = MonarchTokens.Space.space2.dp
    val QuoteBarWidth: Dp = MonarchTokens.Space.space0_5.dp
    val QuoteGap: Dp = MonarchTokens.Space.space2.dp
    val InsetStart: Dp = MonarchTokens.Space.space3.dp

    /** The replies shown before older ones fold, as on the web. */
    const val DEFAULT_VISIBLE_REPLIES: Int = 3

    /**
     * How many replies are folded behind "Show N earlier replies": none when
     * [expanded], or when folding would hide a single reply; otherwise all
     * but the latest [visibleReplies]. A negative [visibleReplies] counts as 0.
     */
    fun folded(replyCount: Int, visibleReplies: Int, expanded: Boolean): Int {
        val keep = visibleReplies.coerceAtLeast(0)
        return if (expanded || replyCount <= keep + 1) 0 else replyCount - keep
    }

    /** Whether the reply field is shown: the app takes replies and the thread is open. */
    fun canReply(takesReplies: Boolean, resolved: Boolean): Boolean = takesReplies && !resolved

    /** The thread's fill: the subtle background when resolved, none while open. */
    fun background(resolved: Boolean, colors: MonarchColors): Color =
        if (resolved) colors.bgSubtle else Color.Transparent

    /** The bar along the start edge of the quote. */
    fun quoteBarColor(colors: MonarchColors): Color = colors.borderStrong

    /** The colour of the quote and the resolved hint. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the failure text. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong
}
