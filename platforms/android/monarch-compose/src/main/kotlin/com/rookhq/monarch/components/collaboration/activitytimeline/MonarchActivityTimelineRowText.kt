package com.rookhq.monarch.components.collaboration.activitytimeline

/**
 * The texts one event row shows.
 *
 * @property relative the relative time, such as "1 hr. ago". Visual only.
 * @property spoken the event as TalkBack reads it.
 * @property automated the "Automated" tag, or `null` for people.
 * @property details the Details disclosure's name.
 * @property hideDetails its name while the details are shown.
 */
internal class MonarchActivityTimelineRowText(
    val relative: String,
    val spoken: String,
    val automated: String?,
    val details: String,
    val hideDetails: String,
)
