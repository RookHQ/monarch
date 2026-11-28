/// How a ``MonarchRolePicker`` shows the roles.
public enum MonarchRolePickerLayout: Sendable, Hashable, CaseIterable {
    /// Every role as a selectable card with its description and capabilities,
    /// for invite and settings forms. The default.
    case cards
    /// A menu picker that shows the chosen role, with its description under
    /// it, for list rows and tight sheets.
    case compact
}
