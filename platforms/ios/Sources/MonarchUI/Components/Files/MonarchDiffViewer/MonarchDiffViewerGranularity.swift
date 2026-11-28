/// How finely ``MonarchDiffViewer`` marks changes.
public enum MonarchDiffViewerGranularity: Sendable, Hashable, CaseIterable {
    /// Whole lines only.
    case line
    /// Whole lines, and the changed words inside a changed line. The default.
    case word
}
