/// A source the reply used.
public struct MonarchAiSourcePart: Identifiable, Sendable, Hashable, Codable {
    /// The id of the source.
    public var id: String
    /// The title.
    public var title: String
    /// Where the source is.
    public var url: String?
    /// What kind of thing the source is.
    public var kind: MonarchAiSourceKind
    /// The product the source lives in, for example `dossier`.
    public var product: String?
    /// The URL of the source's favicon.
    public var favicon: String?

    /// A source.
    ///
    /// - Parameters:
    ///   - id: The id of the source.
    ///   - title: The title.
    ///   - url: Where the source is.
    ///   - kind: What kind of thing the source is.
    ///   - product: The product the source lives in.
    ///   - favicon: The URL of the source's favicon.
    public init(
        id: String,
        title: String,
        url: String? = nil,
        kind: MonarchAiSourceKind,
        product: String? = nil,
        favicon: String? = nil
    ) {
        self.id = id
        self.title = title
        self.url = url
        self.kind = kind
        self.product = product
        self.favicon = favicon
    }
}
