/// What the body of ``MonarchThreadPanel`` shows.
@_spi(Experimental) public enum MonarchThreadPanelStatus: Sendable, Hashable, CaseIterable {
    /// The message the thread answers and its replies.
    case ready
    /// The thread is loading: placeholder rows.
    case loading
    /// The thread could not be loaded, with Try again when the panel has `onRetry`.
    case error
    /// The thread was deleted: a notice, with Close when the panel has `onClose`.
    case deleted
}
