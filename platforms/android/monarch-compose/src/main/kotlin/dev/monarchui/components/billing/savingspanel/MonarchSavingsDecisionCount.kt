package dev.monarchui.components.billing.savingspanel

import androidx.compose.runtime.Immutable

/**
 * The number of unused items with one decision in a `MonarchSavingsPanel`,
 * such as keep, review, let lapse or undecided.
 *
 * @property id a stable key.
 * @property label the visible label, such as "Let lapse".
 * @property count the number of items with this decision.
 */
@Immutable
public class MonarchSavingsDecisionCount(
    public val id: String,
    public val label: String,
    public val count: Int,
)
