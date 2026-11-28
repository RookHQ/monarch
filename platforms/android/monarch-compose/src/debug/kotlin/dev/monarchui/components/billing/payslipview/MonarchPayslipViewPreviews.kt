package dev.monarchui.components.billing.payslipview

import androidx.compose.runtime.Composable
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.math.BigDecimal
import java.util.Locale

@MonarchPreviews
@Composable
internal fun MonarchPayslipViewPreview() {
    PreviewFrame {
        MonarchPayslipView(
            title = "Payslip",
            issuer = "Studio A Ltd",
            subtitle = "June 2026",
            status = MonarchStatusDefinition("Paid", MonarchTone.Success),
            lines = listOf(
                MonarchPayslipLine("basic", "Basic", MonarchPayslipLineKind.Earning, BigDecimal("4000"), "22 days"),
                MonarchPayslipLine("tax", "Income tax", MonarchPayslipLineKind.Deduction, BigDecimal("640.00")),
                MonarchPayslipLine("pension", "Pension", MonarchPayslipLineKind.Employer, BigDecimal("200.00")),
            ),
            totals = MonarchPayslipTotals(BigDecimal("4000"), BigDecimal("640"), BigDecimal("3360")),
            yearToDate = listOf(MonarchPayslipYearToDateItem("Gross", BigDecimal("12000.00"))),
            currencyCode = "GBP",
            locale = Locale.UK,
            onDownload = {},
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchPayslipViewEmptyPreview() {
    PreviewFrame {
        MonarchPayslipView(
            title = "Payslip",
            lines = emptyList(),
            totals = MonarchPayslipTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
            currencyCode = "INR",
            downloading = true,
            onDownload = {},
        )
    }
}
