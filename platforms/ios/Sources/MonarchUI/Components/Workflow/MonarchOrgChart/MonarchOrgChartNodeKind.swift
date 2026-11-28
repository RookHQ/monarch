/// Whether a ``MonarchOrgChartNode`` is a person or an open position.
@_spi(Experimental) public enum MonarchOrgChartNodeKind: Sendable, Hashable, CaseIterable {
    /// A person, drawn with an avatar.
    case person
    /// An open position, drawn with a briefcase on a muted card with a dashed border.
    case vacancy
}
