/// A doc that can be linked from the picker of a ``MonarchLinkedDocsPanel``.
@_spi(Experimental) public struct MonarchLinkedDocsPanelOption: Identifiable, Sendable, Hashable {
    /// The document id passed to the link handler.
    public let docID: String
    /// The doc's title.
    public let label: String
    /// A secondary line, such as the space or the last update.
    public let description: String?

    /// The document id.
    public var id: String { docID }

    /// A doc option.
    ///
    /// - Parameters:
    ///   - docID: The document id passed to the link handler.
    ///   - label: The doc's title, already localised.
    ///   - description: A secondary line.
    public init(docID: String, label: String, description: String? = nil) {
        self.docID = docID
        self.label = label
        self.description = description
    }
}
