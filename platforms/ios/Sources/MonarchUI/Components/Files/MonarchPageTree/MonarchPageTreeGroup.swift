/// A named group of pages in a ``MonarchPageTree``, such as a chapter.
public struct MonarchPageTreeGroup: Identifiable, Hashable, Sendable {
    /// A stable identifier.
    public let id: String
    /// The name shown as the group's heading, already localised.
    public var label: String
    /// The pages, in order.
    public var pages: [MonarchPageTreePage]

    /// A group.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - label: The name shown as the group's heading.
    ///   - pages: The pages, in order.
    public init(id: String, label: String, pages: [MonarchPageTreePage]) {
        self.id = id
        self.label = label
        self.pages = pages
    }
}
