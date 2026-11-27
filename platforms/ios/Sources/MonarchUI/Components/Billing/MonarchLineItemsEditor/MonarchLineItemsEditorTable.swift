import MonarchTokens
import SwiftUI

/// The items of a ``MonarchLineItemsEditor`` as the table of the web: a header row, then one row per item with the
/// description, quantity and unit price fields, the amount and a remove button. It scrolls sideways when the columns
/// are wider than the screen.
struct MonarchLineItemsEditorTable: View {
    let items: [MonarchLineItem]
    let lines: [Int]
    let currencyCode: String
    let labels: MonarchLineItemsEditorLabels
    let isReadOnly: Bool
    let canRemove: Bool
    let onChange: (MonarchLineItem) -> Void
    let onRemove: (String) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    private typealias Tokens = MonarchLineItemsEditorTokens
    private static let regularWidths: [CGFloat] = [150, 64, 96, 88, 44]
    /// Narrower columns, so that all five fit a phone column instead of scrolling.
    private static let compactWidths: [CGFloat] = [0, 72, 110, 0, 44]
    /// Which set of widths this table is drawn with.
    var isCompact = false
    private var widths: [CGFloat] { isCompact ? Self.compactWidths : Self.regularWidths }

    @ViewBuilder var body: some View {
        if !scrolls {
            framed
        } else {
            ViewThatFits(in: .horizontal) {
                MonarchLineItemsEditorTable(
                    items: items,
                    lines: lines,
                    currencyCode: currencyCode,
                    labels: labels,
                    isReadOnly: isReadOnly,
                    canRemove: canRemove,
                    onChange: onChange,
                    onRemove: onRemove,
                    scrolls: false
                )
                MonarchLineItemsEditorTable(
                    items: items,
                    lines: lines,
                    currencyCode: currencyCode,
                    labels: labels,
                    isReadOnly: isReadOnly,
                    canRemove: canRemove,
                    onChange: onChange,
                    onRemove: onRemove,
                    isCompact: true,
                    scrolls: false
                )
                framed
            }
        }
    }

    var scrolls = true

    @ViewBuilder private var framed: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
        Group {
            if scrolls {
                ScrollView(.horizontal, showsIndicators: false) { content }
            } else {
                content
            }
        }
        .background(shape.fill(theme.colors.bgSurface))
        .clipShape(shape)
        .overlay(shape.stroke(theme.colors.borderDefault, lineWidth: 1))
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 0) {
            if !isCompact { header }
            ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                if !isCompact || index > 0 { MonarchDivider() }
                row(item, index: index)
            }
        }
    }

    private var header: some View {
        HStack(spacing: isCompact ? 4 : 6) {
            heading(labels.description, width: widths[0], alignment: .leading)
            heading(labels.quantity, width: widths[1], alignment: .trailing)
            heading(labels.unitPrice(currencyCode), width: widths[2], alignment: .trailing)
            heading(labels.amount, width: widths[3], alignment: .trailing)
            Color.clear.frame(width: widths[4], height: 1)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 8)
        .background(theme.colors.bgSubtle)
        .accessibilityHidden(true)
    }

    private func heading(_ text: String, width: CGFloat, alignment: Alignment) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .lineLimit(1)
            .frame(width: width, alignment: alignment)
    }

    private func row(_ item: MonarchLineItem, index: Int) -> some View {
        let amount = lines.indices.contains(index) ? lines[index] : 0
        return Group {
            if isCompact {
                // A phone column: the description and the remove button on one line, the numbers under them, so no
                // field is cut short.
                VStack(alignment: .leading, spacing: 6) {
                    HStack(spacing: 6) {
                        descriptionView(item)
                        removeView(item, index: index)
                    }
                    HStack(spacing: 6) {
                        caption(labels.quantity) { quantityView(item) }
                        caption(labels.unitPrice(currencyCode)) { priceView(item) }
                        Spacer(minLength: 0)
                        MonarchMoney(minorUnits: amount, currencyCode: currencyCode, textStyle: .label)
                    }
                }
            } else {
                HStack(spacing: 6) {
                    descriptionView(item)
                    quantityView(item)
                    priceView(item)
                    MonarchMoney(minorUnits: amount, currencyCode: currencyCode, textStyle: .label)
                        .frame(width: widths[3], alignment: .trailing)
                    removeView(item, index: index)
                }
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .accessibilityElement(children: .contain)
    }

    private func caption<Content: View>(_ text: String, @ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(text)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            content()
        }
    }

    /// The width of a column, or `nil` to fill what is left when the row is stacked.
    private func width(_ column: Int) -> CGFloat? { isCompact ? (column == 0 ? nil : widths[column]) : widths[column] }

    @ViewBuilder private func descriptionView(_ item: MonarchLineItem) -> some View {
        if isReadOnly {
            Text(item.description)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
                .frame(width: width(0), alignment: .leading)
                .frame(maxWidth: isCompact ? .infinity : nil, alignment: .leading)
        } else {
            field(
                TextField(
                    labels.descriptionPlaceholder,
                    text: Binding(
                        get: { item.description },
                        set: { text in
                            var next = item
                            next.description = text
                            onChange(next)
                        }
                    )
                ),
                width: width(0),
                alignment: .leading
            )
        }
    }

    @ViewBuilder private func quantityView(_ item: MonarchLineItem) -> some View {
        if isReadOnly {
            Text((Tokens.double(item.quantity) ?? 0).formatted(.number.locale(locale)))
                .monarchTextStyle(.body)
                .monospacedDigit()
                .frame(width: width(1), alignment: .trailing)
        } else {
            field(
                TextField(
                    labels.quantity,
                    value: Binding(
                        get: { Tokens.double(item.quantity) },
                        set: { value in
                            var next = item
                            next.quantity = Tokens.decimal(value)
                            onChange(next)
                        }
                    ),
                    format: .number.locale(locale)
                ),
                width: width(1),
                alignment: .trailing
            )
        }
    }

    @ViewBuilder private func priceView(_ item: MonarchLineItem) -> some View {
        if isReadOnly {
            Text(
                (Tokens.double(item.unitPrice) ?? 0).formatted(.number.precision(.fractionLength(2)).locale(locale))
            )
            .monarchTextStyle(.body)
            .monospacedDigit()
            .frame(width: width(2), alignment: .trailing)
        } else {
            field(
                TextField(
                    labels.unitPrice(currencyCode),
                    value: Binding(
                        get: { Tokens.double(item.unitPrice) },
                        set: { value in
                            var next = item
                            next.unitPrice = Tokens.decimal(value)
                            onChange(next)
                        }
                    ),
                    format: .number.precision(.fractionLength(2)).locale(locale)
                ),
                width: width(2),
                alignment: .trailing
            )
        }
    }

    @ViewBuilder private func removeView(_ item: MonarchLineItem, index: Int) -> some View {
        if isReadOnly {
            Color.clear.frame(width: widths[4], height: 1)
        } else {
            Button {
                onRemove(item.id)
            } label: {
                MonarchIcon(systemName: Tokens.removeSymbol, size: .sm)
                    .frame(width: widths[4], height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .disabled(!canRemove)
            .accessibilityLabel(labels.removeItem(index + 1, item.description))
        }
    }

    private func field(_ field: some View, width: CGFloat?, alignment: Alignment) -> some View {
        field
            .textFieldStyle(.plain)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textDefault)
            .multilineTextAlignment(alignment == .leading ? .leading : .trailing)
            .monarchFieldBox(hasError: false, isFocused: false, minHeight: 36)
            .frame(width: width)
            .frame(maxWidth: width == nil ? .infinity : nil)
    }
}
