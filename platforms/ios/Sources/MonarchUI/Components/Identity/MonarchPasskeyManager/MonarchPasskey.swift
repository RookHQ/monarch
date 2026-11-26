import Foundation

/// One registered passkey, shown by ``MonarchPasskeyManager``.
public struct MonarchPasskey: Identifiable, Sendable, Hashable {
    /// The id passed to the rename and remove handlers.
    public let id: String
    /// The name the person gave it, such as "Work laptop".
    public let name: String
    /// When it was added.
    public let created: Date?
    /// When it was last used to sign in. `nil` reads "Never used".
    public let lastUsed: Date?
    /// Whether a password manager or platform account syncs it across devices.
    public let isSynced: Bool
    /// A quieter line, such as the provider ("iCloud Keychain").
    public let detail: String?

    /// A passkey.
    ///
    /// - Parameters:
    ///   - id: The id passed to the rename and remove handlers.
    ///   - name: The name the person gave it.
    ///   - created: When it was added.
    ///   - lastUsed: When it was last used to sign in. `nil` reads "Never used".
    ///   - isSynced: Whether it is synced across devices.
    ///   - detail: A quieter line, such as the provider, already localised.
    public init(
        id: String,
        name: String,
        created: Date? = nil,
        lastUsed: Date? = nil,
        isSynced: Bool = false,
        detail: String? = nil
    ) {
        self.id = id
        self.name = name
        self.created = created
        self.lastUsed = lastUsed
        self.isSynced = isSynced
        self.detail = detail
    }
}
