/// How a consent decision was made.
public enum MonarchConsentMethod: String, Sendable, Hashable, CaseIterable {
    /// Every optional category was allowed.
    case acceptAll
    /// Every optional category was refused.
    case rejectAll
    /// The person chose category by category.
    case custom
    /// A Global Privacy Control signal from the browser refused selling and sharing.
    case globalPrivacyControl
    /// A notice was acknowledged where no choice is required.
    case acknowledged
    /// An earlier consent was withdrawn.
    case withdrawn
}
