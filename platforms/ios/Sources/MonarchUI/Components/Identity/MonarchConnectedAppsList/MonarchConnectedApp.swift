import Foundation

/// One app the person signed in to with their account, and what it can see.
public struct MonarchConnectedApp: Sendable, Hashable, Identifiable {
    /// The id passed back when access is removed.
    public let id: String
    /// The app's name.
    public let name: String
    /// Where the app's logo is. The first letter of the name is shown without one.
    public let logoURL: URL?
    /// The app's domain, such as "notes.example.com".
    public let domain: String?
    /// Whether the identity provider has reviewed the app.
    public let isVerified: Bool
    /// Whether the app is suspended and cannot use its access.
    public let isSuspended: Bool
    /// The workspace the access belongs to, when access is given per workspace.
    public let workspace: String?
    /// What the app can see or do, in plain words, such as "See your email address".
    public let scopes: [String]
    /// When the person first connected the app.
    public let connectedAt: Date?
    /// When the app last used its access.
    public let lastUsedAt: Date?

    /// A connected app.
    ///
    /// - Parameters:
    ///   - id: The id passed back when access is removed.
    ///   - name: The app's name.
    ///   - logoURL: Where the app's logo is.
    ///   - domain: The app's domain.
    ///   - isVerified: Whether the identity provider has reviewed the app.
    ///   - isSuspended: Whether the app is suspended.
    ///   - workspace: The workspace the access belongs to.
    ///   - scopes: What the app can see or do, in plain words, already localised.
    ///   - connectedAt: When the person first connected the app.
    ///   - lastUsedAt: When the app last used its access.
    public init(
        id: String,
        name: String,
        logoURL: URL? = nil,
        domain: String? = nil,
        isVerified: Bool = false,
        isSuspended: Bool = false,
        workspace: String? = nil,
        scopes: [String] = [],
        connectedAt: Date? = nil,
        lastUsedAt: Date? = nil
    ) {
        self.id = id
        self.name = name
        self.logoURL = logoURL
        self.domain = domain
        self.isVerified = isVerified
        self.isSuspended = isSuspended
        self.workspace = workspace
        self.scopes = scopes
        self.connectedAt = connectedAt
        self.lastUsedAt = lastUsedAt
    }
}
