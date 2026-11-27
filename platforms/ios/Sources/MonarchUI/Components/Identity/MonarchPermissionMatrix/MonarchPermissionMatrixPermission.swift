/// A permission of a ``MonarchPermissionMatrix``: a row of the grid.
public struct MonarchPermissionMatrixPermission: Identifiable, Sendable, Hashable {
    /// The stable id, stored in the selection.
    public let id: String
    /// The name people see, such as "Invite members".
    public let label: String
    /// What the permission allows, in plain words.
    public let description: String?
    /// The group it is listed under. Permissions with the same group are
    /// listed together, groups in the order they first appear.
    public let group: String?

    /// A permission.
    ///
    /// - Parameters:
    ///   - id: The stable id, stored in the selection.
    ///   - label: The name people see, already localised.
    ///   - description: What it allows, already localised.
    ///   - group: The group heading it is listed under, already localised.
    public init(id: String, label: String, description: String? = nil, group: String? = nil) {
        self.id = id
        self.label = label
        self.description = description
        self.group = group
    }
}
