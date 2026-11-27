/// Replacements for the built-in strings of ``MonarchCookieConsentBanner``. Each `nil` keeps the built-in text.
public struct MonarchCookieConsentBannerLabels: Sendable, Hashable {
    /// The title. Built-in: "Cookies on this site".
    public var title: String?
    /// The message. Built-in: a message for the regime.
    public var message: String?
    /// The accept button. Built-in: "Accept all".
    public var acceptAll: String?
    /// The reject button. Built-in: "Reject all".
    public var rejectAll: String?
    /// The button that opens the per-category choices. Built-in: "Customize".
    public var customize: String?
    /// The button of a notice and of the opt-out regime. Built-in: "OK".
    public var acknowledge: String?
    /// The opt-out button. Built-in: "Do not sell or share my personal information".
    public var doNotSell: String?
    /// The line shown when a Global Privacy Control signal was honoured.
    public var globalPrivacyControl: String?
    /// The policy link. Built-in: "Cookie policy".
    public var policy: String?
    /// Announced when the banner appears. Built-in: "Cookie notice shown at the bottom of the screen."
    public var announcement: String?

    /// Replacement strings.
    ///
    /// - Parameters:
    ///   - title: The title.
    ///   - message: The message.
    ///   - acceptAll: The accept button.
    ///   - rejectAll: The reject button.
    ///   - customize: The button that opens the per-category choices.
    ///   - acknowledge: The button of a notice and of the opt-out regime.
    ///   - doNotSell: The opt-out button.
    ///   - globalPrivacyControl: The line shown when a Global Privacy Control signal was honoured.
    ///   - policy: The policy link.
    ///   - announcement: Announced when the banner appears.
    public init(
        title: String? = nil,
        message: String? = nil,
        acceptAll: String? = nil,
        rejectAll: String? = nil,
        customize: String? = nil,
        acknowledge: String? = nil,
        doNotSell: String? = nil,
        globalPrivacyControl: String? = nil,
        policy: String? = nil,
        announcement: String? = nil
    ) {
        self.title = title
        self.message = message
        self.acceptAll = acceptAll
        self.rejectAll = rejectAll
        self.customize = customize
        self.acknowledge = acknowledge
        self.doNotSell = doNotSell
        self.globalPrivacyControl = globalPrivacyControl
        self.policy = policy
        self.announcement = announcement
    }
}
