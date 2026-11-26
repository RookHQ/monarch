import Foundation

/// A source that the passages of ``MonarchAiClaimSources`` point at.
@_spi(Experimental) public struct MonarchAiClaimSource: Identifiable, Sendable, Hashable {
    /// A stable id, matched against ``MonarchAiClaimEvidence/sourceID``.
    public let id: String
    /// The title shown with every passage from this source.
    public let title: String
    /// Where the source lives. Opened only when it is http, https or mailto.
    public let url: String?
    /// What kind of thing it is. It picks the icon and the kind name.
    public let kind: MonarchAiSourceKind?
    /// When it was published.
    public let published: Date?

    /// A source.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - title: The title, already localised.
    ///   - url: Where the source lives.
    ///   - kind: What kind of thing it is.
    ///   - published: When it was published.
    public init(id: String, title: String, url: String? = nil, kind: MonarchAiSourceKind? = nil, published: Date? = nil)
    {
        self.id = id
        self.title = title
        self.url = url
        self.kind = kind
        self.published = published
    }

    /// A source from the source part of an AI message.
    ///
    /// - Parameters:
    ///   - part: The source part.
    ///   - published: When it was published.
    public init(_ part: MonarchAiSourcePart, published: Date? = nil) {
        self.init(id: part.id, title: part.title, url: part.url, kind: part.kind, published: published)
    }
}
