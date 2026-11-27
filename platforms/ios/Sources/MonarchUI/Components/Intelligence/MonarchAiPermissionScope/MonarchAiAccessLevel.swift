/// How far an AI agent may go in one app, for ``MonarchAiPermissionScope``.
/// Ordered: `write` includes `read`.
public enum MonarchAiAccessLevel: Int, Sendable, Hashable, CaseIterable, Comparable {
    /// The agent cannot see the app.
    case none
    /// The agent can read but not change.
    case read
    /// The agent can read and edit.
    case write

    /// Whether this level is at least `needed`: `write` includes `read`.
    ///
    /// - Parameter needed: The level an action needs.
    /// - Returns: `true` when this level allows it.
    public func includes(_ needed: MonarchAiAccessLevel) -> Bool {
        self >= needed
    }

    /// Orders the levels from no access to write access.
    public static func < (lhs: MonarchAiAccessLevel, rhs: MonarchAiAccessLevel) -> Bool {
        lhs.rawValue < rhs.rawValue
    }
}
