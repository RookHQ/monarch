import SwiftUI

/// The billable rows of an invoice, quote or proposal (description,
/// quantity and unit price) with each row's amount and an optional
/// discount, tax and total. Put it in a `List` or a `Form`.
///
/// ```swift
/// Form {
///     Section("Client") { … }
///     MonarchLineItemsEditor(
///         items: $items,
///         currencyCode: "USD",
///         taxRate: $taxRate,
///         taxRateOptions: [MonarchLineItemsTaxRateOption(0, label: "No tax"), MonarchLineItemsTaxRateOption(18, label: "18%")],
///         discount: $discount,
///         isTaxRateEditable: true,
///         isDiscountEditable: true
///     )
/// }
/// ```
///
/// The editor is two `Section`s, the rows and the totals, so it sits among
/// the app's own sections. Each row is a list row with the standard trailing
/// `swipeActions` to remove it, and a remove button for those who do not
/// swipe. The description is a ``MonarchTextField``, the quantity a
/// ``MonarchNumberInput`` and the unit price a ``MonarchCurrencyInput``;
/// amounts are ``MonarchMoney``. A typed number is committed when the person
/// submits or leaves the field.
///
/// Amounts are exact: each row is rounded to the currency's minor unit and
/// totals are summed as whole minor units in `Decimal`, so the total always
/// equals the rows shown (see ``MonarchLineItemsTotals``). The discount comes
/// off the subtotal and is capped at it; tax applies to what is left. The
/// editor never converts currencies.
///
/// Accessibility: the heading is a header, read with "Required" when the list
/// is required. Each row starts with a header read as "Item 2, Amount
/// $1,140.00"; its fields are named by their visible labels; the remove
/// button is read as "Remove item 2: Design hours"; the swipe action is
/// also in the VoiceOver actions. Adding and removing a row is announced, and
/// so is a new error. Totals rows are read as one element each. Every field
/// and button is at least 44 points high.
@_spi(Experimental) public struct MonarchLineItemsEditor: View {
    @Binding private var items: [MonarchLineItem]
    private let currencyCode: String
    private let taxRate: Binding<Decimal>?
    private let taxRateOptions: [MonarchLineItemsTaxRateOption]
    private let discount: Binding<MonarchLineItemsDiscount>?
    private let error: String?
    private let minimumItems: Int
    private let isRequired: Bool
    private let isReadOnly: Bool
    private let isTaxRateEditable: Bool
    private let isDiscountEditable: Bool
    private let showsTotals: Bool
    private let labels: MonarchLineItemsEditorLabels
    private let makeItem: @MainActor () -> MonarchLineItem

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    private typealias Tokens = MonarchLineItemsEditorTokens

    /// A line items editor.
    ///
    /// - Parameters:
    ///   - items: The rows. Every edit, addition and removal writes a new array.
    ///   - currencyCode: The ISO 4217 code of the document, such as `USD`. Sets the symbol and the minor unit.
    ///   - taxRate: The tax as a percentage of the discounted subtotal (18 for 18%). `nil` for no tax row.
    ///   - taxRateOptions: Preset rates offered in a picker when the tax rate is editable. Empty for a typed rate.
    ///   - discount: The discount on the subtotal. `nil` for no discount row.
    ///   - error: An error for the whole list, such as "Add at least one item", shown under the rows.
    ///   - minimumItems: Rows cannot be removed below this count.
    ///   - isRequired: Whether the list is required: a marker after the heading, spoken as "Required".
    ///   - isReadOnly: Whether the rows are text without fields or buttons, for a sent document.
    ///   - isTaxRateEditable: Whether the tax rate can be changed in the totals.
    ///   - isDiscountEditable: Whether the discount value and type can be changed in the totals.
    ///   - showsTotals: Whether the subtotal, discount, tax and total are shown.
    ///   - labels: Replacements for the built-in strings.
    ///   - makeItem: Builds the row added by "Add item". Default: an empty row with a new UUID.
    public init(
        items: Binding<[MonarchLineItem]>,
        currencyCode: String,
        taxRate: Binding<Decimal>? = nil,
        taxRateOptions: [MonarchLineItemsTaxRateOption] = [],
        discount: Binding<MonarchLineItemsDiscount>? = nil,
        error: String? = nil,
        minimumItems: Int = 0,
        isRequired: Bool = false,
        isReadOnly: Bool = false,
        isTaxRateEditable: Bool = false,
        isDiscountEditable: Bool = false,
        showsTotals: Bool = true,
        labels: MonarchLineItemsEditorLabels = MonarchLineItemsEditorLabels(),
        makeItem: (@MainActor () -> MonarchLineItem)? = nil
    ) {
        _items = items
        self.currencyCode = currencyCode
        self.taxRate = taxRate
        self.taxRateOptions = taxRateOptions
        self.discount = discount
        self.error = error
        self.minimumItems = minimumItems
        self.isRequired = isRequired
        self.isReadOnly = isReadOnly
        self.isTaxRateEditable = isTaxRateEditable
        self.isDiscountEditable = isDiscountEditable
        self.showsTotals = showsTotals
        self.labels = labels
        self.makeItem = makeItem ?? { MonarchLineItem() }
    }

    private var activeError: String? {
        guard let error, !error.isEmpty else { return nil }
        return error
    }

    /// The content of the view.
    public var body: some View {
        let totals = MonarchLineItemsTotals(
            items,
            currencyCode: currencyCode,
            taxRate: taxRate?.wrappedValue,
            discount: discount?.wrappedValue
        )
        let isInteractive = !isReadOnly && isEnabled
        let canRemove = Tokens.canRemove(count: items.count, minimum: minimumItems, isInteractive: isInteractive)
        Group {
            Section {
                if items.isEmpty {
                    Text(labels.empty)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if !items.isEmpty {
                    MonarchLineItemsEditorTable(
                        items: items,
                        lines: totals.lines,
                        currencyCode: currencyCode,
                        labels: labels,
                        isReadOnly: isReadOnly,
                        canRemove: canRemove,
                        onChange: { items = Tokens.replacing($0, in: items) },
                        onRemove: { remove($0) }
                    )
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                }
                if !isReadOnly {
                    MonarchButton(labels.addItem, systemImage: Tokens.addSymbol, variant: .secondary, size: .sm) {
                        add()
                    }
                }
                if let activeError {
                    errorView(activeError)
                }
            } header: {
                heading
            }
            if showsTotals {
                Section {
                    MonarchLineItemsEditorTotalsView(
                        totals: totals,
                        currencyCode: currencyCode,
                        taxRate: taxRate,
                        taxRateOptions: taxRateOptions,
                        discount: discount,
                        isTaxRateEditable: isTaxRateEditable && isInteractive,
                        isDiscountEditable: isDiscountEditable && isInteractive,
                        labels: labels
                    )
                }
            }
        }
        .onChange(of: activeError) { _, newError in
            if let newError {
                AccessibilityNotification.Announcement(newError).post()
            }
        }
    }

    private var heading: some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchLineItemsEditorTokens.errorIconSpacing) {
            Text(labels.label)
                .foregroundStyle(theme.colors.textDefault)
            if isRequired {
                Text(Tokens.requiredMarker)
                    .foregroundStyle(theme.colors[role: Tokens.requiredRole])
            }
        }
        .monarchTextStyle(.label)
        .textCase(nil)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.headingLabel(isRequired: isRequired, labels: labels))
        .accessibilityAddTraits(.isHeader)
    }

    private func errorView(_ message: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: Tokens.errorIconSpacing) {
            MonarchIcon(systemName: Tokens.errorSymbol, size: .sm)
            Text(message).monarchTextStyle(.caption)
        }
        .foregroundStyle(theme.colors[role: Tokens.errorRole])
        .accessibilityElement(children: .combine)
    }

    private func add() {
        items.append(makeItem())
        AccessibilityNotification.Announcement(labels.added(items.count)).post()
    }

    private func remove(_ id: String) {
        guard Tokens.canRemove(count: items.count, minimum: minimumItems, isInteractive: !isReadOnly && isEnabled),
            let result = Tokens.removing(id, from: items)
        else { return }
        items = result.items
        AccessibilityNotification.Announcement(labels.removed(result.row)).post()
    }
}

#if DEBUG
    #Preview("MonarchLineItemsEditor") {
        let items = [
            MonarchLineItem(id: "1", description: "Discovery workshop", quantity: 1, unitPrice: 800),
            MonarchLineItem(id: "2", description: "Design hours", quantity: 12, unitPrice: 95),
            MonarchLineItem(
                id: "3",
                description: "Hosting (monthly)",
                quantity: 3,
                unitPrice: Decimal(string: "24.50")
            ),
        ]
        MonarchPreviewMatrix {
            List {
                Group {
                    MonarchLineItemsEditor(
                        items: .constant(items),
                        currencyCode: "USD",
                        taxRate: .constant(18),
                        discount: .constant(MonarchLineItemsDiscount(.percent, value: 5)),
                        isRequired: true,
                        isDiscountEditable: true
                    )
                    MonarchLineItemsEditor(
                        items: .constant(items),
                        currencyCode: "EUR",
                        taxRate: .constant(19),
                        isReadOnly: true
                    )
                    MonarchLineItemsEditor(items: .constant([]), currencyCode: "INR", error: "Add at least one item.")

                }
                .monarchRowsOnCanvas()
            }
            .listStyle(.plain)
            .monarchCanvas()
            .frame(height: MonarchLineItemsEditorTokens.previewHeight)
        }
    }
#endif
