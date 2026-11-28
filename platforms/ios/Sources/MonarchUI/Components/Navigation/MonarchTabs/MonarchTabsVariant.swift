/// How a tab strip is drawn.
public enum MonarchTabsVariant: Sendable, Hashable, CaseIterable {
    /// A line under the selected tab, for the sections of a screen. The default.
    case line
    /// A raised pill on a muted track, for a compact switch between views.
    case pill
}
