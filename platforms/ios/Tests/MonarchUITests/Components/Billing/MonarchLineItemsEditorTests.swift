import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchLineItemsEditorFixtures: ComponentFixtures {
    static let id = "line-items-editor"

    static let invoice = [
        MonarchLineItem(id: "r1", description: "Discovery workshop", quantity: 1, unitPrice: 800),
        MonarchLineItem(id: "r2", description: "Design hours", quantity: 12, unitPrice: 95),
        MonarchLineItem(id: "r3", description: "Hosting (monthly)", quantity: 3, unitPrice: Decimal(string: "24.50")),
    ]

    static let fixtures = [
        Fixture("editable, required, editable discount and preset tax rates") {
            List {
                MonarchLineItemsEditor(
                    items: .constant(invoice),
                    currencyCode: "USD",
                    taxRate: .constant(18),
                    taxRateOptions: [
                        MonarchLineItemsTaxRateOption(0, label: "No tax"),
                        MonarchLineItemsTaxRateOption(18, label: "18%"),
                    ],
                    discount: .constant(MonarchLineItemsDiscount(.percent, value: 5)),
                    isRequired: true,
                    isTaxRateEditable: true,
                    isDiscountEditable: true
                )
            }
            .frame(height: 1_200)
        },
        Fixture("typed tax rate, amount discount, minimum rows, empty fields") {
            List {
                MonarchLineItemsEditor(
                    items: .constant([
                        MonarchLineItem(id: "a", description: "", quantity: nil, unitPrice: nil),
                        MonarchLineItem(
                            id: "b",
                            description: "A very long description of a service that wraps onto several lines",
                            quantity: Decimal(string: "1.5"),
                            unitPrice: 1_250
                        ),
                    ]),
                    currencyCode: "EUR",
                    taxRate: .constant(19),
                    discount: .constant(MonarchLineItemsDiscount(.amount, value: 50)),
                    minimumItems: 2,
                    isTaxRateEditable: true,
                    isDiscountEditable: true
                )
            }
            .frame(height: 1_000)
        },
        Fixture("read-only sent invoice with fixed tax and discount") {
            List {
                MonarchLineItemsEditor(
                    items: .constant(invoice),
                    currencyCode: "INR",
                    taxRate: .constant(18),
                    discount: .constant(MonarchLineItemsDiscount(.percent, value: 5)),
                    isReadOnly: true
                )
            }
            .frame(height: 700)
        },
        Fixture("empty with an error, no totals, own labels") {
            List {
                MonarchLineItemsEditor(
                    items: .constant([]),
                    currencyCode: "JPY",
                    error: "Add at least one item.",
                    isRequired: true,
                    showsTotals: false,
                    labels: MonarchLineItemsEditorLabels(label: "Charges", empty: "Nothing to bill.")
                )
            }
            .frame(height: 300)
        },
        Fixture("disabled") {
            List {
                MonarchLineItemsEditor(items: .constant(invoice), currencyCode: "USD", taxRate: .constant(0))
            }
            .disabled(true)
            .frame(height: 900)
        },
    ]
}

@Suite("MonarchLineItemsEditor")
struct MonarchLineItemsEditorTests {
    private let english = Locale(identifier: "en_US")
    private typealias Tokens = MonarchLineItemsEditorTokens

    @Test("Totals are exact in minor units: rows, subtotal, discount, tax and total")
    func totals() {
        let totals = MonarchLineItemsTotals(
            MonarchLineItemsEditorFixtures.invoice,
            currencyCode: "USD",
            taxRate: 18,
            discount: MonarchLineItemsDiscount(.percent, value: 5)
        )
        #expect(totals.lines == [80_000, 114_000, 7_350])
        #expect(totals.subtotal == 201_350)
        #expect(totals.discount == 10_068)  // 5% of 2013.50 = 100.675, half away from zero
        #expect(totals.tax == 34_431)  // 18% of 1912.82 = 344.3076
        #expect(totals.total == 191_282 + 34_431)
    }

    @Test("Decimal arithmetic never loses a cent where floating point would")
    func decimals() {
        let item = MonarchLineItem(quantity: 3, unitPrice: Decimal(string: "0.1"))
        #expect(MonarchLineItemsTotals([item], currencyCode: "USD").subtotal == 30)
        let half = MonarchLineItem(quantity: 1, unitPrice: Decimal(string: "1.005"))
        #expect(MonarchLineItemsTotals([half], currencyCode: "USD").lines == [101])
        let negative = MonarchLineItem(quantity: -1, unitPrice: Decimal(string: "1.005"))
        #expect(MonarchLineItemsTotals([negative], currencyCode: "USD").lines == [-101])
        #expect(Tokens.rounded(Decimal(string: "2.5") ?? 0) == 3)
        #expect(Tokens.rounded(Decimal(string: "-2.5") ?? 0) == -3)
        #expect(Tokens.rounded(Decimal(string: "2.49") ?? 0) == 2)
    }

    @Test("The minor unit follows the currency: JPY none, KWD three")
    func currencies() {
        let item = MonarchLineItem(quantity: 2, unitPrice: Decimal(string: "1.2345"))
        #expect(MonarchLineItemsTotals([item], currencyCode: "JPY").lines == [2])
        #expect(MonarchLineItemsTotals([item], currencyCode: "KWD").lines == [2_469])
        #expect(Tokens.minorUnits(Decimal(string: "19.99") ?? 0, digits: 2) == 1_999)
        #expect(Tokens.majorUnits(1_999, currencyCode: "USD") == Decimal(string: "19.99"))
    }

    @Test("Empty quantities and prices count as zero")
    func emptyFields() {
        let totals = MonarchLineItemsTotals(
            [MonarchLineItem(quantity: nil, unitPrice: 5), MonarchLineItem(quantity: 2, unitPrice: nil)],
            currencyCode: "USD",
            taxRate: 10
        )
        #expect(totals.lines == [0, 0])
        #expect(totals.total == 0)
        #expect(MonarchLineItemsTotals([], currencyCode: "USD").total == 0)
    }

    @Test("A discount is capped at the subtotal and never negative; tax applies after it")
    func discounts() {
        let items = [MonarchLineItem(quantity: 1, unitPrice: 100)]
        let big = MonarchLineItemsTotals(items, currencyCode: "USD", taxRate: 10, discount: .init(.amount, value: 500))
        #expect(big.discount == 10_000)
        #expect(big.tax == 0)
        #expect(big.total == 0)
        let percent = MonarchLineItemsTotals(items, currencyCode: "USD", discount: .init(.percent, value: 250))
        #expect(percent.discount == 10_000)
        let none = MonarchLineItemsTotals(items, currencyCode: "USD", discount: .init(.amount, value: -5))
        #expect(none.discount == 0)
        let fixed = MonarchLineItemsTotals(items, currencyCode: "USD", taxRate: 10, discount: .init(.amount, value: 20))
        #expect(fixed.discount == 2_000)
        #expect(fixed.tax == 800)
        #expect(fixed.total == 8_800)
        let credit = MonarchLineItemsTotals(
            [MonarchLineItem(quantity: -1, unitPrice: 50)],
            currencyCode: "USD",
            discount: .init(.percent, value: 10)
        )
        #expect(credit.discount == 0)
        #expect(credit.total == -5_000)
    }

    @Test("Field numbers convert through their shortest text, so 19.99 stays exact")
    func conversions() {
        #expect(Tokens.decimal(19.99) == Decimal(string: "19.99"))
        #expect(Tokens.decimal(nil) == nil)
        #expect(Tokens.decimal(.nan) == nil)
        #expect(Tokens.double(Decimal(string: "24.5")) == 24.5)
        #expect(Tokens.double(nil) == nil)
        #expect(Tokens.percentage(fromFraction: 0.18) == 18)
        #expect(Tokens.percentage(fromFraction: 0.0725) == Decimal(string: "7.25"))
        #expect(Tokens.percentage(fromFraction: nil) == 0)
    }

    @Test("Rows are removed by id above the minimum and only while editable")
    func rows() {
        let items = MonarchLineItemsEditorFixtures.invoice
        let removed = Tokens.removing("r2", from: items)
        #expect(removed?.items.map(\.id) == ["r1", "r3"])
        #expect(removed?.row == 2)
        #expect(Tokens.removing("missing", from: items) == nil)
        #expect(Tokens.canRemove(count: 3, minimum: 0, isInteractive: true))
        #expect(!Tokens.canRemove(count: 1, minimum: 1, isInteractive: true))
        #expect(!Tokens.canRemove(count: 3, minimum: 0, isInteractive: false))
        #expect(!Tokens.canRemove(count: 0, minimum: -2, isInteractive: true))
        var changed = items[0]
        changed.description = "Workshop"
        #expect(
            Tokens.replacing(changed, in: items).map(\.description) == [
                "Workshop", "Design hours", "Hosting (monthly)",
            ]
        )
        #expect(Tokens.replacing(MonarchLineItem(id: "x"), in: items) == items)
    }

    @Test("A new row is empty, has one unit and a unique id")
    func newRow() {
        let first = MonarchLineItem()
        let second = MonarchLineItem()
        #expect(first.id != second.id)
        #expect(first.description.isEmpty)
        #expect(first.quantity == 1)
        #expect(first.unitPrice == nil)
    }

    @Test("Rate titles show the percentage only where the rate is text")
    func titles() {
        let labels = MonarchLineItemsEditorLabels()
        #expect(Tokens.taxTitle(18, isEditable: false, labels: labels, locale: english) == "Tax (18%)")
        #expect(
            Tokens.taxTitle(Decimal(string: "7.25"), isEditable: false, labels: labels, locale: english)
                == "Tax (7.25%)"
        )
        #expect(Tokens.taxTitle(18, isEditable: true, labels: labels, locale: english) == "Tax")
        #expect(Tokens.taxTitle(0, isEditable: false, labels: labels, locale: english) == "Tax")
        #expect(
            Tokens.discountTitle(.init(.percent, value: 5), isEditable: false, labels: labels, locale: english)
                == "Discount (5%)"
        )
        #expect(
            Tokens.discountTitle(.init(.amount, value: 5), isEditable: false, labels: labels, locale: english)
                == "Discount"
        )
        #expect(
            Tokens.discountTitle(.init(.percent, value: 5), isEditable: true, labels: labels, locale: english)
                == "Discount"
        )
        #expect(Tokens.signedDiscount(500) == -500)
        #expect(Tokens.signedDiscount(0) == 0)
    }

    @Test("VoiceOver reads each row's number with its amount, and the heading with Required")
    func accessibility() {
        let labels = MonarchLineItemsEditorLabels()
        #expect(
            Tokens.rowLabel(row: 2, amount: 114_000, currencyCode: "USD", labels: labels, locale: english)
                == "Item 2, Amount $1,140.00"
        )
        #expect(Tokens.headingLabel(isRequired: true, labels: labels) == "Line items, Required")
        #expect(Tokens.headingLabel(isRequired: false, labels: labels) == "Line items")
        #expect(Tokens.totalsRowLabel("Total", amount: 1_999, currencyCode: "USD", locale: english) == "Total, $19.99")
        #expect(labels.removeItem(2, "  Design hours ") == "Remove item 2: Design hours")
        #expect(labels.removeItem(3, " ") == "Remove item 3")
        #expect(
            Tokens.readOnlyDetail(
                MonarchLineItemsEditorFixtures.invoice[1],
                currencyCode: "USD",
                labels: labels,
                locale: english
            ) == "12 × $95.00"
        )
        #expect(Tokens.quantityText(Decimal(string: "1.5"), locale: Locale(identifier: "de_DE")) == "1,5")
    }

    @Test("Labels can be replaced")
    func ownLabels() {
        let labels = MonarchLineItemsEditorLabels(
            label: "Charges",
            item: { "Line \($0)" },
            unitPrice: { "Rate in \($0)" },
            tax: { $0.isEmpty ? "VAT" : "VAT \($0)" }
        )
        #expect(labels.label == "Charges")
        #expect(labels.item(4) == "Line 4")
        #expect(labels.unitPrice("EUR") == "Rate in EUR")
        #expect(Tokens.taxTitle(20, isEditable: false, labels: labels, locale: english) == "VAT 20%")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        typealias Strings = MonarchLineItemsEditorStrings
        #expect(Strings.label == "Line items")
        #expect(Strings.item(1) == "Item 1")
        #expect(Strings.description == "Description")
        #expect(Strings.quantity == "Qty")
        #expect(Strings.unitPrice("USD") == "Unit price (USD)")
        #expect(Strings.amount == "Amount")
        #expect(Strings.descriptionPlaceholder == "Item or service")
        #expect(Strings.addItem == "Add item")
        #expect(Strings.remove == "Remove")
        #expect(Strings.empty == "No items yet. Add one to start.")
        #expect(Strings.added(3) == "Item 3 added")
        #expect(Strings.removed(2) == "Item 2 removed")
        #expect(Strings.subtotal == "Subtotal")
        #expect(Strings.discount("") == "Discount")
        #expect(Strings.discountType == "Discount type")
        #expect(Strings.percentOption == "%")
        #expect(Strings.tax("") == "Tax")
        #expect(Strings.taxRate == "Tax rate")
        #expect(Strings.total == "Total")
        #expect(Strings.required == "Required")
        #expect(Strings.quantityTimesPrice("2", "$5.00") == "2 × $5.00")
    }
}
