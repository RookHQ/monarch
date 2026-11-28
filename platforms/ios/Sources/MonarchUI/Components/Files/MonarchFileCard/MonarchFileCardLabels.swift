/// Replacements for the built-in strings of ``MonarchFileCard``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchFileCardLabels: Sendable {
    /// The spoken name of the selection checkbox, from the file name. Built in: "Select Q3 report.pdf".
    public var select: (@Sendable (String) -> String)?
    /// The spoken name of the actions menu, from the file name. Built in: "Actions for Q3 report.pdf".
    public var actions: (@Sendable (String) -> String)?
    /// Replaces the type in the meta line, such as "PDF document".
    public var type: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        select: (@Sendable (String) -> String)? = nil,
        actions: (@Sendable (String) -> String)? = nil,
        type: String? = nil
    ) {
        self.select = select
        self.actions = actions
        self.type = type
    }
}
