package com.rookhq.monarch.components.billing.invoiceview

import androidx.compose.runtime.Immutable

/**
 * A label and a value in the facts of a `MonarchInvoiceView`, after the
 * dates: payment terms, a purchase order number, a project.
 *
 * @property label the term, such as "Payment terms".
 * @property value the value, already formatted, such as "Net 14".
 */
@Immutable
public class MonarchInvoiceDetail(
    public val label: String,
    public val value: String,
)
