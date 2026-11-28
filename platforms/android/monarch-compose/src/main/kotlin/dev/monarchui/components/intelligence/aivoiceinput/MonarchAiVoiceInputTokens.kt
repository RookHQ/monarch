package dev.monarchui.components.intelligence.aivoiceinput

import android.speech.SpeechRecognizer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchAiVoiceInput. */
internal object MonarchAiVoiceInputTokens {
    /** The gap between the button, the level meter and the status. */
    val ControlGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the controls, the transcript and the notice. */
    val StackGap: Dp = MonarchTokens.Space.space2.dp

    /** The number of segments of the level meter, as on the web. */
    const val METER_SEGMENTS: Int = 5

    /** Whether the state is a listening session: starting or listening. */
    fun active(state: MonarchAiVoiceInputState): Boolean =
        state == MonarchAiVoiceInputState.Starting || state == MonarchAiVoiceInputState.Listening

    /** The button variant: primary while a session runs, secondary otherwise, as the web's pressed state. */
    fun buttonVariant(active: Boolean): MonarchButtonVariant =
        if (active) MonarchButtonVariant.Primary else MonarchButtonVariant.Secondary

    /** The state a failure leads to. */
    fun state(error: MonarchAiSpeechError): MonarchAiVoiceInputState =
        if (error == MonarchAiSpeechError.Denied) MonarchAiVoiceInputState.Denied else MonarchAiVoiceInputState.Error

    /** The tone of the notice for a failure. The notice also has the tone's spoken name, so it is not colour alone. */
    fun tone(error: MonarchAiSpeechError): MonarchTone = when (error) {
        MonarchAiSpeechError.NoSpeech -> MonarchTone.Info
        MonarchAiSpeechError.Denied, MonarchAiSpeechError.NoMicrophone, MonarchAiSpeechError.Unavailable ->
            MonarchTone.Warning
        MonarchAiSpeechError.Failed -> MonarchTone.Danger
    }

    /** Whether the notice offers to open the app's settings: only when access is off. */
    fun offersSettings(error: MonarchAiSpeechError): Boolean = error == MonarchAiSpeechError.Denied

    /** The live transcript: the settled words, then the current guess. */
    fun preview(committed: String, interim: String): String {
        val guess = interim.trim()
        return when {
            committed.isEmpty() -> guess
            guess.isEmpty() -> committed
            else -> "$committed $guess"
        }
    }

    /** The level limited to 0 through 1. Not a number gives 0. */
    fun clampedLevel(level: Float): Float = if (level.isNaN()) 0f else level.coerceIn(0f, 1f)

    /**
     * The 0 to 1 level of an `onRmsChanged` value. Recognisers report roughly
     * -2dB in silence to 10dB for loud speech.
     */
    fun levelOfRms(rmsDb: Float): Float = clampedLevel((rmsDb - RMS_FLOOR) / (RMS_CEILING - RMS_FLOOR))

    /** The failure of an `android.speech.SpeechRecognizer` error code. */
    fun speechError(code: Int): MonarchAiSpeechError = when (code) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> MonarchAiSpeechError.Denied
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT, SpeechRecognizer.ERROR_NO_MATCH -> MonarchAiSpeechError.NoSpeech
        SpeechRecognizer.ERROR_AUDIO -> MonarchAiSpeechError.NoMicrophone
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
        ERROR_TOO_MANY_REQUESTS,
        ERROR_SERVER_DISCONNECTED,
        ERROR_LANGUAGE_NOT_SUPPORTED,
        ERROR_LANGUAGE_UNAVAILABLE,
        -> MonarchAiSpeechError.Unavailable
        else -> MonarchAiSpeechError.Failed
    }

    private const val RMS_FLOOR = -2f
    private const val RMS_CEILING = 10f

    // The codes added in API 31, written out so they compile clean on minSdk 24.
    private const val ERROR_TOO_MANY_REQUESTS = 10
    private const val ERROR_SERVER_DISCONNECTED = 11
    private const val ERROR_LANGUAGE_NOT_SUPPORTED = 12
    private const val ERROR_LANGUAGE_UNAVAILABLE = 13
}
