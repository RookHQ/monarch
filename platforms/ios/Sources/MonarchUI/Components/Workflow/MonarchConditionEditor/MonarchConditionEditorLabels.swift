/// Replacements for the built-in strings of ``MonarchConditionEditor``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchConditionEditorLabels: Sendable {
    /// The spoken name of the list. Built in: "Conditions".
    public var label: String?
    /// The visible title and spoken name of a condition, from its position. Built in: "Condition 1".
    public var condition: (@Sendable (Int) -> String)?
    /// The spoken name of a condition's field menu. Built in: "Condition 1 field".
    public var field: (@Sendable (Int) -> String)?
    /// The spoken name of a condition's operator menu. Built in: "Condition 1 operator".
    public var operatorLabel: (@Sendable (Int) -> String)?
    /// The spoken name of a condition's value. Built in: "Condition 1 value".
    public var value: (@Sendable (Int) -> String)?
    /// The spoken name of a condition's remove button. Built in: "Remove condition 1".
    public var remove: (@Sendable (Int) -> String)?
    /// Announced after a condition is removed, from the position it had. Built in: "Condition 1 removed."
    public var removed: (@Sendable (Int) -> String)?
    /// The add button. Built in: "Add condition".
    public var add: String?
    /// The text before the all or any switch. Built in: "Match".
    public var matchBefore: String?
    /// The text after the all or any switch. Built in: "of these conditions".
    public var matchAfter: String?
    /// The spoken name of the all or any switch. Built in: "Conditions that must hold".
    public var matchLabel: String?
    /// The all choice. Built in: "all".
    public var matchAll: String?
    /// The any choice. Built in: "any".
    public var matchAny: String?
    /// Shown when there are no conditions. Built in: "No conditions. This applies every time."
    public var empty: String?
    /// Shown instead of the add button when the list is full. Built in: "You can add up to 20 conditions."
    public var maxReached: (@Sendable (Int) -> String)?
    /// The empty choice of a field or select value. Built in: "Choose…".
    public var choose: String?
    /// The button that sets a date value. Built in: "Choose a date".
    public var chooseDate: String?
    /// A stored field the list no longer has, from its key. Built in: "amount (removed)".
    public var removedField: (@Sendable (String) -> String)?
    /// The visible names of the operators. Built in: "is", "is not", "contains", …
    public var operatorName: (@Sendable (MonarchConditionOperator) -> String)?
    /// What to do about a problem. Built in: "Choose a field.", "Enter a value.", …
    public var problem: (@Sendable (MonarchConditionProblem) -> String)?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        label: String? = nil,
        condition: (@Sendable (Int) -> String)? = nil,
        field: (@Sendable (Int) -> String)? = nil,
        operatorLabel: (@Sendable (Int) -> String)? = nil,
        value: (@Sendable (Int) -> String)? = nil,
        remove: (@Sendable (Int) -> String)? = nil,
        removed: (@Sendable (Int) -> String)? = nil,
        add: String? = nil,
        matchBefore: String? = nil,
        matchAfter: String? = nil,
        matchLabel: String? = nil,
        matchAll: String? = nil,
        matchAny: String? = nil,
        empty: String? = nil,
        maxReached: (@Sendable (Int) -> String)? = nil,
        choose: String? = nil,
        chooseDate: String? = nil,
        removedField: (@Sendable (String) -> String)? = nil,
        operatorName: (@Sendable (MonarchConditionOperator) -> String)? = nil,
        problem: (@Sendable (MonarchConditionProblem) -> String)? = nil
    ) {
        self.label = label
        self.condition = condition
        self.field = field
        self.operatorLabel = operatorLabel
        self.value = value
        self.remove = remove
        self.removed = removed
        self.add = add
        self.matchBefore = matchBefore
        self.matchAfter = matchAfter
        self.matchLabel = matchLabel
        self.matchAll = matchAll
        self.matchAny = matchAny
        self.empty = empty
        self.maxReached = maxReached
        self.choose = choose
        self.chooseDate = chooseDate
        self.removedField = removedField
        self.operatorName = operatorName
        self.problem = problem
    }
}
