/// One level of an escalation policy, edited by ``MonarchEscalationPolicyEditor``.
public struct MonarchEscalationLevel: Identifiable, Sendable, Hashable {
    /// A stable, unique id.
    public var id: String
    /// Minutes after the incident or request started, or after the previous
    /// level with ``MonarchEscalationPolicyTiming/interval``.
    public var afterMinutes: Int
    /// The ids of the targets notified at this level, in the order of the targets list.
    public var targets: [String]

    /// A level.
    ///
    /// - Parameters:
    ///   - id: A stable, unique id.
    ///   - afterMinutes: Minutes before this level fires.
    ///   - targets: The ids of the targets notified.
    public init(id: String, afterMinutes: Int, targets: [String] = []) {
        self.id = id
        self.afterMinutes = afterMinutes
        self.targets = targets
    }
}
