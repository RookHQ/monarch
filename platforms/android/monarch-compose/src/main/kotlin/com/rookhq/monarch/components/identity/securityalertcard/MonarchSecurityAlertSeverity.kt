package com.rookhq.monarch.components.identity.securityalertcard

/** How serious a security alert is. */
public enum class MonarchSecurityAlertSeverity {
    /** For information, such as a setting that changed. */
    Info,

    /** Worth knowing. */
    Low,

    /** Worth checking. */
    Medium,

    /** Needs attention, such as a sign-in from a new place. */
    High,

    /** Needs action now. The badge is drawn solid. */
    Critical,
}
