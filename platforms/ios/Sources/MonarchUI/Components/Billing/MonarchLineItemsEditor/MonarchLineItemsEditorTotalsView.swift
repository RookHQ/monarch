import SwiftUI

/// The totals of a line items editor: subtotal, discount, tax and total,
/// with the discount and the tax rate editable when the editor allows it.
struct MonarchLineItemsEditorTotalsView: View {
    let totals: MonarchLineItemsTotals
    let currencyCode: String
    let taxRate: Binding<Decimal>?
    let taxRateOptions: [MonarchLineItemsTaxRateOption]
    let discount: Binding<MonarchLineItemsDiscount>?
    let isTaxRateEditable: Bool
    let isDiscountEditable: Bool
    let labels: MonarchLineItemsEditorLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    private typealias Tokens = MonarchLineItemsEditorTokens

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            amountRow(labels.subtotal, amount: totals.subtotal)
            if let discount {
                if isDiscountEditable {
                    discountRow(discount)
                } else {
                    amountRow(
                        Tokens.discountTitle(
                            discount.wrappedValue,
                            isEditable: false,
                            labels: labels,
                            locale: locale
                        ),
                        amount: Tokens.signedDiscount(totals.discount)
                    )
                }
            }
            if let taxRate {
                if isTaxRateEditable && !taxRateOptions.isEmpty {
                    taxRow(taxRate)
                } else {
                    amountRow(
                        Tokens.taxTitle(
                            taxRate.wrappedValue,
                            isEditable: isTaxRateEditable,
                            labels: labels,
                            locale: locale
                        ),
                        amount: totals.tax
                    )
                    if isTaxRateEditable { taxEditor(taxRate) }
                }
            }
            MonarchDivider()
            amountRow(labels.total, amount: totals.total, isTotal: true)
        }
    }

    private func amountRow(_ title: String, amount: Int, isTotal: Bool = false) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.totalsSpacing) {
            Text(title)
                .monarchTextStyle(isTotal ? .bodyStrong : .body)
                .foregroundStyle(isTotal ? theme.colors.textDefault : theme.colors.textMuted)
            Spacer(minLength: 0)
            MonarchMoney(minorUnits: amount, currencyCode: currencyCode, textStyle: isTotal ? .bodyStrong : .body)
                .foregroundStyle(theme.colors.textDefault)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.totalsRowLabel(title, amount: amount, currencyCode: currencyCode, locale: locale))
    }

    /// "Discount", its value, its kind and the amount taken off, on one row, as on the web.
    private func discountRow(_ discount: Binding<MonarchLineItemsDiscount>) -> some View {
        return HStack(alignment: .center, spacing: Tokens.totalsSpacing) {
            Text(labels.discount(""))
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
            TextField(
                labels.discount(""),
                value: Binding(
                    get: { Tokens.double(discount.wrappedValue.value) },
                    set: { value in discount.wrappedValue.value = Tokens.decimal(value) ?? 0 }
                ),
                format: .number.locale(locale)
            )
            .textFieldStyle(.plain)
            .monarchTextStyle(.body)
            .multilineTextAlignment(.trailing)
            .monarchFieldBox(hasError: false, isFocused: false, minHeight: 36)
            .frame(width: 64)
            MonarchSelectField(
                labels.discountType,
                selection: discount.kind,
                valueText: discount.wrappedValue.kind == .percent ? labels.percentOption : currencyCode,
                labelVisibility: .hidden,
                style: .compact
            ) {
                Text(labels.percentOption).tag(MonarchLineItemsDiscountKind.percent)
                Text(currencyCode).tag(MonarchLineItemsDiscountKind.amount)
            }
            Spacer(minLength: 0)
            MonarchMoney(
                minorUnits: Tokens.signedDiscount(totals.discount),
                currencyCode: currencyCode,
                textStyle: .body
            )
            .foregroundStyle(theme.colors.textMuted)
        }
    }

    /// "Tax", its preset rate and the tax on one row, as on the web.
    private func taxRow(_ taxRate: Binding<Decimal>) -> some View {
        HStack(alignment: .center, spacing: Tokens.totalsSpacing) {
            Text(labels.tax(""))
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
            MonarchSelectField(
                labels.taxRate,
                selection: taxRate,
                valueText: taxRateOptions.first { $0.value == taxRate.wrappedValue }?.label,
                labelVisibility: .hidden,
                style: .compact
            ) {
                ForEach(taxRateOptions) { option in
                    Text(option.label).tag(option.value)
                }
            }
            Spacer(minLength: 0)
            MonarchMoney(minorUnits: totals.tax, currencyCode: currencyCode, textStyle: .body)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    @ViewBuilder private func discountEditor(_ discount: Binding<MonarchLineItemsDiscount>) -> some View {
        HStack(alignment: .bottom, spacing: Tokens.fieldSpacing) {
            switch discount.wrappedValue.kind {
            case .percent:
                MonarchNumberInput(
                    labels.discount(""),
                    value: Binding(
                        get: { Tokens.double(discount.wrappedValue.value / 100) },
                        set: { value in discount.wrappedValue.value = Tokens.percentage(fromFraction: value) }
                    ),
                    format: .percent,
                    minimum: 0,
                    maximum: 1,
                    showsStepper: false
                )
            case .amount:
                MonarchCurrencyInput(
                    labels.discount(""),
                    value: Binding(
                        get: { Tokens.double(discount.wrappedValue.value) },
                        set: { value in discount.wrappedValue.value = Tokens.decimal(value) ?? 0 }
                    ),
                    currencyCode: currencyCode
                )
            }
            Picker(labels.discountType, selection: discount.kind) {
                Text(labels.percentOption).tag(MonarchLineItemsDiscountKind.percent)
                Text(currencyCode).tag(MonarchLineItemsDiscountKind.amount)
            }
            .pickerStyle(.segmented)
            .fixedSize()
            .frame(minHeight: MonarchTextFieldTokens.minHeight)
        }
    }

    @ViewBuilder private func taxEditor(_ taxRate: Binding<Decimal>) -> some View {
        if taxRateOptions.isEmpty {
            MonarchNumberInput(
                labels.taxRate,
                value: Binding(
                    get: { Tokens.double(taxRate.wrappedValue / 100) },
                    set: { value in taxRate.wrappedValue = Tokens.percentage(fromFraction: value) }
                ),
                format: .percent,
                minimum: 0,
                showsStepper: false
            )
        } else {
            HStack(spacing: Tokens.totalsSpacing) {
                Text(labels.taxRate)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                    .accessibilityHidden(true)
                Spacer(minLength: 0)
                MonarchSelectField(
                    labels.taxRate,
                    selection: taxRate,
                    valueText: taxRateOptions.first { $0.value == taxRate.wrappedValue }?.label,
                    style: .compact
                ) {
                    ForEach(taxRateOptions) { option in
                        Text(option.label).tag(option.value)
                    }
                }
            }
            .frame(minHeight: MonarchTextFieldTokens.minHeight)
        }
    }
}
