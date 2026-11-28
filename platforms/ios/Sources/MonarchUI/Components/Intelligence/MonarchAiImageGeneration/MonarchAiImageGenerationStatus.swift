/// Where an image generation is, for ``MonarchAiImageGeneration``.
public enum MonarchAiImageGenerationStatus: Sendable, Hashable, CaseIterable {
    /// Nothing is ready: placeholders show.
    case pending
    /// Low-quality previews are refining.
    case progressive
    /// The final images are ready.
    case done
    /// Generation failed.
    case error
}
