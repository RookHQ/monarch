package com.rookhq.monarch.components.billing.invoiceview

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import java.math.BigDecimal
import java.util.Locale

private const val PreviewIssued = 1_780_272_000_000L
private const val PreviewDue = 1_781_481_600_000L

@MonarchPreviews
@Composable
internal fun MonarchInvoiceViewPreview() {
    PreviewFrame {
        MonarchInvoiceView(
            items = listOf(
                MonarchInvoiceLineItem("1", "Discovery workshop", BigDecimal(2), BigDecimal("400.00"), "Two sessions"),
                MonarchInvoiceLineItem("2", "Support hours", BigDecimal("6.5"), BigDecimal("80.00")),
            ),
            currencyCode = "USD",
            number = "INV-0042",
            status = MonarchStatusDefinition("Sent", MonarchTone.Info),
            issueDateMillis = PreviewIssued,
            dueDateMillis = PreviewDue,
            from = MonarchInvoiceParty("Studio A", listOf("Street address", "City, postcode")),
            to = MonarchInvoiceParty("Client B", listOf("accounts@example.com")),
            locale = Locale.US,
            taxRate = BigDecimal(10),
            discount = MonarchInvoiceDiscount.Percent(BigDecimal(5)),
            notes = "Pay by bank transfer within 14 days.",
            showPrintButton = true,
            onPrint = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchInvoiceViewEmptyPreview() {
    PreviewFrame {
        MonarchInvoiceView(items = emptyList(), currencyCode = "EUR", number = "INV-0043", locale = Locale.GERMANY)
    }
}
