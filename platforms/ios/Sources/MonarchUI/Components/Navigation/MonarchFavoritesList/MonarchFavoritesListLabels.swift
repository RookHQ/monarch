/// Replacements for the built-in strings of ``MonarchFavoritesList``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchFavoritesListLabels: Sendable, Hashable {
    /// The heading. Built in: "Favorites".
    public var title: String?
    /// The visible title of the button that starts editing. Built in: "Edit".
    public var edit: String?
    /// The spoken name of that button. Built in: "Edit favorites".
    public var editLabel: String?
    /// The button that stops editing. Built in: "Done".
    public var done: String?
    /// Announced after a removal, with `%@` for the name. Built in: "%@ removed from favorites."
    public var removed: String?
    /// The accessibility action that moves a row up. Built in: "Move up".
    public var moveUp: String?
    /// The accessibility action that moves a row down. Built in: "Move down".
    public var moveDown: String?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        title: String? = nil,
        edit: String? = nil,
        editLabel: String? = nil,
        done: String? = nil,
        removed: String? = nil,
        moveUp: String? = nil,
        moveDown: String? = nil
    ) {
        self.title = title
        self.edit = edit
        self.editLabel = editLabel
        self.done = done
        self.removed = removed
        self.moveUp = moveUp
        self.moveDown = moveDown
    }
}
