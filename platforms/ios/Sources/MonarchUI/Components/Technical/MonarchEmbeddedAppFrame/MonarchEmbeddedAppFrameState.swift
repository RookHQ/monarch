/// What a ``MonarchEmbeddedAppFrame`` shows.
public enum MonarchEmbeddedAppFrameState: Sendable, Hashable, CaseIterable {
    /// The embedded screen is loading: a spinner and "Loading…".
    case loading
    /// The embedded screen is shown.
    case ready
    /// The embedded screen did not open: a message, Try again and a link to open the page directly.
    case failed
}
