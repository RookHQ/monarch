package com.rookhq.monarch.components.identity.cookieconsentbanner

/** The decision a person makes on a [MonarchCookieConsentBanner]. */
public enum class MonarchCookieConsentChoice {
    /** Every optional category on. */
    AcceptAll,

    /** Every optional category off. */
    RejectAll,

    /** The notice was read (opt-out and notice regimes). */
    Acknowledge,

    /** Sale and sharing off (opt-out regime). */
    DoNotSell,
}
