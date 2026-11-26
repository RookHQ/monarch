/// What a ``MonarchLogViewer`` does with the search text.
public enum MonarchLogViewerSearchMode: Sendable, Hashable, CaseIterable {
    /// Hides the lines that do not contain the text, and marks the matches. The default.
    case filter
    /// Keeps every line and marks the matches.
    case highlight
}
