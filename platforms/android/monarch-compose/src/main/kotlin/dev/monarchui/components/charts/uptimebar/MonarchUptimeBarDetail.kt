package dev.monarchui.components.charts.uptimebar

import androidx.compose.runtime.Immutable

/**
 * An extra line of a period's details, such as the number of checks or the p95 response time.
 *
 * @property label the name, such as "Checks". Already localised.
 * @property value the value as written, such as "1,440". Already formatted.
 */
@Immutable
public class MonarchUptimeBarDetail(
    public val label: String,
    public val value: String,
)
