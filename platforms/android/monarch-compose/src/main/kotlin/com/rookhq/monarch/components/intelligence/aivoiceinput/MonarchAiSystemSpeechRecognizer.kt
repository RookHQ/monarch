package com.rookhq.monarch.components.intelligence.aivoiceinput

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.rookhq.monarch.ExperimentalMonarchApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

/**
 * The speech recogniser of the device, `android.speech.SpeechRecognizer`,
 * behind [MonarchAiSpeechRecognizer]. `MonarchAiVoiceInput` uses it unless it is
 * given another.
 *
 * The app must declare `android.permission.RECORD_AUDIO` in its manifest; the
 * voice input asks for it at run time. On Android 11 and later the app's
 * manifest must also list the recognition service in `<queries>`
 * (`<intent><action android:name="android.speech.RecognitionService" /></intent>`),
 * or [isAvailable] returns `false` and nothing is drawn.
 *
 * Which languages, how long a session may last and whether audio stays on the
 * device depend on the recognition service installed (usually the Google app):
 * [preferOffline] asks for on-device recognition where the language supports
 * it. The system recogniser stops after a pause; in continuous mode it is
 * restarted after each phrase until [stop].
 *
 * Create, collect and stop it on the main thread.
 *
 * Experimental: it compiles and its error mapping is unit tested, but it has
 * not yet been run against a device's recognition service. `MonarchAiVoiceInput`
 * itself is tested with scripted recognisers.
 *
 * @param context a context of the app, used to create the recogniser.
 * @param preferOffline asks the service to recognise on the device, without a network, where it can.
 */
@ExperimentalMonarchApi
public class MonarchAiSystemSpeechRecognizer(
    private val context: Context,
    private val preferOffline: Boolean = false,
) : MonarchAiSpeechRecognizer {
    private var current: SpeechRecognizer? = null
    private var stopping = false

    override val requiredPermissions: List<String> = listOf(Manifest.permission.RECORD_AUDIO)

    override fun isAvailable(locale: Locale?): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun start(locale: Locale?, continuous: Boolean): Flow<MonarchAiSpeechEvent> = callbackFlow {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        current = recognizer
        stopping = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            locale?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it.toLanguageTag()) }
        }
        var heard = false
        recognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    trySend(MonarchAiSpeechEvent.Started)
                }

                override fun onRmsChanged(rmsdB: Float) {
                    trySend(MonarchAiSpeechEvent.Level(MonarchAiVoiceInputTokens.levelOfRms(rmsdB)))
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    firstResult(partialResults)?.let { trySend(MonarchAiSpeechEvent.Partial(it)) }
                }

                override fun onResults(results: Bundle?) {
                    firstResult(results)?.let {
                        heard = true
                        trySend(MonarchAiSpeechEvent.Final(it))
                    }
                    if (continuous && !stopping) recognizer.startListening(intent) else close()
                }

                override fun onError(error: Int) {
                    val failure = MonarchAiVoiceInputTokens.speechError(error)
                    // A pause after some words in continuous mode, or an error after stop, ends normally.
                    val pauseAfterWords = continuous && heard && failure == MonarchAiSpeechError.NoSpeech
                    if (stopping || pauseAfterWords) {
                        close()
                    } else {
                        close(MonarchAiSpeechException(failure))
                    }
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Unit

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            },
        )
        recognizer.startListening(intent)
        awaitClose {
            recognizer.cancel()
            recognizer.destroy()
            if (current === recognizer) current = null
        }
    }

    override fun stop() {
        stopping = true
        current?.stopListening()
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }
}
