/// Built-in strings of the linked docs panel, from the `MonarchLinkedDocsPanel` strings table.
enum MonarchLinkedDocsPanelStrings {
    private static let table = "MonarchLinkedDocsPanel"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.linkedDocsPanel.\(key)", table: table)
    }

    /// The card title.
    static var title: String { string("title") }
    /// Links a doc.
    static var linkDoc: String { string("linkDoc") }
    /// Adds a link.
    static var addLink: String { string("addLink") }
    /// Creates a doc.
    static var newDoc: String { string("newDoc") }
    /// The search prompt.
    static var searchDocs: String { string("searchDocs") }
    /// No doc matches.
    static var noDocs: String { string("noDocs") }
    /// The empty text.
    static var empty: String { string("empty") }
    /// The empty text of a read-only panel.
    static var emptyReadOnly: String { string("emptyReadOnly") }
    /// Read while loading.
    static var loading: String { string("loading") }
    /// The add menu.
    static var add: String { string("add") }
    /// The space filter.
    static var filterBySpace: String { string("filterBySpace") }
    /// Every space.
    static var all: String { string("all") }
    /// No space.
    static var noSpace: String { string("noSpace") }
    /// How many rows the filter hides.
    static func hiddenByFilter(_ count: Int) -> String {
        MonarchStrings.string(
            "monarch.linkedDocsPanel.hiddenByFilter.\(count == 1 ? "one" : "other")",
            table: table,
            count
        )
    }
    /// Clears the filter.
    static var showAll: String { string("showAll") }
    /// The spoken name of a row's unlink button.
    static func unlink(_ title: String) -> String {
        MonarchStrings.string("monarch.linkedDocsPanel.unlink", table: table, title)
    }
    /// Unlinking failed.
    static var unlinkFailed: String { string("unlinkFailed") }
    /// Linking failed.
    static var linkFailed: String { string("linkFailed") }
    /// The URL field.
    static var urlLabel: String { string("urlLabel") }
    /// An invalid URL.
    static var urlInvalid: String { string("urlInvalid") }
    /// The title field.
    static var titleLabel: String { string("titleLabel") }
    /// The confirm button of the link form.
    static var confirmAdd: String { string("confirmAdd") }
    /// Cancel.
    static var cancel: String { string("cancel") }
}
