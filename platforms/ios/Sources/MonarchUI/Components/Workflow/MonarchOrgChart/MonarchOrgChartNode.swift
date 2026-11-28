import Foundation

/// One person or open position in a ``MonarchOrgChart``.
///
/// Nodes are passed flat; reporting lines come from `parentID`.
@_spi(Experimental) public struct MonarchOrgChartNode: Identifiable, Hashable, Sendable {
    /// A stable identifier.
    public let id: String
    /// The manager's id. `nil`, the node's own id or an id that is not in the
    /// chart makes the node top level.
    public var parentID: String?
    /// The person's name, or the title of an open position, already localised.
    public var name: String
    /// The job title, shown under the name.
    public var title: String?
    /// A line of extra facts under the title, such as the department.
    public var meta: String?
    /// The photo. Initials are shown without one, while it loads and when it fails.
    public var avatarURL: URL?
    /// A person or an open position.
    public var kind: MonarchOrgChartNodeKind
    /// Draws a dashed border for a temporary or acting reporting line.
    public var isDashed: Bool
    /// The total number of direct reports, including ones not loaded yet.
    /// When it is larger than the reports in the chart, opening the team
    /// calls `onLoadChildren` first.
    public var childCount: Int?
    /// The tone of the meta badge on a person's card. Neutral when nil. A vacancy's meta stays plain text.
    public var metaTone: MonarchTone?

    /// A node.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - parentID: The manager's id, or `nil` for a top-level node.
    ///   - name: The person's name, or the title of an open position.
    ///   - title: The job title.
    ///   - meta: A line of extra facts, such as the department.
    ///   - avatarURL: The photo.
    ///   - kind: A person or an open position.
    ///   - isDashed: Marks a temporary or acting reporting line.
    ///   - childCount: The total number of direct reports, loaded or not.
    public init(
        id: String,
        parentID: String? = nil,
        name: String,
        title: String? = nil,
        meta: String? = nil,
        avatarURL: URL? = nil,
        kind: MonarchOrgChartNodeKind = .person,
        isDashed: Bool = false,
        childCount: Int? = nil
    ) {
        self.id = id
        self.parentID = parentID
        self.name = name
        self.title = title
        self.meta = meta
        self.avatarURL = avatarURL
        self.kind = kind
        self.isDashed = isDashed
        self.childCount = childCount
    }

    /// A node whose meta shows as a badge in the given tone, such as a warning "Acting".
    public init(
        id: String,
        parentID: String? = nil,
        name: String,
        title: String? = nil,
        meta: String? = nil,
        metaTone: MonarchTone?,
        avatarURL: URL? = nil,
        kind: MonarchOrgChartNodeKind = .person,
        isDashed: Bool = false,
        childCount: Int? = nil
    ) {
        self.init(
            id: id,
            parentID: parentID,
            name: name,
            title: title,
            meta: meta,
            avatarURL: avatarURL,
            kind: kind,
            isDashed: isDashed,
            childCount: childCount
        )
        self.metaTone = metaTone
    }
}
