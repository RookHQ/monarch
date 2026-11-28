/// Built-in strings of the consent preferences, from the `MonarchConsentPreferences` strings table.
enum MonarchConsentPreferencesStrings {
    private static let table = "MonarchConsentPreferences"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.consentPreferences.\(key)", table: table)
    }

    /// The title.
    static var title: String { string("title") }
    /// The text under the title.
    static var description: String { string("description") }
    /// The title of the policy link.
    static var policy: String { string("policy") }
    /// Shown on a required category.
    static var alwaysOn: String { string("alwaysOn") }
    /// Shown on a sale-or-sharing category under a Global Privacy Control signal.
    static var globalPrivacyControl: String { string("globalPrivacyControl") }
    /// The status of an allowed category.
    static var on: String { string("on") }
    /// The status of a refused category.
    static var off: String { string("off") }
    /// Refuses every optional category.
    static var rejectAll: String { string("rejectAll") }
    /// Allows every optional category.
    static var acceptAll: String { string("acceptAll") }
    /// Saves the switches.
    static var save: String { string("save") }
    /// Withdraws a saved choice.
    static var withdraw: String { string("withdraw") }

    /// The policy version line.
    static func policyVersion(_ version: String) -> String {
        MonarchStrings.string("monarch.consentPreferences.policyVersion", table: table, version)
    }

    /// The saved-time line.
    static func saved(_ time: String) -> String {
        MonarchStrings.string("monarch.consentPreferences.saved", table: table, time)
    }
}
