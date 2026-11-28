package com.rookhq.monarch.components.identity.consenthistory

/** The rules that applied to a consent decision. */
public enum class MonarchConsentRegime {
    /** Consent was asked before anything was stored or used. */
    OptIn,

    /** Use was allowed until the person refused. */
    OptOut,

    /** A notice only; no choice was needed. */
    Notice,
}
