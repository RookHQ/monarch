package com.rookhq.monarch.components.intelligence.aithread

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationMessage
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiConversationStatus
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiRole
import kotlinx.coroutines.Job

/** What the announcer adds after a finished reply. */
internal enum class AiThreadEnding { Complete, Stopped }

/** How much of the newest reply has been read out. [live] is false for a reply first seen finished. */
internal data class AiThreadSpeechState(val id: String, val length: Int, val live: Boolean)

/** One step of the announcer: its new state, the text to queue, and the ending when the reply finished. */
internal data class AiThreadSpeechStep(
    val state: AiThreadSpeechState?,
    val text: String,
    val finished: Boolean = false,
    val ending: AiThreadEnding? = null,
)

/** The strings the announcer reads, resolved by the thread. */
internal class AiThreadAnnouncerTexts(
    val thinking: String,
    val responseComplete: String,
    val responseStopped: String,
    val codeBlock: String,
)

/** The announcer's memory between changes, outside of state: read and written in effects only. */
internal class AiThreadAnnouncerMemory {
    var speech: AiThreadSpeechState? = null
    var queue: String = ""
    var cooling: Job? = null
}

/** The pure logic of what a MonarchAiThread reads out while a reply streams. */
internal object AiThreadSpeech {
    private val Fence = Regex("```[\\s\\S]*?(```|$)")
    private val Heading = Regex("(?m)^#{1,6}\\s+")
    private val Emphasis = Regex("\\*\\*|__|`")
    private val Boundary = Regex("[.!?…。！？](?=[\"'”’)\\]]*\\s)|\\n")
    private val Space = Regex("\\s+")

    /**
     * The text of a reply as it is read out: fenced code blocks are named by
     * [codeBlock], and heading marks, bold marks and backticks are dropped.
     */
    fun spoken(text: String, codeBlock: String): String = text
        .replace(Fence, "\n$codeBlock.\n")
        .replace(Heading, "")
        .replace(Emphasis, "")

    /** The end of the last sentence or line in [text] after [from], or `-1` when none ends there. */
    fun lastBoundary(text: String, from: Int): Int {
        var end = -1
        var match = Boundary.find(text, from.coerceIn(0, text.length))
        while (match != null) {
            end = match.range.last + 1
            match = match.next()
        }
        return end
    }

    /** Text with its runs of white space made single spaces and trimmed. */
    fun tidy(text: String): String = text.replace(Space, " ").trim()

    /**
     * The next step of the announcer, as on the web: only a reply that
     * arrives while the thread is shown is read out, in whole sentences while
     * it streams, then the rest and "Response complete" or "Response stopped"
     * once it finishes. A reply first seen finished (history, a branch switch)
     * is not read. Nothing ends while the reply waits for a tool.
     */
    fun speak(
        previous: AiThreadSpeechState?,
        last: MonarchAiConversationMessage?,
        spokenText: String,
        status: MonarchAiConversationStatus,
    ): AiThreadSpeechStep {
        val state = if (last == null || last.role != MonarchAiRole.Assistant) {
            null
        } else {
            current(previous, last, spokenText, status)
        }
        return when {
            state == null -> AiThreadSpeechStep(previous, "")
            !state.live || last == null -> AiThreadSpeechStep(state, "")
            last.streaming -> streamed(state, spokenText)
            else -> settled(state, spokenText, status)
        }
    }

    /** The state for [last]: kept for the same reply, new for another one, live if it is still arriving. */
    private fun current(
        previous: AiThreadSpeechState?,
        last: MonarchAiConversationMessage,
        spokenText: String,
        status: MonarchAiConversationStatus,
    ): AiThreadSpeechState {
        if (previous != null && previous.id == last.id) return previous
        val live = last.streaming || MonarchAiThreadTokens.busy(status)
        return AiThreadSpeechState(last.id, if (live) 0 else spokenText.length, live)
    }

    /** While the reply streams: the whole sentences that arrived since the last step. */
    private fun streamed(state: AiThreadSpeechState, spokenText: String): AiThreadSpeechStep {
        val start = state.length.coerceAtMost(spokenText.length)
        val end = lastBoundary(spokenText, start)
        return if (end > start) {
            AiThreadSpeechStep(state.copy(length = end), spokenText.substring(start, end))
        } else {
            AiThreadSpeechStep(state, "")
        }
    }

    /** Once the reply stopped arriving: nothing while it runs or waits for a tool, else the rest and the end. */
    private fun settled(
        state: AiThreadSpeechState,
        spokenText: String,
        status: MonarchAiConversationStatus,
    ): AiThreadSpeechStep {
        val waiting = MonarchAiThreadTokens.busy(status) ||
            status == MonarchAiConversationStatus.AwaitingApproval ||
            status == MonarchAiConversationStatus.AwaitingToolResult
        if (waiting) return AiThreadSpeechStep(state, "")
        val ending = when (status) {
            MonarchAiConversationStatus.Error -> null
            MonarchAiConversationStatus.Stopped -> AiThreadEnding.Stopped
            else -> AiThreadEnding.Complete
        }
        return AiThreadSpeechStep(
            state = state.copy(length = spokenText.length, live = false),
            text = spokenText.substring(state.length.coerceAtMost(spokenText.length)),
            finished = true,
            ending = ending,
        )
    }
}
