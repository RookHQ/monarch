package dev.monarchui.components.intelligence.aighosttext

/** How a suggestion of `MonarchAiGhostText` was accepted. */
public enum class MonarchAiGhostTextAcceptKind {
    /** The whole suggestion. */
    All,

    /** Its next word only; the rest keeps showing. */
    Word,
}
