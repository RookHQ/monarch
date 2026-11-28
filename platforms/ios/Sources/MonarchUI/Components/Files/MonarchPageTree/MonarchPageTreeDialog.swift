/// The dialog a ``MonarchPageTree`` is showing.
enum MonarchPageTreeDialog: Equatable {
    /// Rename a group, with the name in a text field.
    case rename(MonarchPageTreeGroup)
    /// Add a group, with the name in a text field.
    case addGroup
    /// Confirm deleting a group; with pages, its name must be typed.
    case deleteGroup(MonarchPageTreeGroup)
    /// Confirm deleting a page.
    case deletePage(MonarchPageTreePage)

    /// Whether the dialog asks for text.
    var hasTextField: Bool {
        switch self {
        case .rename, .addGroup: true
        case .deleteGroup(let group): MonarchPageTreeTokens.needsPhrase(group)
        case .deletePage: false
        }
    }
}
