/// Replacements for the built-in strings of ``MonarchActionEditor``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchActionEditorLabels: Sendable {
    /// The spoken name of the list. Built in: "Actions".
    public var label: String?
    /// The visible title of an action, from its position. Built in: "Action 1".
    public var action: (@Sendable (Int) -> String)?
    /// The label of the type menu. Built in: "Action".
    public var type: String?
    /// The spoken name of an action's remove button. Built in: "Remove action 1".
    public var remove: (@Sendable (Int) -> String)?
    /// The add button. Built in: "Add action".
    public var add: String?
    /// Shown when there are no actions. Built in: "No actions yet."
    public var empty: String?
    /// The empty choice of an optional select. Built in: "Choose…".
    public var choose: String?
    /// The problem of an empty required parameter, from its label. Built in: "Fill in “Team”."
    public var required: (@Sendable (String) -> String)?
    /// The problem of a number outside its range, from the parameter's label. Built in: "“Delay” is out of range."
    public var outOfRange: (@Sendable (String) -> String)?
    /// A stored type the list no longer has, from its id. Built in: "escalate (not available)".
    public var unknownType: (@Sendable (String) -> String)?
    /// The warning on an action whose type is disabled. Built in: "This action cannot run right now."
    public var unavailable: String?
    /// Shown instead of the add button at the limit. Built in: "You can add up to 20 actions."
    public var maxReached: (@Sendable (Int) -> String)?
    /// The spoken name of an action's reorder menu. Built in: "Reorder action 1".
    public var reorder: (@Sendable (Int) -> String)?
    /// Built in: "Move up".
    public var moveUp: String?
    /// Built in: "Move down".
    public var moveDown: String?
    /// Announced after a move, from the new position. Built in: "Moved to position 2."
    public var moved: (@Sendable (Int) -> String)?
    /// The text of a multi-select with choices, from how many. Built in: "2 chosen".
    public var chosen: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        label: String? = nil,
        action: (@Sendable (Int) -> String)? = nil,
        type: String? = nil,
        remove: (@Sendable (Int) -> String)? = nil,
        add: String? = nil,
        empty: String? = nil,
        choose: String? = nil,
        required: (@Sendable (String) -> String)? = nil,
        outOfRange: (@Sendable (String) -> String)? = nil,
        unknownType: (@Sendable (String) -> String)? = nil,
        unavailable: String? = nil,
        maxReached: (@Sendable (Int) -> String)? = nil,
        reorder: (@Sendable (Int) -> String)? = nil,
        moveUp: String? = nil,
        moveDown: String? = nil,
        moved: (@Sendable (Int) -> String)? = nil,
        chosen: (@Sendable (Int) -> String)? = nil
    ) {
        self.label = label
        self.action = action
        self.type = type
        self.remove = remove
        self.add = add
        self.empty = empty
        self.choose = choose
        self.required = required
        self.outOfRange = outOfRange
        self.unknownType = unknownType
        self.unavailable = unavailable
        self.maxReached = maxReached
        self.reorder = reorder
        self.moveUp = moveUp
        self.moveDown = moveDown
        self.moved = moved
        self.chosen = chosen
    }
}
