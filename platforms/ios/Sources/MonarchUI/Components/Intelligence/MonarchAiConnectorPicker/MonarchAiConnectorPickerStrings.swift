/// Built-in strings of the connector picker, from the `MonarchAiConnectorPicker` strings table.
enum MonarchAiConnectorPickerStrings {
    private static let table = "MonarchAiConnectorPicker"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiConnectorPicker.\(key)", table: table)
    }

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// A count noun: the `.one` key for 1, else the `.other` key, with the formatted count.
    private static func counted(_ key: String, _ count: Int) -> String {
        string(count == 1 ? "\(key).one" : "\(key).other", count.formatted())
    }

    /// The navigation title.
    static var title: String { string("title") }
    /// The search prompt.
    static var searchPrompt: String { string("searchPrompt") }
    /// The done button.
    static var done: String { string("done") }
    /// The summary line.
    static func summary(on: Int, total: Int, asking: Int) -> String {
        let connectors = string(
            total == 1 ? "connectorsOn.one" : "connectorsOn.other",
            on.formatted(),
            total.formatted()
        )
        return string("summary", connectors, counted("toolsAsk", asking))
    }
    /// The search result count.
    static func results(_ count: Int) -> String {
        count == 0 ? string("results.none") : counted("results", count)
    }
    /// The no-results message.
    static func noResults(_ query: String) -> String { string("noResults", query) }
    /// The clear search button.
    static var clearSearch: String { string("clearSearch") }
    /// The empty message.
    static var empty: String { string("empty") }
    /// The text of a status.
    static func status(_ status: MonarchAiConnectorStatus) -> String { string("status.\(status.rawValue)") }
    /// After Connected.
    static func account(_ account: String) -> String { string("account", account) }
    /// The spoken name of a switch.
    static func enable(_ name: String) -> String { string("enable", name) }
    /// Why a switch is off.
    static func disabledReason(_ status: MonarchAiConnectorStatus) -> String {
        string("disabledReason.\(status.rawValue)")
    }
    /// The sign-in button.
    static var signIn: String { string("signIn") }
    /// The reconnect button.
    static var reconnect: String { string("reconnect") }
    /// The manage button.
    static var manage: String { string("manage") }
    /// The spoken name of a row button.
    static func actionLabel(_ action: String, _ name: String) -> String { string("actionLabel", action, name) }
    /// The tools title.
    static func tools(_ count: Int) -> String { counted("tools", count) }
    /// The name of a permission picker.
    static func permissionLabel(_ tool: String) -> String { string("permissionLabel", tool) }
    /// The name of a permission.
    static func permission(_ permission: MonarchAiConnectorToolPermission) -> String {
        string("permission.\(permission.rawValue)")
    }
    /// The badge of a risk.
    static func risk(_ risk: MonarchAiConnectorToolRisk) -> String { string("risk.\(risk.rawValue)") }
    /// Why Allow is not offered.
    static var destructiveLocked: String { string("destructiveLocked") }
}
