package dev.monarchui.components.intelligence.aighosttext

/** Where a `MonarchAiCompletion` is. The same names exist on every platform. */
public enum class MonarchAiCompletionStatus {
    /** Nothing is asked and nothing is shown. */
    Idle,

    /** The person is still typing; the request waits for the pause. */
    Waiting,

    /** The request is in flight. */
    Loading,

    /** A suggestion is showing. */
    Ready,

    /** The last request failed. Nothing is shown. */
    Error,
}
