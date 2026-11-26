package com.rookhq.monarch.components.collaboration.activitytimeline

import java.util.Calendar
import java.util.TimeZone

/**
 * The events of one day.
 *
 * @property day the day number in the time zone: days since 1 January 1970.
 * @property events the day's events, in the order given.
 */
internal class MonarchActivityTimelineDay(val day: Long, val events: List<MonarchActivityTimelineEvent>)

/** The pure day rules of MonarchActivityTimeline: day numbers, grouping and headings. */
internal object MonarchActivityTimelineDays {
    /** One day in milliseconds. */
    const val DAY: Long = 86_400_000L

    /** The day number of [time] in [zone]: days since 1 January 1970 in that zone's calendar. */
    fun dayNumber(time: Long, zone: TimeZone): Long = Math.floorDiv(time + zone.getOffset(time), DAY)

    /** The year of [time] in [zone]. */
    fun year(time: Long, zone: TimeZone): Int =
        Calendar.getInstance(zone).apply { timeInMillis = time }.get(Calendar.YEAR)

    /** [events] grouped by day in [zone]. Days appear in the order of their first event; the order inside is kept. */
    fun group(events: List<MonarchActivityTimelineEvent>, zone: TimeZone): List<MonarchActivityTimelineDay> =
        events.groupBy { dayNumber(it.time, zone) }.map { (day, list) -> MonarchActivityTimelineDay(day, list) }

    /**
     * The heading of the day of [time]: [today] or [yesterday] relative to [now], otherwise [format] of the day
     * (which writes the year when [time] is in another year than [now]).
     */
    @Suppress("LongParameterList")
    fun heading(
        time: Long,
        now: Long,
        zone: TimeZone,
        today: String,
        yesterday: String,
        format: (time: Long, withYear: Boolean) -> String,
    ): String {
        val day = dayNumber(time, zone)
        val current = dayNumber(now, zone)
        return when (day) {
            current -> today
            current - 1 -> yesterday
            else -> format(time, year(time, zone) != year(now, zone))
        }
    }

    /** The sentence of [event]: the actor, the verb and the target, joined by spaces. */
    fun sentence(event: MonarchActivityTimelineEvent): String =
        listOfNotNull(event.actor?.name, event.verb, event.target).joinToString(" ")

    /** Whether [event] was done by an automation or the system. */
    fun automated(event: MonarchActivityTimelineEvent): Boolean =
        event.actor != null && event.actor.kind != MonarchActivityTimelineActorKind.Person

    /**
     * The event as TalkBack reads it: the sentence, the absolute [date], "Automated" for rules, the tags and the
     * body: "Ana Ruiz moved Acme renewal, Mar 4, 2026, 2:00 PM, Negotiation".
     */
    fun spoken(event: MonarchActivityTimelineEvent, date: String, automated: String): String =
        (listOf(sentence(event), date) + listOfNotNull(automated.takeIf { automated(event) }) + event.tags +
            listOfNotNull(event.body)).joinToString(", ")
}
