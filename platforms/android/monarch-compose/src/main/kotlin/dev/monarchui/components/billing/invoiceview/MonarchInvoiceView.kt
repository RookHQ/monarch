package dev.monarchui.components.billing.invoiceview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.internal.monarchString
import java.math.BigDecimal
import java.util.Locale

/**
 * A read-only invoice: issuer and billed party, number and dates, a status,
 * the line items, the totals and notes, with an optional Print button.
 *
 * Built by Monarch from `MonarchText`, `MonarchStatusBadge`,
 * `MonarchDescriptionList` and `MonarchMoney` in a `Column`, so it sits inside
 * the screen's own `verticalScroll` with other content. From 640dp wide the
 * lines are a table with column headings; below that each line is a block
 * with inline labels, so nothing scrolls sideways on a phone. Amounts the
 * billing system calculated are shown as given; a missing line amount or
 * total is worked out from the lines, [taxRate] and [discount], each line
 * rounded to the currency's minor unit.
 *
 * Print, when [onPrint] is `null`, composes a copy of the invoice off screen
 * in the light theme without the header actions and sends its picture to the
 * system print dialog (`PrintManager`), which also saves it as a PDF.
 *
 * Each line is one TalkBack element read as its description and labelled
 * figures; each total is read as its label and amount; the title and the
 * section names are headings.
 *
 * @param items the billed lines, read-only. Empty shows "No line items.".
 * @param currencyCode the ISO 4217 code of every amount, such as USD.
 * @param modifier the modifier of the invoice.
 * @param title the main heading. `null` uses "Invoice".
 * @param number the invoice number, written after the title: "Invoice INV-0042".
 * @param status a status beside the title, drawn as a small `MonarchStatusBadge`.
 * @param issueDateMillis the issue date, in epoch milliseconds (UTC), as the Material 3 date picker gives it.
 * @param dueDateMillis the due date, in epoch milliseconds (UTC).
 * @param details more facts after the dates: payment terms, purchase order, project.
 * @param from who issues the invoice.
 * @param to who is billed.
 * @param locale the locale of amounts and quantities, and of dates unless [dateLocale] is set. `null` uses the first
 *   locale of the configuration.
 * @param dateLocale the locale of the dates alone (an invoice in dollars dated day month year). `null` uses [locale].
 * @param taxRate the tax rate in percent, used to work out tax when [totals] has none.
 * @param discount the discount used to work out the totals when [totals] has none.
 * @param totals the totals from the billing system; they win over anything worked out here.
 * @param notes payment instructions, terms or a thank-you note, under the totals.
 * @param footer small print at the bottom, such as the registered office.
 * @param showPrintButton shows a Print button in the header.
 * @param onPrint replaces the built-in printing for the Print button, for example to print the app's PDF.
 * @param brand a logo or wordmark at the top. Give it a content description, or `null` when decorative.
 * @param actions more header actions, such as Download PDF or Send. Left out of the printed copy.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchInvoiceView(
    items: List<MonarchInvoiceLineItem>,
    currencyCode: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    number: String? = null,
    status: MonarchStatusDefinition? = null,
    issueDateMillis: Long? = null,
    dueDateMillis: Long? = null,
    details: List<MonarchInvoiceDetail> = emptyList(),
    from: MonarchInvoiceParty? = null,
    to: MonarchInvoiceParty? = null,
    locale: Locale? = null,
    dateLocale: Locale? = null,
    taxRate: BigDecimal? = null,
    discount: MonarchInvoiceDiscount? = null,
    totals: MonarchInvoiceTotals = MonarchInvoiceTotals(),
    notes: String? = null,
    footer: String? = null,
    showPrintButton: Boolean = false,
    onPrint: (() -> Unit)? = null,
    brand: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    labels: MonarchInvoiceViewLabels = MonarchInvoiceViewLabels(),
) {
    val tokens = MonarchInvoiceViewTokens
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0]
    val figures = remember(items, currencyCode, taxRate, discount) {
        InvoiceViewMath.figures(items, currencyCode, taxRate, discount)
    }
    val issueLabel = monarchString(labels.issueDate, R.string.monarch_invoiceview_issue_date)
    val dueLabel = monarchString(labels.dueDate, R.string.monarch_invoiceview_due_date)
    val facts = listOfNotNull(
        issueDateMillis?.let { MonarchInvoiceDetail(issueLabel, tokens.dateText(it, dateLocale ?: resolvedLocale)) },
        dueDateMillis?.let { MonarchInvoiceDetail(dueLabel, tokens.dateText(it, dateLocale ?: resolvedLocale)) },
    ) + details
    val resolvedTitle = title ?: monarchString(labels.title, R.string.monarch_invoiceview_title)
    val data = InvoiceViewData(
        title = resolvedTitle,
        number = number,
        status = status,
        facts = facts,
        from = from,
        to = to,
        items = items,
        currencyCode = currencyCode,
        locale = resolvedLocale,
        taxRate = taxRate,
        totals = totals,
        figures = figures,
        notes = notes,
        footer = footer,
        brand = brand,
    )
    val text = invoiceViewText(labels, totals, taxRate, resolvedLocale)
    val printer = if (showPrintButton && onPrint == null) {
        rememberInvoiceViewPrinter(tokens.heading(resolvedTitle, number))
    } else {
        null
    }
    val printLabel = monarchString(labels.print, R.string.monarch_invoiceview_print)
    val headerActions: (@Composable RowScope.() -> Unit)? = if (showPrintButton || actions != null) {
        {
            if (showPrintButton) {
                MonarchButton(
                    text = printLabel,
                    onClick = { if (onPrint != null) onPrint() else printer?.printing = true },
                    variant = MonarchButtonVariant.Secondary,
                    size = MonarchButtonSize.Sm,
                    enabled = printer?.printing != true,
                )
            }
            actions?.invoke(this)
        }
    } else {
        null
    }
    Box(modifier) {
        InvoiceViewBody(data = data, text = text, actions = headerActions)
        printer?.let { InvoiceViewPrintCopy(it) { InvoiceViewBody(data = data, text = text, actions = null) } }
    }
}

@Composable
private fun invoiceViewText(
    labels: MonarchInvoiceViewLabels,
    totals: MonarchInvoiceTotals,
    taxRate: BigDecimal?,
    locale: Locale,
): InvoiceViewText {
    val tokens = MonarchInvoiceViewTokens
    val rate = taxRate?.takeIf { InvoiceViewMath.rateShown(totals, it) }?.let { tokens.rateText(it, locale) }
    val withRate = rate?.let {
        labels.taxWithRate?.invoke(it) ?: stringResource(R.string.monarch_invoiceview_tax_with_rate, it)
    }
    return InvoiceViewText(
        from = monarchString(labels.from, R.string.monarch_invoiceview_from),
        to = monarchString(labels.to, R.string.monarch_invoiceview_to),
        lineItems = monarchString(labels.lineItems, R.string.monarch_invoiceview_line_items),
        description = monarchString(labels.description, R.string.monarch_invoiceview_description),
        quantity = monarchString(labels.quantity, R.string.monarch_invoiceview_quantity),
        unitPrice = monarchString(labels.unitPrice, R.string.monarch_invoiceview_unit_price),
        amount = monarchString(labels.amount, R.string.monarch_invoiceview_amount),
        subtotal = monarchString(labels.subtotal, R.string.monarch_invoiceview_subtotal),
        discount = monarchString(labels.discount, R.string.monarch_invoiceview_discount),
        tax = withRate ?: monarchString(labels.tax, R.string.monarch_invoiceview_tax),
        total = monarchString(labels.total, R.string.monarch_invoiceview_total),
        amountPaid = monarchString(labels.amountPaid, R.string.monarch_invoiceview_amount_paid),
        amountDue = monarchString(labels.amountDue, R.string.monarch_invoiceview_amount_due),
        noItems = monarchString(labels.noItems, R.string.monarch_invoiceview_no_items),
        notes = monarchString(labels.notes, R.string.monarch_invoiceview_notes),
    )
}
