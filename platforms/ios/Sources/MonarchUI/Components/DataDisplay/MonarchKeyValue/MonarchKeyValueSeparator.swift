/// What sits between the pairs of a wrapping ``MonarchKeyValue``.
public enum MonarchKeyValueSeparator: Sendable, Hashable, CaseIterable {
    /// A small middle dot. The default.
    case dot
    /// A thin vertical line.
    case divider
    /// Only space.
    case none
}
