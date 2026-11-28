/// Replacements for the built-in strings of ``MonarchGoalTree``.
/// Every value is optional; leave one out to keep the built-in English text.
@_spi(Experimental) public struct MonarchGoalTreeLabels: Sendable {
    /// The name of a status, shown in its badge and spoken.
    public var status: @Sendable (_ status: MonarchGoalStatus) -> String
    /// The spoken name of the button that opens a goal: "Expand …".
    public var expand: @Sendable (_ title: String) -> String
    /// The spoken name of the button that closes a goal: "Collapse …".
    public var collapse: @Sendable (_ title: String) -> String
    /// The words before an aligned goal: "Aligned to".
    public var alignedTo: String
    /// The title of the add-child button.
    public var addChild: String
    /// The spoken name of the add-child button: "Add key result to …".
    public var addChildTo: @Sendable (_ title: String) -> String
    /// The number of children of a closed goal: "3 key results".
    public var childCount: @Sendable (_ count: Int) -> String
    /// Shown when there are no goals.
    public var empty: String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - status: Replaces "Not started", "On track", "At risk", "Off track" and "Done".
    ///   - expand: Replaces "Expand …".
    ///   - collapse: Replaces "Collapse …".
    ///   - alignedTo: Replaces "Aligned to".
    ///   - addChild: Replaces "Add key result".
    ///   - addChildTo: Replaces "Add key result to …".
    ///   - childCount: Replaces "… key results".
    ///   - empty: Replaces "No goals yet".
    public init(
        status: (@Sendable (_ status: MonarchGoalStatus) -> String)? = nil,
        expand: (@Sendable (_ title: String) -> String)? = nil,
        collapse: (@Sendable (_ title: String) -> String)? = nil,
        alignedTo: String? = nil,
        addChild: String? = nil,
        addChildTo: (@Sendable (_ title: String) -> String)? = nil,
        childCount: (@Sendable (_ count: Int) -> String)? = nil,
        empty: String? = nil
    ) {
        self.status = status ?? { MonarchGoalTreeStrings.status($0) }
        self.expand = expand ?? { MonarchGoalTreeStrings.expand($0) }
        self.collapse = collapse ?? { MonarchGoalTreeStrings.collapse($0) }
        self.alignedTo = alignedTo ?? MonarchGoalTreeStrings.alignedTo
        self.addChild = addChild ?? MonarchGoalTreeStrings.addChild
        self.addChildTo = addChildTo ?? { MonarchGoalTreeStrings.addChildTo($0) }
        self.childCount = childCount ?? { MonarchGoalTreeStrings.childCount($0) }
        self.empty = empty ?? MonarchGoalTreeStrings.empty
    }
}
