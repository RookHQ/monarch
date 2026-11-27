/// Which claims ``MonarchAiClaimSources`` shows.
@_spi(Experimental) public enum MonarchAiClaimSourcesFilter: Sendable, Hashable, CaseIterable {
    /// Every claim.
    case all
    /// The claims that are not fully supported.
    case needsChecking
}
