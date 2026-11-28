package dev.monarchui.components.billing.lineitemseditor

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * A discount on the subtotal of a `MonarchLineItemsEditor`. It is never more than the subtotal; zero or a
 * negative value takes nothing off.
 *
 * @property kind a percentage or a fixed amount.
 * @property value the percentage (5 for 5%) or the amount in major units.
 */
@Immutable
public class MonarchLineItemsDiscount(
    public val kind: MonarchLineItemsDiscountKind,
    public val value: BigDecimal,
)
