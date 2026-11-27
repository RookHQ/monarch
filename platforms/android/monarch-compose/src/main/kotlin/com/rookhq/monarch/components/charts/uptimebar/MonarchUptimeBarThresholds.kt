package com.rookhq.monarch.components.charts.uptimebar

import androidx.compose.runtime.Immutable

/**
 * The cut points for periods that have an uptime but no status.
 *
 * @property operational at or above this uptime, in percent, a period is operational.
 * @property degraded at or above this uptime (and below [operational]) a period is degraded; below it, down.
 */
@Immutable
public class MonarchUptimeBarThresholds(
    public val operational: Double = 99.9,
    public val degraded: Double = 99.0,
)
