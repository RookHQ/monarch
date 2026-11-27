/// Replacements for the built-in strings of ``MonarchQueryBuilder``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchQueryBuilderLabels: Sendable {
    /// The spoken name of the whole builder. Built in: "Query".
    public var label: String?
    /// The name of a group from its path: `[1]` is the top group, `[1, 2]` its second
    /// sub-group. Built in: "Top group", "Group 2", "Group 2.1".
    public var group: (@Sendable ([Int]) -> String)?
    /// The text before the AND and OR switch. Built in: "Join rules with".
    public var combinatorBefore: String?
    /// The name of AND. Built in: "AND".
    public var and: String?
    /// The name of OR. Built in: "OR".
    public var or: String?
    /// The spoken name of a group's switch, from the group name. Built in: "Top group combinator".
    public var combinatorLabel: (@Sendable (String) -> String)?
    /// The visible title of a rule, from its position. Built in: "Rule 1".
    public var ruleTitle: (@Sendable (Int) -> String)?
    /// The spoken name of a rule, from the group name and position. Built in: "Top group, rule 1".
    public var rule: (@Sendable (String, Int) -> String)?
    /// The spoken name of a rule's field menu. Built in: "Top group, rule 1 field".
    public var field: (@Sendable (String, Int) -> String)?
    /// The spoken name of a rule's operator menu. Built in: "Top group, rule 1 operator".
    public var operatorLabel: (@Sendable (String, Int) -> String)?
    /// The spoken name of a rule's value. Built in: "Top group, rule 1 value".
    public var value: (@Sendable (String, Int) -> String)?
    /// The spoken name of a rule's remove button. Built in: "Remove top group, rule 1".
    public var removeRule: (@Sendable (String, Int) -> String)?
    /// Built in: "Add rule".
    public var addRule: String?
    /// Built in: "Add group".
    public var addGroup: String?
    /// The spoken name of a group's remove button, from the group name. Built in: "Remove group 2".
    public var removeGroup: (@Sendable (String) -> String)?
    /// Shown in a group with no rules. Built in: "No rules in this group."
    public var emptyGroup: String?
    /// Shown instead of Add group at the deepest level. Built in: "Groups cannot be nested any deeper."
    public var maxDepthReached: String?
    /// Shown instead of Add rule when a group is full. Built in: "A group can have up to 20 rules."
    public var maxRulesReached: (@Sendable (Int) -> String)?
    /// The empty choice of a field or select value. Built in: "Choose…".
    public var choose: String?
    /// A stored field the list no longer has, from its key. Built in: "amount (removed)".
    public var removedField: (@Sendable (String) -> String)?
    /// The button that sets a date value. Built in: "Choose a date".
    public var chooseDate: String?
    /// The visible names of the operators. Built in: "is", "is not", "contains", …
    public var operatorName: (@Sendable (MonarchConditionOperator) -> String)?
    /// What to do about a problem. Built in: "Choose a field.", "Enter a value.", …
    public var problem: (@Sendable (MonarchConditionProblem) -> String)?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        label: String? = nil,
        group: (@Sendable ([Int]) -> String)? = nil,
        combinatorBefore: String? = nil,
        and: String? = nil,
        or: String? = nil,
        combinatorLabel: (@Sendable (String) -> String)? = nil,
        ruleTitle: (@Sendable (Int) -> String)? = nil,
        rule: (@Sendable (String, Int) -> String)? = nil,
        field: (@Sendable (String, Int) -> String)? = nil,
        operatorLabel: (@Sendable (String, Int) -> String)? = nil,
        value: (@Sendable (String, Int) -> String)? = nil,
        removeRule: (@Sendable (String, Int) -> String)? = nil,
        addRule: String? = nil,
        addGroup: String? = nil,
        removeGroup: (@Sendable (String) -> String)? = nil,
        emptyGroup: String? = nil,
        maxDepthReached: String? = nil,
        maxRulesReached: (@Sendable (Int) -> String)? = nil,
        choose: String? = nil,
        removedField: (@Sendable (String) -> String)? = nil,
        chooseDate: String? = nil,
        operatorName: (@Sendable (MonarchConditionOperator) -> String)? = nil,
        problem: (@Sendable (MonarchConditionProblem) -> String)? = nil
    ) {
        self.label = label
        self.group = group
        self.combinatorBefore = combinatorBefore
        self.and = and
        self.or = or
        self.combinatorLabel = combinatorLabel
        self.ruleTitle = ruleTitle
        self.rule = rule
        self.field = field
        self.operatorLabel = operatorLabel
        self.value = value
        self.removeRule = removeRule
        self.addRule = addRule
        self.addGroup = addGroup
        self.removeGroup = removeGroup
        self.emptyGroup = emptyGroup
        self.maxDepthReached = maxDepthReached
        self.maxRulesReached = maxRulesReached
        self.choose = choose
        self.removedField = removedField
        self.chooseDate = chooseDate
        self.operatorName = operatorName
        self.problem = problem
    }
}
