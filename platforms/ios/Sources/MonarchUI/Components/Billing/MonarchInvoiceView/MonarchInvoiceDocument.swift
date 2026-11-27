import Foundation
import SwiftUI

/// Everything a ``MonarchInvoiceView`` shows, as one value, so the screen and
/// the printed copy are drawn from the same data.
struct MonarchInvoiceContent {
    var title: String?
    var number: String?
    var status: MonarchStatusDefinition?
    var issueDate: Date?
    var dueDate: Date?
    var details: [MonarchInvoiceDetail]
    var brand: Image?
    var from: MonarchInvoiceParty?
    var to: MonarchInvoiceParty?
    var items: [MonarchInvoiceLineItem]
    var currencyCode: String
    var locale: Locale?
    var taxRate: Decimal?
    var discount: MonarchInvoiceDiscount?
    var totals: MonarchInvoiceTotals
    var notes: String?
    var footer: String?
}

/// The invoice itself: header, parties and facts, lines, totals, notes and
/// footer. ``MonarchInvoiceView`` shows it on screen with its actions; the
/// printer renders it without them.
struct MonarchInvoiceDocument<Actions: View>: View {
    let content: MonarchInvoiceContent
    let labels: MonarchInvoiceViewLabels
    /// `nil` uses the width and text size to decide; printing passes `false`.
    let isStacked: Bool?
    let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var environmentLocale
    @Environment(\.timeZone) private var timeZone
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @ScaledMetric(relativeTo: .title) private var brandHeight = MonarchInvoiceViewTokens.brandHeight

    private typealias Tokens = MonarchInvoiceViewTokens

    /// One line with its texts worked out.
    private struct Line {
        let item: MonarchInvoiceLineItem
        let quantity: String
        let unitPrice: String
        let amount: Decimal
        let label: String
    }

    var body: some View {
        let texts = Tokens.texts(labels)
        let locale = content.locale ?? environmentLocale
        let stacked =
            isStacked
            ?? Tokens.usesStackedRows(
                isRegularWidth: horizontalSizeClass == .regular,
                isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
            )
        let figures = Tokens.figures(
            items: content.items,
            currencyCode: content.currencyCode,
            taxRate: content.taxRate,
            discount: content.discount
        )
        let heading = Tokens.heading(title: content.title, number: content.number, texts: texts)
        VStack(alignment: .leading, spacing: Tokens.sectionSpacing) {
            header(heading: heading)
            meta(texts: texts, locale: locale, stacked: stacked)
            lines(texts: texts, locale: locale, figures: figures, stacked: stacked)
            totals(texts: texts, locale: locale, figures: figures)
            if let notes = content.notes, !notes.isEmpty {
                VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                    sectionHeading(texts.notes)
                    Text(notes)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textDefault)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            if let footer = content.footer, !footer.isEmpty {
                Text(footer)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(heading)
    }

    // MARK: Header

    private func header(heading: String) -> some View {
        VStack(alignment: .leading, spacing: Tokens.headerSpacing) {
            if let brand = content.brand {
                brand
                    .resizable()
                    .scaledToFit()
                    .frame(height: brandHeight)
                    .accessibilityHidden(true)
            }
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: Tokens.headerSpacing) {
                    title(heading)
                    status
                }
                VStack(alignment: .leading, spacing: Tokens.headerSpacing) {
                    title(heading)
                    status
                }
            }
            actions
        }
    }

    private func title(_ heading: String) -> some View {
        // As on the web: the title at 20 points, the invoice number muted after it.
        Group {
            if let number = content.number, !number.isEmpty, heading.hasSuffix(number) {
                Text(String(heading.dropLast(number.count)))
                    + Text(number).foregroundStyle(theme.colors.textMuted)
            } else {
                Text(heading)
            }
        }
        .monarchTextStyle(.heading)
        .foregroundStyle(theme.colors.textDefault)
        .fixedSize(horizontal: false, vertical: true)
        .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder private var status: some View {
        if let status = content.status {
            MonarchStatusBadge(status, size: .sm)
        }
    }

    private func sectionHeading(_ text: String) -> some View {
        // As on the web: small, semibold, capitals with wide letter spacing.
        Text(text)
            .monarchTextStyle(.caption, weight: .semibold)
            .textCase(.uppercase)
            .tracking(0.6)
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityAddTraits(.isHeader)
    }

    // MARK: Parties and facts

    @ViewBuilder private func meta(texts: MonarchInvoiceViewTokens.Texts, locale: Locale, stacked: Bool) -> some View {
        let facts = Tokens.facts(
            issueDate: content.issueDate,
            dueDate: content.dueDate,
            details: content.details,
            texts: texts,
            // As on the web, dates follow the environment; the locale of the invoice is for amounts and quantities.
            locale: environmentLocale,
            timeZone: timeZone
        )
        if content.from != nil || content.to != nil || !facts.isEmpty {
            let layout =
                stacked
                ? AnyLayout(VStackLayout(alignment: .leading, spacing: Tokens.blockSpacing))
                : AnyLayout(HStackLayout(alignment: .top, spacing: Tokens.blockSpacing))
            layout {
                if let from = content.from {
                    party(texts.from, from)
                }
                if let to = content.to {
                    party(texts.to, to)
                }
                if !facts.isEmpty {
                    VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                        ForEach(Array(facts.enumerated()), id: \.offset) { _, fact in
                            LabeledContent(fact.label, value: fact.value)
                        }
                    }
                    .labeledContentStyle(.monarch(.inline))
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
    }

    private func party(_ heading: String, _ party: MonarchInvoiceParty) -> some View {
        VStack(alignment: .leading, spacing: Tokens.textSpacing) {
            sectionHeading(heading)
            VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                Text(party.name)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
                ForEach(Array(party.details.enumerated()), id: \.offset) { _, line in
                    Text(line)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityElement(children: .combine)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: Lines

    @ViewBuilder private func lines(
        texts: MonarchInvoiceViewTokens.Texts,
        locale: Locale,
        figures: MonarchInvoiceViewTokens.Figures,
        stacked: Bool
    ) -> some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            sectionHeading(texts.lineItems)
            if content.items.isEmpty {
                Text(texts.noItems)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            } else if stacked {
                stackedLines(texts: texts, locale: locale, figures: figures)
            } else {
                table(texts: texts, locale: locale, figures: figures)
            }
        }
    }

    private func line(
        _ index: Int,
        texts: MonarchInvoiceViewTokens.Texts,
        locale: Locale,
        figures: MonarchInvoiceViewTokens.Figures
    ) -> Line {
        let item = content.items[index]
        let amount = Tokens.lineAmount(item, index: index, figures: figures)
        let quantity = Tokens.quantityText(item.quantity, locale: locale)
        let unitPrice = MonarchMoney.formatted(item.unitPrice, currencyCode: content.currencyCode, locale: locale)
        let amountText = MonarchMoney.formatted(amount, currencyCode: content.currencyCode, locale: locale)
        let label = Tokens.lineLabel(item, quantity: quantity, unitPrice: unitPrice, amount: amountText, texts: texts)
        return Line(item: item, quantity: quantity, unitPrice: unitPrice, amount: amount, label: label)
    }

    private func table(
        texts: MonarchInvoiceViewTokens.Texts,
        locale: Locale,
        figures: MonarchInvoiceViewTokens.Figures
    ) -> some View {
        Grid(alignment: .leading, horizontalSpacing: Tokens.columnSpacing, verticalSpacing: Tokens.rowSpacing) {
            GridRow {
                columnHeading(texts.description)
                    .frame(maxWidth: .infinity, alignment: .leading)
                columnHeading(texts.quantity).gridColumnAlignment(.trailing)
                columnHeading(texts.unitPrice).gridColumnAlignment(.trailing)
                columnHeading(texts.amount).gridColumnAlignment(.trailing)
            }
            ForEach(content.items.indices, id: \.self) { index in
                let line = line(index, texts: texts, locale: locale, figures: figures)
                MonarchDivider()
                GridRow(alignment: .firstTextBaseline) {
                    description(line.item)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(line.label)
                    Text(verbatim: line.quantity)
                        .monarchTextStyle(.body)
                        .monospacedDigit()
                        .foregroundStyle(theme.colors.textDefault)
                        .fixedSize()
                        .accessibilityHidden(true)
                    MonarchMoney(line.item.unitPrice, currencyCode: content.currencyCode, locale: locale)
                        .foregroundStyle(theme.colors.textDefault)
                        .fixedSize()
                        .accessibilityHidden(true)
                    MonarchMoney(line.amount, currencyCode: content.currencyCode, locale: locale)
                        .foregroundStyle(theme.colors.textDefault)
                        .fixedSize()
                        .accessibilityHidden(true)
                }
            }
        }
    }

    private func columnHeading(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityHidden(true)
    }

    private func description(_ item: MonarchInvoiceLineItem) -> some View {
        VStack(alignment: .leading, spacing: Tokens.textSpacing) {
            Text(item.description)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
            if let detail = item.detail, !detail.isEmpty {
                Text(detail)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    private func stackedLines(
        texts: MonarchInvoiceViewTokens.Texts,
        locale: Locale,
        figures: MonarchInvoiceViewTokens.Figures
    ) -> some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            ForEach(content.items.indices, id: \.self) { index in
                let line = line(index, texts: texts, locale: locale, figures: figures)
                MonarchDivider()
                VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                    description(line.item)
                    ViewThatFits(in: .horizontal) {
                        HStack(alignment: .firstTextBaseline, spacing: Tokens.amountGap) {
                            stackedMeta(line, texts: texts)
                            Spacer(minLength: 0)
                            MonarchMoney(line.amount, currencyCode: content.currencyCode, locale: locale)
                                .foregroundStyle(theme.colors.textDefault)
                                .fixedSize()
                        }
                        VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                            stackedMeta(line, texts: texts)
                            MonarchMoney(line.amount, currencyCode: content.currencyCode, locale: locale)
                                .foregroundStyle(theme.colors.textDefault)
                        }
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(line.label)
            }
        }
    }

    private func stackedMeta(
        _ line: Line,
        texts: MonarchInvoiceViewTokens.Texts
    ) -> some View {
        Text(Tokens.stackedMeta(quantity: line.quantity, unitPrice: line.unitPrice, texts: texts))
            .monarchTextStyle(.caption)
            .monospacedDigit()
            .foregroundStyle(theme.colors.textMuted)
            .fixedSize(horizontal: false, vertical: true)
    }

    // MARK: Totals

    private func totals(
        texts: MonarchInvoiceViewTokens.Texts,
        locale: Locale,
        figures: MonarchInvoiceViewTokens.Figures
    ) -> some View {
        let rows = Tokens.totalRows(
            totals: content.totals,
            figures: figures,
            taxRate: content.taxRate,
            texts: texts,
            locale: locale
        )
        return VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                if Tokens.hasDivider(row.kind) {
                    MonarchDivider()
                }
                totalRow(row, locale: locale)
            }
        }
    }

    private func totalRow(_ row: MonarchInvoiceViewTokens.TotalRow, locale: Locale) -> some View {
        let style = Tokens.textStyle(row.kind)
        let label = Text(row.label)
            .monarchTextStyle(style.label)
            .foregroundStyle(theme.colors[role: Tokens.labelRole(row.kind)])
        let amount = MonarchMoney(
            row.amount,
            currencyCode: content.currencyCode,
            locale: locale,
            textStyle: style.amount
        )
        .foregroundStyle(theme.colors.textDefault)
        return ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: Tokens.amountGap) {
                label
                Spacer(minLength: 0)
                amount.fixedSize()
            }
            VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                label
                amount
            }
        }
        .accessibilityElement(children: .combine)
    }
}
