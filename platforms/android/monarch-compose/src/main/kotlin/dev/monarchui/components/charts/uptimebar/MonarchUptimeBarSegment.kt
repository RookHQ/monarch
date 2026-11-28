package dev.monarchui.components.charts.uptimebar

import androidx.compose.runtime.Immutable

/**
 * One period of a `MonarchUptimeBar`, usually a day.
 *
 * @property id the stable id of the period, such as the ISO date.
 * @property label the period as written, such as "Mar 12". Shown in the details and read by TalkBack. Already
 * localised.
 * @property status the state. `null` derives it from [uptime] with the bar's thresholds; with neither, the period
 * has no data.
 * @property uptime the share of checks that passed, in percent (0 to 100). `null` when nothing was checked.
 * @property details extra lines of the details, such as checks or response times.
 * @property note a short note, such as the title of an incident.
 */
@Immutable
public class MonarchUptimeBarSegment(
    public val id: String,
    public val label: String,
    public val status: MonarchUptimeBarStatus? = null,
    public val uptime: Double? = null,
    public val details: List<MonarchUptimeBarDetail> = emptyList(),
    public val note: String? = null,
)
