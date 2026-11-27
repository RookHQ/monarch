/// How ``MonarchLogoCloud`` arranges its logos.
public enum MonarchLogoCloudLayout: Sendable, Hashable, CaseIterable {
    /// A centred row that wraps. The default.
    case row
    /// Equal bordered cells, as many per row as fit.
    case grid
}
