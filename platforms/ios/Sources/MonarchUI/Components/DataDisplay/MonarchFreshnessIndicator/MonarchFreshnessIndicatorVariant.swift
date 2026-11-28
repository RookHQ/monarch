/// How a freshness indicator is drawn.
public enum MonarchFreshnessIndicatorVariant: Sendable, Hashable, CaseIterable {
    /// One line of muted text, for under a title or above a table. The default.
    case inline
    /// A bordered bar on a subtle fill, for the top of a page or a panel.
    case banner
}
