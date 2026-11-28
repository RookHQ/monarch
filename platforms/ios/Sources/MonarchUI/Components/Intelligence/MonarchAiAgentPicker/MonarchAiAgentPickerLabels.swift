/// Replacements for the built-in strings of ``MonarchAiAgentPicker``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchAiAgentPickerLabels: Sendable {
    /// The spoken name of the group when no label is given. Built in: "Choose an agent".
    public var label: String?
    /// Shown when there are no agents. Built in: "No agents are available."
    public var empty: String?
    /// The tag on an agent that cannot be chosen. Built in: "Unavailable".
    public var unavailable: String?
    /// The strings of each ``MonarchAiAgentCard``.
    public var card: MonarchAiAgentCardLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        empty: String? = nil,
        unavailable: String? = nil,
        card: MonarchAiAgentCardLabels = MonarchAiAgentCardLabels()
    ) {
        self.label = label
        self.empty = empty
        self.unavailable = unavailable
        self.card = card
    }
}
