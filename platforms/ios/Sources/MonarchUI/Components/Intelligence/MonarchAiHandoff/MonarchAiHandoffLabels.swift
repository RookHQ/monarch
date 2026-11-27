/// Replacements for the built-in strings of ``MonarchAiHandoff``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiHandoffLabels: Sendable {
    /// Read first, naming the marker. Built in: "Handoff".
    public var label: String?
    /// The sentence. Receives the two names and whether the work went to a
    /// person. Built in: "Research agent handed off to Sam Lee".
    public var handedOff: (@Sendable (String, String, Bool) -> String)?
    /// Read after an agent's name, so an agent is never taken for a person. Built in: "AI agent".
    public var agentKind: String?
    /// Read before the reason. Built in: "Reason:".
    public var reason: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - label: Read first, naming the marker.
    ///   - handedOff: The sentence, from the two names and whether the target is a person.
    ///   - agentKind: Read after an agent's name.
    ///   - reason: Read before the reason.
    public init(
        label: String? = nil,
        handedOff: (@Sendable (String, String, Bool) -> String)? = nil,
        agentKind: String? = nil,
        reason: String? = nil
    ) {
        self.label = label
        self.handedOff = handedOff
        self.agentKind = agentKind
        self.reason = reason
    }
}
