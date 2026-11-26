import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchLineItemsEditor``. Money is
/// `Decimal` throughout; a `Double` exists only at the boundary of the
/// number fields, converted through its shortest decimal text.
enum MonarchLineItemsEditorTokens {
    /// The gap between the parts of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the quantity and the unit price fields.
    static let fieldSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the label and the value of a totals row.
    static let totalsSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the error icon and its text.
    static let errorIconSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The SF Symbol of the add button.
    static let addSymbol = "plus"
    /// The SF Symbol of the remove button and swipe action.
    static let removeSymbol = "trash"
    /// The SF Symbol of the error message.
    static let errorSymbol = "exclamationmark.circle.fill"
    /// The colour of the error message.
    static var errorRole: MonarchColorRole { \.dangerStrong }
    /// The colour of the required marker.
    static var requiredRole: MonarchColorRole { \.dangerStrong }
    /// The height of the preview's list, tall enough for three editors.
    static let previewHeight: CGFloat = MonarchTokens.Space.space20 * 12
    /// The visible required marker after the heading. Spoken as "Required".
    static let requiredMarker = "*"

    // MARK: Decimal arithmetic

    /// A decimal rounded to a whole number, half away from zero.
    static func rounded(_ value: Decimal) -> Int {
        // Rounded on the magnitude, so a half rounds away from zero for
        // negative amounts too.
        var plain = Decimal()
        var magnitude = value < 0 ? -value : value
        NSDecimalRound(&plain, &magnitude, 0, .plain)
        let whole = NSDecimalNumber(decimal: plain).intValue
        return value < 0 ? -whole : whole
    }

    /// An amount in major units as whole minor units: 19.99 with 2 digits is 1999.
    static func minorUnits(_ major: Decimal, digits: Int) -> Int {
        var input = major
        var scaled = Decimal()
        NSDecimalMultiplyByPowerOf10(&scaled, &input, Int16(digits), .plain)
        return rounded(scaled)
    }

    /// Whole minor units as an exact decimal amount in major units.
    static func majorUnits(_ minor: Int, currencyCode: String) -> Decimal {
        MonarchMoneyTokens.decimal(minorUnits: minor, currencyCode: currencyCode)
    }

    /// A field's number as a decimal, through its shortest text, so 19.99 stays 19.99.
    static func decimal(_ value: Double?) -> Decimal? {
        value.flatMap(MonarchNumberInputMath.decimal)
    }

    /// A decimal as the field's number.
    static func double(_ value: Decimal?) -> Double? {
        value.flatMap(MonarchNumberInputMath.double)
    }

    /// A percentage (18 for 18%) from a percent field's fraction (0.18). An empty field is 0.
    static func percentage(fromFraction value: Double?) -> Decimal {
        (decimal(value) ?? 0) * 100
    }

    // MARK: Rows

    /// Whether rows can be removed: only above the minimum, and only while editable.
    static func canRemove(count: Int, minimum: Int, isInteractive: Bool) -> Bool {
        isInteractive && count > max(0, minimum)
    }

    /// The rows after one is removed, and the number (from 1) it had, or
    /// `nil` when the id is not there.
    static func removing(_ id: String, from items: [MonarchLineItem]) -> (items: [MonarchLineItem], row: Int)? {
        guard let index = items.firstIndex(where: { $0.id == id }) else { return nil }
        var next = items
        next.remove(at: index)
        return (next, index + 1)
    }

    /// The rows after one is changed. The rows are unchanged when the id is not there.
    static func replacing(_ item: MonarchLineItem, in items: [MonarchLineItem]) -> [MonarchLineItem] {
        items.map { $0.id == item.id ? item : $0 }
    }

    // MARK: Text

    /// A percentage, given as 18 for 18%, in the locale: "18%", "7.25%".
    static func percentText(_ rate: Decimal, locale: Locale) -> String {
        (rate / 100).formatted(.percent.precision(.fractionLength(0...3)).locale(locale))
    }

    /// A quantity in the locale, or an empty text.
    static func quantityText(_ quantity: Decimal?, locale: Locale) -> String {
        quantity.map { $0.formatted(.number.locale(locale)) } ?? ""
    }

    /// An amount in minor units as the editor writes it.
    static func moneyText(_ minor: Int, currencyCode: String, locale: Locale) -> String {
        MonarchMoney.formatted(
            majorUnits(minor, currencyCode: currencyCode),
            currencyCode: currencyCode,
            locale: locale
        )
    }

    /// The title of the tax row: with its rate when the rate is shown as text.
    static func taxTitle(
        _ rate: Decimal?,
        isEditable: Bool,
        labels: MonarchLineItemsEditorLabels,
        locale: Locale
    ) -> String {
        guard !isEditable, let rate, rate != 0 else { return labels.tax("") }
        return labels.tax(percentText(rate, locale: locale))
    }

    /// The title of the discount row: with its percentage for a percentage
    /// discount shown as text.
    static func discountTitle(
        _ discount: MonarchLineItemsDiscount?,
        isEditable: Bool,
        labels: MonarchLineItemsEditorLabels,
        locale: Locale
    ) -> String {
        guard !isEditable, let discount, discount.kind == .percent, discount.value > 0 else {
            return labels.discount("")
        }
        return labels.discount(percentText(discount.value, locale: locale))
    }

    /// What VoiceOver reads for a row's heading: its number and its amount.
    static func rowLabel(
        row: Int,
        amount: Int,
        currencyCode: String,
        labels: MonarchLineItemsEditorLabels,
        locale: Locale
    ) -> String {
        "\(labels.item(row)), \(labels.amount) \(moneyText(amount, currencyCode: currencyCode, locale: locale))"
    }

    /// What VoiceOver reads for the heading: the label, and "Required" when it is.
    static func headingLabel(isRequired: Bool, labels: MonarchLineItemsEditorLabels) -> String {
        isRequired ? "\(labels.label), \(labels.required)" : labels.label
    }

    /// The detail of a read-only row: "12 × $95.00".
    static func readOnlyDetail(
        _ item: MonarchLineItem,
        currencyCode: String,
        labels: MonarchLineItemsEditorLabels,
        locale: Locale
    ) -> String {
        let price = MonarchMoney.formatted(item.unitPrice ?? 0, currencyCode: currencyCode, locale: locale)
        return labels.quantityTimesPrice(quantityText(item.quantity ?? 0, locale: locale), price)
    }

    /// What VoiceOver reads for a totals row: its title and amount.
    static func totalsRowLabel(_ title: String, amount: Int, currencyCode: String, locale: Locale) -> String {
        "\(title), \(moneyText(amount, currencyCode: currencyCode, locale: locale))"
    }

    /// The discount as shown: a minus sign before a discount that is not zero.
    static func signedDiscount(_ discount: Int) -> Int {
        discount == 0 ? 0 : -discount
    }
}
