/// How a ``MonarchMessageRow`` shows its time.
public enum MonarchMessageRowTimeStyle: Sendable, Hashable, CaseIterable {
    /// Relative to now, "5 minutes ago", redrawn while it is on screen.
    case relative
    /// The clock time, "14:05", in the view's locale.
    case clock
}
