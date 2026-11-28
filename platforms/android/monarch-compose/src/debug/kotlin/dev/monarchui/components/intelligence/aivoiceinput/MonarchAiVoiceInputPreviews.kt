@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aivoiceinput

import androidx.compose.runtime.Composable
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** A recogniser for the previews that hears one phrase. */
private object MonarchAiVoiceInputPreviewRecognizer : MonarchAiSpeechRecognizer {
    override fun isAvailable(locale: Locale?): Boolean = true

    override fun start(locale: Locale?, continuous: Boolean): Flow<MonarchAiSpeechEvent> =
        flowOf(MonarchAiSpeechEvent.Started, MonarchAiSpeechEvent.Final("Book a table for two"))

    override fun stop() = Unit
}

@MonarchPreviews
@Composable
internal fun MonarchAiVoiceInputPreview() {
    PreviewFrame {
        MonarchAiVoiceInput(onTranscript = { _, _ -> }, recognizer = MonarchAiVoiceInputPreviewRecognizer)
        MonarchAiVoiceInput(
            onTranscript = { _, _ -> },
            recognizer = MonarchAiVoiceInputPreviewRecognizer,
            mode = MonarchAiVoiceInputMode.PushToTalk,
            size = MonarchButtonSize.Sm,
        )
    }
}
