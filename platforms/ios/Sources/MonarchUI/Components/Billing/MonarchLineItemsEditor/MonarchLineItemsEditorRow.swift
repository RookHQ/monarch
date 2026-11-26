import SwiftUI

/// One row of a line items editor: the heading with the row's amount and a
/// remove button, then the description, quantity and unit price fields, or
/// their text when read-only.
struct MonarchLineItemsEditorRow: View {
    let item: MonarchLineItem
    let row: Int
    let amount: Int
    let currencyCode: String
    let labels: MonarchLineItemsEditorLabels
    let isReadOnly: Bool
    let canRemove: Bool
    let onChange: (MonarchLineItem) -> Void
    let onRemove: () -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private typealias Tokens = MonarchLineItemsEditorTokens

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            HStack(alignment: .center, spacing: Tokens.totalsSpacing) {
                heading
                if !isReadOnly {
                    Button(action: onRemove) {
                        MonarchIcon(systemName: Tokens.removeSymbol, size: .sm)
                            .monarchMinimumHitTarget()
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                    .disabled(!canRemove)
                    .accessibilityLabel(labels.removeItem(row, item.description))
                }
            }
            if isReadOnly {
                readOnlyContent
            } else {
                fields
            }
        }
    }

    private var heading: some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.totalsSpacing) {
            Text(labels.item(row))
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textDefault)
            Spacer(minLength: 0)
            MonarchMoney(minorUnits: amount, currencyCode: currencyCode, textStyle: .label)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            Tokens.rowLabel(row: row, amount: amount, currencyCode: currencyCode, labels: labels, locale: locale)
        )
        .accessibilityAddTraits(.isHeader)
    }

    private var readOnlyContent: some View {
        VStack(alignment: .leading, spacing: Tokens.errorIconSpacing) {
            if !item.description.isEmpty {
                Text(item.description)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
            }
            Text(Tokens.readOnlyDetail(item, currencyCode: currencyCode, labels: labels, locale: locale))
                .monarchTextStyle(.caption)
                .monospacedDigit()
                .foregroundStyle(theme.colors.textMuted)
        }
        .accessibilityElement(children: .combine)
    }

    private var fields: some View {
        // Side by side, or stacked at accessibility text sizes so neither field is squeezed.
        let layout =
            dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: Tokens.fieldSpacing))
            : AnyLayout(HStackLayout(alignment: .top, spacing: Tokens.fieldSpacing))
        return VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            MonarchTextField(
                labels.description,
                text: Binding(
                    get: { item.description },
                    set: { text in
                        var next = item
                        next.description = text
                        onChange(next)
                    }
                ),
                prompt: labels.descriptionPlaceholder
            )
            layout {
                MonarchNumberInput(
                    labels.quantity,
                    value: Binding(
                        get: { Tokens.double(item.quantity) },
                        set: { value in
                            var next = item
                            next.quantity = Tokens.decimal(value)
                            onChange(next)
                        }
                    ),
                    minimum: 0,
                    showsStepper: false
                )
                MonarchCurrencyInput(
                    labels.unitPrice(currencyCode),
                    value: Binding(
                        get: { Tokens.double(item.unitPrice) },
                        set: { value in
                            var next = item
                            next.unitPrice = Tokens.decimal(value)
                            onChange(next)
                        }
                    ),
                    currencyCode: currencyCode
                )
            }
        }
    }
}
