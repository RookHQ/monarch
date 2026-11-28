package dev.monarchui.components.billing.lineitemseditor

import androidx.compose.runtime.Immutable
import java.math.BigDecimal
import java.util.UUID

/**
 * One billable row of a `MonarchLineItemsEditor`: what is charged for, how many and the price of one.
 *
 * @property id the stable key of the row. Give every row a unique id: rows are matched by it.
 * @property description what is being charged for. Already localised by whoever typed it.
 * @property quantity how many, or `null` while the field is empty (counted as zero).
 * @property unitPrice the price of one in major units (dollars, rupees), or `null` while the field is empty
 * (counted as zero). Amounts are worked out in decimal arithmetic, never floating point.
 */
@Immutable
public class MonarchLineItem(
    public val id: String = UUID.randomUUID().toString(),
    public val description: String = "",
    public val quantity: BigDecimal? = BigDecimal.ONE,
    public val unitPrice: BigDecimal? = null,
) {
    /** A copy with the given fields changed. */
    internal fun edited(
        description: String = this.description,
        quantity: BigDecimal? = this.quantity,
        unitPrice: BigDecimal? = this.unitPrice,
    ): MonarchLineItem = MonarchLineItem(id, description, quantity, unitPrice)
}
