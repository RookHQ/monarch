import Foundation

/// One doc or link attached to a record, shown by ``MonarchLinkedDocsPanel``.
@_spi(Experimental) public struct MonarchLinkedDocsPanelItem: Identifiable, Sendable, Hashable {
    /// The link's id, unique per row.
    public let id: String
    /// The title.
    public let title: String
    /// A doc or an external link.
    public let kind: MonarchLinkedDocsPanelKind
    /// Where the row opens. For a link, its host is the default secondary line.
    public let url: URL?
    /// For a doc: the document id, used to leave linked docs out of the picker.
    public let docID: String?
    /// A secondary line, such as "Updated 2 hours ago".
    public let meta: String?
    /// A short status, such as "Draft", shown as a badge.
    public let status: String?
    /// The id of the space the doc lives in. It drives the space filter.
    public let spaceID: String?
    /// The name of the space the doc lives in.
    public let spaceName: String?
    /// A file name that picks the doc's icon, such as "spec.pdf".
    public let fileName: String?
    /// Whether the row can be unlinked.
    public let isUnlinkable: Bool

    /// A linked doc or link.
    ///
    /// - Parameters:
    ///   - id: The link's id.
    ///   - title: The title, already localised.
    ///   - kind: A doc or an external link.
    ///   - url: Where the row opens.
    ///   - docID: For a doc, the document id.
    ///   - meta: A secondary line.
    ///   - status: A short status shown as a badge.
    ///   - spaceID: The id of the doc's space.
    ///   - spaceName: The name of the doc's space.
    ///   - fileName: A file name that picks the icon.
    ///   - isUnlinkable: Whether the row can be unlinked.
    public init(
        id: String,
        title: String,
        kind: MonarchLinkedDocsPanelKind = .doc,
        url: URL? = nil,
        docID: String? = nil,
        meta: String? = nil,
        status: String? = nil,
        spaceID: String? = nil,
        spaceName: String? = nil,
        fileName: String? = nil,
        isUnlinkable: Bool = true
    ) {
        self.id = id
        self.title = title
        self.kind = kind
        self.url = url
        self.docID = docID
        self.meta = meta
        self.status = status
        self.spaceID = spaceID
        self.spaceName = spaceName
        self.fileName = fileName
        self.isUnlinkable = isUnlinkable
    }
}
