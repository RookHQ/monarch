/// A way of sending marketing messages that ``MonarchMarketingConsentCheckbox`` asks about.
public enum MonarchMarketingChannel: String, Sendable, Hashable, CaseIterable {
    /// Email.
    case email
    /// Text messages.
    case sms
    /// Phone calls.
    case phone
    /// Letters.
    case post
}
