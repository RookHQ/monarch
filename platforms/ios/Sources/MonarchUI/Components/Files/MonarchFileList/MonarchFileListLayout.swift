/// How a ``MonarchFileList`` arranges its files.
public enum MonarchFileListLayout: Sendable, Hashable, CaseIterable {
    /// One row per file. The default.
    case list
    /// Cards with previews in an adaptive grid.
    case grid
}
