package com.rookhq.monarch.components.intelligence.aighosttext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.BreakIterator

/**
 * The state of an inline completion, without any UI: it debounces requests,
 * cancels the stale ones, never suggests while an input method is composing,
 * and accepts a suggestion whole or word by word.
 *
 * `MonarchAiGhostText` wires it to a text field. Keep one yourself with
 * [rememberMonarchAiCompletion] to read or drive it, or call [update] from your
 * own editor. Every property is snapshot state.
 *
 * @param scope where requests run. Requests are cancelled with it.
 * @param debounceMillis how long typing must pause before a request is sent.
 * @param minimumCharacters the fewest characters before the cursor, without surrounding white space, for a
 * request.
 * @param enabled whether suggestions are made. See [MonarchAiCompletion.enabled].
 * @param complete asks the model for a continuation of the text at the cursor, a UTF-16 offset. Return the text
 * to insert, or `null` or an empty string for none. It is cancelled when its answer is stale.
 */
@Stable
public class MonarchAiCompletion(
    private val scope: CoroutineScope,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
    private val minimumCharacters: Int = DEFAULT_MINIMUM_CHARACTERS,
    enabled: Boolean = true,
    private val complete: suspend (text: String, cursor: Int) -> String?,
) {
    /** Where the completion is. */
    public var status: MonarchAiCompletionStatus by mutableStateOf(MonarchAiCompletionStatus.Idle)
        private set

    /** The suggested continuation; empty when there is none. */
    public var suggestion: String by mutableStateOf("")
        private set

    /** The text the suggestion, or the pending request, belongs to. */
    public var text: String by mutableStateOf("")
        private set

    /** The cursor the suggestion goes in at, as a UTF-16 offset. */
    public var cursor: Int by mutableIntStateOf(0)
        private set

    private var enabledState by mutableStateOf(enabled)

    /** Whether suggestions are made. Setting it to `false` dismisses the current suggestion. */
    public var enabled: Boolean
        get() = enabledState
        set(value) {
            enabledState = value
            if (!value) dismiss()
        }

    /** Called when [complete] throws, with what it threw. Not called for a cancelled request. */
    public var onError: ((Throwable) -> Unit)? = null

    private var sequence = 0
    private var composing = false
    private var job: Job? = null

    /**
     * Tells the completion what the field holds now. It debounces a request,
     * keeps the rest of a suggestion the person is typing through, and does
     * nothing when neither value changed. While composing or disabled it only
     * records the text.
     *
     * @param text the whole text of the field.
     * @param cursor the collapsed cursor, as a UTF-16 offset into [text].
     */
    public fun update(text: String, cursor: Int) {
        val at = cursor.coerceIn(0, text.length)
        val unchanged = this.text == text && this.cursor == at
        when {
            !enabled || composing -> {
                cancel()
                if (status != MonarchAiCompletionStatus.Idle || !unchanged) {
                    commit(MonarchAiCompletionStatus.Idle, "", text, at)
                }
            }
            unchanged -> Unit
            keepsSuggestion(text, at) ->
                commit(MonarchAiCompletionStatus.Ready, suggestion.substring(at - this.cursor), text, at)
            text.substring(0, at).trim().length < maxOf(0, minimumCharacters) -> {
                cancel()
                commit(MonarchAiCompletionStatus.Idle, "", text, at)
            }
            else -> {
                cancel()
                commit(MonarchAiCompletionStatus.Waiting, "", text, at)
                request(sequence, text, at)
            }
        }
    }

    /** Drops the suggestion and cancels any request. Nothing is suggested again until the text changes. */
    public fun dismiss() {
        cancel()
        if (status != MonarchAiCompletionStatus.Idle) commit(MonarchAiCompletionStatus.Idle, "", text, cursor)
    }

    /** Accepts the whole suggestion. Returns what to insert, or `null` when there is none. */
    public fun accept(): MonarchAiCompletionAcceptance? = take(wordOnly = false)

    /** Accepts the next word of the suggestion and keeps the rest. Returns `null` when there is none. */
    public fun acceptWord(): MonarchAiCompletionAcceptance? = take(wordOnly = true)

    /** Call with `true` while an input method composes text and `false` when it is done. Composing dismisses. */
    public fun setComposing(composing: Boolean) {
        if (this.composing == composing) return
        this.composing = composing
        if (composing) dismiss()
    }

    private fun keepsSuggestion(next: String, at: Int): Boolean {
        if (status != MonarchAiCompletionStatus.Ready || at <= cursor) return false
        val typed = next.substring(cursor, at)
        val sameAround = next.regionMatches(0, text, 0, cursor) && next.substring(at) == text.substring(cursor)
        return sameAround && typed.length < suggestion.length && suggestion.startsWith(typed)
    }

    private fun take(wordOnly: Boolean): MonarchAiCompletionAcceptance? {
        if (status != MonarchAiCompletionStatus.Ready || suggestion.isEmpty()) return null
        val inserted = if (wordOnly) nextWord(suggestion) else suggestion
        val rest = suggestion.substring(inserted.length)
        val next = text.substring(0, cursor) + inserted + text.substring(cursor)
        val at = cursor + inserted.length
        commit(if (rest.isEmpty()) MonarchAiCompletionStatus.Idle else MonarchAiCompletionStatus.Ready, rest, next, at)
        return MonarchAiCompletionAcceptance(inserted, next, at)
    }

    private fun request(id: Int, text: String, at: Int) {
        job = scope.launch {
            delay(maxOf(0L, debounceMillis))
            if (id != sequence) return@launch
            commit(MonarchAiCompletionStatus.Loading, "", text, at)
            val result = try {
                Result.success(complete(text, at))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
                Result.failure(failure)
            }
            if (id != sequence) return@launch
            job = null
            result.fold(
                onSuccess = { answer ->
                    val shown = answer.orEmpty()
                    val next = if (shown.isEmpty()) MonarchAiCompletionStatus.Idle else MonarchAiCompletionStatus.Ready
                    commit(next, shown, text, at)
                },
                onFailure = { failure ->
                    commit(MonarchAiCompletionStatus.Error, "", text, at)
                    onError?.invoke(failure)
                },
            )
        }
    }

    private fun cancel() {
        sequence += 1
        job?.cancel()
        job = null
    }

    private fun commit(status: MonarchAiCompletionStatus, suggestion: String, text: String, cursor: Int) {
        this.status = status
        this.suggestion = suggestion
        this.text = text
        this.cursor = cursor
    }

    /** Defaults and the word rule. */
    public companion object {
        /** The default pause before a request, as on the web. */
        public const val DEFAULT_DEBOUNCE_MILLIS: Long = 300L

        /** The default fewest characters before a request, as on the web. */
        public const val DEFAULT_MINIMUM_CHARACTERS: Int = 3

        /**
         * The next word of [suggestion]: any leading white space, then one word with the punctuation around
         * it, so "word," never leaves a lone comma behind. Text without spaces is split at word boundaries.
         */
        public fun nextWord(suggestion: String): String {
            if (suggestion.isEmpty()) return ""
            val lead = suggestion.takeWhile { it.isWhitespace() }
            val run = suggestion.substring(lead.length).takeWhile { !it.isWhitespace() }
            if (run.isEmpty()) return suggestion
            val words = BreakIterator.getWordInstance().apply { setText(run) }
            var start = words.first()
            var end = words.next()
            val word = StringBuilder()
            while (end != BreakIterator.DONE) {
                val segment = run.substring(start, end)
                word.append(segment)
                if (segment.any { it.isLetterOrDigit() }) break
                start = end
                end = words.next()
            }
            val tail = run.substring(word.length).takeWhile { !it.isLetterOrDigit() }
            return lead + word + tail
        }
    }
}

/**
 * Remembers a [MonarchAiCompletion] whose requests run in the composition's scope and use the latest
 * [complete].
 *
 * @param debounceMillis how long typing must pause before a request is sent.
 * @param minimumCharacters the fewest characters before the cursor for a request.
 * @param enabled whether suggestions are made. Changing it to `false` dismisses the current one.
 * @param complete asks the model for a continuation of the text at the cursor. Return `null` for none.
 */
@Composable
public fun rememberMonarchAiCompletion(
    debounceMillis: Long = MonarchAiCompletion.DEFAULT_DEBOUNCE_MILLIS,
    minimumCharacters: Int = MonarchAiCompletion.DEFAULT_MINIMUM_CHARACTERS,
    enabled: Boolean = true,
    complete: suspend (text: String, cursor: Int) -> String?,
): MonarchAiCompletion {
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(complete)
    val completion = remember(scope, debounceMillis, minimumCharacters) {
        MonarchAiCompletion(scope, debounceMillis, minimumCharacters, enabled) { text, cursor -> latest(text, cursor) }
    }
    SideEffect { if (completion.enabled != enabled) completion.enabled = enabled }
    return completion
}
