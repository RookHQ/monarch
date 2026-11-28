package com.rookhq.monarch.components.intelligence.aiusagemeter

import androidx.compose.runtime.Immutable

/**
 * One quota of a `MonarchAiUsageMeter`, for example this turn, this conversation or this month.
 *
 * @property label what the scope covers, such as "This month". Already localised.
 * @property used the amount used so far, in [unit]. A negative amount counts as zero.
 * @property limit the quota. `null`, zero, a negative or a value that is not finite is no limit: the amount is
 * shown alone, with no meter and no warning.
 * @property unit what is counted.
 * @property id a stable key. `null` identifies the scope by its position, so labels may repeat.
 */
@Immutable
public class MonarchAiUsageScope(
    public val label: String,
    public val used: Double,
    public val limit: Double? = null,
    public val unit: MonarchAiUsageUnit = MonarchAiUsageUnit.Tokens,
    public val id: String? = null,
)
