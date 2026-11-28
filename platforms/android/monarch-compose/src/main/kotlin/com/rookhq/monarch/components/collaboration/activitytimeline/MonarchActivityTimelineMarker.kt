package com.rookhq.monarch.components.collaboration.activitytimeline

/** What marks each event of a `MonarchActivityTimeline` on its line. An event's own icon always wins. */
public enum class MonarchActivityTimelineMarker {
    /** A dot in the event's tone. */
    Dot,

    /** The actor's avatar; a dot for events without an actor. */
    Avatar,
}
