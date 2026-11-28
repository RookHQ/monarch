/// Replacements for the built-in strings of ``MonarchIssueCard``.
/// Every value is optional; leave one out to keep the built-in English text.
@_spi(Experimental) public struct MonarchIssueCardLabels: Sendable {
    /// The due date text, from the formatted date: "Due Mar 12".
    public var due: @Sendable (_ date: String) -> String
    /// The marker after a due date that has passed.
    public var overdue: String
    /// The spoken name of the tags.
    public var tags: String
    /// Spoken for an empty assignee slot.
    public var unassigned: String
    /// The spoken name of the assignee's avatar: "Assigned to …".
    public var assignee: @Sendable (_ name: String) -> String
    /// The spoken status, from its name: "Status: …".
    public var status: @Sendable (_ name: String) -> String
    /// The spoken priority, from the level's name: "Priority: …".
    public var priority: @Sendable (_ level: String) -> String
    /// The name of a priority level.
    public var priorityLevel: @Sendable (_ priority: MonarchIssueCardPriority) -> String
    /// Spoken on a selected card.
    public var selected: String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - due: Replaces "Due …".
    ///   - overdue: Replaces "Overdue".
    ///   - tags: Replaces "Labels".
    ///   - unassigned: Replaces "Unassigned".
    ///   - assignee: Replaces "Assigned to …".
    ///   - status: Replaces "Status: …".
    ///   - priority: Replaces "Priority: …".
    ///   - priorityLevel: Replaces "Urgent", "High", "Medium", "Low" and "No priority".
    ///   - selected: Replaces "Selected".
    public init(
        due: (@Sendable (_ date: String) -> String)? = nil,
        overdue: String? = nil,
        tags: String? = nil,
        unassigned: String? = nil,
        assignee: (@Sendable (_ name: String) -> String)? = nil,
        status: (@Sendable (_ name: String) -> String)? = nil,
        priority: (@Sendable (_ level: String) -> String)? = nil,
        priorityLevel: (@Sendable (_ priority: MonarchIssueCardPriority) -> String)? = nil,
        selected: String? = nil
    ) {
        self.due = due ?? { MonarchIssueCardStrings.due($0) }
        self.overdue = overdue ?? MonarchIssueCardStrings.overdue
        self.tags = tags ?? MonarchIssueCardStrings.tags
        self.unassigned = unassigned ?? MonarchIssueCardStrings.unassigned
        self.assignee = assignee ?? { MonarchIssueCardStrings.assignee($0) }
        self.status = status ?? { MonarchIssueCardStrings.status($0) }
        self.priority = priority ?? { MonarchIssueCardStrings.priority($0) }
        self.priorityLevel = priorityLevel ?? { MonarchIssueCardStrings.priorityLevel($0) }
        self.selected = selected ?? MonarchIssueCardStrings.selected
    }
}
