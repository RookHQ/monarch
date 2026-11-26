/// A field a ``MonarchCondition`` can test, such as "Status" or "Amount".
public struct MonarchConditionField: Identifiable, Hashable, Sendable {
    /// The stable key stored on a rule, such as `status`.
    public let id: String
    /// The visible name, already localised.
    public let label: String
    /// The kind of value. It decides the operators and the value input.
    public let kind: MonarchConditionFieldType
    /// The operators offered instead of the defaults for the kind.
    public let operators: [MonarchConditionOperator]?
    /// Placeholder of a text or number value, such as "Amount".
    public let prompt: String?
    /// A heading the field is listed under in the field menu, such as "Custom fields". Ungrouped fields come first.
    public let group: String?

    /// A field.
    ///
    /// - Parameters:
    ///   - id: The stable key stored on a rule.
    ///   - label: The visible name, already localised.
    ///   - kind: The kind of value.
    ///   - operators: The operators offered instead of the defaults for the kind.
    ///   - prompt: Placeholder of a text or number value.
    ///   - group: A heading the field is listed under in the field menu.
    public init(
        id: String,
        label: String,
        kind: MonarchConditionFieldType,
        operators: [MonarchConditionOperator]? = nil,
        prompt: String? = nil,
        group: String? = nil
    ) {
        self.id = id
        self.label = label
        self.kind = kind
        self.operators = operators
        self.prompt = prompt
        self.group = group
    }

    /// The operators offered: ``operators`` if set, else the defaults for the kind.
    public var availableOperators: [MonarchConditionOperator] {
        if let operators, !operators.isEmpty { return operators }
        switch kind {
        case .text: return [.equals, .notEquals, .contains, .notContains, .isEmpty, .isNotEmpty]
        case .number: return [.equals, .notEquals, .greaterThan, .atLeast, .lessThan, .atMost, .isEmpty, .isNotEmpty]
        case .date: return [.equals, .before, .after, .isEmpty, .isNotEmpty]
        case .boolean: return [.isTrue, .isFalse]
        case .select: return [.equals, .notEquals, .anyOf, .noneOf, .isEmpty, .isNotEmpty]
        case .tags: return [.anyOf, .noneOf, .isEmpty, .isNotEmpty]
        }
    }

    /// The empty value a rule on this field starts with for an operator.
    public func blankValue(for queryOperator: MonarchConditionOperator) -> MonarchConditionValue {
        guard queryOperator.takesValue else { return .none }
        if queryOperator == .anyOf || queryOperator == .noneOf { return .options([]) }
        switch kind {
        case .text: return .text("")
        case .number: return .number(nil)
        case .date: return .date(nil)
        case .boolean: return .none
        case .select: return .option(nil)
        case .tags: return .options([])
        }
    }
}
