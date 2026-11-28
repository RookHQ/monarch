package dev.monarchui.components.billing.invoiceview

import androidx.compose.runtime.Immutable

/**
 * One side of a `MonarchInvoiceView`: who bills (`from`) or who is billed (`to`).
 *
 * @property name the organisation or person name.
 * @property details the address, tax registration, email and similar lines, one per entry.
 */
@Immutable
public class MonarchInvoiceParty(
    public val name: String,
    public val details: List<String> = emptyList(),
)
