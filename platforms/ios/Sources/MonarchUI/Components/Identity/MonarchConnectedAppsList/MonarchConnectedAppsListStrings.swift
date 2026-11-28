/// Built-in strings of the connected apps list, from the `MonarchConnectedAppsList` strings table.
enum MonarchConnectedAppsListStrings {
    private static let table = "MonarchConnectedAppsList"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.connectedAppsList.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: String) -> String {
        MonarchStrings.string("monarch.connectedAppsList.\(key)", table: table, argument)
    }

    /// The card title.
    static var title: String { string("title") }
    /// The line under the title.
    static var description: String { string("description") }
    /// The badge of a reviewed app.
    static var verified: String { string("verified") }
    /// The badge of a suspended app.
    static var suspended: String { string("suspended") }
    /// The remove button.
    static var revoke: String { string("revoke") }
    /// The confirmation message.
    static var confirmMessage: String { string("confirmMessage") }
    /// The cancel button of the confirmation.
    static var cancel: String { string("cancel") }
    /// Read while loading.
    static var loading: String { string("loading") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state message.
    static var emptyMessage: String { string("emptyMessage") }
    /// The error state title.
    static var errorTitle: String { string("errorTitle") }

    /// The connected time.
    static func connected(_ time: String) -> String { string("connected", time) }
    /// The last-used time.
    static func lastUsed(_ time: String) -> String { string("lastUsed", time) }
    /// The workspace.
    static func workspace(_ name: String) -> String { string("workspace", name) }
    /// The spoken name of a scope list.
    static func scopes(_ name: String) -> String { string("scopes", name) }
    /// The spoken name of a remove button.
    static func revokeLabel(_ name: String) -> String { string("revokeLabel", name) }
    /// The confirmation title.
    static func confirmTitle(_ name: String) -> String { string("confirmTitle", name) }
    /// Announced after access is removed.
    static func revoked(_ name: String) -> String { string("revoked", name) }
}
