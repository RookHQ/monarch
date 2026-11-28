/// Replacements for the built-in strings of ``MonarchAssigneePicker``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchAssigneePickerLabels: Sendable {
    /// The row that clears the assignee, and the field with nobody chosen. Built in: "Unassigned".
    public var unassigned: String?
    /// The current user's name in the list. Built in: "Ana Silva (you)".
    public var you: (@Sendable (String) -> String)?
    /// The shortcut that assigns the current user. Built in: "Assign to me".
    public var assignToMe: String?
    /// Announced after Assign to me. Built in: "Assigned to you".
    public var assigned: String?
    /// The search field's placeholder. Built in: "Assign to…".
    public var search: String?
    /// The strings of the underlying ``MonarchEntityPicker``.
    public var picker: MonarchEntityPickerLabels

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        unassigned: String? = nil,
        you: (@Sendable (String) -> String)? = nil,
        assignToMe: String? = nil,
        assigned: String? = nil,
        search: String? = nil,
        picker: MonarchEntityPickerLabels = MonarchEntityPickerLabels()
    ) {
        self.unassigned = unassigned
        self.you = you
        self.assignToMe = assignToMe
        self.assigned = assigned
        self.search = search
        self.picker = picker
    }
}
