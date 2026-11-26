import Foundation

/// One move in a ``MonarchStageHistory``.
public struct MonarchStageHistoryEntry: Sendable, Hashable, Identifiable {
    /// A stable identifier.
    public let id: String
    /// The stage moved from, or `nil` for the first stage ("Started at …").
    public let from: String?
    /// The stage moved to.
    public let to: String
    /// When the move happened.
    public let at: Date
    /// Who or what made the move, such as a name or "Automation".
    public let actor: String?
    /// Extra facts, such as "2 deadlines added".
    public let meta: String?
    /// Why, in the mover's words.
    public let note: String?
    /// Whether the move went back to an earlier stage.
    public let isBackward: Bool

    /// A move.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - from: The stage moved from, or `nil` for the first stage.
    ///   - to: The stage moved to.
    ///   - at: When the move happened.
    ///   - actor: Who or what made the move.
    ///   - meta: Extra facts.
    ///   - note: Why, in the mover's words.
    ///   - isBackward: Whether the move went back to an earlier stage.
    public init(
        id: String,
        from: String? = nil,
        to: String,
        at: Date,
        actor: String? = nil,
        meta: String? = nil,
        note: String? = nil,
        isBackward: Bool = false
    ) {
        self.id = id
        self.from = from
        self.to = to
        self.at = at
        self.actor = actor
        self.meta = meta
        self.note = note
        self.isBackward = isBackward
    }
}
