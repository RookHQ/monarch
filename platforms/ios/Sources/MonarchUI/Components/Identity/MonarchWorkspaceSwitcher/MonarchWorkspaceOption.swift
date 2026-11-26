import Foundation

/// One workspace a person belongs to, offered by ``MonarchWorkspaceSwitcher``.
public struct MonarchWorkspaceOption: Identifiable, Sendable, Hashable {
    /// A stable id. It is the selection.
    public let id: String
    /// The workspace's name, such as "Acme".
    public let name: String
    /// A short handle shown as `@handle` and matched by search.
    public let handle: String?
    /// The URL of the workspace's logo, loaded with `AsyncImage`. Initials are shown without it.
    public let avatarURL: URL?
    /// Extra words the search matches but never shows, such as old names.
    public let keywords: String?

    /// A workspace.
    ///
    /// - Parameters:
    ///   - id: A stable id. It is the selection.
    ///   - name: The workspace's name.
    ///   - handle: A short handle shown as `@handle` and matched by search.
    ///   - avatarURL: The URL of the workspace's logo. Initials are shown without it.
    ///   - keywords: Extra words the search matches but never shows.
    public init(id: String, name: String, handle: String? = nil, avatarURL: URL? = nil, keywords: String? = nil) {
        self.id = id
        self.name = name
        self.handle = handle
        self.avatarURL = avatarURL
        self.keywords = keywords
    }
}
