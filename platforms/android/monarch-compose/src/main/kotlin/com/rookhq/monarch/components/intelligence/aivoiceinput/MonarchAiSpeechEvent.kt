package com.rookhq.monarch.components.intelligence.aivoiceinput

/** One event of a listening session, delivered by a `MonarchAiSpeechRecognizer`. */
public sealed interface MonarchAiSpeechEvent {
    /** The microphone is open and the recogniser is listening. */
    public data object Started : MonarchAiSpeechEvent

    /**
     * The input level.
     *
     * @property value the loudness from 0 to 1.
     */
    public data class Level(public val value: Float) : MonarchAiSpeechEvent

    /**
     * A guess at the words of the current phrase. It may change, and is replaced by the next guess.
     *
     * @property text the words so far.
     */
    public data class Partial(public val text: String) : MonarchAiSpeechEvent

    /**
     * A settled phrase. It arrives once.
     *
     * @property text the words of the phrase.
     */
    public data class Final(public val text: String) : MonarchAiSpeechEvent
}
