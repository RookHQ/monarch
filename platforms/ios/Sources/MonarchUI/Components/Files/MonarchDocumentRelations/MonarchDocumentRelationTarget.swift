/// A document at the other end of a relation, or one that can be linked.
@_spi(Experimental) public struct MonarchDocumentRelationTarget: Identifiable, Sendable, Hashable {
    /// A stable id.
    public let id: String
    /// The document's title.
    public let title: String
    /// A second line, such as its space or folder.
    public let description: String?
    /// The file name, for its type icon. A text document icon without it.
    public let fileName: String?

    /// A document.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - title: The document's title.
    ///   - description: A second line, such as its space or folder.
    ///   - fileName: The file name, for its type icon.
    public init(id: String, title: String, description: String? = nil, fileName: String? = nil) {
        self.id = id
        self.title = title
        self.description = description
        self.fileName = fileName
    }
}
