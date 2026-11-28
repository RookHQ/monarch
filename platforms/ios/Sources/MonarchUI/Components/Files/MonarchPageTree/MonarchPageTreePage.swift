/// One page of a ``MonarchPageTree``.
public struct MonarchPageTreePage: Identifiable, Hashable, Sendable {
    /// A stable identifier, used for the selection.
    public let id: String
    /// The title, already localised. A blank title is shown as "Untitled page".
    public var title: String

    /// A page.
    ///
    /// - Parameters:
    ///   - id: A stable identifier, used for the selection.
    ///   - title: The title, already localised.
    public init(id: String, title: String) {
        self.id = id
        self.title = title
    }
}
