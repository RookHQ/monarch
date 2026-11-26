package com.rookhq.monarch.components.intelligence.aimessage

import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiFinishReason
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiRole
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiTextPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallState
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.currentIcuTimeZone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Date
import java.util.Locale

/** Component tokens and the pure logic of MonarchAiMessage. */
internal object MonarchAiMessageTokens {
    /** The gap between the header, the body and the footer. */
    val SectionGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the parts of the body. */
    val PartGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap inside the header and the footer. */
    val InlineGap: Dp = MonarchTokens.Space.space2.dp

    /** The horizontal padding of the person's bubble. */
    val BubblePaddingHorizontal: Dp = MonarchTokens.Space.space4.dp

    /** The vertical padding of the person's bubble. */
    val BubblePaddingVertical: Dp = MonarchTokens.Space.space3.dp

    /** The widest the person's bubble grows, as a share of the message width, as on the web. */
    const val BUBBLE_MAX_FRACTION: Float = 0.85f

    private val Running = setOf(
        MonarchAiToolCallState.Running,
        MonarchAiToolCallState.PendingApproval,
        MonarchAiToolCallState.AwaitingResult,
    )

    private val Notices = setOf(
        MonarchAiFinishReason.Length,
        MonarchAiFinishReason.ContentFilter,
        MonarchAiFinishReason.Refusal,
    )

    /** The fill and text of the person's bubble: the muted background, as on the web. */
    fun bubble(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.bgMuted, content = colors.textDefault, border = null)

    /** The colour of the agent's role, the time and the queued note. */
    fun metaColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of the finish notice. */
    fun noticeColor(colors: MonarchColors): Color = colors.warningStrong

    /** Whether the reply as a whole is finished: no text streams and no tool call is still going. */
    fun replyDone(message: MonarchAiConversationMessage): Boolean =
        !message.streaming && message.toolCalls.none { it.state in Running }

    /** The index of the last text part, or -1. Only it may announce "Response complete". */
    fun lastTextIndex(parts: List<MonarchAiPart>): Int = parts.indexOfLast { it is MonarchAiTextPart }

    /** The finish reason that gets a notice under a finished assistant reply, or `null`. */
    fun finishNotice(message: MonarchAiConversationMessage): MonarchAiFinishReason? =
        message.finishReason?.takeIf {
            message.role == MonarchAiRole.Assistant && it in Notices && replyDone(message)
        }

    /** Whether the person's message is still waiting to be sent. */
    fun queued(message: MonarchAiConversationMessage): Boolean =
        message.role == MonarchAiRole.User && message.queued

    /**
     * The time a message was created, "9:05 AM" or "09:05", in [timeZone] (an IANA id; `null` is the device's
     * zone).
     */
    fun time(millis: Long, locale: Locale, timeZone: String?, is24Hour: Boolean): String {
        val format = DateFormat.getInstanceForSkeleton(if (is24Hour) TIME_24 else TIME_12, locale)
        format.timeZone = timeZone?.let { TimeZone.getTimeZone(it) } ?: currentIcuTimeZone()
        return format.format(Date(millis))
    }

    private const val TIME_12 = "hmm"
    private const val TIME_24 = "Hmm"
}
