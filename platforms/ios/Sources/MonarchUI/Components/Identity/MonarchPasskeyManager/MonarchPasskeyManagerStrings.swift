/// Built-in strings of the passkey manager, from the `MonarchPasskeyManager` strings table.
enum MonarchPasskeyManagerStrings {
    private static let table = "MonarchPasskeyManager"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.passkeyManager.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.passkeyManager.\(key)", table: table, argument)
    }

    /// The card title.
    static var title: String { string("title") }
    /// The line under the title.
    static var description: String { string("description") }
    /// The spoken name of the list.
    static var listLabel: String { string("listLabel") }
    /// The add button.
    static var add: String { string("add") }
    /// The rename button.
    static var rename: String { string("rename") }
    /// The spoken name of a rename button.
    static func renameLabel(_ name: String) -> String { string("renameLabel", name) }
    /// The title of the rename prompt.
    static var renameTitle: String { string("renameTitle") }
    /// The name field of the rename prompt.
    static var nameField: String { string("nameField") }
    /// The save button.
    static var save: String { string("save") }
    /// The remove button.
    static var remove: String { string("remove") }
    /// The spoken name of a remove button.
    static func removeLabel(_ name: String) -> String { string("removeLabel", name) }
    /// Before the time it was added.
    static var created: String { string("created") }
    /// Before the last-used time.
    static var lastUsed: String { string("lastUsed") }
    /// When it was never used.
    static var neverUsed: String { string("neverUsed") }
    /// The synced badge.
    static var synced: String { string("synced") }
    /// The confirmation title.
    static func confirmTitle(_ name: String) -> String { string("confirmTitle", name) }
    /// The confirmation message.
    static var confirmMessage: String { string("confirmMessage") }
    /// The extra warning for the last sign-in method.
    static var lastMethodWarning: String { string("lastMethodWarning") }
    /// The confirm button.
    static var confirmRemove: String { string("confirmRemove") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// The unsupported notice.
    static var unsupported: String { string("unsupported") }
    /// The cancelled notice.
    static var cancelled: String { string("cancelled") }
    /// The add failure notice.
    static var addFailed: String { string("addFailed") }
    /// Announced after adding.
    static var added: String { string("added") }
    /// Announced after removing.
    static func removed(_ name: String) -> String { string("removed", name) }
    /// The rename failure notice.
    static func renameFailed(_ name: String) -> String { string("renameFailed", name) }
    /// The remove failure notice.
    static func removeFailed(_ name: String) -> String { string("removeFailed", name) }
    /// Read while loading.
    static var loading: String { string("loading") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state message.
    static var emptyMessage: String { string("emptyMessage") }
    /// The error state title.
    static var errorTitle: String { string("errorTitle") }
}
