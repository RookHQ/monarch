package dev.monarchui.sample.sections.billing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.billing.payslipview.MonarchPayslipDetail
import dev.monarchui.components.billing.payslipview.MonarchPayslipLine
import dev.monarchui.components.billing.payslipview.MonarchPayslipLineKind
import dev.monarchui.components.billing.payslipview.MonarchPayslipTotals
import dev.monarchui.components.billing.payslipview.MonarchPayslipView
import dev.monarchui.components.billing.payslipview.MonarchPayslipYearToDateItem
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import java.math.BigDecimal
import kotlinx.coroutines.delay

/** The catalogue page of PayslipView. */
object MonarchPayslipViewSection : CataloguePage {
    override val title = "PayslipView"

    private const val PREPARE_MILLIS = 1500L

    private val lines = listOf(
        MonarchPayslipLine("basic", "Basic", MonarchPayslipLineKind.Earning, BigDecimal("40000"), "22 days"),
        MonarchPayslipLine("hra", "House rent allowance", MonarchPayslipLineKind.Earning, BigDecimal("16000.00")),
        MonarchPayslipLine("pf", "Provident fund", MonarchPayslipLineKind.Deduction, BigDecimal("4800"), "12% basic"),
        MonarchPayslipLine("tds", "Income tax (TDS)", MonarchPayslipLineKind.Deduction, BigDecimal("5200.00")),
        MonarchPayslipLine("travel", "Travel", MonarchPayslipLineKind.Reimbursement, BigDecimal("1500.00")),
        MonarchPayslipLine("epf", "Employer provident fund", MonarchPayslipLineKind.Employer, BigDecimal("4800.00")),
    )

    @Composable
    override fun Content() {
        var hideEmployer by rememberSaveable { mutableStateOf(false) }
        var downloading by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(downloading) {
            if (downloading) {
                delay(PREPARE_MILLIS)
                downloading = false
            }
        }
        MonarchSwitchRow(
            checked = hideEmployer,
            onCheckedChange = { hideEmployer = it },
            label = "Hide employer contributions",
        )
        CatalogueCaption("A monthly payslip")
        MonarchPayslipView(
            title = "Payslip",
            issuer = "Studio A Private Limited",
            subtitle = "June 2026 · PS-2026-06-014",
            status = MonarchStatusDefinition("Paid", MonarchTone.Success),
            details = listOf(
                MonarchPayslipDetail("Employee", "Ana Silva"),
                MonarchPayslipDetail("Employee ID", "E-1042"),
                MonarchPayslipDetail("Paid days", "22"),
            ),
            lines = lines,
            totals = MonarchPayslipTotals(
                gross = BigDecimal("56000.00"),
                deductions = BigDecimal("10000.00"),
                net = BigDecimal("47500.00"),
                reimbursements = BigDecimal("1500.00"),
            ),
            yearToDate = listOf(
                MonarchPayslipYearToDateItem("Gross", BigDecimal("168000.00")),
                MonarchPayslipYearToDateItem("Tax paid", BigDecimal("15600.00")),
            ),
            yearToDateTitle = "Year to date (from April)",
            currencyCode = "INR",
            hideEmployerContributions = hideEmployer,
            downloading = downloading,
            onDownload = { downloading = true },
        )
    }
}
