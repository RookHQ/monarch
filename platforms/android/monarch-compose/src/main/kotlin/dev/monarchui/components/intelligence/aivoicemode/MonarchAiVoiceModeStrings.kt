package dev.monarchui.components.intelligence.aivoicemode

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import dev.monarchui.R

/** The strings of a `MonarchAiVoiceMode`: the replacements in [labels], or the built-in resources. */
internal class MonarchAiVoiceModeStrings(
    private val labels: MonarchAiVoiceModeLabels,
    private val resources: Resources,
) {
    private fun text(override: String?, @StringRes id: Int): String = override ?: resources.getString(id)

    val assistant: String = text(labels.assistant, R.string.monarch_aivoicemode_assistant)
    val you: String = text(labels.you, R.string.monarch_aivoicemode_you)
    val microphoneMuted: String = text(labels.microphoneMuted, R.string.monarch_aivoicemode_microphone_muted)
    val mute: String = text(labels.mute, R.string.monarch_aivoicemode_mute)
    val interrupt: String = text(labels.interrupt, R.string.monarch_aivoicemode_interrupt)
    val captions: String = text(labels.captions, R.string.monarch_aivoicemode_captions)
    val captionsLabel: String = text(labels.captionsLabel, R.string.monarch_aivoicemode_captions_label)
    val end: String = text(labels.end, R.string.monarch_aivoicemode_end)
    val restart: String = text(labels.restart, R.string.monarch_aivoicemode_restart)
    val retry: String = text(labels.retry, R.string.monarch_aivoicemode_retry)
    val captionsEmpty: String = text(labels.captionsEmpty, R.string.monarch_aivoicemode_captions_empty)
    val errorFallback: String = text(labels.errorFallback, R.string.monarch_aivoicemode_error_fallback)

    /** The visible text of a state. */
    fun state(state: MonarchAiVoiceModeState): String = text(labels.states[state], stateResource(state))

    /** What TalkBack reads for a caption: "You: book a table". */
    fun caption(speaker: String, text: String): String =
        resources.getString(R.string.monarch_aivoicemode_caption, speaker, text)
}

/** Resolves the strings once per labels and configuration. */
@Composable
internal fun monarchAiVoiceModeStrings(labels: MonarchAiVoiceModeLabels): MonarchAiVoiceModeStrings {
    val configuration = LocalConfiguration.current
    val resources = LocalResources.current
    return remember(labels, configuration, resources) { MonarchAiVoiceModeStrings(labels, resources) }
}

@StringRes
private fun stateResource(state: MonarchAiVoiceModeState): Int = when (state) {
    MonarchAiVoiceModeState.Connecting -> R.string.monarch_aivoicemode_state_connecting
    MonarchAiVoiceModeState.Listening -> R.string.monarch_aivoicemode_state_listening
    MonarchAiVoiceModeState.Thinking -> R.string.monarch_aivoicemode_state_thinking
    MonarchAiVoiceModeState.Speaking -> R.string.monarch_aivoicemode_state_speaking
    MonarchAiVoiceModeState.Muted -> R.string.monarch_aivoicemode_state_muted
    MonarchAiVoiceModeState.Reconnecting -> R.string.monarch_aivoicemode_state_reconnecting
    MonarchAiVoiceModeState.Ended -> R.string.monarch_aivoicemode_state_ended
    MonarchAiVoiceModeState.Error -> R.string.monarch_aivoicemode_state_error
}
