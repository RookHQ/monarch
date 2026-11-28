/// One result in ``MonarchSearchResults``. Text fields are plain text; the
/// list highlights the query's words.
public struct MonarchSearchResult: Identifiable, Sendable, Hashable {
    /// A stable id, unique across all groups.
    public let id: String
    /// The title, as plain text.
    public let title: String
    /// Breadcrumb segments, such as `["Handbook", "Travel"]`.
    public let path: [String]
    /// An excerpt of the matching content, as plain text.
    public let snippet: String?
    /// Exact matches in the title from the search service, as UTF-16 offsets.
    public let titleRanges: [Range<Int>]?
    /// Exact matches in the snippet from the search service, as UTF-16 offsets.
    public let snippetRanges: [Range<Int>]?
    /// Whether the snippet carries `<mark>` markers from the search service.
    public let snippetHasMarkTags: Bool
    /// An SF Symbol for the leading tile.
    public let systemImage: String?
    /// A file name whose type icon fills the leading tile.
    public let fileName: String?
    /// A short trailing detail, such as a type or a date, already formatted.
    public let meta: String?
    /// A status tone for the title and the icon, such as ``MonarchTone/danger`` for a destructive command.
    public let tone: MonarchTone?
    /// Whether the trailing detail is drawn as a badge, as the web does for a type.
    private(set) var showsMetaAsBadge = false

    /// The result with its trailing detail drawn as a badge (a type such as "Policy") instead of plain text
    /// (a date such as "Thursday").
    ///
    /// - Parameter isBadge: Whether `meta` is a badge.
    /// - Returns: A copy of the result.
    public func metaAsBadge(_ isBadge: Bool = true) -> MonarchSearchResult {
        var copy = self
        copy.showsMetaAsBadge = isBadge
        return copy
    }

    /// A search result.
    ///
    /// - Parameters:
    ///   - title: The title, as plain text.
    ///   - id: A stable id, unique across all groups.
    ///   - path: Breadcrumb segments.
    ///   - snippet: An excerpt of the matching content.
    ///   - titleRanges: Exact matches in the title, as UTF-16 offsets.
    ///   - snippetRanges: Exact matches in the snippet, as UTF-16 offsets.
    ///   - snippetHasMarkTags: Whether the snippet carries `<mark>` markers.
    ///   - systemImage: An SF Symbol for the leading tile.
    ///   - fileName: A file name whose type icon fills the leading tile.
    ///   - meta: A short trailing detail, such as a type or a date.
    ///   - tone: A status tone for the title and the icon.
    public init(
        _ title: String,
        id: String,
        path: [String] = [],
        snippet: String? = nil,
        titleRanges: [Range<Int>]? = nil,
        snippetRanges: [Range<Int>]? = nil,
        snippetHasMarkTags: Bool = false,
        systemImage: String? = nil,
        fileName: String? = nil,
        meta: String? = nil,
        tone: MonarchTone? = nil
    ) {
        self.title = title
        self.id = id
        self.path = path
        self.snippet = snippet
        self.titleRanges = titleRanges
        self.snippetRanges = snippetRanges
        self.snippetHasMarkTags = snippetHasMarkTags
        self.systemImage = systemImage
        self.fileName = fileName
        self.meta = meta
        self.tone = tone
    }
}
