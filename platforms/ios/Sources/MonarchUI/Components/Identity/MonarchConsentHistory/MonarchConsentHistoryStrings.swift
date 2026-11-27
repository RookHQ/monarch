/// Built-in strings of the consent history, from the `MonarchConsentHistory` strings table.
enum MonarchConsentHistoryStrings {
    /// Reads one key of the table.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.consentHistory.\(key)", table: "MonarchConsentHistory")
    }

    /// The default heading.
    static var title: String { string("title") }
    /// The default title of the export button.
    static var export: String { string("export") }
    /// The text of a method.
    static func method(_ method: MonarchConsentMethod) -> String { string("method.\(method.rawValue)") }
    /// The text of a regime.
    static func regime(_ regime: MonarchConsentRegime) -> String { string("regime.\(regime.rawValue)") }
    /// The name of the method line.
    static var methodTitle: String { string("methodTitle") }
    /// The name of the policy version line.
    static var policyVersionTitle: String { string("policyVersionTitle") }
    /// The name of the regime line.
    static var regimeTitle: String { string("regimeTitle") }
    /// The name of the categories list.
    static var categoriesTitle: String { string("categoriesTitle") }
    /// After an allowed category.
    static var allowed: String { string("allowed") }
    /// After a refused category.
    static var denied: String { string("denied") }
    /// The extra line for a privacy signal.
    static var signalPresent: String { string("signalPresent") }
    /// The empty state title.
    static var emptyTitle: String { string("emptyTitle") }
    /// The empty state message.
    static var emptyMessage: String { string("emptyMessage") }
    /// The export failure message.
    static var exportFailed: String { string("exportFailed") }
    /// The label of the date of a record.
    static var dateTitle: String { string("dateTitle") }
}
