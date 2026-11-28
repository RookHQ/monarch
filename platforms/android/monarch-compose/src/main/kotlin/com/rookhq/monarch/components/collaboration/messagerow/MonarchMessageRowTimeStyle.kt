package com.rookhq.monarch.components.collaboration.messagerow

/** How a `MonarchMessageRow` shows its time. TalkBack always reads the full date and time. */
public enum class MonarchMessageRowTimeStyle {
    /** "5 minutes ago", then a short date after a week. */
    Relative,

    /** The time of day, "2:05 PM". */
    Clock,
}
