import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchSavingsPanel``.
enum MonarchSavingsPanelTokens {
    /// The gap between the parts of the panel.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between rows, around the divider.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between lines of a row.
    static let lineSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between chips and facts on one line.
    static let inlineSpacing: CGFloat = MonarchTokens.Space.space2
    /// How many items are listed by default.
    static let maxItems = 6
    /// The separator between amounts in different currencies.
    static let amountSeparator = " + "
    /// The separator between decision counts.
    static let decisionSeparator = " · "
    /// The SF Symbol after a row or chip that opens something.
    static let disclosureSymbol = "chevron.forward"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "checkmark.seal"

    /// What the card shows: an error wins over loading, and no unused items is the empty state.
    static func cardState(isLoading: Bool, errorMessage: String?, unusedCount: Int) -> MonarchSectionCardState {
        if let errorMessage, !errorMessage.isEmpty { return .error }
        if isLoading { return .loading }
        return unusedCount <= 0 ? .empty : .ready
    }

    /// Amounts in several currencies, each in its own, joined with " + ". Never converted.
    static func join(_ amounts: [MonarchSavingsAmount], locale: Locale) -> String? {
        guard !amounts.isEmpty else { return nil }
        return
            amounts
            .map { MonarchMoney.formatted($0.amount, currencyCode: $0.currencyCode, locale: locale) }
            .joined(separator: amountSeparator)
    }

    /// The categories that have items.
    static func shownCategories(_ categories: [MonarchSavingsCategory]) -> [MonarchSavingsCategory] {
        categories.filter { $0.count > 0 }
    }

    /// The items listed and how many are left out.
    static func shownItems(_ items: [MonarchSavingsItem], max: Int) -> (shown: [MonarchSavingsItem], hidden: Int) {
        let shown = Array(items.prefix(Swift.max(0, max)))
        return (shown, items.count - shown.count)
    }

    /// The decision counts on one line: "Keep 3 · Review 2".
    static func decisionsLine(_ decisions: [MonarchSavingsDecisionCount], locale: Locale) -> String? {
        guard !decisions.isEmpty else { return nil }
        return
            decisions
            .map { "\($0.label) \($0.count.formatted(.number.locale(locale)))" }
            .joined(separator: decisionSeparator)
    }

    /// The medium date, in the calendar's time zone and the locale.
    static func date(_ date: Date, locale: Locale, calendar: Calendar) -> String {
        var style = Date.FormatStyle(date: .abbreviated, time: .omitted)
        style.locale = locale
        style.calendar = calendar
        style.timeZone = calendar.timeZone
        return date.formatted(style)
    }

    /// What VoiceOver reads after an item's name: category, date, amount and decision, as shown.
    static func itemValue(
        _ item: MonarchSavingsItem,
        category: String?,
        decision: String?,
        locale: Locale,
        calendar: Calendar,
        unpriced: String
    ) -> String {
        let amount: String? =
            if let money = item.amount {
                MonarchMoney.formatted(money.amount, currencyCode: money.currencyCode, locale: locale)
            } else {
                item.isUnpriced ? unpriced : nil
            }
        return [category, item.date.map { date($0, locale: locale, calendar: calendar) }, amount, decision]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }
}
