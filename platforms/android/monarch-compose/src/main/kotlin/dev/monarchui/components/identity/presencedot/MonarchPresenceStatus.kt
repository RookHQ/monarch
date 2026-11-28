package dev.monarchui.components.identity.presencedot

/** A person's presence, shown by `MonarchPresenceDot`. The same five names exist on every platform. */
public enum class MonarchPresenceStatus {
    /** Online: a dot in the success colour. */
    Online,

    /** Away: a dot in the warning colour. */
    Away,

    /** Busy: a dot in the danger colour. */
    Busy,

    /** Do not disturb: a dot in the danger colour with a bar across it, so it differs from busy by form. */
    DoNotDisturb,

    /** Offline: a ring in the subtle text colour. */
    Offline,
}
