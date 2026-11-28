package dev.monarchui.components.intelligence.aitoolcall

import androidx.annotation.StringRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTimestamp
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiToolCallState
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** A value ready to show: the text, and whether it is JSON. */
internal data class MonarchAiToolCallValue(val text: String, val json: Boolean)

/** Text cut for display, and whether anything was cut. */
internal data class MonarchAiToolCallCut(val text: String, val truncated: Boolean)

/** Component tokens and the pure logic of MonarchAiToolCall. */
internal object MonarchAiToolCallTokens {
    /** The padding inside the card. */
    val Padding: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of the header and between the blocks of the details. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the title and the state line. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The least height of the header, so it is a full touch target. */
    val HeaderMinHeight: Dp = MonarchTokens.Control.xl.dp

    /** The turn of the toggle icon while expanded. */
    const val EXPANDED_ROTATION: Float = 90f

    /** The longest value shown before Show all, by default. */
    const val DEFAULT_MAX_VALUE_LENGTH: Int = 4000

    private const val MILLIS_PER_SECOND = 1000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SHORTEST_MILLIS = 100L

    /** The card: the subtle background with the subtle border. */
    fun cardColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(colors.bgSubtle, colors.textDefault, colors.borderSubtle)

    /** The tone of the state badge. */
    fun tone(state: MonarchAiToolCallState): MonarchTone = when (state) {
        MonarchAiToolCallState.PendingApproval -> MonarchTone.Warning
        MonarchAiToolCallState.Running -> MonarchTone.Info
        MonarchAiToolCallState.Succeeded -> MonarchTone.Success
        MonarchAiToolCallState.Failed -> MonarchTone.Danger
        MonarchAiToolCallState.AwaitingResult,
        MonarchAiToolCallState.Denied,
        MonarchAiToolCallState.Cancelled,
        -> MonarchTone.Neutral
    }

    /** The built-in text of a state. */
    @StringRes
    fun stateText(state: MonarchAiToolCallState): Int = when (state) {
        MonarchAiToolCallState.PendingApproval -> R.string.monarch_aitoolcall_state_pending_approval
        MonarchAiToolCallState.AwaitingResult -> R.string.monarch_aitoolcall_state_awaiting_result
        MonarchAiToolCallState.Running -> R.string.monarch_aitoolcall_state_running
        MonarchAiToolCallState.Succeeded -> R.string.monarch_aitoolcall_state_succeeded
        MonarchAiToolCallState.Failed -> R.string.monarch_aitoolcall_state_failed
        MonarchAiToolCallState.Denied -> R.string.monarch_aitoolcall_state_denied
        MonarchAiToolCallState.Cancelled -> R.string.monarch_aitoolcall_state_cancelled
    }

    /** A tool name in words: `search_documents`, `searchDocuments` and `search-documents` give "Search documents". */
    fun humanize(name: String): String {
        val words = name
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .replace(Regex("[_\\-.:/]+"), " ")
            .trim()
            .lowercase()
        return if (words.isEmpty()) name else words.replaceFirstChar { it.uppercaseChar() }
    }

    /** The display name of a tool: its entry in [toolLabels], else the humanized name. */
    fun label(name: String, toolLabels: Map<String, String>): String = toolLabels[name] ?: humanize(name)

    /** Milliseconds between the start and the end, or `null` while either is missing, unreadable or reversed. */
    fun duration(part: MonarchAiToolCallPart): Long? {
        val start = part.startedAt?.let(MonarchAiTimestamp::millis) ?: return null
        val end = part.endedAt?.let(MonarchAiTimestamp::millis) ?: return null
        return (end - start).takeIf { it >= 0 }
    }

    /** The parts of a duration: tenths of a second under a second, else whole seconds and minutes. */
    fun durationParts(millis: Long): Triple<Double?, Long, Long> {
        if (millis < MILLIS_PER_SECOND) {
            return Triple(maxOf(millis, SHORTEST_MILLIS) / MILLIS_PER_SECOND.toDouble(), 0, 0)
        }
        val seconds = Math.round(millis / MILLIS_PER_SECOND.toDouble())
        return Triple(null, seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
    }

    /** The input shown: the raw streaming text while the parsed input is still empty, else the input. */
    fun input(part: MonarchAiToolCallPart): MonarchAiJSON? {
        val empty = part.input == MonarchAiJSON.Null ||
            (part.input as? MonarchAiJSON.Object)?.members?.isEmpty() == true
        val streaming = part.inputText?.takeIf { it.isNotEmpty() && empty }
        return when {
            streaming != null -> MonarchAiJSON.Text(streaming)
            part.input == MonarchAiJSON.Null -> null
            else -> part.input
        }
    }
}
