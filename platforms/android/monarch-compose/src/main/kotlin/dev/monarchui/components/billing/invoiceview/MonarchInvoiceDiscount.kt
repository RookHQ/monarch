package dev.monarchui.components.billing.invoiceview

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * The discount a `MonarchInvoiceView` uses to work out its totals when the
 * billing system gives no discount amount. It applies to the subtotal, before
 * tax, and never exceeds the subtotal.
 */
@Immutable
public sealed interface MonarchInvoiceDiscount {
    /**
     * A share of the subtotal.
     *
     * @property percent the share in percent, 0 to 100. Larger values count as 100.
     */
    @Immutable
    public class Percent(public val percent: BigDecimal) : MonarchInvoiceDiscount

    /**
     * A fixed amount off the subtotal.
     *
     * @property amount the amount, in major units of the invoice currency.
     */
    @Immutable
    public class Amount(public val amount: BigDecimal) : MonarchInvoiceDiscount
}
