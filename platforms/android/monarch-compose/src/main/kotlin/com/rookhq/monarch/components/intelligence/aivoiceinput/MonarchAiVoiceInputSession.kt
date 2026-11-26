package com.rookhq.monarch.components.intelligence.aivoiceinput

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/** The texts the session announces, resolved by the composable. */
internal class MonarchAiVoiceInputAnnouncements(val listening: String, val stopped: String)

/**
 * The dictation session behind `MonarchAiVoiceInput`: it collects a
 * recogniser's events and turns them into state, a transcript and
 * announcements. Separate from the composable so it is tested without one.
 */
internal class MonarchAiVoiceInputSession {
    /** Where dictation is. */
    var state: MonarchAiVoiceInputState by mutableStateOf(MonarchAiVoiceInputState.Idle)
        private set

    /** The live transcript of this session. */
    var preview: String by mutableStateOf("")
        private set

    /** The input level, 0 through 1. */
    var level: Float by mutableFloatStateOf(0f)
        private set

    /** Why the last session failed, or `null`. */
    var failure: MonarchAiSpeechError? by mutableStateOf(null)
        private set

    /** The text of the polite live region: "Listening…" or "Stopped listening.", empty before the first session. */
    var announcement: String by mutableStateOf("")
        private set

    /** Receives recognised text: guesses with `false`, settled phrases with `true`. */
    var onTranscript: (String, Boolean) -> Unit = { _, _ -> }

    /** Receives every state change. */
    var onStateChange: ((MonarchAiVoiceInputState) -> Unit)? = null

    /** The texts announced on start and on stop. */
    var announcements: MonarchAiVoiceInputAnnouncements = MonarchAiVoiceInputAnnouncements("", "")

    private var recognizer: MonarchAiSpeechRecognizer? = null
    private var job: Job? = null
    private var committed = ""
    private var session = 0

    /** Whether a session is starting or listening. */
    val active: Boolean get() = MonarchAiVoiceInputTokens.active(state)

    /** Marks the session as starting while the permission is asked for. */
    fun awaitPermission() {
        if (active) return
        session++
        reset()
        moveTo(MonarchAiVoiceInputState.Starting)
    }

    /** Fails the session because a permission was refused. Ignored if the person stopped it meanwhile. */
    fun deny() {
        if (state != MonarchAiVoiceInputState.Starting || job != null) return
        fail(MonarchAiSpeechError.Denied)
    }

    /**
     * Starts a session with [recognizer] in [scope], unless one is listening.
     * A session that is waiting for a permission continues.
     */
    fun start(scope: CoroutineScope, recognizer: MonarchAiSpeechRecognizer, locale: Locale?, continuous: Boolean) {
        if (job != null) return
        if (state != MonarchAiVoiceInputState.Starting) {
            session++
            reset()
        }
        val id = session
        this.recognizer = recognizer
        moveTo(MonarchAiVoiceInputState.Starting)
        announcement = announcements.listening
        job = scope.launch {
            try {
                recognizer.start(locale, continuous).collect { event -> if (id == session) handle(event) }
                finish(null, id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught") failed: Exception) {
                finish(failed, id)
            }
        }
    }

    /** Asks the recogniser to stop; the session ends when its last words are in. While waiting for a permission it
     * ends at once. */
    fun stop() {
        if (!active) return
        if (job == null) {
            session++
            moveTo(MonarchAiVoiceInputState.Idle)
            return
        }
        recognizer?.stop()
    }

    /** Ends the session at once and discards pending words, for example when the composable leaves. */
    fun cancel() {
        session++
        job?.cancel()
        job = null
        recognizer = null
        level = 0f
        if (active) moveTo(MonarchAiVoiceInputState.Idle)
    }

    /** Applies one event of the current session. */
    fun handle(event: MonarchAiSpeechEvent) {
        when (event) {
            MonarchAiSpeechEvent.Started -> moveTo(MonarchAiVoiceInputState.Listening)
            is MonarchAiSpeechEvent.Level -> level = MonarchAiVoiceInputTokens.clampedLevel(event.value)
            is MonarchAiSpeechEvent.Partial -> {
                moveTo(MonarchAiVoiceInputState.Listening)
                val text = event.text.trim()
                if (text.isEmpty()) return
                preview = MonarchAiVoiceInputTokens.preview(committed, text)
                onTranscript(text, false)
            }
            is MonarchAiSpeechEvent.Final -> {
                moveTo(MonarchAiVoiceInputState.Listening)
                val text = event.text.trim()
                if (text.isEmpty()) return
                committed = MonarchAiVoiceInputTokens.preview(committed, text)
                preview = committed
                onTranscript(text, true)
            }
        }
    }

    private fun finish(error: Exception?, id: Int) {
        if (id != session) return
        job = null
        recognizer = null
        level = 0f
        if (error == null) {
            if (active) {
                moveTo(MonarchAiVoiceInputState.Idle)
                announcement = announcements.stopped
            }
        } else {
            fail((error as? MonarchAiSpeechException)?.error ?: MonarchAiSpeechError.Failed)
        }
    }

    private fun fail(error: MonarchAiSpeechError) {
        failure = error
        moveTo(MonarchAiVoiceInputTokens.state(error))
    }

    private fun reset() {
        committed = ""
        preview = ""
        failure = null
        level = 0f
    }

    private fun moveTo(next: MonarchAiVoiceInputState) {
        if (state == next) return
        state = next
        onStateChange?.invoke(next)
    }
}
