package dev.monarchui.components.intelligence.aistreamingtext

/**
 * What a streaming answer announces and when: whole sentences and paragraphs,
 * never single tokens, at most once per interval, then "Response complete".
 * A port of the web's announcement logic, kept free of Compose and of the
 * clock so it can be tested with explicit times.
 */
internal class MonarchAiStreamingTextAnnouncer(private val intervalMillis: Long = MIN_INTERVAL_MILLIS) {
    /**
     * What to say now and how long to wait before asking again (`null` for never).
     *
     * @property chunk the new sentences, or `null`.
     * @property complete whether "Response complete" follows them.
     */
    class Step(val chunk: String?, val complete: Boolean, val retryAfterMillis: Long?) {
        /** The announcement, with [completeText] for "Response complete"; `null` for nothing. */
        fun say(completeText: String): String? =
            listOfNotNull(chunk, completeText.takeIf { complete }).joinToString("\n\n").ifEmpty { null }
    }

    private var announced = 0
    private var prefix = ""
    private var lastMillis: Long? = null
    private var wasStreaming = false

    /** The text finished while the reply went on; "Response complete" is still owed. */
    private var owesComplete = false

    /**
     * Works out the next announcement.
     *
     * @param spoken the spoken text so far: finished blocks, then the block still arriving.
     * @param done the length of the part of [spoken] whose blocks are finished.
     * @param streaming whether more text is arriving.
     * @param announceComplete whether "Response complete" may be said when the text stops.
     * @param nowMillis the current time.
     */
    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    fun step(
        spoken: String,
        done: Int,
        streaming: Boolean,
        announceComplete: Boolean,
        nowMillis: Long,
    ): Step {
        if (streaming) {
            wasStreaming = true
            owesComplete = false
        }
        // The reply finished after this text did: only "Response complete" is left to say.
        if (!streaming && !wasStreaming && owesComplete) {
            if (!announceComplete) return Nothing
            owesComplete = false
            return Step(null, complete = true, retryAfterMillis = null)
        }
        // Text that was never streamed here (history) is not announced.
        if (!streaming && !wasStreaming) return Nothing
        // A different answer (regenerate, reset): start over.
        if (!spoken.startsWith(prefix)) announced = 0
        val boundary = if (streaming) sentenceBoundary(spoken, done) else spoken.length
        val finished = !streaming && wasStreaming
        val chunk = if (boundary > announced) spoken.substring(announced, boundary).trim() else ""
        if (chunk.isEmpty() && !finished) return Nothing
        val last = lastMillis
        if (last != null) {
            val wait = last + intervalMillis - nowMillis
            if (wait > 0) return Step(null, complete = false, retryAfterMillis = wait)
        }
        announced = maxOf(announced, boundary)
        prefix = spoken.substring(0, announced)
        lastMillis = nowMillis
        if (finished) {
            wasStreaming = false
            owesComplete = !announceComplete
        }
        return Step(chunk.ifEmpty { null }, complete = finished && announceComplete, retryAfterMillis = null)
    }

    internal companion object {
        /** The shortest time between two announcements. */
        const val MIN_INTERVAL_MILLIS: Long = 1_000L

        private val Nothing = Step(null, complete = false, retryAfterMillis = null)

        /** A full stop, question or exclamation mark (also CJK) with closing quotes or brackets, before a space. */
        private val SENTENCE_END = Regex("[.!?…。！？][”’\"')\\]]*(?=\\s)")

        /** The end of the last whole sentence in the block still arriving, or [done] when it has none. */
        fun sentenceBoundary(spoken: String, done: Int): Int {
            var boundary = done
            if (done > spoken.length) return spoken.length
            for (m in SENTENCE_END.findAll(spoken, done)) boundary = m.range.last + 1
            return boundary
        }
    }
}
