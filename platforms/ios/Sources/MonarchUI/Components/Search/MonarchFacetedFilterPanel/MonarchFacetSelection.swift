/// The filter chosen in one facet of a ``MonarchFacetedFilterPanel``.
///
/// The panel's selection is a dictionary from facet id to this value. A
/// facet with no filter has no entry, so the dictionary is easy to store in
/// a URL or a saved view.
public enum MonarchFacetSelection: Hashable, Sendable {
    /// The ids of the checked options. Never empty in the panel's selection.
    case options(Set<String>)
    /// The chosen range. Never the facet's whole bounds in the panel's selection.
    case range(ClosedRange<Double>)
}
