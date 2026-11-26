/// A role of a ``MonarchPermissionMatrix``: a column of the grid, or a tab of the compact layout.
public struct MonarchPermissionMatrixRole: Identifiable, Sendable, Hashable {
    /// The stable id, the key of the selection.
    public let id: String
    /// The name people see, such as "Admin".
    public let label: String
    /// A short line under the name.
    public let description: String?

    /// A role.
    ///
    /// - Parameters:
    ///   - id: The stable id, the key of the selection.
    ///   - label: The name people see, already localised.
    ///   - description: A short line under the name, already localised.
    public init(id: String, label: String, description: String? = nil) {
        self.id = id
        self.label = label
        self.description = description
    }
}
