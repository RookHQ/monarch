package dev.monarchui.components.intelligence.aiimagegeneration

/** Where an image generation is, for `MonarchAiImageGeneration`. The same names exist on every platform. */
public enum class MonarchAiImageGenerationStatus {
    /** Nothing is ready: placeholders show. */
    Pending,

    /** Low-quality previews are refining. */
    Progressive,

    /** The final images are ready. */
    Done,

    /** Generation failed. */
    Error,
}
