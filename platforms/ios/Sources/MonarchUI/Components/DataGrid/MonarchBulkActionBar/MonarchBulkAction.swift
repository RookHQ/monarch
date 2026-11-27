/// One action of a ``MonarchBulkActionBar``, run on the whole selection.
public struct MonarchBulkAction: Identifiable {
    /// A stable identifier, also used for `busyActionID`.
    public let id: String
    /// The button title, already localised.
    public let title: String
    /// A decorative SF Symbol before the title.
    public let systemImage: String?
    /// Whether the action destroys data. It is drawn in the danger variant;
    /// confirm it in `action` (for example with `confirmationDialog`).
    public let isDestructive: Bool
    /// Whether the action cannot run on this selection.
    public let isDisabled: Bool
    /// Runs the action on the selection.
    public let action: @MainActor () -> Void

    /// An action.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - title: The button title, already localised.
    ///   - systemImage: A decorative SF Symbol before the title.
    ///   - isDestructive: Whether the action destroys data.
    ///   - isDisabled: Whether the action cannot run on this selection.
    ///   - action: Runs the action on the selection.
    public init(
        id: String,
        title: String,
        systemImage: String? = nil,
        isDestructive: Bool = false,
        isDisabled: Bool = false,
        action: @escaping @MainActor () -> Void
    ) {
        self.id = id
        self.title = title
        self.systemImage = systemImage
        self.isDestructive = isDestructive
        self.isDisabled = isDisabled
        self.action = action
    }
}
