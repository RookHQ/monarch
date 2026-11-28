package com.rookhq.monarch.components.billing.invoiceview

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

/**
 * The totals of a `MonarchInvoiceView` as the billing system calculated them.
 * A value left `null` is worked out from the line items, the tax rate and the
 * discount; a value given here is shown exactly as given.
 *
 * @property subtotal the sum of the lines.
 * @property discount the discount, as a positive amount. Shown only when it is not zero.
 * @property tax the tax. Shown when given or when the invoice has a tax rate.
 * @property total the amount of the invoice.
 * @property amountPaid payments already received. Shown only when given.
 * @property amountDue what is still owed. Shown only when given.
 */
@Immutable
public class MonarchInvoiceTotals(
    public val subtotal: BigDecimal? = null,
    public val discount: BigDecimal? = null,
    public val tax: BigDecimal? = null,
    public val total: BigDecimal? = null,
    public val amountPaid: BigDecimal? = null,
    public val amountDue: BigDecimal? = null,
)
