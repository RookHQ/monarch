package com.rookhq.monarch.components.identity.securityalertcard

/** The loading state of a `MonarchSecurityAlertCard`. */
public enum class MonarchSecurityAlertCardState {
    /** The alerts are loaded. An empty list shows the empty state. */
    Ready,

    /** The alerts are loading. */
    Loading,

    /** Loading failed. */
    Failed,
}
