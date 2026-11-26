import Foundation

/// One thing an AI agent did, shown by ``MonarchAiActionLog``.
@_spi(Experimental) public struct MonarchAiActionLogEntry: Identifiable, Sendable, Hashable {
    /// A stable id, passed to the undo handler.
    public let id: String
    /// When it happened.
    public let date: Date
    /// The agent that acted.
    public let agent: MonarchAiAgentRef
    /// What it did, as a verb phrase, such as "updated the status of".
    public let action: String
    /// What it acted on, such as "Quarterly report".
    public let target: String?
    /// Where the target opens. Only `http` and `https` links are followed.
    public let url: URL?
    /// Whether the action can be reversed. Shows Undo when the log has an undo handler.
    public let isUndoable: Bool
    /// Whether the action has already been undone.
    public let isUndone: Bool

    /// An entry.
    ///
    /// - Parameters:
    ///   - id: A stable id, passed to the undo handler.
    ///   - date: When it happened.
    ///   - agent: The agent that acted.
    ///   - action: What it did, as a verb phrase, already localised.
    ///   - target: What it acted on.
    ///   - url: Where the target opens. Only `http` and `https` links are followed.
    ///   - isUndoable: Whether the action can be reversed.
    ///   - isUndone: Whether the action has already been undone.
    public init(
        id: String,
        date: Date,
        agent: MonarchAiAgentRef,
        action: String,
        target: String? = nil,
        url: URL? = nil,
        isUndoable: Bool = false,
        isUndone: Bool = false
    ) {
        self.id = id
        self.date = date
        self.agent = agent
        self.action = action
        self.target = target
        self.url = url
        self.isUndoable = isUndoable
        self.isUndone = isUndone
    }
}
