import Foundation

/// One discussion listed by ``MonarchCommentsPanel``: its comments, the
/// passage it is about and whether it is resolved.
@_spi(Experimental) public struct MonarchCommentsPanelThread: Identifiable, Sendable, Hashable {
    /// A stable id, passed back in `selectedThreadID` and to the panel's actions.
    public let id: String
    /// The comments, oldest first. The first one starts the thread.
    public let comments: [MonarchCommentThreadComment]
    /// The passage the thread is about, shown as a quote. `nil` for a general comment.
    public let quote: String?
    /// The last activity shown in the list. The newest comment's time by default.
    public let updatedAt: Date?
    /// Whether the thread is resolved: it is listed under Resolved and takes no replies until reopened.
    public let isResolved: Bool
    /// Whether Resolve and Reopen are offered for this thread, with the panel's `onResolvedChange`.
    public let canResolve: Bool
    /// Whether the reply field is offered for this thread, with the panel's `onReply`.
    public let canReply: Bool

    /// A thread.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - comments: The comments, oldest first.
    ///   - quote: The passage the thread is about.
    ///   - updatedAt: The last activity. The newest comment's time by default.
    ///   - isResolved: Whether the thread is resolved.
    ///   - canResolve: Whether Resolve and Reopen are offered.
    ///   - canReply: Whether the reply field is offered.
    public init(
        id: String,
        comments: [MonarchCommentThreadComment],
        quote: String? = nil,
        updatedAt: Date? = nil,
        isResolved: Bool = false,
        canResolve: Bool = true,
        canReply: Bool = true
    ) {
        self.id = id
        self.comments = comments
        self.quote = quote
        self.updatedAt = updatedAt
        self.isResolved = isResolved
        self.canResolve = canResolve
        self.canReply = canReply
    }
}
