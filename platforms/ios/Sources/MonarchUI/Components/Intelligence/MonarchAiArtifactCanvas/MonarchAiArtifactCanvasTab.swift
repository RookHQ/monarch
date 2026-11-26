/// The two views of ``MonarchAiArtifactCanvas``.
@_spi(Experimental) public enum MonarchAiArtifactCanvasTab: String, Sendable, Hashable, CaseIterable {
    /// The rendered artifact.
    case preview
    /// The source text.
    case code
}
