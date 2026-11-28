/// What a facet of a ``MonarchFacetedFilterPanel`` filters with.
public enum MonarchFacetKind: Hashable, Sendable {
    /// Checkboxes, any number checked. Only the first `maxVisible` are shown
    /// until the person asks for more; checked choices are always shown.
    case options([MonarchFacetOption], maxVisible: Int)
    /// A lower and an upper value between `bounds`, chosen on a range slider.
    /// `unit` (such as "MB") is shown after the values and read with them.
    case range(ClosedRange<Double>, step: Double, unit: String?)
}
