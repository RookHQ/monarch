package dev.monarchui.components.identity.consenthistory

/** How a consent decision was made. */
public enum class MonarchConsentMethod {
    /** The person accepted every category. */
    AcceptAll,

    /** The person rejected every optional category. */
    RejectAll,

    /** The person chose category by category. */
    Custom,

    /** The browser or device sent a privacy signal, such as Global Privacy Control. */
    GlobalPrivacyControl,

    /** The person acknowledged a notice that asked for no choice. */
    Acknowledged,

    /** The person withdrew an earlier consent. */
    Withdrawn,
}
