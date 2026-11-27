/// One Space of a workspace, offered by ``MonarchSpaceSwitcher``.
public struct MonarchSpaceOption: Identifiable, Sendable, Hashable {
    /// A stable id, such as the Rook ID Space id. It is the selection.
    public let id: String
    /// The Space's name, such as "Marketing".
    public let name: String
    /// A secondary line in the list, such as "9 members".
    public let description: String?
    /// An SF Symbol for the Space. Decorative.
    public let systemImage: String?

    /// A Space.
    ///
    /// - Parameters:
    ///   - id: A stable id. It is the selection.
    ///   - name: The Space's name.
    ///   - description: A secondary line in the list, such as the member count.
    ///   - systemImage: An SF Symbol for the Space. Decorative.
    public init(id: String, name: String, description: String? = nil, systemImage: String? = nil) {
        self.id = id
        self.name = name
        self.description = description
        self.systemImage = systemImage
    }
}
