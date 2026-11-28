/// Someone or something a level of ``MonarchEscalationPolicyEditor`` can
/// notify: a channel, a person, a rota.
public struct MonarchEscalationTarget: Identifiable, Sendable, Hashable {
    /// The stable value stored on a level.
    public let id: String
    /// The name shown, already localised.
    public let label: String
    /// Short extra text, such as the channel kind.
    public let description: String?

    /// A target.
    ///
    /// - Parameters:
    ///   - id: The stable value stored on a level.
    ///   - label: The name shown.
    ///   - description: Short extra text, such as "Slack channel".
    public init(id: String, label: String, description: String? = nil) {
        self.id = id
        self.label = label
        self.description = description
    }
}
