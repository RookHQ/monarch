package dev.monarchui.components.billing.invoiceview

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * One billed line of a `MonarchInvoiceView`. Amounts are in major units of
 * the invoice currency (19.99, not 1999).
 *
 * @property id a stable key of the line.
 * @property description what was billed, such as "Design review".
 * @property detail a second line under the description, such as a period or a SKU.
 * @property quantity the quantity, which may have decimals (6.5 hours).
 * @property unitPrice the price of one unit.
 * @property amount the line amount as the billing system calculated it. `null` works it out as quantity
 * times unit price, rounded to the currency's minor unit.
 */
@Immutable
public class MonarchInvoiceLineItem(
    public val id: String,
    public val description: String,
    public val quantity: BigDecimal,
    public val unitPrice: BigDecimal,
    public val detail: String? = null,
    public val amount: BigDecimal? = null,
)
