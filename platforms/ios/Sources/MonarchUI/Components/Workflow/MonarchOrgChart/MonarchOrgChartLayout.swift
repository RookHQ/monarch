/// How a ``MonarchOrgChart`` is drawn.
@_spi(Experimental) public enum MonarchOrgChartLayout: Sendable, Hashable, CaseIterable {
    /// The chart in a regular width at standard text sizes, and the list in
    /// a compact width or at an accessibility text size.
    case auto
    /// Cards top-down under their managers, joined by lines, with zoom and pan.
    case chart
    /// An indented list, one row per person.
    case list
}
