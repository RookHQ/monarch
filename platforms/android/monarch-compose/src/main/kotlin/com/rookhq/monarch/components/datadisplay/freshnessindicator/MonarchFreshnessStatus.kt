package com.rookhq.monarch.components.datadisplay.freshnessindicator

/** How fresh the data of a `MonarchFreshnessIndicator` is. The same names exist on every platform. */
public enum class MonarchFreshnessStatus {
    /** Updated within the stale limit. Success tone. */
    Fresh,

    /** Older than the stale limit. Warning tone, with "Out of date". */
    Stale,

    /** Older than the expired limit. Danger tone, with "Very out of date". */
    Expired,

    /** Never updated. Neutral tone. */
    Never,

    /** The status itself is being read. Info tone, with "Checking…". */
    Checking,

    /** The status could not be read, or the last sync failed. Danger tone. */
    Error,
}
