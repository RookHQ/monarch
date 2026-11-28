package com.rookhq.monarch.components.identity.mystatus

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** What the line under the name shows. */
internal enum class MonarchMyStatusLineKind { Custom, Presence, Invitation }

/** The line under the name and what it is. */
internal data class MonarchMyStatusLine(val text: String, val kind: MonarchMyStatusLineKind)

/** Component tokens and the pure logic of MonarchMyStatus. */
internal object MonarchMyStatusTokens {
    /** The padding inside the row. */
    val Padding: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the avatar and the text. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The status, unless it is blank or its until time has passed at [now]. */
    fun activeStatus(status: MonarchMyStatusCustomStatus?, now: Long): MonarchMyStatusCustomStatus? {
        if (status == null) return null
        val blank = status.text.isNullOrBlank() && status.emoji.isNullOrBlank()
        val expired = status.until != null && status.until <= now
        return if (blank || expired) null else status
    }

    /** The line: the emoji and message, else a presence other than online and offline, else the invitation. */
    fun line(
        active: MonarchMyStatusCustomStatus?,
        presence: MonarchPresenceStatus,
        presenceText: String,
        setStatus: String,
    ): MonarchMyStatusLine {
        if (active != null) {
            val parts = listOfNotNull(active.emoji, active.text).map { it.trim() }
            val text = parts.filter { it.isNotEmpty() }.joinToString(" ")
            return MonarchMyStatusLine(text, MonarchMyStatusLineKind.Custom)
        }
        val writesPresence = presence != MonarchPresenceStatus.Online && presence != MonarchPresenceStatus.Offline
        return if (writesPresence) {
            MonarchMyStatusLine(presenceText, MonarchMyStatusLineKind.Presence)
        } else {
            MonarchMyStatusLine(setStatus, MonarchMyStatusLineKind.Invitation)
        }
    }

    /** What TalkBack reads after the name: the presence, then the line unless the line is the presence. */
    fun spokenState(line: MonarchMyStatusLine, presenceText: String): String =
        if (line.kind == MonarchMyStatusLineKind.Presence) presenceText else "$presenceText, ${line.text}"

    /** How long to wait before the status ends, or `null` when nothing is due. */
    fun waitMillis(status: MonarchMyStatusCustomStatus?, now: Long): Long? {
        val until = status?.until ?: return null
        return (until - now).takeIf { it > 0 }
    }

    /** The colour of the line: muted for a status or presence, subtle for the invitation. */
    fun lineColor(kind: MonarchMyStatusLineKind, colors: MonarchColors): Color =
        if (kind == MonarchMyStatusLineKind.Invitation) colors.textSubtle else colors.textMuted
}
