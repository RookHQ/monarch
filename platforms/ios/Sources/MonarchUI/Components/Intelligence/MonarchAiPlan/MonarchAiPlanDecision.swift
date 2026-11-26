/// Where a person is with a ``MonarchAiPlan``.
@_spi(Experimental) public enum MonarchAiPlanDecision: String, Sendable, Hashable, Codable, CaseIterable {
    /// Waiting for the person to approve or reject.
    case pending
    /// Approved: the assistant works through the steps.
    case approved
    /// Rejected: the assistant does not take the steps.
    case rejected
}
