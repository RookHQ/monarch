/// Replacements for the built-in strings of ``MonarchConsentPreferences``.
///
/// Every property is `nil` by default, which keeps the built-in string. Use
/// them to translate or to use your legal team's wording.
public struct MonarchConsentPreferencesLabels: Sendable {
    /// The title. Built in: "Privacy choices".
    public var title: String?
    /// The text under the title.
    public var description: String?
    /// The title of the policy link. Built in: "Privacy policy".
    public var policy: String?
    /// Shown on a required category. Built in: "Always on".
    public var alwaysOn: String?
    /// Shown on a sale-or-sharing category while a Global Privacy Control signal is on.
    public var globalPrivacyControl: String?
    /// The status of an allowed category in choice mode. Built in: "On".
    public var on: String?
    /// The status of a refused category in choice mode. Built in: "Off".
    public var off: String?
    /// The button that refuses every optional category. Built in: "Reject all".
    public var rejectAll: String?
    /// The button that allows every optional category. Built in: "Accept all".
    public var acceptAll: String?
    /// The button that saves the switches. Built in: "Save choices".
    public var save: String?
    /// The button that withdraws a saved choice. Built in: "Withdraw consent".
    public var withdraw: String?
    /// The policy version line, from the version. Built in: "Policy version 3".
    public var policyVersion: (@Sendable (String) -> String)?
    /// The saved-time line, from the formatted time. Built in: "Saved 2 days ago".
    public var saved: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        title: String? = nil,
        description: String? = nil,
        policy: String? = nil,
        alwaysOn: String? = nil,
        globalPrivacyControl: String? = nil,
        on: String? = nil,
        off: String? = nil,
        rejectAll: String? = nil,
        acceptAll: String? = nil,
        save: String? = nil,
        withdraw: String? = nil,
        policyVersion: (@Sendable (String) -> String)? = nil,
        saved: (@Sendable (String) -> String)? = nil
    ) {
        self.title = title
        self.description = description
        self.policy = policy
        self.alwaysOn = alwaysOn
        self.globalPrivacyControl = globalPrivacyControl
        self.on = on
        self.off = off
        self.rejectAll = rejectAll
        self.acceptAll = acceptAll
        self.save = save
        self.withdraw = withdraw
        self.policyVersion = policyVersion
        self.saved = saved
    }
}
