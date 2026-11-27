import Foundation

/// Replacements for the built-in strings of ``MonarchEscalationPolicyEditor``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchEscalationPolicyEditorLabels: Sendable {
    /// The name of the list. Built in: "Escalation levels".
    public var label: String?
    /// The title of a level, from its position. Built in: "Level 1".
    public var level: (@Sendable (Int) -> String)?
    /// The label of the delay. Built in: "After".
    public var after: String?
    /// The label of the targets. Built in: "Notify".
    public var notify: String?
    /// The summary of a level, from the delay and the names. Built in: "After 15m, notify On-call".
    public var summary: (@Sendable (String, String) -> String)?
    /// The names when the level notifies nobody. Built in: "nobody yet".
    public var nobody: String?
    /// The remove button, from the position. Built in: "Remove level 1".
    public var remove: (@Sendable (Int) -> String)?
    /// The add button. Built in: "Add level".
    public var add: String?
    /// The warning when there are no targets. Built in: "There is nothing to notify yet. …"
    public var noTargets: String?
    /// The text of a problem. Built in: "Choose at least one target. …", "This level must come later than the one before it."
    public var problem: (@Sendable (MonarchEscalationLevelProblem) -> String)?
    /// Shown instead of the add button at the most levels. Built in: "A policy can have up to 10 levels."
    public var maxReached: (@Sendable (Int) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - label: The name of the list.
    ///   - level: The title of a level.
    ///   - after: The label of the delay.
    ///   - notify: The label of the targets.
    ///   - summary: The summary of a level.
    ///   - nobody: The names when the level notifies nobody.
    ///   - remove: The remove button.
    ///   - add: The add button.
    ///   - noTargets: The warning when there are no targets.
    ///   - problem: The text of a problem.
    ///   - maxReached: Shown instead of the add button at the most levels.
    public init(
        label: String? = nil,
        level: (@Sendable (Int) -> String)? = nil,
        after: String? = nil,
        notify: String? = nil,
        summary: (@Sendable (String, String) -> String)? = nil,
        nobody: String? = nil,
        remove: (@Sendable (Int) -> String)? = nil,
        add: String? = nil,
        noTargets: String? = nil,
        problem: (@Sendable (MonarchEscalationLevelProblem) -> String)? = nil,
        maxReached: (@Sendable (Int) -> String)? = nil
    ) {
        self.label = label
        self.level = level
        self.after = after
        self.notify = notify
        self.summary = summary
        self.nobody = nobody
        self.remove = remove
        self.add = add
        self.noTargets = noTargets
        self.problem = problem
        self.maxReached = maxReached
    }
}
