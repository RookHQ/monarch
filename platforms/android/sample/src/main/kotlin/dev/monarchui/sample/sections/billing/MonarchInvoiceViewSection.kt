package dev.monarchui.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.billing.invoiceview.MonarchInvoiceDetail
import dev.monarchui.components.billing.invoiceview.MonarchInvoiceDiscount
import dev.monarchui.components.billing.invoiceview.MonarchInvoiceLineItem
import dev.monarchui.components.billing.invoiceview.MonarchInvoiceParty
import dev.monarchui.components.billing.invoiceview.MonarchInvoiceTotals
import dev.monarchui.components.billing.invoiceview.MonarchInvoiceView
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import java.math.BigDecimal

/** The catalogue page of InvoiceView. */
object MonarchInvoiceViewSection : CataloguePage {
    override val title = "InvoiceView"

    private const val ISSUED = 1_780_272_000_000L
    private const val DUE = 1_781_481_600_000L

    private val items = listOf(
        MonarchInvoiceLineItem("1", "Discovery workshop", BigDecimal(2), BigDecimal("400.00"), "Two sessions"),
        MonarchInvoiceLineItem("2", "Design system setup", BigDecimal.ONE, BigDecimal("1200.00")),
        MonarchInvoiceLineItem("3", "Support hours", BigDecimal("6.5"), BigDecimal("80.00"), "Billed hourly"),
    )

    @Composable
    override fun Content() {
        var sent by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Issued invoice, totals worked out, printable")
        MonarchInvoiceView(
            items = items,
            currencyCode = "USD",
            number = "INV-0042",
            status = if (sent) {
                MonarchStatusDefinition("Sent", MonarchTone.Info)
            } else {
                MonarchStatusDefinition("Draft", MonarchTone.Neutral)
            },
            issueDateMillis = ISSUED,
            dueDateMillis = DUE,
            details = listOf(MonarchInvoiceDetail("Payment terms", "Net 14")),
            from = MonarchInvoiceParty("Studio A", listOf("Street address", "City, postcode")),
            to = MonarchInvoiceParty("Client B", listOf("accounts@example.com")),
            taxRate = BigDecimal.TEN,
            discount = MonarchInvoiceDiscount.Percent(BigDecimal("5")),
            notes = "Pay by bank transfer within 14 days, quoting the invoice number.",
            footer = "Studio A Ltd, registered in England.",
            showPrintButton = true,
            actions = {
                MonarchButton(
                    text = if (sent) "Sent" else "Send",
                    onClick = { sent = true },
                    variant = MonarchButtonVariant.Primary,
                    size = MonarchButtonSize.Sm,
                    enabled = !sent,
                )
            },
        )
        CatalogueCaption("Totals from the billing system, part paid")
        MonarchInvoiceView(
            items = items.take(1),
            currencyCode = "EUR",
            number = "INV-0043",
            status = MonarchStatusDefinition("Overdue", MonarchTone.Danger),
            totals = MonarchInvoiceTotals(
                subtotal = BigDecimal("800.00"),
                tax = BigDecimal("168.00"),
                total = BigDecimal("968.00"),
                amountPaid = BigDecimal("500.00"),
                amountDue = BigDecimal("468.00"),
            ),
        )
        CatalogueCaption("No lines")
        MonarchInvoiceView(items = emptyList(), currencyCode = "USD", number = "INV-0044")
    }
}
