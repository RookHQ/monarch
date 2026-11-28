/// How ``MonarchDiffViewer`` lays out the two texts.
public enum MonarchDiffViewerMode: Sendable, Hashable, CaseIterable {
    /// One column: removed lines, then added lines. The default on phones.
    case unified
    /// Two columns: the earlier text beside the later one. The default at regular width.
    case split
}
