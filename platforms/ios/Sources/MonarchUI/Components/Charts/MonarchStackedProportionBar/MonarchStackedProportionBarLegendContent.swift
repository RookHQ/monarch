/// What each legend entry of a ``MonarchStackedProportionBar`` shows after its label.
public enum MonarchStackedProportionBarLegendContent: Sendable, Hashable, CaseIterable {
    /// The value and the share, such as "1,204 · 34%". The default.
    case both
    /// The value only.
    case value
    /// The share only.
    case percent
    /// The label only.
    case none
}
