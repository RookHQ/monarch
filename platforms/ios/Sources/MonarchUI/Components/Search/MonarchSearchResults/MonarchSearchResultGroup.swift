/// A labelled group of results in ``MonarchSearchResults``, such as
/// "Documents" or "People".
public struct MonarchSearchResultGroup: Identifiable, Sendable, Hashable {
    /// A stable id.
    public let id: String
    /// The heading, or `nil` for results without groups.
    public let title: String?
    /// The results shown in the group.
    public let results: [MonarchSearchResult]
    /// All matches in the group when more exist than are shown.
    public let total: Int?
    /// A decorative SF Symbol before the heading.
    public let systemImage: String?

    /// A group of results.
    ///
    /// - Parameters:
    ///   - title: The heading, already localised, or `nil` for one list without groups.
    ///   - id: A stable id.
    ///   - results: The results shown in the group.
    ///   - total: All matches in the group, when more exist than are shown.
    ///     The default is the number of results.
    ///   - systemImage: A decorative SF Symbol before the heading.
    public init(
        _ title: String?,
        id: String,
        results: [MonarchSearchResult],
        total: Int? = nil,
        systemImage: String? = nil
    ) {
        self.title = title
        self.id = id
        self.results = results
        self.total = total
        self.systemImage = systemImage
    }
}
