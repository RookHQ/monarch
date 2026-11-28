package dev.monarchui.components.billing.usagemeter

import androidx.compose.runtime.Immutable

/**
 * One metered quantity of a `MonarchUsageMeter`: requests, messages, storage, seats, minutes.
 *
 * @property label what is measured, such as "Requests". Already localised.
 * @property used the amount used in the period.
 * @property limit the limit or quota. `null`, zero, a negative or a value that is not finite is no limit:
 * the count is shown alone, with no meter.
 * @property unit written after the figures with a space, such as "GB".
 * @property description one line under the figures, such as what counts as a unit.
 * @property id a stable key. Defaults to the label.
 */
@Immutable
public class MonarchUsageMeterItem(
    public val label: String,
    public val used: Double,
    public val limit: Double? = null,
    public val unit: String? = null,
    public val description: String? = null,
    public val id: String = label,
)
