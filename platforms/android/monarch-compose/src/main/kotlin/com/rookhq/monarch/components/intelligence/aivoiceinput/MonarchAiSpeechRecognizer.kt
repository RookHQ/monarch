package com.rookhq.monarch.components.intelligence.aivoiceinput

import kotlinx.coroutines.flow.Flow
import java.util.Locale

/**
 * A speech recogniser that `MonarchAiVoiceInput` listens with.
 *
 * Monarch ships [MonarchAiSystemSpeechRecognizer], built on
 * `android.speech.SpeechRecognizer`. Implement this interface to use another
 * engine: a recogniser on your server, an on-device model, or a scripted one in
 * previews and tests.
 */
public interface MonarchAiSpeechRecognizer {
    /**
     * The runtime permissions the recogniser needs, such as `android.permission.RECORD_AUDIO`.
     * `MonarchAiVoiceInput` asks for the missing ones before it starts. None by default.
     */
    public val requiredPermissions: List<String> get() = emptyList()

    /**
     * Whether recognition can run for a language on this device. When it returns `false` the voice input draws
     * nothing.
     *
     * @param locale the language of the speech, or `null` for the device language.
     */
    public fun isAvailable(locale: Locale?): Boolean

    /**
     * The events of one listening session. Collecting the flow opens the
     * microphone and starts recognising. The flow completes after [stop] once
     * the last words are in, and throws a [MonarchAiSpeechException] when it
     * fails. When the collector is cancelled, stop at once and discard what is
     * pending.
     *
     * @param locale the language of the speech, or `null` for the device language.
     * @param continuous `true` for dictation that runs until stopped, `false` for push-to-talk.
     */
    public fun start(locale: Locale?, continuous: Boolean): Flow<MonarchAiSpeechEvent>

    /** Stops listening. The flow delivers the last words and completes. */
    public fun stop()
}
