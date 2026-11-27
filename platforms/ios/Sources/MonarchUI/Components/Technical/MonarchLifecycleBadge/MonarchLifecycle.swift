/// The release stage of an API, SDK or feature, shown by
/// ``MonarchLifecycleBadge``.
public enum MonarchLifecycle: String, Sendable, Hashable, CaseIterable {
    /// Announced and not yet available.
    case planned
    /// Available to try; it may still change.
    case preview
    /// Released and supported.
    case stable
    /// Still works, and will be removed.
    case deprecated
}
