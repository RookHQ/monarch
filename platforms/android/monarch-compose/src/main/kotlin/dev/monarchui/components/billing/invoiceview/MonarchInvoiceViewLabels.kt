package dev.monarchui.components.billing.invoiceview

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchInvoiceView`, for
 * example with the labels of the invoice's own language. Every `null` uses
 * the built-in string named after it in `strings_invoiceview.xml`.
 *
 * @property title the default title: "Invoice".
 * @property from the heading over the issuer: "From".
 * @property to the heading over the billed party: "Bill to".
 * @property issueDate the label of the issue date: "Issue date".
 * @property dueDate the label of the due date: "Due date".
 * @property lineItems the heading over the lines, also the name of the list: "Line items".
 * @property description the column heading of the descriptions: "Description".
 * @property quantity the column heading and inline label of quantities: "Qty".
 * @property unitPrice the column heading and inline label of unit prices: "Unit price".
 * @property amount the column heading and inline label of line amounts: "Amount".
 * @property subtotal the subtotal row: "Subtotal".
 * @property discount the discount row: "Discount".
 * @property tax the tax row when no rate is shown: "Tax".
 * @property taxWithRate the tax row with the formatted rate ("18%"): "Tax (18%)".
 * @property total the total row: "Total".
 * @property amountPaid the amount paid row: "Amount paid".
 * @property amountDue the amount due row: "Amount due".
 * @property noItems shown when there are no lines: "No line items.".
 * @property notes the heading over the notes: "Notes".
 * @property print the print button: "Print".
 */
@Immutable
public class MonarchInvoiceViewLabels(
    public val title: String? = null,
    public val from: String? = null,
    public val to: String? = null,
    public val issueDate: String? = null,
    public val dueDate: String? = null,
    public val lineItems: String? = null,
    public val description: String? = null,
    public val quantity: String? = null,
    public val unitPrice: String? = null,
    public val amount: String? = null,
    public val subtotal: String? = null,
    public val discount: String? = null,
    public val tax: String? = null,
    public val taxWithRate: ((String) -> String)? = null,
    public val total: String? = null,
    public val amountPaid: String? = null,
    public val amountDue: String? = null,
    public val noItems: String? = null,
    public val notes: String? = null,
    public val print: String? = null,
)
