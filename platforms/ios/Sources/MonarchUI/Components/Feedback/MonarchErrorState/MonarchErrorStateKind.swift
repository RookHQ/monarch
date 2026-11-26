/// What went wrong when a region failed to load.
public enum MonarchErrorStateKind: Sendable, Hashable, CaseIterable {
    /// An unspecified failure. The default.
    case error
    /// The person may not see this. No retry.
    case forbidden
    /// The thing does not exist. No retry.
    case notFound
    /// The device has no network.
    case offline
    /// The server failed.
    case server
    /// The service is down for maintenance.
    case maintenance
}
