import Foundation

/// One comment in a ``MonarchCommentThread``: the author, the text and when
/// it was written.
@_spi(Experimental) public struct MonarchCommentThreadComment: Identifiable, Sendable, Hashable {
    /// A stable id, passed back to `onDelete`.
    public let id: String
    /// The author's display name.
    public let author: String
    /// The text, already localised or written by a person.
    public let body: String
    /// When the comment was written.
    public let createdAt: Date
    /// The author's picture, loaded with `AsyncImage`. Initials are shown without it.
    public let avatarURL: URL?
    /// A stable value, such as the author's id, that picks the initials' colour.
    public let seed: String?
    /// Whether the person may delete this comment. Adds Delete to its menu when `onDelete` is given.
    public let canDelete: Bool

    /// A comment.
    ///
    /// - Parameters:
    ///   - id: A stable id, passed back to `onDelete`.
    ///   - author: The author's display name.
    ///   - body: The text.
    ///   - createdAt: When the comment was written.
    ///   - avatarURL: The author's picture.
    ///   - seed: A stable value that picks the initials' colour.
    ///   - canDelete: Whether the person may delete this comment.
    public init(
        id: String,
        author: String,
        body: String,
        createdAt: Date,
        avatarURL: URL? = nil,
        seed: String? = nil,
        canDelete: Bool = false
    ) {
        self.id = id
        self.author = author
        self.body = body
        self.createdAt = createdAt
        self.avatarURL = avatarURL
        self.seed = seed
        self.canDelete = canDelete
    }
}
