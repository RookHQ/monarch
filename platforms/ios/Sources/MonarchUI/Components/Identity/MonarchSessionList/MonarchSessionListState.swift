/// Whether the sessions of a ``MonarchSessionList`` are loaded.
@_spi(Experimental) public enum MonarchSessionListState: Sendable, Hashable, CaseIterable {
    /// The sessions are loaded. An empty list shows the empty state.
    case ready
    /// The sessions are loading.
    case loading
    /// The sessions failed to load.
    case failed
}
