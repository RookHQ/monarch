package dev.monarchui.components.identity.cookieconsentbanner

/** Which consent rule a [MonarchCookieConsentBanner] follows; it decides the text and the choices. */
public enum class MonarchCookieConsentRegime {
    /** Ask first: optional tracking stays off until the person accepts. Customize, Reject all and Accept all. */
    OptIn,

    /** On until the person opts out of sale or sharing. "Do not sell or share my personal information" and OK. */
    OptOut,

    /** A notice only. OK. */
    Notice,
}
