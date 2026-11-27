/// One stage of a pipeline or lifecycle in a ``MonarchStageJourney``.
public struct MonarchStageJourneyStage: Sendable, Hashable, Identifiable {
    /// A stable identifier, matched against the current stage.
    public let id: String
    /// A short name, such as "Examination".
    public let label: String
    /// The phase or group the stage belongs to. Consecutive stages with the
    /// same group are shown together under its name.
    public let group: String?
    /// What the stage means, shown in the current-stage card.
    public let description: String?

    /// A stage.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - label: A short name.
    ///   - group: The phase or group heading, if any.
    ///   - description: What the stage means, shown while it is current.
    public init(id: String, label: String, group: String? = nil, description: String? = nil) {
        self.id = id
        self.label = label
        self.group = group
        self.description = description
    }
}
