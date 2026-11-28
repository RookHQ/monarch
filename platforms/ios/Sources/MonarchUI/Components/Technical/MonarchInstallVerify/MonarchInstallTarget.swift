import Foundation

/// A site the install is expected on, and when the code last reported from it, for
/// ``MonarchInstallVerify``.
@_spi(Experimental) public struct MonarchInstallTarget: Identifiable, Hashable, Sendable {
    /// A stable, unique id.
    public let id: String
    /// Usually the host name, such as `www.example.com`.
    public let label: String
    /// When the installed code last reported from this site; `nil` while it has not.
    public let lastSeen: Date?

    /// A site.
    ///
    /// - Parameters:
    ///   - label: Usually the host name.
    ///   - lastSeen: When the code last reported from this site; `nil` while it has not.
    ///   - id: A stable, unique id. By default the label.
    public init(_ label: String, lastSeen: Date? = nil, id: String? = nil) {
        self.id = id ?? label
        self.label = label
        self.lastSeen = lastSeen
    }
}
