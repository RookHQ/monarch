/// Where the data of a ``MonarchConnectedAppsList`` is.
public enum MonarchConnectedAppsListState: Sendable, Hashable, CaseIterable {
    /// The apps are loaded. An empty list shows the empty state. The default.
    case ready
    /// The apps are loading.
    case loading
    /// Loading failed. Shows the error with a retry button when there is a handler.
    case error
}
