/// What an artifact is.
public enum MonarchAiArtifactKind: String, Sendable, Hashable, Codable, CaseIterable {
    /// A document.
    case document
    /// Code.
    case code
    /// A table.
    case table
    /// A chart.
    case chart
    /// An image.
    case image
    /// An HTML page.
    case html
}
