/// Replacements for the built-in strings of ``MonarchStageHistory``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchStageHistoryLabels: Sendable {
    /// The first move. Receives the stage. Built in: "Started at Filed".
    public var started: (@Sendable (String) -> String)?
    /// Read by VoiceOver between the two stages. Built in: "to".
    public var to: String?
    /// After a move back. Built in: "(moved back)".
    public var movedBack: String?
    /// Shown when there are no moves. Built in: "No stage moves yet.".
    public var empty: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        started: (@Sendable (String) -> String)? = nil,
        to: String? = nil,
        movedBack: String? = nil,
        empty: String? = nil
    ) {
        self.started = started
        self.to = to
        self.movedBack = movedBack
        self.empty = empty
    }
}
