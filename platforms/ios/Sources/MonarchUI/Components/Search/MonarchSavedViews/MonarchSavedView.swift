/// One named view of a list, offered by ``MonarchSavedViews``.
///
/// ```swift
/// MonarchSavedView("Open deals", id: "open", count: 12, isDefault: true)
/// MonarchSavedView("All deals", id: "all", isReadOnly: true, group: "Built-in")
/// ```
public struct MonarchSavedView: Identifiable, Hashable, Sendable {
    /// The identity of the view.
    public let id: String
    /// The name, as the person typed it.
    public var name: String
    /// The number of items in the view, shown after its name.
    public var count: Int?
    /// Whether the view is shared with other people.
    public var isShared: Bool
    /// Whether the view opens by default.
    public var isDefault: Bool
    /// Whether the view is built in or someone else's: it cannot be saved over, renamed or deleted.
    public var isReadOnly: Bool
    /// The group heading in the menu, such as "Built-in" or "Shared with me". Views keep their
    /// order within a group.
    public var group: String?

    /// A saved view.
    ///
    /// - Parameters:
    ///   - name: The name, as the person typed it.
    ///   - id: The identity of the view.
    ///   - count: The number of items in the view.
    ///   - isShared: Whether the view is shared with other people.
    ///   - isDefault: Whether the view opens by default.
    ///   - isReadOnly: Whether the view cannot be saved over, renamed or deleted.
    ///   - group: The group heading in the menu.
    public init(
        _ name: String,
        id: String,
        count: Int? = nil,
        isShared: Bool = false,
        isDefault: Bool = false,
        isReadOnly: Bool = false,
        group: String? = nil
    ) {
        self.name = name
        self.id = id
        self.count = count
        self.isShared = isShared
        self.isDefault = isDefault
        self.isReadOnly = isReadOnly
        self.group = group
    }
}
