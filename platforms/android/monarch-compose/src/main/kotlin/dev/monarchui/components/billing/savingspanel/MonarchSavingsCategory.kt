package dev.monarchui.components.billing.savingspanel

import androidx.compose.runtime.Immutable

/**
 * A group of unused items in a `MonarchSavingsPanel`, such as "Parked" or
 * "No website", shown as a status badge with its count.
 *
 * @property id a stable key, also the status key looked up in the panel's registry.
 * @property count the number of items in the group. A group with 0 is not shown.
 * @property label the visible label. `null` uses the registry's label for [id].
 */
@Immutable
public class MonarchSavingsCategory(
    public val id: String,
    public val count: Int,
    public val label: String? = null,
)
