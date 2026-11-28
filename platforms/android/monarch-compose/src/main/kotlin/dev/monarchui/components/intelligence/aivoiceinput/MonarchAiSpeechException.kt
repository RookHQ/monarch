package dev.monarchui.components.intelligence.aivoiceinput

/**
 * The failure a `MonarchAiSpeechRecognizer` throws from its event flow.
 *
 * @property error why the session failed. `MonarchAiVoiceInput` shows a message for it.
 */
public class MonarchAiSpeechException(public val error: MonarchAiSpeechError) :
    Exception("Speech recognition failed: $error")
