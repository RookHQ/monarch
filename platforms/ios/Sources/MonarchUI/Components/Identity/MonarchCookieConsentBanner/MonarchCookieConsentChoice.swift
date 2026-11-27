/// What a person chose in a ``MonarchCookieConsentBanner``.
public enum MonarchCookieConsentChoice: Sendable, Hashable, CaseIterable {
    /// Every optional category allowed.
    case acceptAll
    /// Only strictly necessary cookies.
    case rejectAll
    /// Opted out of the sale or sharing of personal information.
    case optOutOfSaleOrSharing
    /// The notice was read.
    case acknowledge
}
