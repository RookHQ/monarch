/// The delivery state of a ``MonarchMessageRow``.
public enum MonarchMessageRowStatus: Sendable, Hashable, CaseIterable {
    /// Delivered. Actions and reactions are available.
    case sent
    /// Being sent: the body is muted and "Sending…" is shown under it.
    case pending
    /// Not sent: the error, Retry and Discard are shown under the body.
    case failed
}
