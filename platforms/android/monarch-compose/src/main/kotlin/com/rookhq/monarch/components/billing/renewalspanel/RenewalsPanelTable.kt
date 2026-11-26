package com.rookhq.monarch.components.billing.renewalspanel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.billing.money.MonarchMoney
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

private val ItemWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space20).dp
private val RenewalWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space12).dp
private val AmountWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp
private val StatusWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space16).dp
private val ActionWidth: Dp = MonarchTokens.Space.space24.dp
private val TotalWidth: Dp = ItemWidth + RenewalWidth + AmountWidth + StatusWidth + ActionWidth
private val CellPadding: Dp = MonarchTokens.Space.space2.dp

/** One renewal of the table: the cells' strings and what the row does. */
internal class RenewalsPanelTableRow(
    val item: MonarchRenewalItem,
    val status: MonarchStatusDefinition,
    val onClick: (() -> Unit)?,
    val onAction: (() -> Unit)?,
)

/**
 * The renewals as a table, as on the web: Item, Renewal, Amount and Status columns and a row action, with the
 * table scrolling sideways when it is wider than the panel. Each row is one TalkBack element.
 */
@Composable
internal fun RenewalsPanelTable(rows: List<RenewalsPanelTableRow>, context: RenewalsPanelRowContext) {
    val tokens = MonarchRenewalsPanelTokens
    val colors = MonarchTheme.colors
    val muted = tokens.mutedColor(colors)
    val labels = context.labels
    val shape = RoundedCornerShape(MonarchTokens.Space.space3.dp)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(MonarchTokens.Space.px.dp, colors.borderSubtle, shape),
    ) {
        val scale = maxOf(1f, maxWidth / TotalWidth)
        val widths = TableWidths(
            ItemWidth * scale,
            RenewalWidth * scale,
            AmountWidth * scale,
            StatusWidth * scale,
            ActionWidth * scale,
        )
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Row(modifier = Modifier.background(colors.bgSubtle).clearAndSetSemantics { }) {
                Heading(
                    monarchString(labels.columnItem, R.string.monarch_renewalspanel_column_item),
                    widths.item,
                    muted,
                )
                Heading(
                    monarchString(labels.columnRenewal, R.string.monarch_renewalspanel_column_renewal),
                    widths.renewal,
                    muted,
                )
                Heading(
                    monarchString(labels.columnAmount, R.string.monarch_renewalspanel_column_amount),
                    widths.amount,
                    muted,
                    TextAlign.End,
                )
                Heading(
                    monarchString(labels.columnStatus, R.string.monarch_renewalspanel_column_status),
                    widths.status,
                    muted,
                )
                Heading("", widths.action, muted)
            }
            rows.forEach { row ->
                MonarchDivider(variant = MonarchDividerVariant.Subtle)
                TableRow(row, context, muted, widths)
            }
        }
    }
}

private class TableWidths(val item: Dp, val renewal: Dp, val amount: Dp, val status: Dp, val action: Dp)

@Composable
private fun Heading(text: String, width: Dp, color: Color, align: TextAlign = TextAlign.Start) {
    MonarchText(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = CellPadding, vertical = MonarchTokens.Space.space2_5.dp),
        style = MonarchTextStyle.Label,
        color = color,
        textAlign = align,
    )
}

@Composable
private fun TableRow(
    row: RenewalsPanelTableRow,
    context: RenewalsPanelRowContext,
    muted: Color,
    widths: TableWidths,
) {
    val tokens = MonarchRenewalsPanelTokens
    val colors = MonarchTheme.colors
    val labels = context.labels
    val item = row.item
    val parts = renewalsPanelRowParts(item, row.status, context)
    val (date, under) = renewalsPanelDateParts(item, tokens.daysUntil(item, context.now), context)
    val mode = renewalsPanelRenewMode(item, labels)
    Row(
        modifier = Modifier
            .then(if (row.onClick != null) Modifier.clickable(role = Role.Button, onClick = row.onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = parts.spoken },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(widths.item).padding(CellPadding)) {
            MonarchText(text = item.name, style = MonarchTextStyle.BodyStrong)
            item.description?.takeIf { it.isNotBlank() }?.let {
                MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted)
            }
            mode?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted) }
        }
        Column(Modifier.width(widths.renewal).padding(CellPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MonarchTokens.Space.space1.dp),
            ) {
                if (parts.urgent) {
                    CompositionLocalProvider(LocalContentColor provides colors.warningStrong) {
                        Box(Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Warning() }
                    }
                }
                MonarchText(
                    text = date,
                    color = if (parts.urgent) colors.warningStrong else colors.textDefault,
                )
            }
            under?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted) }
        }
        Column(Modifier.width(widths.amount).padding(CellPadding), horizontalAlignment = Alignment.End) {
            val amount = item.amount
            val currency = item.currencyCode
            if (amount != null && currency != null) {
                MonarchMoney(amount, currency, locale = context.locale, style = MonarchTextStyle.Body)
            } else {
                parts.unpriced?.let { MonarchText(text = it, color = muted) }
            }
            item.amountNote?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted) }
        }
        Column(Modifier.width(widths.status).padding(CellPadding)) {
            Box(Modifier.clearAndSetSemantics { }) {
                MonarchStatusBadge(definition = row.status, size = MonarchStatusBadgeSize.Sm)
            }
            parts.extra?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted) }
        }
        Box(Modifier.width(widths.action).padding(CellPadding)) {
            row.onAction?.let { act ->
                val action = monarchString(labels.action, R.string.monarch_renewalspanel_action)
                val spoken = labels.actionFor?.invoke(action, item.name)
                    ?: stringResource(
                        R.string.monarch_renewalspanel_action_for,
                        action,
                        item.name,
                    )
                MonarchButton(
                    text = action,
                    onClick = act,
                    modifier = Modifier.semantics { contentDescription = spoken },
                    variant = MonarchButtonVariant.Secondary,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}
