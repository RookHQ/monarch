/// Where a duplicate panel is used, which selects how it is drawn.
public enum MonarchDuplicatePanelVariant: Sendable, Hashable, CaseIterable {
    /// A warning-tinted box under a create form. Announced when it appears.
    /// The default.
    case hint
    /// A plain section on a record screen. Not announced.
    case panel
}
