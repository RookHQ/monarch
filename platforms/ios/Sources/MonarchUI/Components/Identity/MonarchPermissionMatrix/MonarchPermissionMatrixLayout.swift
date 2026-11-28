/// How a ``MonarchPermissionMatrix`` is laid out.
public enum MonarchPermissionMatrixLayout: Sendable, Hashable, CaseIterable {
    /// The grid when it fits the width, otherwise one role at a time.
    case automatic
    /// Roles as columns and permissions as rows.
    case grid
    /// A role picker, then the permissions of the chosen role.
    case byRole
}
