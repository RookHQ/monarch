import Foundation

/// An item that can be linked in a ``MonarchDependencyPicker``, such as an issue.
public struct MonarchDependencyItem: Identifiable, Hashable, Sendable {
    /// A stable id.
    public let id: String
    /// A short key shown before the title, such as `STR-12`.
    public let key: String?
    /// The title, already localised.
    public let title: String
    /// The current status, shown as a badge with its name.
    public let status: MonarchStatusDefinition?
    /// Where the item opens. Only `http` and `https` URLs become links.
    public let url: URL?

    /// An item.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - key: A short key shown before the title.
    ///   - title: The title, already localised.
    ///   - status: The current status.
    ///   - url: Where the item opens.
    public init(
        id: String,
        key: String? = nil,
        title: String,
        status: MonarchStatusDefinition? = nil,
        url: URL? = nil
    ) {
        self.id = id
        self.key = key
        self.title = title
        self.status = status
        self.url = url
    }
}
