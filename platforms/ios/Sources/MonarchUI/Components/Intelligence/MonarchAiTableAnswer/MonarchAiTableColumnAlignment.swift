/// Where the values of a column of ``MonarchAiTableAnswer`` sit.
public enum MonarchAiTableColumnAlignment: Sendable, Hashable, CaseIterable {
    /// At the leading edge: the left in left-to-right languages.
    case leading
    /// At the trailing edge, so digits line up: the right in left-to-right languages.
    case trailing
}
