package com.rookhq.monarch.components.intelligence.aidisclaimer

/** How much harm a wrong answer could do where a `MonarchAiDisclaimer` appears. */
public enum class MonarchAiDisclaimerRisk {
    /** A quiet, centred line under a composer or an answer. */
    Low,

    /** A line at the start in the default text colour with stronger wording. */
    Medium,

    /** A warning notice, for answers people might act on: money, health, legal, deleting data. */
    High,
}
