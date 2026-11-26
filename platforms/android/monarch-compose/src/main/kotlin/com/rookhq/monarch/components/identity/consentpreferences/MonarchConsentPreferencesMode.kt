package com.rookhq.monarch.components.identity.consentpreferences

/** How a `MonarchConsentPreferences` asks. */
public enum class MonarchConsentPreferencesMode {
    /** A switch per category, with Reject all, Accept all and Save choices. */
    Categories,

    /**
     * No switches: each category with whether it is on, and only Reject all
     * and Accept all. For a product with a single optional category, where a
     * switch would only repeat the buttons.
     */
    Choice,
}
