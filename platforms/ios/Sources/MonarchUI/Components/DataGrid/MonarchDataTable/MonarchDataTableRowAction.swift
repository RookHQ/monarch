/// An action on one row of a ``MonarchDataTable``, offered in the row's
/// actions menu, its context menu and, in the list layout, as a swipe action
/// and a VoiceOver action.
public struct MonarchDataTableRowAction: Identifiable {
    /// A stable key.
    public var id: String
    /// The title, already localised.
    public var title: String
    /// An SF Symbol shown with the title.
    public var systemImage: String?
    /// Whether the action destroys data. It is drawn in the danger role.
    public var isDestructive: Bool
    /// Whether the action is unavailable for this row.
    public var isDisabled: Bool
    /// Runs the action.
    public var action: @MainActor () -> Void

    /// A row action.
    ///
    /// - Parameters:
    ///   - title: The title, already localised.
    ///   - id: A stable key. By default the title.
    ///   - systemImage: An SF Symbol shown with the title.
    ///   - isDestructive: Whether the action destroys data.
    ///   - isDisabled: Whether the action is unavailable for this row.
    ///   - action: Runs the action.
    public init(
        _ title: String,
        id: String? = nil,
        systemImage: String? = nil,
        isDestructive: Bool = false,
        isDisabled: Bool = false,
        action: @escaping @MainActor () -> Void
    ) {
        self.id = id ?? title
        self.title = title
        self.systemImage = systemImage
        self.isDestructive = isDestructive
        self.isDisabled = isDisabled
        self.action = action
    }
}
