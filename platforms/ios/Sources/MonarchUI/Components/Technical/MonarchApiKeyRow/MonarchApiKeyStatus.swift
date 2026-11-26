/// The lifecycle of an API key in a ``MonarchApiKeyRow``. Expired and
/// revoked keys show no actions.
public enum MonarchApiKeyStatus: String, Sendable, Hashable, CaseIterable {
    /// In use. The default.
    case active
    /// Still works but expires soon.
    case expiring
    /// Past its expiry date.
    case expired
    /// Revoked; requests with it are rejected.
    case revoked
}
