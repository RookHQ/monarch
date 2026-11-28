/// One input of a ``MonarchActionType``, such as "Team" for "Set team".
public struct MonarchActionParameter: Identifiable, Sendable {
    /// The key of the value in ``MonarchRuleAction/parameters``.
    public let key: String
    /// The visible label, already localised.
    public let label: String
    /// The input.
    public let kind: MonarchActionParameterKind
    /// Whether it must be filled in before the action is complete.
    public let isRequired: Bool
    /// Help under the input.
    public let description: String?
    /// Placeholder text.
    public let prompt: String?
    /// The text of a select's empty choice, such as "The ticket's current team".
    public let emptyLabel: String?
    /// Shows the input only while this holds for the action's parameters.
    public let visibleWhen: (@Sendable ([String: MonarchActionValue]) -> Bool)?

    /// The key, which identifies the parameter within its type.
    public var id: String { key }

    /// A parameter.
    ///
    /// - Parameters:
    ///   - key: The key of the value in the action's parameters.
    ///   - label: The visible label, already localised.
    ///   - kind: The input.
    ///   - isRequired: Whether it must be filled in.
    ///   - description: Help under the input.
    ///   - prompt: Placeholder text.
    ///   - emptyLabel: The text of a select's empty choice.
    ///   - visibleWhen: Shows the input only while this holds.
    public init(
        key: String,
        label: String,
        kind: MonarchActionParameterKind,
        isRequired: Bool = false,
        description: String? = nil,
        prompt: String? = nil,
        emptyLabel: String? = nil,
        visibleWhen: (@Sendable ([String: MonarchActionValue]) -> Bool)? = nil
    ) {
        self.key = key
        self.label = label
        self.kind = kind
        self.isRequired = isRequired
        self.description = description
        self.prompt = prompt
        self.emptyLabel = emptyLabel
        self.visibleWhen = visibleWhen
    }

    /// The empty value of the input.
    public var blankValue: MonarchActionValue {
        switch kind {
        case .text, .multilineText: .text("")
        case .number: .number(nil)
        case .select: .option(nil)
        case .multiSelect, .tags: .list([])
        case .boolean: .boolean(false)
        }
    }
}
