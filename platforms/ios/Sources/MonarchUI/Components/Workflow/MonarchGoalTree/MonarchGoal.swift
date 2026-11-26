import Foundation

/// One goal of a ``MonarchGoalTree``: an objective, a key result or any nested target.
@_spi(Experimental) public struct MonarchGoal: Identifiable, Sendable, Hashable {
    /// A stable, unique id.
    public let id: String
    /// The parent goal's id. `nil`, or an id that is not in the tree, makes it a top-level goal.
    public let parentID: String?
    /// What the goal is, already localised.
    public let title: String
    /// Progress from 0 to 1. Values outside are clamped; not-a-number counts as 0.
    public let progress: Double
    /// How the goal is doing.
    public let status: MonarchGoalStatus
    /// The owner's name, shown with an avatar.
    public let ownerName: String?
    /// The owner's picture, loaded with `AsyncImage`. Initials without it.
    public let ownerAvatarURL: URL?
    /// Short facts after the title, such as "Weight 30%" or "Due 31 Mar", already localised.
    public let meta: String?
    /// A goal elsewhere that this one supports, shown as "Aligned to …".
    public let alignedTo: String?

    /// A goal.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - parentID: The parent goal's id, or `nil` for a top-level goal.
    ///   - title: What the goal is, already localised.
    ///   - progress: Progress from 0 to 1.
    ///   - status: How the goal is doing.
    ///   - ownerName: The owner's name.
    ///   - ownerAvatarURL: The owner's picture.
    ///   - meta: Short facts after the title.
    ///   - alignedTo: A goal elsewhere that this one supports.
    public init(
        id: String,
        parentID: String? = nil,
        title: String,
        progress: Double,
        status: MonarchGoalStatus,
        ownerName: String? = nil,
        ownerAvatarURL: URL? = nil,
        meta: String? = nil,
        alignedTo: String? = nil
    ) {
        self.id = id
        self.parentID = parentID
        self.title = title
        self.progress = progress
        self.status = status
        self.ownerName = ownerName
        self.ownerAvatarURL = ownerAvatarURL
        self.meta = meta
        self.alignedTo = alignedTo
    }
}
