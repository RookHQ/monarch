/// Where the check of a ``MonarchUsernameAvailabilityInput`` stands.
public enum MonarchUsernameAvailabilityStatus: Sendable, Hashable, CaseIterable {
    /// There is nothing to check: the text is empty, or the check returned `nil`.
    case idle
    /// The check is waiting for typing to pause, or is running.
    case checking
    /// The value can be used.
    case valid
    /// The value cannot be used.
    case invalid
    /// The check itself failed, for example without a connection.
    case error
}
