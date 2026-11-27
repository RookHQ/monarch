/// Where a person in a ``MonarchOrgChart`` is asked to move.
@_spi(Experimental) public enum MonarchOrgChartMoveTarget: Hashable, Sendable {
    /// Under the manager with this id: the person was dropped on that card.
    case manager(String)
    /// To the top level: the person was dropped on the empty background.
    case topLevel
    /// Not chosen yet: the person chose Move from the card's menu or the
    /// VoiceOver actions, so the app asks where (for example in a sheet).
    case unspecified
}
