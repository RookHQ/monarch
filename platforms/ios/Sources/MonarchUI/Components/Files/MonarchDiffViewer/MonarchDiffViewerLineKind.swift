/// What happened to a line between two texts.
public enum MonarchDiffViewerLineKind: Sendable, Hashable, CaseIterable {
    /// The line is in both texts.
    case same
    /// The line is only in the later text.
    case added
    /// The line is only in the earlier text.
    case removed
}
