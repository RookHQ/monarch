package dev.monarchui.components.intelligence.aiactionlog

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Where an undo stands for one entry. */
internal enum class MonarchAiActionLogUndo { Pending, Done, Failed }

/** Component tokens and pure logic of MonarchAiActionLog, built from primitives and roles only. */
internal object MonarchAiActionLogTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val EntryGap: Dp = MonarchTokens.Space.space3.dp
    val EntryPadding: Dp = MonarchTokens.Space.space2.dp
    val LineGap: Dp = MonarchTokens.Space.space1.dp


    /** The entry as a sentence without the actor kind: "Research agent added a summary to Quarterly report". */
    fun description(entry: MonarchAiActionLogEntry): String =
        listOfNotNull(entry.agent.name, entry.action, entry.target?.takeIf { it.isNotBlank() }).joinToString(" ")

    /** What TalkBack reads for the entry: "Research agent (AI agent) added a summary to Quarterly report". */
    fun spoken(entry: MonarchAiActionLogEntry, actorKind: String): String =
        listOfNotNull("${entry.agent.name} ($actorKind)", entry.action, entry.target?.takeIf { it.isNotBlank() })
            .joinToString(" ")

    /** The target's URL when it is safe to open: http or https only. */
    fun safeUrl(url: String?): String? = url?.trim()?.takeIf {
        it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
    }

    /** At least one entry per page. */
    fun step(pageSize: Int): Int = pageSize.coerceAtLeast(1)

    /** Whether Undo is offered: the entry can be undone, there is a handler, and it is not undone yet. */
    fun canUndo(entry: MonarchAiActionLogEntry, hasHandler: Boolean, state: MonarchAiActionLogUndo?): Boolean =
        entry.undoable && hasHandler && !undone(entry, state)

    fun undone(entry: MonarchAiActionLogEntry, state: MonarchAiActionLogUndo?): Boolean =
        entry.undone || state == MonarchAiActionLogUndo.Done

    /** [time] relative to [now] in [locale], by the shared Monarch rule (the web rule): "20 minutes ago". */
    fun relativeTime(time: Long, now: Long, locale: Locale): String = MonarchRelativeTime.format(time, now, locale)
}

/** The colour roles of MonarchAiActionLog. */
internal object MonarchAiActionLogColors {
    fun nameColor(colors: MonarchColors): Color = colors.textDefault

    /** The action text: muted once undone. */
    fun actionColor(undone: Boolean, colors: MonarchColors): Color =
        if (undone) colors.textMuted else colors.textDefault

    fun metaColor(colors: MonarchColors): Color = colors.textMuted

    fun linkColor(colors: MonarchColors): Color = colors.textLink

    fun failureColor(colors: MonarchColors): Color = colors.dangerStrong
}
