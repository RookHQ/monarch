/// Replacements for the built-in strings of ``MonarchPermissionMatrix``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchPermissionMatrixLabels: Sendable {
    /// The heading of the first column of the grid. Built in: "Permission".
    public var permissionHeader: String?
    /// The spoken name of the role picker. Built in: "Role".
    public var rolePicker: String?
    /// The spoken name of a cell, given the permission and the role. Built in: "<permission> for <role>".
    public var cellLabel: (@Sendable (String, String) -> String)?
    /// Read for a granted cell when read only. Built in: "Allowed".
    public var allowed: String?
    /// Read for a cell that is not granted when read only. Built in: "Not allowed".
    public var notAllowed: String?
    /// The reason of a locked cell when the lock gives none. Built in: "This can’t be changed".
    public var locked: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        permissionHeader: String? = nil,
        rolePicker: String? = nil,
        cellLabel: (@Sendable (String, String) -> String)? = nil,
        allowed: String? = nil,
        notAllowed: String? = nil,
        locked: String? = nil
    ) {
        self.permissionHeader = permissionHeader
        self.rolePicker = rolePicker
        self.cellLabel = cellLabel
        self.allowed = allowed
        self.notAllowed = notAllowed
        self.locked = locked
    }
}
