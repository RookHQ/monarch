package com.rookhq.monarch.components.billing.invoiceview

import com.rookhq.monarch.internal.MonarchSmallCapsLabel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.components.billing.money.MonarchMoney
import com.rookhq.monarch.components.billing.money.MonarchMoneyFormat
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionItem
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionList
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionListLayout
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme
import java.math.BigDecimal
import java.util.Locale

/** Everything a `MonarchInvoiceView` shows, with the locale resolved. */
@Suppress("LongParameterList")
internal class InvoiceViewData(
    val title: String,
    val number: String?,
    val status: MonarchStatusDefinition?,
    val facts: List<MonarchInvoiceDetail>,
    val from: MonarchInvoiceParty?,
    val to: MonarchInvoiceParty?,
    val items: List<MonarchInvoiceLineItem>,
    val currencyCode: String,
    val locale: Locale,
    val taxRate: BigDecimal?,
    val totals: MonarchInvoiceTotals,
    val figures: InvoiceViewFigures,
    val notes: String?,
    val footer: String?,
    val brand: (@Composable () -> Unit)?,
)

/** The built-in strings of a `MonarchInvoiceView`, resolved against the labels. */
@Suppress("LongParameterList")
internal class InvoiceViewText(
    val from: String,
    val to: String,
    val lineItems: String,
    val description: String,
    val quantity: String,
    val unitPrice: String,
    val amount: String,
    val subtotal: String,
    val discount: String,
    val tax: String,
    val total: String,
    val amountPaid: String,
    val amountDue: String,
    val noItems: String,
    val notes: String,
)

/** The invoice itself: header, parties and facts, lines, totals, notes and footer. */
@Composable
internal fun InvoiceViewBody(
    data: InvoiceViewData,
    text: InvoiceViewText,
    actions: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val tokens = MonarchInvoiceViewTokens
    BoxWithConstraints(modifier.semantics { isTraversalGroup = true }) {
        val wide = maxWidth >= tokens.WideFrom
        val table = maxWidth >= tokens.TableFrom
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            InvoiceViewHeader(data, actions)
            InvoiceViewMeta(data, text, wide)
            InvoiceViewLines(data, text, table)
            InvoiceViewTotalRows(data, text)
            data.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Column(verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
                    InvoiceViewLabel(text.notes)
                    MonarchText(text = notes)
                }
            }
            data.footer?.takeIf { it.isNotBlank() }?.let { footer ->
                MonarchText(
                    text = footer,
                    style = MonarchTextStyle.Caption,
                    color = tokens.mutedColor(MonarchTheme.colors),
                )
            }
        }
    }
}

@Composable
private fun InvoiceViewHeader(data: InvoiceViewData, actions: (@Composable RowScope.() -> Unit)?) {
    val tokens = MonarchInvoiceViewTokens
    Column(verticalArrangement = Arrangement.spacedBy(tokens.BlockGap)) {
        data.brand?.invoke()
        // The actions sit at the end of the heading row, as on the web, and wrap below it when there is no room.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchText(
                    text = tokens.heading(data.title, data.number),
                    style = MonarchTextStyle.Heading,
                    heading = true,
                )
                data.status?.let { MonarchStatusBadge(definition = it, size = MonarchStatusBadgeSize.Sm) }
            }
            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Composable
private fun InvoiceViewMeta(data: InvoiceViewData, text: InvoiceViewText, wide: Boolean) {
    val tokens = MonarchInvoiceViewTokens
    val blocks = buildList<@Composable (Modifier) -> Unit> {
        data.from?.let { party -> add { InvoiceViewPartyBlock(text.from, party, it) } }
        data.to?.let { party -> add { InvoiceViewPartyBlock(text.to, party, it) } }
        if (data.facts.isNotEmpty()) {
            add {
                MonarchDescriptionList(
                    items = data.facts.map { fact -> MonarchDescriptionItem(term = fact.label, value = fact.value) },
                    modifier = it,
                    layout = MonarchDescriptionListLayout.Inline,
                )
            }
        }
    }
    if (blocks.isEmpty()) return
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            blocks.forEach { block -> block(Modifier.weight(1f)) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.BlockGap)) {
            blocks.forEach { block -> block(Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun InvoiceViewPartyBlock(heading: String, party: MonarchInvoiceParty, modifier: Modifier) {
    val tokens = MonarchInvoiceViewTokens
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
        InvoiceViewLabel(heading)
        MonarchText(text = party.name, style = MonarchTextStyle.BodyStrong)
        party.details.forEach { line ->
            MonarchText(text = line, style = MonarchTextStyle.Caption, color = tokens.mutedColor(MonarchTheme.colors))
        }
    }
}

/** A small section heading in the muted colour, read by TalkBack as a heading. */
@Composable
private fun InvoiceViewLabel(text: String) {
    // As on the web: small, upper case and muted.
    MonarchSmallCapsLabel(text = text, color = MonarchInvoiceViewTokens.mutedColor(MonarchTheme.colors))
}

@Composable
private fun InvoiceViewLines(data: InvoiceViewData, text: InvoiceViewText, wide: Boolean) {
    val tokens = MonarchInvoiceViewTokens
    val colors = MonarchTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(tokens.BlockGap)) {
        InvoiceViewLabel(text.lineItems)
        if (data.items.isEmpty()) {
            MonarchText(text = text.noItems, color = tokens.mutedColor(colors))
            return@Column
        }
        if (wide) InvoiceViewColumnHeadings(text)
        Column(
            modifier = Modifier.semantics {
                contentDescription = text.lineItems
                collectionInfo = CollectionInfo(rowCount = data.items.size, columnCount = 1)
            },
            verticalArrangement = Arrangement.spacedBy(tokens.BlockGap),
        ) {
            data.items.forEachIndexed { index, item ->
                MonarchDivider(variant = MonarchDividerVariant.Subtle)
                InvoiceViewLine(data, text, item, data.figures.lines[index], index, wide)
            }
        }
    }
}

@Composable
private fun InvoiceViewColumnHeadings(text: InvoiceViewText) {
    val tokens = MonarchInvoiceViewTokens
    val muted = tokens.mutedColor(MonarchTheme.colors)
    // Hidden from TalkBack: every line reads its own labelled figures.
    Row(Modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(tokens.CellGap)) {
        MonarchText(text.description, Modifier.weight(tokens.DESCRIPTION_WEIGHT), MonarchTextStyle.Label, muted)
        val columns = listOf(
            text.quantity to tokens.QuantityWidth,
            text.unitPrice to tokens.FigureWidth,
            text.amount to tokens.FigureWidth,
        )
        columns.forEach { (heading, width) ->
            MonarchText(
                heading,
                Modifier.width(width),
                MonarchTextStyle.Label,
                muted,
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun InvoiceViewLine(
    data: InvoiceViewData,
    text: InvoiceViewText,
    item: MonarchInvoiceLineItem,
    worked: BigDecimal,
    index: Int,
    wide: Boolean,
) {
    val tokens = MonarchInvoiceViewTokens
    val muted = tokens.mutedColor(MonarchTheme.colors)
    val amount = item.amount ?: worked
    val quantity = tokens.quantityText(item.quantity, data.locale)
    fun money(value: BigDecimal) = MonarchMoneyFormat.format(value, data.currencyCode, data.locale)
    val spoken = tokens.spokenLine(
        item.description,
        item.detail,
        listOf(text.quantity to quantity, text.unitPrice to money(item.unitPrice), text.amount to money(amount)),
    )
    // The texts stay in the tree; TalkBack reads the content description of the line.
    val semantics = Modifier.semantics(mergeDescendants = true) {
        contentDescription = spoken
        collectionItemInfo = CollectionItemInfo(rowIndex = index, rowSpan = 1, columnIndex = 0, columnSpan = 1)
    }
    val name: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            MonarchText(text = item.description)
            item.detail?.takeIf { it.isNotBlank() }?.let {
                MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted)
            }
        }
    }
    if (wide) {
        Row(semantics, horizontalArrangement = Arrangement.spacedBy(tokens.CellGap)) {
            name(Modifier.weight(tokens.DESCRIPTION_WEIGHT))
            MonarchText(quantity, Modifier.width(tokens.QuantityWidth), textAlign = TextAlign.End)
            MonarchMoney(
                amount = item.unitPrice,
                currencyCode = data.currencyCode,
                modifier = Modifier.width(tokens.FigureWidth),
                locale = data.locale,
                textAlign = TextAlign.End,
            )
            MonarchMoney(
                amount = amount,
                currencyCode = data.currencyCode,
                modifier = Modifier.width(tokens.FigureWidth),
                locale = data.locale,
                style = MonarchTextStyle.BodyStrong,
                textAlign = TextAlign.End,
            )
        }
    } else {
        Column(semantics, verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.CellGap)) {
                name(Modifier.weight(1f))
                MonarchMoney(amount, data.currencyCode, locale = data.locale, style = MonarchTextStyle.BodyStrong)
            }
            MonarchText(
                text = "${text.quantity} $quantity · ${text.unitPrice} ${money(item.unitPrice)}",
                style = MonarchTextStyle.Caption,
                color = muted,
            )
        }
    }
}

@Composable
private fun InvoiceViewTotalRows(data: InvoiceViewData, text: InvoiceViewText) {
    val tokens = MonarchInvoiceViewTokens
    val totals = data.totals
    val figures = data.figures
    Column(verticalArrangement = Arrangement.spacedBy(tokens.BlockGap)) {
        InvoiceViewTotalRow(text.subtotal, totals.subtotal ?: figures.subtotal, data)
        if (InvoiceViewMath.discountShown(totals, figures)) {
            InvoiceViewTotalRow(text.discount, totals.discount ?: figures.discount, data, minus = true)
        }
        if (InvoiceViewMath.taxShown(totals, data.taxRate)) {
            InvoiceViewTotalRow(text.tax, totals.tax ?: figures.tax, data)
        }
        MonarchDivider()
        InvoiceViewTotalRow(text.total, totals.total ?: figures.total, data, style = MonarchTextStyle.Heading)
        totals.amountPaid?.let { InvoiceViewTotalRow(text.amountPaid, it, data) }
        totals.amountDue?.let { InvoiceViewTotalRow(text.amountDue, it, data, style = MonarchTextStyle.BodyStrong) }
    }
}

/** One total: the label and the amount, read together. */
@Composable
private fun InvoiceViewTotalRow(
    label: String,
    amount: BigDecimal,
    data: InvoiceViewData,
    style: MonarchTextStyle = MonarchTextStyle.Body,
    minus: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(text = label, modifier = Modifier.weight(1f), style = style, heading = false)
        // The minus sign is decorative; the "Discount" label carries the meaning.
        if (minus) MonarchText(text = "−", modifier = Modifier.clearAndSetSemantics { }, style = style)
        MonarchMoney(amount, data.currencyCode, locale = data.locale, style = style, textAlign = TextAlign.End)
    }
}
