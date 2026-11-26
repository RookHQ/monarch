/// A move offered from the current stage of a ``MonarchStageJourney``.
public struct MonarchStageJourneyTransition: Sendable, Hashable, Identifiable {
    /// A stable identifier, usually the target stage's.
    public let id: String
    /// The name of the target stage, such as "Granted".
    public let label: String
    /// Whether it is a next step, an early exit or a step back.
    public let kind: MonarchStageJourneyTransitionKind
    /// Whether the move asks for more details. VoiceOver says so.
    public let needsInput: Bool
    /// Whether the move is not possible right now.
    public let isDisabled: Bool

    /// A move.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - label: The name of the target stage.
    ///   - kind: `forward`, `exit` or `back`.
    ///   - needsInput: Whether the move asks for more details.
    ///   - isDisabled: Whether the move is not possible right now.
    public init(
        id: String,
        label: String,
        kind: MonarchStageJourneyTransitionKind = .forward,
        needsInput: Bool = false,
        isDisabled: Bool = false
    ) {
        self.id = id
        self.label = label
        self.kind = kind
        self.needsInput = needsInput
        self.isDisabled = isDisabled
    }
}
