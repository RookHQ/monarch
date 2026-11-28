package dev.monarchui.components.billing.savingspanel

import androidx.compose.runtime.Immutable

/**
 * One unused item listed in a `MonarchSavingsPanel`.
 *
 * @property id a stable key.
 * @property name the item name, such as the asset or the subscription.
 * @property category the category status key, shown as an outline badge.
 * @property dateMillis the next renewal or expiry date, in epoch milliseconds (UTC).
 * @property amount what renewing it costs.
 * @property unpriced the item has no price: "No price" is shown when [amount] is `null`.
 * @property decision the decision status key, shown as a badge, such as "keep" or "let_lapse".
 */
@Immutable
public class MonarchSavingsItem(
    public val id: String,
    public val name: String,
    public val category: String? = null,
    public val dateMillis: Long? = null,
    public val amount: MonarchSavingsAmount? = null,
    public val unpriced: Boolean = false,
    public val decision: String? = null,
)
