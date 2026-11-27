/// Built-in strings of the cookie consent banner, from the `MonarchCookieConsentBanner` strings table.
enum MonarchCookieConsentBannerStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.cookieConsentBanner.\(key)", table: "MonarchCookieConsentBanner")
    }

    /// The title.
    static var title: String { string("title") }
    /// The message where consent is asked first.
    static var messageOptIn: String { string("messageOptIn") }
    /// The message where people opt out.
    static var messageOptOut: String { string("messageOptOut") }
    /// The message of a plain notice.
    static var messageNotice: String { string("messageNotice") }
    /// The accept button.
    static var acceptAll: String { string("acceptAll") }
    /// The reject button.
    static var rejectAll: String { string("rejectAll") }
    /// The button that opens the per-category choices.
    static var customize: String { string("customize") }
    /// The acknowledge button.
    static var acknowledge: String { string("acknowledge") }
    /// The opt-out button.
    static var doNotSell: String { string("doNotSell") }
    /// The line shown when a Global Privacy Control signal was honoured.
    static var globalPrivacyControl: String { string("globalPrivacyControl") }
    /// The policy link.
    static var policy: String { string("policy") }
    /// Announced when the banner appears.
    static var announcement: String { string("announcement") }
}
