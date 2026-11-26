/// What a ``MonarchDecisionCard`` passes to `onDecide`.
public struct MonarchDecision: Sendable, Hashable {
    /// The chosen value, or `nil` for "no decision".
    public let value: String?
    /// The reason without surrounding white space, or `nil` when empty.
    public let reason: String?

    /// A decision.
    ///
    /// - Parameters:
    ///   - value: The chosen value, or `nil` for "no decision".
    ///   - reason: The reason, or `nil` when there is none.
    public init(value: String?, reason: String? = nil) {
        self.value = value
        self.reason = reason
    }
}
