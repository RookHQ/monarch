package com.rookhq.monarch.components.intelligence.aispendcap

import androidx.compose.runtime.Immutable

/**
 * One row of the breakdown of a `MonarchAiSpendCap`: what an agent or a feature has spent this period.
 *
 * @property id a stable key for the row.
 * @property label the agent or feature, such as "Research agent". Already localised.
 * @property spent spent so far this period, as an integer in the component's unit. Negative values count as zero.
 */
@Immutable
public class MonarchAiSpendCapBreakdownItem(
    public val id: String,
    public val label: String,
    public val spent: Long,
)
