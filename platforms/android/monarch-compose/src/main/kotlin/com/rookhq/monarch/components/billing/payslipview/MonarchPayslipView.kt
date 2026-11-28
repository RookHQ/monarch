package com.rookhq.monarch.components.billing.payslipview

import com.rookhq.monarch.internal.MonarchSmallCapsLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.billing.money.MonarchMoney
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionItem
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionList
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens
import java.math.BigDecimal
import java.util.Locale

/**
 * An on-screen payslip: issuer and title, employee and period details, pay
 * lines grouped into earnings, deductions, reimbursements and employer
 * contributions, the gross-to-net summary and optional year-to-date figures.
 * It shows the numbers payroll calculated and never recalculates them.
 *
 * Built by Monarch from a small label for each group,
 * `MonarchDescriptionList` for the details, `MonarchMoney` for every amount
 * and a `MonarchButton` for Download, in a `Column` so it sits inside the
 * screen's own `verticalScroll`. Each line, summary row and year-to-date tile
 * is one TalkBack element read as its name and amount; the title and the
 * group names are headings.
 *
 * @param title the main heading, such as "Payslip" or "Final settlement".
 * @param lines the calculated lines, grouped by kind. Empty shows "No pay lines.".
 * @param totals the totals from payroll.
 * @param currencyCode the ISO 4217 code of every amount.
 * @param modifier the modifier of the payslip.
 * @param issuer who pays: the organisation or legal entity, above the title.
 * @param subtitle a short line under the title: the period, the payslip number.
 * @param status a status beside the title, drawn as a small `MonarchStatusBadge`, such as "Paid".
 * @param details employee and period facts, in one column on a phone and three from 640dp.
 * @param yearToDate year-to-date figures, shown as small tiles.
 * @param yearToDateTitle replaces the year-to-date heading, such as "Year to date (from April)".
 * @param locale the locale of amounts. `null` uses the first locale of the configuration.
 * @param hideEmployerContributions hides the employer lines, for the employee's copy where policy requires it.
 * @param downloading the file is being prepared: the Download button shows its loading state.
 * @param onDownload shows a Download PDF button; the app makes and saves the file.
 * @param actions more header actions after Download, such as Share.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchPayslipView(
    title: String,
    lines: List<MonarchPayslipLine>,
    totals: MonarchPayslipTotals,
    currencyCode: String,
    modifier: Modifier = Modifier,
    issuer: String? = null,
    subtitle: String? = null,
    status: MonarchStatusDefinition? = null,
    details: List<MonarchPayslipDetail> = emptyList(),
    yearToDate: List<MonarchPayslipYearToDateItem> = emptyList(),
    yearToDateTitle: String? = null,
    locale: Locale? = null,
    hideEmployerContributions: Boolean = false,
    downloading: Boolean = false,
    onDownload: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    labels: MonarchPayslipViewLabels = MonarchPayslipViewLabels(),
) {
    val tokens = MonarchPayslipViewTokens
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0]
    val money = PayslipViewMoney(currencyCode, resolvedLocale)
    Box(modifier.semantics { isTraversalGroup = true }) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            PayslipViewHeader(title, issuer, subtitle, status, downloading, onDownload, actions, labels)
            if (details.isNotEmpty()) {
                MonarchDescriptionList(
                    items = details.map { MonarchDescriptionItem(term = it.label, value = it.value) },
                    columns = tokens.WIDE_COLUMNS,
                )
            }
            val groups = tokens.groups(lines, hideEmployerContributions)
            if (groups.isEmpty()) {
                MonarchText(
                    text = monarchString(labels.noLines, R.string.monarch_payslipview_no_lines),
                    color = tokens.mutedColor(MonarchTheme.colors),
                )
            }
            groups.forEach { (kind, groupLines) ->
                PayslipViewGroup(payslipViewGroupTitle(kind, labels), groupLines, money)
            }
            PayslipViewSummary(totals, money, labels)
            if (yearToDate.isNotEmpty()) {
                PayslipViewYearToDate(
                    title = yearToDateTitle
                        ?: monarchString(labels.yearToDate, R.string.monarch_payslipview_year_to_date),
                    items = yearToDate,
                    money = money,
                )
            }
        }
    }
}

/** The currency and locale every amount of one payslip is written in. */
private class PayslipViewMoney(val currencyCode: String, val locale: Locale)

@Composable
@Suppress("LongParameterList")
private fun PayslipViewHeader(
    title: String,
    issuer: String?,
    subtitle: String?,
    status: MonarchStatusDefinition?,
    downloading: Boolean,
    onDownload: (() -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?,
    labels: MonarchPayslipViewLabels,
) {
    val tokens = MonarchPayslipViewTokens
    val muted = tokens.mutedColor(MonarchTheme.colors)
    // The actions sit at the end of the header, as on the web, and wrap below it when there is no room.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
        verticalArrangement = Arrangement.spacedBy(tokens.BlockGap),
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
        ) {
            issuer?.takeIf { it.isNotBlank() }?.let {
                MonarchText(text = it, style = MonarchTextStyle.Label, color = muted)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(text = title, style = MonarchTextStyle.Title, heading = true)
                status?.let { shown ->
                    val definition = if (shown.icon == null) {
                        MonarchStatusDefinition(
                            label = shown.label,
                            tone = shown.tone,
                            icon = MonarchDefaultIcons.tone(shown.tone),
                            description = shown.description,
                        )
                    } else {
                        shown
                    }
                    MonarchStatusBadge(definition = definition, size = MonarchStatusBadgeSize.Sm)
                }
            }
            subtitle?.takeIf { it.isNotBlank() }?.let {
                MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted)
            }
        }
        if (onDownload != null || actions != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onDownload != null) {
                    MonarchButton(
                        text = monarchString(labels.download, R.string.monarch_payslipview_download),
                        onClick = onDownload,
                        variant = MonarchButtonVariant.Secondary,
                        size = MonarchButtonSize.Sm,
                        leadingIcon = MonarchDefaultIcons.Download,
                        loading = downloading,
                        loadingLabel = monarchString(labels.downloading, R.string.monarch_payslipview_downloading),
                    )
                }
                actions?.invoke(this)
            }
        }
    }
}

@Composable
private fun payslipViewGroupTitle(kind: MonarchPayslipLineKind, labels: MonarchPayslipViewLabels): String =
    when (kind) {
        MonarchPayslipLineKind.Earning -> monarchString(labels.earnings, R.string.monarch_payslipview_earnings)
        MonarchPayslipLineKind.Deduction -> monarchString(labels.deductions, R.string.monarch_payslipview_deductions)
        MonarchPayslipLineKind.Reimbursement ->
            monarchString(labels.reimbursements, R.string.monarch_payslipview_reimbursements)
        MonarchPayslipLineKind.Employer ->
            monarchString(labels.employerContributions, R.string.monarch_payslipview_employer_contributions)
    }

@Composable
private fun PayslipViewGroup(title: String, lines: List<MonarchPayslipLine>, money: PayslipViewMoney) {
    val tokens = MonarchPayslipViewTokens
    val muted = tokens.mutedColor(MonarchTheme.colors)
    val colors = MonarchTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(tokens.BlockGap)) {
        // As on the web: a small upper-case label over a bordered group of lines.
        MonarchSmallCapsLabel(text = title, color = muted)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(MonarchShapeDefaults.BorderWidth, colors.borderSubtle, MonarchShapeDefaults.Control)
                .semantics {
                    contentDescription = title
                    isTraversalGroup = true
                },
        ) {
            lines.forEachIndexed { index, line ->
                if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = tokens.BlockGap, vertical = tokens.CellGap)
                        .semantics(mergeDescendants = true) { },
                    horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
                        MonarchText(text = line.name)
                        line.note?.takeIf { it.isNotBlank() }?.let {
                            MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted)
                        }
                    }
                    MonarchMoney(line.amount, money.currencyCode, locale = money.locale, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun PayslipViewSummary(
    totals: MonarchPayslipTotals,
    money: PayslipViewMoney,
    labels: MonarchPayslipViewLabels,
) {
    val tokens = MonarchPayslipViewTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.panelColor(MonarchTheme.colors), MonarchShapeDefaults.Container)
            .padding(tokens.SectionPadding),
        verticalArrangement = Arrangement.spacedBy(tokens.BlockGap),
    ) {
        PayslipViewSummaryRow(
            monarchString(labels.grossEarnings, R.string.monarch_payslipview_gross_earnings),
            totals.gross,
            money,
        )
        PayslipViewSummaryRow(
            monarchString(labels.totalDeductions, R.string.monarch_payslipview_total_deductions),
            totals.deductions,
            money,
        )
        val reimbursements = totals.reimbursements
        if (reimbursements != null && tokens.reimbursementsShown(totals)) {
            PayslipViewSummaryRow(
                monarchString(labels.reimbursements, R.string.monarch_payslipview_reimbursements),
                reimbursements,
                money,
            )
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
        PayslipViewSummaryRow(
            monarchString(labels.netPay, R.string.monarch_payslipview_net_pay),
            totals.net,
            money,
            MonarchTextStyle.Heading,
            strong = true,
        )
    }
}

@Composable
private fun PayslipViewSummaryRow(
    label: String,
    amount: BigDecimal,
    money: PayslipViewMoney,
    style: MonarchTextStyle = MonarchTextStyle.Body,
    strong: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(MonarchPayslipViewTokens.CellGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(
            text = label,
            modifier = Modifier.weight(1f),
            style = if (strong) MonarchTextStyle.BodyStrong else MonarchTextStyle.Body,
            color = if (strong) {
                MonarchTheme.colors.textDefault
            } else {
                MonarchPayslipViewTokens.mutedColor(MonarchTheme.colors)
            },
            heading = false,
        )
        MonarchMoney(amount, money.currencyCode, locale = money.locale, style = style, textAlign = TextAlign.End)
    }
}

private val WideWindowDp = MonarchTokens.Space.space24.dp * 5

@Composable
private fun PayslipViewYearToDate(title: String, items: List<MonarchPayslipYearToDateItem>, money: PayslipViewMoney) {
    val tokens = MonarchPayslipViewTokens
    val colors = MonarchTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(tokens.BlockGap)) {
        MonarchSmallCapsLabel(text = title, color = tokens.mutedColor(colors))
        BoxWithConstraints {
            // Four tiles share a row, as on the web; a phone fits two, so no figure breaks in the middle.
            val perRow = if (maxWidth < WideWindowDp) 2 else tokens.TILES_PER_ROW
            Column(verticalArrangement = Arrangement.spacedBy(tokens.BlockGap)) {
                items.chunked(perRow).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(tokens.CellGap)) {
                        row.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(MonarchShapeDefaults.BorderWidth, colors.borderSubtle, tokens.TileShape)
                                    .padding(tokens.TilePadding)
                                    .semantics(mergeDescendants = true) { },
                                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                            ) {
                                MonarchText(
                                    text = item.label,
                                    style = MonarchTextStyle.Caption,
                                    color = tokens.mutedColor(colors),
                                )
                                MonarchMoney(
                                    amount = item.amount,
                                    currencyCode = money.currencyCode,
                                    locale = money.locale,
                                    style = MonarchTextStyle.Body,
                                )
                            }
                        }
                        repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
