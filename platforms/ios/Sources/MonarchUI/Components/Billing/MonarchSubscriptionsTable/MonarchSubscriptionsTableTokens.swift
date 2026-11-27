import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchSubscriptionsTable``.
enum MonarchSubscriptionsTableTokens {
    /// The gap between rows.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between table columns.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between lines inside a row.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// The smallest gap between the name and the price, and before the chevron.
    static let gap: CGFloat = MonarchTokens.Space.space3
    /// The SF Symbol after a row that opens the subscription.
    static let disclosureSymbol = "chevron.forward"
    /// The SF Symbol of the empty state.
    static let emptySymbol = "arrow.triangle.2.circlepath"
    /// The statuses for which a missing monthly figure means "Not converted".
    static let liveStatuses: Set<String> = ["active", "past_due"]

    /// The built-in subscription statuses, their tones and their symbols.
    static let builtInStatuses: [(key: String, tone: MonarchTone, symbol: String)] = [
        ("draft", .neutral, "pencil.circle.fill"),
        ("trial", .info, "hourglass.circle.fill"),
        ("active", .success, "checkmark.circle.fill"),
        ("past_due", .warning, "exclamationmark.triangle.fill"),
        ("paused", .neutral, "pause.circle.fill"),
        ("cancelled", .danger, "xmark.circle.fill"),
        ("expired", .neutral, "minus.circle.fill"),
    ]

    /// The built-in statuses with their localised labels.
    static var statuses: MonarchStatusRegistry {
        MonarchStatusRegistry(
            Dictionary(
                uniqueKeysWithValues: builtInStatuses.map {
                    (
                        $0.key,
                        MonarchStatusDefinition(
                            MonarchSubscriptionsTableStrings.status($0.key),
                            tone: $0.tone,
                            systemImage: $0.symbol
                        )
                    )
                }
            )
        )
    }

    /// The statuses the table looks up first: the built-in subscription
    /// statuses, overridden by the app's registry.
    static func registry(_ app: MonarchStatusRegistry?) -> MonarchStatusRegistry {
        app.map { statuses.merging($0) } ?? statuses
    }

    /// Every built-in string, with the app's replacements applied.
    struct Texts {
        var caption: String
        var subscription: String
        var customer: String
        var status: String
        var price: String
        var monthly: String
        var renewal: String
        var customPrice: String
        var quantity: (Int) -> String
        var interval: (MonarchSubscriptionInterval?, Int) -> String
        var priceSuffix: (String) -> String
        var spokenPrice: (String, String) -> String
        var spokenOneTimePrice: (String) -> String
        var cancels: (String) -> String
        var changes: (String) -> String
        var renews: (String) -> String
        var ends: (String) -> String
        var evergreen: String
        var notConverted: String
        var notConvertedHint: String
        var noValue: String
        var emptyTitle: String
        var emptyMessage: String
    }

    /// The strings to show: each replacement, else the built-in string.
    static func texts(_ labels: MonarchSubscriptionsTableLabels) -> Texts {
        typealias Built = MonarchSubscriptionsTableStrings
        return Texts(
            caption: labels.caption ?? Built.caption,
            subscription: labels.subscription ?? Built.subscription,
            customer: labels.customer ?? Built.customer,
            status: labels.status ?? Built.status,
            price: labels.price ?? Built.price,
            monthly: labels.monthly ?? Built.monthly,
            renewal: labels.renewal ?? Built.renewal,
            customPrice: labels.customPrice ?? Built.customPrice,
            quantity: labels.quantity ?? Built.quantity,
            interval: labels.interval ?? intervalText,
            priceSuffix: labels.priceSuffix ?? Built.priceSuffix,
            spokenPrice: labels.spokenPrice ?? Built.spokenPrice,
            spokenOneTimePrice: labels.spokenOneTimePrice ?? Built.spokenOneTimePrice,
            cancels: labels.cancels ?? Built.cancels,
            changes: labels.changes ?? Built.changes,
            renews: labels.renews ?? Built.renews,
            ends: labels.ends ?? Built.ends,
            evergreen: labels.evergreen ?? Built.evergreen,
            notConverted: labels.notConverted ?? Built.notConverted,
            notConvertedHint: labels.notConvertedHint ?? Built.notConvertedHint,
            noValue: labels.noValue ?? Built.noValue,
            emptyTitle: labels.emptyTitle ?? Built.emptyTitle,
            emptyMessage: labels.emptyMessage ?? Built.emptyMessage
        )
    }

    /// The built-in billing period: "month", "quarter", "3 weeks", "one time".
    static func intervalText(_ interval: MonarchSubscriptionInterval?, count: Int) -> String {
        guard let interval else { return MonarchSubscriptionsTableStrings.oneTime }
        if interval == .month && count == 3 { return MonarchSubscriptionsTableStrings.quarter }
        return count == 1
            ? MonarchSubscriptionsTableStrings.interval(interval)
            : MonarchSubscriptionsTableStrings.intervals(interval, count: count)
    }

    /// A date as the table writes it, such as "Apr 12, 2026".
    static func dateText(_ date: Date, locale: Locale, timeZone: TimeZone) -> String {
        var style = Date.FormatStyle(date: .abbreviated, time: .omitted).locale(locale)
        style.timeZone = timeZone
        return date.formatted(style)
    }

    /// The line under the name: quantity, plan, then scheduled changes,
    /// such as "3 × Team plan · cancels Mar 4, 2026".
    static func meta(_ subscription: MonarchSubscription, texts: Texts, locale: Locale, timeZone: TimeZone) -> String {
        let plan = subscription.plan ?? texts.customPrice
        var parts = [subscription.quantity > 1 ? "\(texts.quantity(subscription.quantity)) \(plan)" : plan]
        if let cancelsAt = subscription.cancelsAt {
            parts.append(texts.cancels(dateText(cancelsAt, locale: locale, timeZone: timeZone)))
        }
        if let changesAt = subscription.changesAt {
            parts.append(texts.changes(dateText(changesAt, locale: locale, timeZone: timeZone)))
        }
        return parts.joined(separator: " · ")
    }

    /// The renewal: "Renews <date>", "Ends <date>" or "Evergreen".
    static func renewal(_ subscription: MonarchSubscription, texts: Texts, locale: Locale, timeZone: TimeZone) -> String
    {
        guard let date = subscription.renewalDate else { return texts.evergreen }
        let text = dateText(date, locale: locale, timeZone: timeZone)
        return subscription.renewsAutomatically ? texts.renews(text) : texts.ends(text)
    }

    /// The line under the meta at compact width: the customer, then the renewal.
    static func secondaryLine(_ customer: String?, renewal: String, showsCustomer: Bool) -> String {
        guard showsCustomer, let customer, !customer.isEmpty else { return renewal }
        return "\(customer) · \(renewal)"
    }

    /// The billing period of a subscription.
    static func period(_ subscription: MonarchSubscription, texts: Texts) -> String {
        texts.interval(subscription.interval, max(subscription.intervalCount, 1))
    }

    /// What the monthly column shows.
    enum Monthly: Equatable {
        case amount(Decimal, currencyCode: String)
        case notConverted
        case none
    }

    /// The monthly column: the amount, "Not converted" for a live
    /// subscription without one, or a dash.
    static func monthly(_ subscription: MonarchSubscription) -> Monthly {
        switch subscription.monthly {
        case .amount(let amount, let code)?: .amount(amount, currencyCode: code)
        case .notConverted? where liveStatuses.contains(subscription.status): .notConverted
        default: .none
        }
    }

    /// Whether the monthly column is shown: as asked, else when any row has a figure.
    static func showsMonthly(_ isShown: Bool?, subscriptions: [MonarchSubscription]) -> Bool {
        isShown ?? subscriptions.contains { $0.monthly != nil }
    }

    /// The spoken price: "$49.00 per month" or "$500.00, one time". A
    /// missing amount is read as Money reads it.
    static func spokenPrice(_ subscription: MonarchSubscription, texts: Texts, locale: Locale) -> String {
        let amount =
            subscription.amount.map {
                MonarchMoney.formatted($0, currencyCode: subscription.currencyCode, locale: locale)
            } ?? MonarchMoneyTokens.emptyLabel(nil)
        guard subscription.interval != nil else { return texts.spokenOneTimePrice(amount) }
        return texts.spokenPrice(amount, period(subscription, texts: texts))
    }

    /// What VoiceOver reads for a row: name, meta, customer, status, price,
    /// monthly figure and renewal, so no column depends on its position.
    static func rowLabel(
        _ subscription: MonarchSubscription,
        statusLabel: String,
        texts: Texts,
        locale: Locale,
        timeZone: TimeZone,
        showsCustomer: Bool,
        showsMonthly: Bool
    ) -> String {
        var parts = [subscription.name, meta(subscription, texts: texts, locale: locale, timeZone: timeZone)]
        if showsCustomer, let customer = subscription.customer, !customer.isEmpty {
            parts.append(customer)
        }
        parts.append(statusLabel)
        parts.append(spokenPrice(subscription, texts: texts, locale: locale))
        if showsMonthly {
            switch monthly(subscription) {
            case .amount(let amount, let code):
                parts.append("\(texts.monthly) \(MonarchMoney.formatted(amount, currencyCode: code, locale: locale))")
            case .notConverted:
                parts.append("\(texts.monthly) \(texts.notConverted)")
            case .none:
                break
            }
        }
        parts.append(renewal(subscription, texts: texts, locale: locale, timeZone: timeZone))
        return parts.joined(separator: ", ")
    }

    /// The hint of a row: the explanation of "Not converted" when the row
    /// shows it, else `nil`.
    static func rowHint(_ subscription: MonarchSubscription, texts: Texts, showsMonthly: Bool) -> String? {
        showsMonthly && monthly(subscription) == .notConverted ? texts.notConvertedHint : nil
    }

    /// Whether rows are stacked blocks instead of table rows: at compact
    /// width and at the accessibility text sizes.
    static func usesStackedRows(isRegularWidth: Bool, isAccessibilitySize: Bool) -> Bool {
        !isRegularWidth || isAccessibilitySize
    }

    /// The colour of the name: the link colour when the row opens the
    /// subscription, so it reads as a link in the table, else the default text colour.
    static func nameRole(isSelectable: Bool) -> MonarchColorRole {
        isSelectable ? \.textLink : \.textDefault
    }
}
