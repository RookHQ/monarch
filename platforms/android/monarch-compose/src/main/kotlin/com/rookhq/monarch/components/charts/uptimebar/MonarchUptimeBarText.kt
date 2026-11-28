package com.rookhq.monarch.components.charts.uptimebar

import kotlin.math.floor

/**
 * The built-in words and patterns of an uptime bar, resolved from resources or the app's labels.
 *
 * @property names the name of each state.
 * @property uptime "Uptime".
 * @property periods "Periods".
 * @property namedValue a name and a value: "%1$s %2$s".
 * @property namedCount a label and a number: "%1$s: %2$d".
 * @property periodDetails a period and its details: "%1$s: %2$s".
 */
internal class MonarchUptimeBarWords(
    val names: Map<MonarchUptimeBarStatus, String>,
    val uptime: String,
    val periods: String,
    val namedValue: String,
    val namedCount: String,
    val periodDetails: String,
)

/** The pure rules of MonarchUptimeBar: states, the overall uptime, the summary, each period's text, positions. */
internal object MonarchUptimeBarText {
    private const val SEPARATOR = ", "
    private const val SENTENCE = ". "

    /** The state of [segment]: its own status, or one derived from its uptime with [thresholds]. */
    fun status(segment: MonarchUptimeBarSegment, thresholds: MonarchUptimeBarThresholds): MonarchUptimeBarStatus {
        segment.status?.let { return it }
        val uptime = segment.uptime?.takeIf { it.isFinite() } ?: return MonarchUptimeBarStatus.NoData
        return when {
            uptime >= thresholds.operational -> MonarchUptimeBarStatus.Operational
            uptime >= thresholds.degraded -> MonarchUptimeBarStatus.Degraded
            else -> MonarchUptimeBarStatus.Down
        }
    }

    /** The overall uptime: [given] when finite, otherwise the mean of the measured periods, or `null` without any. */
    fun overall(segments: List<MonarchUptimeBarSegment>, given: Double?): Double? {
        given?.takeIf { it.isFinite() }?.let { return it }
        val measured = segments.mapNotNull { it.uptime?.takeIf { value -> value.isFinite() } }
        return if (measured.isEmpty()) null else measured.average()
    }

    /** How many periods have each state, only the states present, in the order of [MonarchUptimeBarStatus]. */
    fun counts(statuses: List<MonarchUptimeBarStatus>): Map<MonarchUptimeBarStatus, Int> =
        MonarchUptimeBarStatus.entries.associateWith { status -> statuses.count { it == status } }
            .filterValues { it > 0 }

    /** The spoken summary: "Periods: 30. Operational 28, Down 2. Uptime 99.83%". */
    fun summary(
        count: Int,
        counts: Map<MonarchUptimeBarStatus, Int>,
        overall: String?,
        words: MonarchUptimeBarWords,
    ): String {
        val parts = listOf(words.namedCount.format(words.periods, count)) +
            listOfNotNull(
                counts.entries.joinToString(SEPARATOR) { (status, n) ->
                    words.namedValue.format(words.names.getValue(status), n.toString())
                }.takeIf { counts.isNotEmpty() },
                overall?.let { words.namedValue.format(words.uptime, it) },
            )
        return parts.joinToString(SENTENCE)
    }

    /**
     * One period as TalkBack reads it: "Mar 12: Degraded, Uptime 99.42%, Checks 1,440, Slow responses". The uptime
     * is written by [percent] when the period has one.
     */
    fun describe(
        segment: MonarchUptimeBarSegment,
        status: MonarchUptimeBarStatus,
        percent: (Double) -> String,
        words: MonarchUptimeBarWords,
    ): String {
        val uptime = segment.uptime?.takeIf { it.isFinite() }?.let {
            words.namedValue.format(words.uptime, percent(it))
        }
        val details = segment.details.map { words.namedValue.format(it.label, it.value) }
        val parts = listOf(words.names.getValue(status)) + listOfNotNull(uptime) + details + listOfNotNull(segment.note)
        return words.periodDetails.format(segment.label, parts.joinToString(SEPARATOR))
    }

    /** The segment under [x] pixels from the start edge of a bar of [width] with [count] segments. */
    fun indexAt(x: Float, width: Float, count: Int): Int? {
        val inside = x in 0f..width
        if (count <= 0 || width <= 0f || !inside) return null
        return floor(x / width * count).toInt().coerceIn(0, count - 1)
    }

    /**
     * The start and length of each of [count] segments along [width] pixels from the start edge, with [gap]
     * between them, or [thinGap] when [gap] would leave a segment narrower than [thin].
     */
    fun spans(count: Int, width: Float, gap: Float, thinGap: Float, thin: Float): List<Pair<Float, Float>> {
        if (count <= 0) return emptyList()
        val wide = (width - gap * (count - 1)) / count
        val space = if (wide < thin) thinGap else gap
        val length = ((width - space * (count - 1)) / count).coerceAtLeast(0f)
        return List(count) { it * (length + space) to length }
    }
}
