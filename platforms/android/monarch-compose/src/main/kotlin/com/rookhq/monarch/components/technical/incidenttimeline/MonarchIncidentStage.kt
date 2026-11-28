package com.rookhq.monarch.components.technical.incidenttimeline

/**
 * The standard incident stages, in the order an incident usually moves through them. `MonarchIncidentTimeline` gives
 * each a built-in label and tone; products add their own stages by key.
 *
 * @property key the stage key used in `MonarchIncidentUpdate.stage`, as on the web.
 */
public enum class MonarchIncidentStage(public val key: String) {
    /** The cause is not known yet. Danger tone. */
    Investigating("investigating"),

    /** The cause is known and a fix is in progress. Warning tone. */
    Identified("identified"),

    /** A fix is out and being watched. Info tone. */
    Monitoring("monitoring"),

    /** The incident is over. Success tone. */
    Resolved("resolved"),
}
